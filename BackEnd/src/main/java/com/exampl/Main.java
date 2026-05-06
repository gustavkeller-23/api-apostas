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

public class Main {

    private static final int  PORTA = 8080;
    private static LutadorDAO dao   = new LutadorDAO();

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

        servidor.setExecutor(null);
        servidor.start();

        System.out.println("✅ Servidor rodando em http://localhost:" + PORTA);
        System.out.println("   GET    /lutadores                                             → lista todos");
        System.out.println("   POST   /lutadores?nome=X&apelido=X&categoria=X&arte=X        → cria lutador");
        System.out.println("   GET    /lutadores/{id}                                        → busca por ID");
        System.out.println("   PUT    /lutadores/{id}?nome=X&apelido=X&categoria=X&arte=X   → atualiza campos");
        System.out.println("   DELETE /lutadores/{id}                                        → remove");
    }

    // ─────────────────────────────────────────────────────────
    // GET todos / POST criar
    // ─────────────────────────────────────────────────────────
    private static void handleLutadores(HttpExchange exchange) throws IOException {
        adicionarCorsHeaders(exchange);

        switch (exchange.getRequestMethod().toUpperCase()) {

            case "GET" -> {
                List<Lutador> lista = dao.listarTodos();

                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < lista.size(); i++) {
                    sb.append(lista.get(i).toJson());
                    if (i < lista.size() - 1) sb.append(",");
                }
                sb.append("]");

                responder(exchange, 200, sb.toString());
            }

            // POST /lutadores?nome=X&apelido=X&categoria=X&arte=X
            case "POST" -> {
                Map<String, String> params = extrairQueryParams(exchange.getRequestURI());

                String  nome      = params.get("nome");
                String  apelido   = params.get("apelido");
                String categoria = params.get("categoria");
                String arte      = params.get("arte");

                Lutador novo = new Lutador(
                    nome      != null ? nome      : "",
                    categoria != null ? categoria : "",
                    apelido   != null ? apelido   : "",
                    arte      != null ? arte      : ""
                );

                dao.inserir(novo);
                responder(exchange, 201, novo.toJson());
            }

            case "OPTIONS" -> responder(exchange, 204, "");

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

            case "OPTIONS" -> responder(exchange, 204, "");

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

    private static void responder(HttpExchange exchange, int status, String corpo) throws IOException {
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
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}