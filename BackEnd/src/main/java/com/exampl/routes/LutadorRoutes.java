package com.exampl.routes;

import com.exampl.controller.LutadorController;
import com.sun.net.httpserver.HttpServer;

public class LutadorRoutes {

    public void registrar(HttpServer servidor){ 
        
        LutadorController controller = new LutadorController();

        servidor.createContext("/lutadores", exchange -> {
        
            String caminho = exchange.getRequestURI().getPath();

            if (caminho.matches("/lutadores/\\d+")) {
                
                controller.handleLutadorPorId(exchange);
            
            } else {
            
                controller.handleLutadores(exchange);
            
            }
        
        });
    }
}