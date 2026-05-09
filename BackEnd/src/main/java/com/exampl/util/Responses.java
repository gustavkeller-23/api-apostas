package com.exampl.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.exampl.config.Cors;
import com.sun.net.httpserver.HttpExchange;

public class Responses {
    
    public void responder(HttpExchange exchange, int status, String corpo) throws IOException {
        
        Cors cors = new Cors();
        cors.adicionarCorsHeaders(exchange); // 👈 GARANTE CORS EM TODAS RESPOSTAS
        
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
}
