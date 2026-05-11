package com.exampl.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.exampl.config.Cors;
import com.exampl.util.SecurityUtils;
import com.sun.net.httpserver.HttpExchange;

public class ChavePublicaController {

    Cors cors = new Cors();

    /**
     * GET /chave-publica
     *
     * Retorna a chave pública RSA do SERVIDOR em Base64 (formato SPKI).
     * O browser importa essa chave e a usa para criptografar o login
     * antes de enviá-lo — assim as credenciais nunca trafegam em texto puro.
     */
    public void handleChavePublica(HttpExchange exchange) throws IOException {
        cors.adicionarCorsHeaders(exchange);

        String method = exchange.getRequestMethod().toUpperCase();

        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!method.equals("GET")) {
            String erro = "{\"erro\": \"Método não permitido\"}";
            byte[] bytes = erro.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(405, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            return;
        }

        String publicKeyBase64 = SecurityUtils.getServerPublicKeyBase64();
        String corpo = "{\"publicKey\": \"" + publicKeyBase64 + "\"}";
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
