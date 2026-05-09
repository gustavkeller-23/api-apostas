package com.exampl.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.exampl.model.Usuario;
import com.exampl.repository.UsuarioDAO;
import com.exampl.util.Responses;
import com.exampl.util.Utils;
import com.sun.net.httpserver.HttpExchange;

public class UsuarioService {

    Utils utils = new Utils();
    Responses response = new Responses();
    UsuarioDAO dao = new UsuarioDAO();

    public void cadastrar(HttpExchange exchange) throws IOException{
        String bodyJson;
        try (InputStream is = exchange.getRequestBody()) {
            bodyJson = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }

        String login = utils.extrairCampoJson(bodyJson, "usuario");
        String senha = utils.extrairCampoJson(bodyJson, "senha");

        if (login == null || login.isBlank() ||
            senha == null || senha.isBlank()) {
            response.responder(exchange, 400, "{\"erro\": \"Campos obrigatórios: usuario, senha\"}");
            return;
        }

        try {
            Usuario novo = new Usuario(login, senha);
            dao.inserir(novo);
            response.responder(exchange, 201, novo.toJson());
        } catch (Exception e) {
            e.printStackTrace(); // aparece no console do IntelliJ
            boolean duplicado = e.getMessage() != null && e.getMessage().contains("duplicate key");
            if (duplicado) {
                response.responder(exchange, 409, "{\"erro\": \"Usuário já cadastrado\"}");
            } else {
                response.responder(exchange, 500, "{\"erro\": \"Erro interno ao cadastrar usuário\"}");
            }
        }
    }

    public void autenticar(HttpExchange exchange) throws IOException{
        String bodyJson;
        try (InputStream is = exchange.getRequestBody()) {
            bodyJson = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }

        String login = utils.extrairCampoJson(bodyJson, "usuario");
        String senha = utils.extrairCampoJson(bodyJson, "senha");

        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            response.responder(exchange, 400, "{\"erro\": \"Campos obrigatórios: usuario, senha\"}");
            return;
        }

        try {
            Usuario u = dao.autenticar(login, senha);
            if (u != null) {
                response.responder(exchange, 200, u.toJson());
            } else {
                response.responder(exchange, 401, "{\"erro\": \"Usuário ou senha incorretos\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.responder(exchange, 500, "{\"erro\": \"Erro ao autenticar\"}" );
        }
    }
}
