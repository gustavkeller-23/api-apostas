package com.exampl.repository;

import com.exampl.model.Usuario;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    // Lê a URI do MongoDB da variável de ambiente (Heroku) ou usa localhost como fallback
    private static final String CONNECTION_STRING = System.getenv("MONGODB_URI") != null
            ? System.getenv("MONGODB_URI")
            : "mongodb://localhost:27017";
    private static final String DATABASE_NAME     = "autenticacaoDB";
    private static final String COLLECTION_NAME   = "usuarios";

    private final MongoClient               mongoClient;
    private final MongoDatabase             database;
    private final MongoCollection<Document> collection;

    public UsuarioDAO() {
        this.mongoClient = MongoClients.create(CONNECTION_STRING);
        this.database    = mongoClient.getDatabase(DATABASE_NAME);
        this.collection  = database.getCollection(COLLECTION_NAME);
        System.out.println("🍃 MongoDB (Usuários) conectado: " + CONNECTION_STRING.replaceAll(":.*@", ":***@"));

        // Remove índice antigo de 'email' caso ainda exista no banco (migração)
        try { collection.dropIndex("email_1"); } catch (Exception ignored) {}

        // Garante índice único em login
        collection.createIndex(new Document("login", 1), new IndexOptions().unique(true));
    }

    // ──────────────────────────────────────────────
    // ID sequencial (mesmo padrão do LutadorDAO)
    // ──────────────────────────────────────────────
    private int gerarId() {
        Document ultimo = collection.find()
            .sort(new Document("id", -1))
            .first();
        return (ultimo != null) ? ultimo.getInteger("id", 0) + 1 : 1;
    }

    // ──────────────────────────────────────────────
    // Hash SHA-256 da senha
    // ──────────────────────────────────────────────
    public static String hashSenha(String senha) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(senha.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash da senha", e);
        }
    }

    // ──────────────────────────────────────────────
    // CREATE
    // ──────────────────────────────────────────────
    public Usuario inserir(Usuario usuario) {
        usuario.setId(gerarId());
        usuario.setSenhaHash(hashSenha(usuario.getSenhaHash()));

        Document doc = new Document()
            .append("id",        usuario.getId())
            .append("login",     usuario.getLogin())
            .append("senhaHash", usuario.getSenhaHash());

        collection.insertOne(doc);
        return usuario;
    }

    // ──────────────────────────────────────────────
    // AUTENTICAR — login + senha plana
    // ──────────────────────────────────────────────
    public Usuario autenticar(String login, String senhaPlana) {
        String hash = hashSenha(senhaPlana);

        Document doc = collection.find(
            Filters.and(
                Filters.eq("login",     login),
                Filters.eq("senhaHash", hash)
            )
        ).first();

        return (doc != null) ? documentParaUsuario(doc) : null;
    }

    // ──────────────────────────────────────────────
    // READ — todos
    // ──────────────────────────────────────────────
    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();
        for (Document doc : collection.find()) {
            lista.add(documentParaUsuario(doc));
        }
        return lista;
    }

    // ──────────────────────────────────────────────
    // READ — por ID
    // ──────────────────────────────────────────────
    public Usuario buscarPorId(int id) {
        Document doc = collection.find(Filters.eq("id", id)).first();
        return (doc != null) ? documentParaUsuario(doc) : null;
    }

    // ──────────────────────────────────────────────
    // READ — por login
    // ──────────────────────────────────────────────
    public Usuario buscarPorLogin(String login) {
        Document doc = collection.find(Filters.eq("login", login)).first();
        return (doc != null) ? documentParaUsuario(doc) : null;
    }

    // ──────────────────────────────────────────────
    // UPDATE — dados gerais (sem senha)
    // ──────────────────────────────────────────────
    public boolean atualizar(int id, Usuario usuario) {
        Document update = new Document("$set", new Document()
            .append("login", usuario.getLogin())
        );

        return collection.updateOne(Filters.eq("id", id), update)
                         .getModifiedCount() > 0;
    }

    // ──────────────────────────────────────────────
    // UPDATE — troca de senha
    // ──────────────────────────────────────────────
    public boolean trocarSenha(int id, String senhaAntiga, String senhaNova) {
        Usuario usuario = buscarPorId(id);
        if (usuario == null) return false;

        // Valida senha antiga antes de trocar
        if (!usuario.getSenhaHash().equals(hashSenha(senhaAntiga))) return false;

        Document update = new Document("$set",
            new Document("senhaHash", hashSenha(senhaNova))
        );

        return collection.updateOne(Filters.eq("id", id), update)
                         .getModifiedCount() > 0;
    }

    // ──────────────────────────────────────────────
    // DELETE
    // ──────────────────────────────────────────────
    public boolean deletar(int id) {
        DeleteResult result = collection.deleteOne(Filters.eq("id", id));
        return result.getDeletedCount() > 0;
    }

    // ──────────────────────────────────────────────
    // Utilitário: Document → Usuario
    // ──────────────────────────────────────────────
    private Usuario documentParaUsuario(Document doc) {
        return new Usuario(
            doc.getInteger("id",  0),
            doc.getString("login"),
            doc.getString("senhaHash")
        );
    }

    // ──────────────────────────────────────────────
    // Fecha conexão
    // ──────────────────────────────────────────────
    public void fechar() {
        mongoClient.close();
    }
}
