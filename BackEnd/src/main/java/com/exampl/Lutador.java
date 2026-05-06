package com.exampl;

public class Lutador {
 
    private int    id;
    private String nome;
    private String    categoria;
    private String apelido;
    private String    arte;
 
    public Lutador() {}
 
    public Lutador(int id, String nome, String categoria, String apelido, String arte) {
        this.id        = id;
        this.nome      = nome;
        this.categoria = categoria;
        this.apelido   = apelido;
        this.arte      = arte;
    }
 
    public Lutador(String nome, String categoria, String apelido, String arte) {
        this.nome      = nome;
        this.categoria = categoria;
        this.apelido   = apelido;
        this.arte      = arte;
    }
 
 
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getCategoria() { return categoria; }
    public String getApelido() { return apelido; }
    public String getArte() { return arte; }

    public void setId(int id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setApelido(String apelido) { this.apelido = apelido; }
    public void setArte(String arte) { this.arte = arte; }
 
    // ──────────────────────────────────────────────
    // Serialização JSON manual
    // ──────────────────────────────────────────────
 
    public String toJson() {
        return String.format(
            "{\"id\": %d, \"nome\": \"%s\", \"categoria\": \"%s\", \"apelido\": \"%s\", \"arte\": \"%s\"}",
            id, nome, categoria, apelido, arte
        );
    }
 
    @Override
    public String toString() {
        return toJson();
    }
}