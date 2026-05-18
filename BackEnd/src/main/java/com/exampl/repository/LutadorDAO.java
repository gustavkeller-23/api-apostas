package com.exampl.repository;

import com.exampl.model.Lutador;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
 
import java.util.ArrayList;
import java.util.List;
 
public class LutadorDAO {
 
    // Lê a URI do MongoDB da variável de ambiente (Heroku) ou usa localhost como fallback
    private static final String CONNECTION_STRING = System.getenv("MONGODB_URI") != null
            ? System.getenv("MONGODB_URI")
            : "mongodb://localhost:27017";
    private static final String DATABASE_NAME     = "lutadoresDB";
    private static final String COLLECTION_NAME   = "lutadores";
 
    private final MongoClient               mongoClient;
    private final MongoDatabase             database;
    private final MongoCollection<Document> collection;
 
    public LutadorDAO() {
        this.mongoClient = MongoClients.create(CONNECTION_STRING);
        this.database    = mongoClient.getDatabase(DATABASE_NAME);
        this.collection  = database.getCollection(COLLECTION_NAME);
        System.out.println("🍃 MongoDB (Lutadores) conectado: " + CONNECTION_STRING.replaceAll(":.*@", ":***@"));
    }
 
    // ──────────────────────────────────────────────
    // Gera um ID inteiro sequencial simples
    // ──────────────────────────────────────────────
    private int gerarId() {
        Document ultimo = collection.find()
            .sort(new Document("id", -1))
            .first();
        return (ultimo != null) ? ultimo.getInteger("id", 0) + 1 : 1;
    }
 
    // ──────────────────────────────────────────────
    // CREATE
    // ──────────────────────────────────────────────
    public Lutador inserir(Lutador lutador) {
        int novoId = gerarId();
        lutador.setId(novoId);
 
        Document doc = new Document()
            .append("id",        lutador.getId())
            .append("nome",      lutador.getNome())
            .append("categoria", lutador.getCategoria())
            .append("apelido",   lutador.getApelido())
            .append("arte",      lutador.getArte());
 
        collection.insertOne(doc);
        return lutador;
    }
 
    // ──────────────────────────────────────────────
    // READ — todos
    // ──────────────────────────────────────────────
    public List<Lutador> listarTodos() {
        List<Lutador> lista = new ArrayList<>();
 
        for (Document doc : collection.find()) {
            lista.add(documentParaLutador(doc));
        }
        return lista;
    }
 
    // ──────────────────────────────────────────────
    // READ — por ID
    // ──────────────────────────────────────────────
    public Lutador buscarPorId(int id) {
        Document doc = collection
            .find(Filters.eq("id", id))
            .first();
 
        return (doc != null) ? documentParaLutador(doc) : null;
    }
 
    // ──────────────────────────────────────────────
    // UPDATE
    // ──────────────────────────────────────────────
    public boolean atualizar(int id, Lutador lutador) {
        Document update = new Document("$set", new Document()
            .append("nome",      lutador.getNome())
            .append("categoria", lutador.getCategoria())
            .append("apelido",   lutador.getApelido())
            .append("arte",      lutador.getArte())
        );
 
        long modificados = collection
            .updateOne(Filters.eq("id", id), update)
            .getModifiedCount();
 
        return modificados > 0;
    }
 
    // ──────────────────────────────────────────────
    // DELETE
    // ──────────────────────────────────────────────
    public boolean deletar(int id) {
        DeleteResult result = collection.deleteOne(Filters.eq("id", id));
        return result.getDeletedCount() > 0;
    }
 
    // ──────────────────────────────────────────────
    // Utilitário: Document → Lutador
    // ──────────────────────────────────────────────
    private Lutador documentParaLutador(Document doc) {
        return new Lutador(
            doc.getInteger("id",        0),
            doc.getString("nome"),
            doc.getString("categoria"),
            doc.getString("apelido"),
            doc.getString("arte")
        );
    }
 
    // ──────────────────────────────────────────────
    // Fecha conexão
    // ──────────────────────────────────────────────
    public void fechar() {
        mongoClient.close();
    }
}