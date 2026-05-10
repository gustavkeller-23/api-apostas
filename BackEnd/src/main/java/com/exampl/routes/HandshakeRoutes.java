package com.exampl.routes;

import com.exampl.controller.HandshakeController;
import com.sun.net.httpserver.HttpServer;

public class HandshakeRoutes {

    public void registrar(HttpServer servidor) {
        HandshakeController controller = new HandshakeController();
        servidor.createContext("/handshake", exchange -> controller.handleHandshake(exchange));
    }
}
