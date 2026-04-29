package com.exampl;

public class Lutador {
 
    private int    id;
    private String nome;
    private int    categoria;
    private String apelido;
    private int    arte;
 
    public Lutador() {}
 
    public Lutador(int id, String nome, int categoria, String apelido, int arte) {
        this.id        = id;
        this.nome      = nome;
        this.categoria = categoria;
        this.apelido   = apelido;
        this.arte      = arte;
    }
 
    public Lutador(String nome, int categoria, String apelido, int arte) {
        this.nome      = nome;
        this.categoria = categoria;
        this.apelido   = apelido;
        this.arte      = arte;
    }
 
 
    public int getId() { return id; }
    public String getNome() { return nome; }
    public int getCategoria() { return categoria; }
    public String getApelido() { return apelido; }
    public int getArte() { return arte; }

    public void setId(int id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setCategoria(int categoria) { this.categoria = categoria; }
    public void setApelido(String apelido) { this.apelido = apelido; }
    public void setArte(int arte) { this.arte = arte; }
 
    // ──────────────────────────────────────────────
    // Serialização JSON manual
    // ──────────────────────────────────────────────
 
    public String toJson() {
        return String.format(
            "{\"id\": %d, \"nome\": \"%s\", \"categoria\": %d, \"apelido\": \"%s\", \"arte\": %d}",
            id, nome, categoria, apelido, arte
        );
    }
 
    @Override
    public String toString() {
        return toJson();
    }
}