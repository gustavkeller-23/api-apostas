package com.exampl.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.exampl.model.Lutador;
import com.exampl.repository.LutadorDAO;
import com.exampl.util.Responses;
import com.exampl.util.Utils;
import com.sun.net.httpserver.HttpExchange;

public class LutadorService {

    LutadorDAO dao = new LutadorDAO();
    Utils utils = new Utils();
    Responses response = new Responses();

    public void listarTodos(HttpExchange exchange) throws IOException{
        
        try {
            List<Lutador> lista = dao.listarTodos();

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < lista.size(); i++) {
                sb.append(lista.get(i).toJson());
                if (i < lista.size() - 1) sb.append(",");
            }
            sb.append("]");

            response.responder(exchange, 200, sb.toString());
        } catch (Exception e) {                
            e.printStackTrace();
            response.responder(exchange, 500, "{\"erro\": \"Erro ao acessar o banco de dados: \" + e.getMessage()}");
        }
    }

    public void listarPorId(HttpExchange exchange, Integer id) throws IOException{
        Lutador l = dao.buscarPorId(id);
        if (l == null) {
            response.responder(exchange, 404, "{\"erro\": \"Lutador não encontrado\"}");
        } else {
            response.responder(exchange, 200, l.toJson());
        }
    }

    public void salvar(HttpExchange exchange) throws IOException{
        Map<String, String> params = utils.extrairQueryParams(exchange.getRequestURI());

        String nome      = params.get("nome");
        String apelido   = params.get("apelido");
        String categoria = params.get("categoria");
        String arte      = params.get("arte");

        Lutador novo = new Lutador(
            nome      != null ? nome      : "",
            categoria != null ? categoria : "",
            apelido   != null ? apelido   : "",
            arte      != null ? arte      : ""
        );

        try {
            dao.inserir(novo);
            response.responder(exchange, 201, novo.toJson());
        } catch (Exception e) {
            e.printStackTrace();
            response.responder(exchange, 500, "{\"erro\": \"Erro ao inserir no banco de dados\"}");            
        }
    }

    public void atualizar(HttpExchange exchange, Integer id) throws IOException{             // Apenas os parâmetros enviados serão alterados
        Lutador existente = dao.buscarPorId(id);
        if (existente == null) {
            response.responder(exchange, 404, "{\"erro\": \"Lutador não encontrado\"}");
            return;
        }

        Map<String, String> params = utils.extrairQueryParams(exchange.getRequestURI());

        if (params.isEmpty()) {
            response.responder(exchange, 400, "{\"erro\": \"Informe ao menos um campo para atualizar na URL\"}");
            return;
        }

        // Atualiza apenas os campos presentes na URL
        if (params.containsKey("nome"))      existente.setNome(params.get("nome"));
        if (params.containsKey("apelido"))   existente.setApelido(params.get("apelido"));
        if (params.containsKey("categoria")) existente.setCategoria(params.get("categoria"));
        if (params.containsKey("arte"))      existente.setArte(params.get("arte"));

        dao.atualizar(id, existente);
        response.responder(exchange, 200, existente.toJson());
    }

    public void deletar(HttpExchange exchange, Integer id) throws IOException{
        boolean ok = dao.deletar(id);
        if (ok) {
            response.responder(exchange, 200, "{\"mensagem\": \"Lutador removido com sucesso\"}");
        } else {
            response.responder(exchange, 404, "{\"erro\": \"Lutador não encontrado\"}");
        }
    }
}
