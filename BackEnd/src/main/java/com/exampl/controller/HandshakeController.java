package com.exampl.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.exampl.config.Cors;
import com.exampl.util.SecurityUtils;
import com.sun.net.httpserver.HttpExchange;

public class HandshakeController {

    Cors cors = new Cors();

    /**
     * POST /handshake
     * Body: {"publicKey": "<Base64 SPKI RSA-2048>"}
     *
     * Recebe a chave pública RSA do cliente e a armazena no servidor.
     * A partir daí, todas as respostas serão criptografadas com essa chave.
     */
    public void handleHandshake(HttpExchange exchange) throws IOException {
        cors.adicionarCorsHeaders(exchange);

        String method = exchange.getRequestMethod().toUpperCase();

        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!method.equals("POST")) {
            String erro = "{\"erro\": \"Método não permitido\"}";
            byte[] bytes = erro.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(405, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            return;
        }

        try {
            // Lê o body JSON
            String bodyJson;
            try (InputStream is = exchange.getRequestBody()) {
                bodyJson = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            }

            // Extrai o campo "publicKey" manualmente (sem dependência Jackson)
            String publicKeyBase64 = extrairCampoJson(bodyJson, "publicKey");

            if (publicKeyBase64 == null || publicKeyBase64.isBlank()) {
                String erro = "{\"erro\": \"Campo 'publicKey' é obrigatório\"}";
                byte[] bytes = erro.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(400, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                return;
            }

            // Armazena a chave pública do cliente
            SecurityUtils.setClientPublicKey(publicKeyBase64);

            String ok = "{\"status\": \"ok\", \"mensagem\": \"Chave pública registrada com sucesso\"}";
            byte[] bytes = ok.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }

        } catch (Exception e) {
            e.printStackTrace();
            String erro = "{\"erro\": \"Chave pública inválida: " + e.getMessage().replace("\"", "'") + "\"}";
            byte[] bytes = erro.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(400, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    /** Extrai campo simples de JSON: {"campo":"valor"} */
    private String extrairCampoJson(String json, String campo) {
        if (json == null || campo == null) return null;
        String pattern = "\"" + campo + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
