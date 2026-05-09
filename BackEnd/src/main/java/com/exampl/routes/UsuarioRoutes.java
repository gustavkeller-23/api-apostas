package com.exampl.routes;

import com.exampl.controller.UsuarioController;
import com.sun.net.httpserver.HttpServer;

public class UsuarioRoutes {

    public void registrar(HttpServer servidor){ 

        UsuarioController controller = new UsuarioController();

        servidor.createContext("/cadastro", exchange -> controller.handleCadastro(exchange));
        servidor.createContext("/login", exchange -> controller.handleLogin(exchange)); 
    
    }
}