package com.exampl;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    private static final int  PORTA       = 8080;
    private static LutadorDAO dao         = new LutadorDAO();
    private static UsuarioDAO usuarioDAO  = new UsuarioDAO();

    public static void main(String[] args) throws IOException {

        HttpServer servidor = HttpServer.create(new InetSocketAddress(PORTA), 0);

        servidor.createContext("/lutadores", exchange -> {
            String caminho = exchange.getRequestURI().getPath();

            if (caminho.matches("/lutadores/\\d+")) {
                handleLutadorPorId(exchange);
            } else {
                handleLutadores(exchange);
            }
        });

        servidor.createContext("/cadastro", exchange -> handleCadastro(exchange));
        servidor.createContext("/login", exchange -> handleLogin(exchange));

        servidor.setExecutor(null);
        servidor.start();

        System.out.println("✅ Servidor rodando em http://localhost:" + PORTA);
        System.out.println("   GET    /lutadores                                                         → lista todos");
        System.out.println("   POST   /lutadores?nome=X&apelido=X&categoria=X&arte=X                    → cria lutador");
        System.out.println("   GET    /lutadores/{id}                                                    → busca por ID");
        System.out.println("   PUT    /lutadores/{id}?nome=X&apelido=X&categoria=X&arte=X               → atualiza campos");
        System.out.println("   DELETE /lutadores/{id}                                                    → remove");
        System.out.println("   POST   /cadastro?login=X&senha=X                                         → cadastra usuário");
        System.out.println("   POST   /login                                                             → autentica usuário");
    }

    // ─────────────────────────────────────────────────────────
    // GET todos / POST criar
    // ─────────────────────────────────────────────────────────
    private static void handleLutadores(HttpExchange exchange) throws IOException {
        adicionarCorsHeaders(exchange);

        switch (exchange.getRequestMethod().toUpperCase()) {

            case "GET" -> {
                try {
                    List<Lutador> lista = dao.listarTodos();

                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < lista.size(); i++) {
                        sb.append(lista.get(i).toJson());
                        if (i < lista.size() - 1) sb.append(",");
                    }
                    sb.append("]");

                    responder(exchange, 200, sb.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    responder(exchange, 500, "{\"erro\": \"Erro ao acessar o banco de dados: \" + e.getMessage()}");
                }
            }

            // POST /lutadores?nome=X&apelido=X&categoria=X&arte=X
            case "POST" -> {
                Map<String, String> params = extrairQueryParams(exchange.getRequestURI());

                String nome      = params.get("nome");
                String apelido   = params.get("apelido");
                String categoria = params.get("categoria");
                String arte      = params.get("arte");

                Lutador novo = new Lutador(
                    nome      != null ? nome      : "",
                    categoria != null ? categoria : "",
                    apelido   != null ? apelido   : "",
                    arte      != null ? arte      : ""
                );

                try {
                    dao.inserir(novo);
                    responder(exchange, 201, novo.toJson());
                } catch (Exception e) {
                    e.printStackTrace();
                    responder(exchange, 500, "{\"erro\": \"Erro ao inserir no banco de dados\"}");
                }
            }

            case "OPTIONS" -> {
                adicionarCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
            }

            default -> responder(exchange, 405, "{\"erro\": \"Método não permitido\"}");
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET por ID / PUT atualizar / DELETE remover
    // ─────────────────────────────────────────────────────────
    private static void handleLutadorPorId(HttpExchange exchange) throws IOException {
        adicionarCorsHeaders(exchange);

        String caminho  = exchange.getRequestURI().getPath();
        String segmento = caminho.replace("/lutadores/", "").trim();

        int id;
        try {
            id = Integer.parseInt(segmento);
        } catch (NumberFormatException e) {
            responder(exchange, 400, "{\"erro\": \"ID deve ser um número inteiro\"}");
            return;
        }

        switch (exchange.getRequestMethod().toUpperCase()) {

            case "GET" -> {
                Lutador l = dao.buscarPorId(id);
                if (l == null) {
                    responder(exchange, 404, "{\"erro\": \"Lutador não encontrado\"}");
                } else {
                    responder(exchange, 200, l.toJson());
                }
            }

            // PUT /lutadores/{id}?nome=X&apelido=X&categoria=X&arte=X
            // Apenas os parâmetros enviados serão alterados
            case "PUT" -> {
                Lutador existente = dao.buscarPorId(id);
                if (existente == null) {
                    responder(exchange, 404, "{\"erro\": \"Lutador não encontrado\"}");
                    return;
                }

                Map<String, String> params = extrairQueryParams(exchange.getRequestURI());

                if (params.isEmpty()) {
                    responder(exchange, 400, "{\"erro\": \"Informe ao menos um campo para atualizar na URL\"}");
                    return;
                }

                // Atualiza apenas os campos presentes na URL
                if (params.containsKey("nome"))      existente.setNome(params.get("nome"));
                if (params.containsKey("apelido"))   existente.setApelido(params.get("apelido"));
                if (params.containsKey("categoria")) existente.setCategoria(params.get("categoria"));
                if (params.containsKey("arte"))      existente.setArte(params.get("arte"));

                dao.atualizar(id, existente);
                responder(exchange, 200, existente.toJson());
            }

            case "DELETE" -> {
                boolean ok = dao.deletar(id);
                if (ok) {
                    responder(exchange, 200, "{\"mensagem\": \"Lutador removido com sucesso\"}");
                } else {
                    responder(exchange, 404, "{\"erro\": \"Lutador não encontrado\"}");
                }
            }

            case "OPTIONS" -> {
                adicionarCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1); // sem corpo
            }

            default -> responder(exchange, 405, "{\"erro\": \"Método não permitido\"}");
        }
    }

    // ─────────────────────────────────────────────────────────
    // POST /cadastro
    // ─────────────────────────────────────────────────────────
    private static void handleCadastro(HttpExchange exchange) throws IOException {
        adicionarCorsHeaders(exchange);

        switch (exchange.getRequestMethod().toUpperCase()) {

            case "POST" -> {
                String bodyJson;
                try (InputStream is = exchange.getRequestBody()) {
                    bodyJson = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                }

                String login = extrairCampoJson(bodyJson, "usuario");
                String senha = extrairCampoJson(bodyJson, "senha");

                if (login == null || login.isBlank() ||
                    senha == null || senha.isBlank()) {
                    responder(exchange, 400, "{\"erro\": \"Campos obrigatórios: usuario, senha\"}");
                    return;
                }

                try {
                    Usuario novo = new Usuario(login, senha);
                    usuarioDAO.inserir(novo);
                    responder(exchange, 201, novo.toJson());
                } catch (Exception e) {
                    String msg = e.getMessage() != null && e.getMessage().contains("duplicate key")
                        ? "{\"erro\": \"Usuário já cadastrado\"}"
                        : "{\"erro\": \"Erro interno ao cadastrar usuário\"}";
                    responder(exchange, 409, msg);
                }
            }

            case "OPTIONS" -> {
                adicionarCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
            }

            default -> responder(exchange, 405, "{\"erro\": \"Método não permitido\"}");
        }
    }

    // ─────────────────────────────────────────────────────────
    // POST /login
    // ─────────────────────────────────────────────────────────
    private static void handleLogin(HttpExchange exchange) throws IOException {
        adicionarCorsHeaders(exchange);

        switch (exchange.getRequestMethod().toUpperCase()) {

            case "POST" -> {
                String bodyJson;
                try (InputStream is = exchange.getRequestBody()) {
                    bodyJson = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                }

                String login = extrairCampoJson(bodyJson, "usuario");
                String senha = extrairCampoJson(bodyJson, "senha");

                if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
                    responder(exchange, 400, "{\"erro\": \"Campos obrigatórios: usuario, senha\"}");
                    return;
                }

                Usuario u = usuarioDAO.autenticar(login, senha);
                if (u != null) {
                    responder(exchange, 200, u.toJson());
                } else {
                    responder(exchange, 401, "{\"erro\": \"Usuário ou senha incorretos\"}");
                }
            }

            case "OPTIONS" -> {
                adicionarCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
            }

            default -> responder(exchange, 405, "{\"erro\": \"Método não permitido\"}");
        }
    }

    // ─────────────────────────────────────────────────────────
    // Utilitários
    // ─────────────────────────────────────────────────────────

    /**
     * Extrai os query params da URI.
     * Ex: /lutadores/1?nome=Tyson&categoria=1  →  {nome=Tyson, categoria=1}
     */
    private static Map<String, String> extrairQueryParams(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getQuery(); // retorna null se não houver query string

        if (query == null || query.isBlank()) return params;

        for (String par : query.split("&")) {
            String[] partes = par.split("=", 2);
            if (partes.length == 2) {
                params.put(partes[0].trim(), partes[1].trim());
            } else if (partes.length == 1) {
                params.put(partes[0].trim(), "");
            }
        }
        return params;
    }

    /** Converte String para Integer, retorna null se inválido */
    private static Integer parseIntOuNull(String valor) {
        if (valor == null) return null;
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Extrai campo simples de JSON: {"campo":"valor"} */
    private static String extrairCampoJson(String json, String campo) {
        if (json == null || campo == null) return null;
        String pattern = "\"" + campo + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static void responder(HttpExchange exchange, int status, String corpo) throws IOException {
        adicionarCorsHeaders(exchange); // 👈 GARANTE CORS EM TODAS RESPOSTAS
        try {
            // Criptografamos o corpo da resposta antes de enviar
            String corpoCriptografado = SecurityUtils.encrypt(corpo);
            byte[] bytes = corpoCriptografado.getBytes(StandardCharsets.UTF_8);

            // Adicionamos um Header para avisar o cliente que o dado está criptografado
            exchange.getResponseHeaders().set("X-Content-Encrypted", "true");
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");

            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String erro = "Erro ao criptografar dados";
            exchange.sendResponseHeaders(500, erro.length());
            exchange.getResponseBody().write(erro.getBytes());
        }
    }

    private static void adicionarCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Content-Encrypted");
        exchange.getResponseHeaders().set("Access-Control-Expose-Headers", "X-Content-Encrypted"); // permite JS ler o header
    }
}