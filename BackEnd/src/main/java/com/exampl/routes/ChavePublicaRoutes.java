package com.exampl.routes;

import com.exampl.controller.ChavePublicaController;
import com.sun.net.httpserver.HttpServer;

public class ChavePublicaRoutes {

    public void registrar(HttpServer servidor) {
        ChavePublicaController controller = new ChavePublicaController();
        servidor.createContext("/chave-publica", exchange -> controller.handleChavePublica(exchange));
    }
}
