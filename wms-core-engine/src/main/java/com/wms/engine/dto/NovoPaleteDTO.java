package com.wms.engine.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) utilizado como objeto de formulário (Form Backing Object).
 * Transporta os dados preenchidos pelo usuário na interface (Thymeleaf) durante o
 * recebimento de mercadorias na doca para a criação de um novo palete no sistema.
 */
public class NovoPaleteDTO {

    private String codigoLote;
    private Long produtoId;
    private Integer quantidadeItens;
    private BigDecimal pesoTotalKg;
    private BigDecimal volumeTotalM3;

    /**
     * Construtor padrão vazio.
     * Necessário para o mecanismo de data binding do Spring MVC e Thymeleaf (@ModelAttribute).
     */
    public NovoPaleteDTO() {
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public void setCodigoLote(String codigoLote) {
        this.codigoLote = codigoLote;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
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
}