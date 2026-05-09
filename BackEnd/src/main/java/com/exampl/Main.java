package com.exampl;

import com.sun.net.httpserver.HttpServer;
import com.exampl.routes.LutadorRoutes;
import com.exampl.routes.UsuarioRoutes;

import java.io.*;
import java.net.InetSocketAddress;

public class Main {

    private static final int  PORTA       = 8080;
    private static LutadorRoutes lutadorRoute  = new LutadorRoutes();
    private static UsuarioRoutes usuarioRoute  = new UsuarioRoutes();

    public static void main(String[] args) throws IOException {

        HttpServer servidor = HttpServer.create(new InetSocketAddress(PORTA), 0);

        lutadorRoute.registrar(servidor);
        usuarioRoute.registrar(servidor);

        servidor.setExecutor(null);
        servidor.start();

        System.out.println("✅ Servidor rodando em http://localhost:" + PORTA);
        System.out.println("   GET    /lutadores                                                         → lista todos");
        System.out.println("   POST   /lutadores?nome=X&apelido=X&categoria=X&arte=X                    → cria lutador");
        System.out.println("   GET    /lutadores/{id}                                                    → busca por ID");
        System.out.println("   PUT    /lutadores/{id}?nome=X&apelido=X&categoria=X&arte=X               → atualiza campos");
        System.out.println("   DELETE /lutadores/{id}                                                    → remove");
        System.out.println("   POST   /cadastro?login=X&senha=X                                         → cadastra usuário");
        System.out.println("   POST   /login                                                             → autentica usuário");
    }
}