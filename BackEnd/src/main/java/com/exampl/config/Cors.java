package com.exampl.config;

import com.sun.net.httpserver.HttpExchange;

public class Cors {
    
    public void adicionarCorsHeaders(HttpExchange exchange) {
    
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Content-Encrypted");
        exchange.getResponseHeaders().set("Access-Control-Expose-Headers", "X-Content-Encrypted"); // permite JS ler o header
    
    }

}
