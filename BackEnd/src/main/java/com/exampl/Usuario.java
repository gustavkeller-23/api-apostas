package com.exampl;

public class Usuario {

    private int    id;
    private String login;
    private String senhaHash;

    public Usuario() {}

    public Usuario(int id, String login, String senhaHash) {
        this.id        = id;
        this.login     = login;
        this.senhaHash = senhaHash;
    }

    public Usuario(String login, String senhaHash) {
        this.login     = login;
        this.senhaHash = senhaHash;
    }

    public int    getId()        { return id; }
    public String getLogin()     { return login; }
    public String getSenhaHash() { return senhaHash; }

    public void setId(int id)           { this.id        = id; }
    public void setLogin(String login)  { this.login     = login; }
    public void setSenhaHash(String h)  { this.senhaHash = h; }

    public String toJson() {
        return String.format(
            "{\"id\": %d, \"login\": \"%s\"}",
            id, login
        );
    }

    @Override
    public String toString() { return toJson(); }
}