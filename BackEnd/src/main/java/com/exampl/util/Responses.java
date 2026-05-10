package com.exampl.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.exampl.config.Cors;
import com.sun.net.httpserver.HttpExchange;

public class Responses {

    public void responder(HttpExchange exchange, int status, String corpo) throws IOException {

        Cors cors = new Cors();
        cors.adicionarCorsHeaders(exchange);

        try {
            byte[] bytes;

            if (SecurityUtils.hasClientPublicKey()) {
                // Criptografa o corpo com a chave pública RSA do cliente
                String corpoCriptografado = SecurityUtils.encryptForClient(corpo);
                bytes = corpoCriptografado.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("X-Content-Encrypted", "true");
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            } else {
                // Handshake ainda não foi feito: retorna JSON sem criptografia
                bytes = corpo.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("X-Content-Encrypted", "false");
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            }

            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }

        } catch (Exception e) {
            e.printStackTrace();
            String erro = "{\"erro\": \"Erro ao processar resposta\"}";
            byte[] erroBytes = erro.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, erroBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(erroBytes);
            }
        }
    }
}
