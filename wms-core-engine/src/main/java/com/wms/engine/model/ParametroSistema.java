package com.wms.engine.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade responsável por armazenar configurações dinâmicas e parâmetros globais do sistema WMS.
 * Permite ajustar comportamentos da aplicação (ex: prompts do motor de IA, chaves de API,
 * regras de contingência) sem a necessidade de recompilar ou fazer um novo deploy do código.
 */
@Entity
@Table(name = "parametro_sistema")
public class ParametroSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Chave única de identificação do parâmetro (ex: "IA_PROMPT_ALOCACAO").
     */
    @Column(nullable = false, unique = true, length = 100)
    private String chave;

    /**
     * Valor configurado para o parâmetro. Utiliza o tipo TEXT no banco de dados para suportar
     * payloads extensos, como templates longos de prompts ou strings JSON de configuração.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String valor;

    /**
     * Descrição legível em linguagem natural sobre o propósito deste parâmetro,
     * facilitando a manutenção para administradores do sistema.
     */
    @Column(length = 255)
    private String descricao;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    /**
     * Construtor padrão sem argumentos, exigido pela especificação JPA/Hibernate.
     */
    public ParametroSistema() {
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChave() {
        return chave;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}