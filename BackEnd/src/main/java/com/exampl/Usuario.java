package com.exampl;

import java.time.Instant;

public class Usuario {

    private int    id;
    private String login;
    private String senhaHash;
    private String email;
    private String role;        // ex: "admin", "user"
    private boolean ativo;
    private String criadoEm;

    public Usuario() {}

    public Usuario(int id, String login, String senhaHash, String email, String role, boolean ativo, String criadoEm) {
        this.id        = id;
        this.login     = login;
        this.senhaHash = senhaHash;
        this.email     = email;
        this.role      = role;
        this.ativo     = ativo;
        this.criadoEm  = criadoEm;
    }

    public Usuario(String login, String senhaHash, String email, String role) {
        this.login     = login;
        this.senhaHash = senhaHash;
        this.email     = email;
        this.role      = role;
        this.ativo     = true;
        this.criadoEm  = Instant.now().toString();
    }

    public int     getId()        { return id; }
    public String  getLogin()     { return login; }
    public String  getSenhaHash() { return senhaHash; }
    public String  getEmail()     { return email; }
    public String  getRole()      { return role; }
    public boolean isAtivo()      { return ativo; }
    public String  getCriadoEm()  { return criadoEm; }

    public void setId(int id)              { this.id        = id; }
    public void setLogin(String login)     { this.login     = login; }
    public void setSenhaHash(String h)     { this.senhaHash = h; }
    public void setEmail(String email)     { this.email     = email; }
    public void setRole(String role)       { this.role      = role; }
    public void setAtivo(boolean ativo)    { this.ativo     = ativo; }
    public void setCriadoEm(String d)      { this.criadoEm  = d; }

    public String toJson() {
        return String.format(
            "{\"id\": %d, \"login\": \"%s\", \"email\": \"%s\", \"role\": \"%s\", \"ativo\": %b, \"criadoEm\": \"%s\"}",
            id, login, email, role, ativo, criadoEm
        );
    }

    @Override
    public String toString() { return toJson(); }
}