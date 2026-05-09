package com.exampl.controller;

import java.io.IOException;

import com.exampl.config.Cors;
import com.exampl.services.UsuarioService;
import com.exampl.util.Responses;
import com.sun.net.httpserver.HttpExchange;

public class UsuarioController {
    
    UsuarioService service = new UsuarioService();
    Cors cors = new Cors();
    Responses util = new Responses();

    public void handleCadastro(HttpExchange exchange) throws IOException {
        cors.adicionarCorsHeaders(exchange);

        switch (exchange.getRequestMethod().toUpperCase()) {

            case "POST" -> { service.cadastrar(exchange); }

            case "OPTIONS" -> exchange.sendResponseHeaders(204, -1);    // CORS preflight — headers já adicionados no início do método

            default -> util.responder(exchange, 405, "{\"erro\": \"Método não permitido\"}");
        }
    }

    public void handleLogin(HttpExchange exchange) throws IOException {
        cors.adicionarCorsHeaders(exchange);

        switch (exchange.getRequestMethod().toUpperCase()) {

            case "POST" -> { service.autenticar(exchange); }

            case "OPTIONS" -> exchange.sendResponseHeaders(204, -1);   // CORS preflight — headers já adicionados no início do método

            default -> util.responder(exchange, 405, "{\"erro\": \"Método não permitido\"}");
        }
    }

}
