package com.wms.engine.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade que representa o cadastro mestre de um Produto (SKU) no sistema WMS.
 * Define as características físicas, descrições e regras de negócio base (como categoria de risco)
 * que guiarão o motor de IA e o sistema heurístico na tomada de decisão para a alocação física.
 */
@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código SKU (Stock Keeping Unit) único que identifica o produto no catálogo logístico.
     */
    @Column(name = "codigo_sku", nullable = false, unique = true, length = 50)
    private String codigoSku;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    /**
     * Classificação de risco do produto (ex: PADRAO, INFLAMAVEL, QUIMICO, FRAGIL).
     * Fundamental para validações de segurança e sugestões de alocação especializada.
     */
    @Column(name = "categoria_risco", nullable = false, length = 30)
    private String categoriaRisco = "PADRAO";

    /**
     * Instruções textuais ou regras adicionais para o manuseio e armazenamento correto da carga.
     */
    @Column(name = "recomendacao_armazenagem", columnDefinition = "TEXT")
    private String recomendacaoArmazenagem;

    @Column(name = "peso_unitario_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal pesoUnitarioKg;

    @Column(name = "volume_unitario_m3", nullable = false, precision = 10, scale = 4)
    private BigDecimal volumeUnitarioM3;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    /**
     * Construtor padrão sem argumentos, exigido pela especificação JPA/Hibernate.
     */
    public Produto() {
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoSku() {
        return codigoSku;
    }

    public void setCodigoSku(String codigoSku) {
        this.codigoSku = codigoSku;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCategoriaRisco() {
        return categoriaRisco;
    }

    public void setCategoriaRisco(String categoriaRisco) {
        this.categoriaRisco = categoriaRisco;
    }

    public String getRecomendacaoArmazenagem() {
        return recomendacaoArmazenagem;
    }

    public void setRecomendacaoArmazenagem(String recomendacaoArmazenagem) {
        this.recomendacaoArmazenagem = recomendacaoArmazenagem;
    }

    public BigDecimal getPesoUnitarioKg() {
        return pesoUnitarioKg;
    }

    public void setPesoUnitarioKg(BigDecimal pesoUnitarioKg) {
        this.pesoUnitarioKg = pesoUnitarioKg;
    }

    public BigDecimal getVolumeUnitarioM3() {
        return volumeUnitarioM3;
    }

    public void setVolumeUnitarioM3(BigDecimal volumeUnitarioM3) {
        this.volumeUnitarioM3 = volumeUnitarioM3;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}