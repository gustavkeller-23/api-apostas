package com.exampl;

import com.sun.net.httpserver.HttpServer;
import com.exampl.routes.LutadorRoutes;
import com.exampl.routes.UsuarioRoutes;
import com.exampl.routes.HandshakeRoutes;
import com.exampl.routes.ChavePublicaRoutes;

import java.io.*;
import java.net.InetSocketAddress;

public class Main {

    private static final int  PORTA          = 8080;
    private static LutadorRoutes     lutadorRoute    = new LutadorRoutes();
    private static UsuarioRoutes     usuarioRoute    = new UsuarioRoutes();
    private static HandshakeRoutes   handshakeRoute  = new HandshakeRoutes();
    private static ChavePublicaRoutes chaveRoute     = new ChavePublicaRoutes();

    public static void main(String[] args) throws IOException {

        HttpServer servidor = HttpServer.create(new InetSocketAddress(PORTA), 0);

        chaveRoute.registrar(servidor);     // GET  /chave-publica
        handshakeRoute.registrar(servidor);  // POST /handshake
        lutadorRoute.registrar(servidor);
        usuarioRoute.registrar(servidor);

        servidor.setExecutor(null);
        servidor.start();

        System.out.println("✅ Servidor rodando em http://localhost:" + PORTA);
        System.out.println("   GET    /chave-publica                                                        → chave pública RSA do servidor");
        System.out.println("   POST   /handshake                                                            → registra chave pública RSA do cliente");
        System.out.println("   GET    /lutadores                                                            → lista todos (resposta criptografada)");
        System.out.println("   POST   /lutadores?nome=X&apelido=X&categoria=X&arte=X                       → cria lutador");
        System.out.println("   GET    /lutadores/{id}                                                       → busca por ID");
        System.out.println("   PUT    /lutadores/{id}?nome=X&apelido=X&categoria=X&arte=X                  → atualiza campos");
        System.out.println("   DELETE /lutadores/{id}                                                       → remove");
        System.out.println("   POST   /cadastro   (body criptografado)                                     → cadastra usuário");
        System.out.println("   POST   /login      (body criptografado)                                     → autentica usuário");
    }
}