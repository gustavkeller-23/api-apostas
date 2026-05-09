package com.exampl.controller;

import java.io.IOException;

import com.exampl.config.Cors;
import com.exampl.services.LutadorService;
import com.exampl.util.Responses;
import com.sun.net.httpserver.HttpExchange;

public class LutadorController {

    LutadorService service = new LutadorService();
    Cors cors = new Cors();
    Responses util = new Responses();
 
    public void handleLutadores(HttpExchange exchange) throws IOException {
        cors.adicionarCorsHeaders(exchange);

        switch (exchange.getRequestMethod().toUpperCase()) {

            case "GET" -> { service.listarTodos(exchange); }

            case "POST" -> { service.salvar(exchange); }

            case "OPTIONS" -> exchange.sendResponseHeaders(204, -1); // CORS preflight — headers já adicionados no início do método

            default -> util.responder(exchange, 405, "{\"erro\": \"Método não permitido\"}");
        }
    }

    public void handleLutadorPorId(HttpExchange exchange) throws IOException {
        cors.adicionarCorsHeaders(exchange);

        String caminho  = exchange.getRequestURI().getPath();
        String segmento = caminho.replace("/lutadores/", "").trim();

        int id;
        try {
            id = Integer.parseInt(segmento);
        } catch (NumberFormatException e) {
            util.responder(exchange, 400, "{\"erro\": \"ID deve ser um número inteiro\"}");
            return;
        }

        switch (exchange.getRequestMethod().toUpperCase()) {

            case "GET" -> { service.listarPorId(exchange, id); }

            case "PUT" -> { service.atualizar(exchange, id); }  // PUT /lutadores/{id}?nome=X&apelido=X&categoria=X&arte=X

            case "DELETE" -> { service.deletar(exchange, id); }

            case "OPTIONS" -> exchange.sendResponseHeaders(204, -1);   // CORS preflight — headers já adicionados no início do método

            default -> util.responder(exchange, 405, "{\"erro\": \"Método não permitido\"}");
        }
    }
}
