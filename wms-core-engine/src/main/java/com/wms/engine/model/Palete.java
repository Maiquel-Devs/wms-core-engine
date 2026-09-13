package com.wms.engine.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade que representa um palete ou lote físico de mercadorias dentro do armazém.
 * Rastreia todo o ciclo de vida da carga, desde a sua entrada na doca de recebimento,
 * o armazenamento físico em uma vaga, até o momento final de expedição.
 */
@Entity
@Table(name = "palete")
public class Palete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código de identificação único do lote (geralmente lido via código de barras ou RFID).
     */
    @Column(name = "codigo_lote", nullable = false, unique = true, length = 50)
    private String codigoLote;

    /**
     * O produto mestre (SKU) contido neste palete.
     * Utiliza carregamento preguiçoso (LAZY) para evitar queries desnecessárias no banco.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "quantidade_itens", nullable = false)
    private Integer quantidadeItens;

    @Column(name = "peso_total_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal pesoTotalKg;

    @Column(name = "volume_total_m3", nullable = false, precision = 10, scale = 4)
    private BigDecimal volumeTotalM3;

    /**
     * Endereço físico (vaga na estante) onde o palete está alocado.
     * Pode estar nulo se o palete ainda estiver aguardando alocação na doca
     * ou se já tiver sido expedido do armazém.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_id", unique = true)
    private EnderecoEstoque endereco;

    @Column(name = "alocado_em")
    private LocalDateTime alocadoEm;

    /**
     * Situação operacional do palete no sistema (ex: RECEBIDO_DOCA, ARMAZENADO, EXPEDIDO).
     * O estado inicial padrão sempre será o recebimento na doca.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusPalete status = StatusPalete.RECEBIDO_DOCA;

    @Column(name = "expedido_em")
    private LocalDateTime expedidoEm;

    /**
     * Construtor padrão sem argumentos, exigido pela especificação JPA/Hibernate.
     */
    public Palete() {
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public void setCodigoLote(String codigoLote) {
        this.codigoLote = codigoLote;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public Integer getQuantidadeItens() {
        return quantidadeItens;
    }

    public void setQuantidadeItens(Integer quantidadeItens) {
        this.quantidadeItens = quantidadeItens;
    }

    public BigDecimal getPesoTotalKg() {
        return pesoTotalKg;
    }

    public void setPesoTotalKg(BigDecimal pesoTotalKg) {
        this.pesoTotalKg = pesoTotalKg;
    }

    public BigDecimal getVolumeTotalM3() {
        return volumeTotalM3;
    }

    public void setVolumeTotalM3(BigDecimal volumeTotalM3) {
        this.volumeTotalM3 = volumeTotalM3;
    }

    public EnderecoEstoque getEndereco() {
        return endereco;
    }

    public void setEndereco(EnderecoEstoque endereco) {
        this.endereco = endereco;
    }

    public LocalDateTime getAlocadoEm() {
        return alocadoEm;
    }

    public void setAlocadoEm(LocalDateTime alocadoEm) {
        this.alocadoEm = alocadoEm;
    }

    public StatusPalete getStatus() {
        return status;
    }

    public void setStatus(StatusPalete status) {
        this.status = status;
    }

    public LocalDateTime getExpedidoEm() {
        return expedidoEm;
    }

    public void setExpedidoEm(LocalDateTime expedidoEm) {
        this.expedidoEm = expedidoEm;
    }
}