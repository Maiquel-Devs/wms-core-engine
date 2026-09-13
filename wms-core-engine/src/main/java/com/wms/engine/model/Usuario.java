package com.wms.engine.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade que representa um usuário do sistema WMS.
 * Responsável por armazenar as credenciais e os níveis de acesso para a integração
 * com os mecanismos de autenticação e autorização (Spring Security).
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    /**
     * Nome de usuário único (login) utilizado para acessar o sistema.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Senha do usuário. Na prática, deve armazenar o hash criptográfico (ex: BCrypt),
     * e nunca a senha em texto plano.
     */
    @Column(nullable = false)
    private String senha;

    /**
     * Perfil de acesso do usuário, definindo suas permissões no sistema.
     * Valores esperados por convenção do Spring Security: 'ROLE_OPERADOR' ou 'ROLE_ADMIN'.
     */
    @Column(nullable = false, length = 30)
    private String perfil;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    /**
     * Construtor padrão sem argumentos, exigido pela especificação JPA/Hibernate.
     */
    public Usuario() {
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}