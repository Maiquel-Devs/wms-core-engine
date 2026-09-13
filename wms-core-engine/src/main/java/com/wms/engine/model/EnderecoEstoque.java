package com.wms.engine.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidade que representa um endereço físico de armazenamento (vaga/estante) dentro do galpão do armazém.
 * Utiliza controle de concorrência otimista (@Version) para garantir que múltiplas threads
 * ou requisições simultâneas não consigam alocar paletes diferentes na mesma vaga.
 */
@Entity
@Table(name = "endereco_estoque")
public class EnderecoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código identificador único e legível da posição no armazém.
     * Exemplo de padronização: R01-B01-N01-P01 (Rua-Bloco-Nível-Posição).
     */
    @Column(name = "codigo_endereco", nullable = false, unique = true, length = 30)
    private String codigoEndereco;

    @Column(nullable = false, length = 10)
    private String rua;

    @Column(nullable = false, length = 10)
    private String bloco;

    /**
     * Nível de altura da estante.
     * Por convenção, Nível 1 refere-se ao Chão/Piso.
     */
    @Column(nullable = false)
    private Integer nivel;

    @Column(nullable = false)
    private Integer posicao;

    @Column(name = "capacidade_peso_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacidadePesoKg;

    @Column(name = "capacidade_volume_m3", nullable = false, precision = 10, scale = 4)
    private BigDecimal capacidadeVolumeM3;

    @Column(nullable = false)
    private Boolean ocupado = false;

    /**
     * Trava de concorrência otimista gerenciada automaticamente pelo Hibernate
     * para prevenir condições de corrida (race conditions) e alocação dupla.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    /**
     * Construtor padrão exigido pelo JPA.
     */
    public EnderecoEstoque() {
    }

    public EnderecoEstoque(String codigoEndereco, String rua, String bloco, Integer nivel,
                           Integer posicao, BigDecimal capacidadePesoKg, BigDecimal capacidadeVolumeM3) {
        this.codigoEndereco = codigoEndereco;
        this.rua = rua;
        this.bloco = bloco;
        this.nivel = nivel;
        this.posicao = posicao;
        this.capacidadePesoKg = capacidadePesoKg;
        this.capacidadeVolumeM3 = capacidadeVolumeM3;
        this.ocupado = false;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoEndereco() {
        return codigoEndereco;
    }

    public void setCodigoEndereco(String codigoEndereco) {
        this.codigoEndereco = codigoEndereco;
    }

    public String getRua() {
        return rua;
    }

    public void setRua(String rua) {
        this.rua = rua;
    }

    public String getBloco() {
        return bloco;
    }

    public void setBloco(String bloco) {
        this.bloco = bloco;
    }

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public Integer getPosicao() {
        return posicao;
    }

    public void setPosicao(Integer posicao) {
        this.posicao = posicao;
    }

    public BigDecimal getCapacidadePesoKg() {
        return capacidadePesoKg;
    }

    public void setCapacidadePesoKg(BigDecimal capacidadePesoKg) {
        this.capacidadePesoKg = capacidadePesoKg;
    }

    public BigDecimal getCapacidadeVolumeM3() {
        return capacidadeVolumeM3;
    }

    public void setCapacidadeVolumeM3(BigDecimal capacidadeVolumeM3) {
        this.capacidadeVolumeM3 = capacidadeVolumeM3;
    }

    public Boolean getOcupado() {
        return ocupado;
    }

    public void setOcupado(Boolean ocupado) {
        this.ocupado = ocupado;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}