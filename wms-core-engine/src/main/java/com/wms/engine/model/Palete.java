package com.wms.engine.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "palete")
public class Palete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_lote", nullable = false, unique = true, length = 50)
    private String codigoLote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "quantidade_itens", nullable = false)
    private Integer quantidadeItens;

    @Column(name = "peso_total_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal pesoTotalKg;

    @Column(name = "volume_total_m3", nullable = false, precision = 10, scale = 4)
    private BigDecimal volumeTotalM3;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_id", unique = true)
    private EnderecoEstoque endereco;

    @Column(name = "alocado_em")
    private LocalDateTime alocadoEm;

    public Palete() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigoLote() { return codigoLote; }
    public void setCodigoLote(String codigoLote) { this.codigoLote = codigoLote; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public Integer getQuantidadeItens() { return quantidadeItens; }
    public void setQuantidadeItens(Integer quantidadeItens) { this.quantidadeItens = quantidadeItens; }

    public BigDecimal getPesoTotalKg() { return pesoTotalKg; }
    public void setPesoTotalKg(BigDecimal pesoTotalKg) { this.pesoTotalKg = pesoTotalKg; }

    public BigDecimal getVolumeTotalM3() { return volumeTotalM3; }
    public void setVolumeTotalM3(BigDecimal volumeTotalM3) { this.volumeTotalM3 = volumeTotalM3; }

    public EnderecoEstoque getEndereco() { return endereco; }
    public void setEndereco(EnderecoEstoque endereco) { this.endereco = endereco; }

    public LocalDateTime getAlocadoEm() { return alocadoEm; }
    public void setAlocadoEm(LocalDateTime alocadoEm) { this.alocadoEm = alocadoEm; }
}