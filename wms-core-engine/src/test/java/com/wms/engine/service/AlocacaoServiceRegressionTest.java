package com.wms.engine.service;

import com.wms.engine.AbstractIntegrationTest;
import com.wms.engine.exception.CapacidadeExcedidaException;
import com.wms.engine.model.EnderecoEstoque;
import com.wms.engine.model.Palete;
import com.wms.engine.model.Produto;
import com.wms.engine.repository.EnderecoEstoqueRepository;
import com.wms.engine.repository.PaleteRepository;
import com.wms.engine.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AlocacaoServiceRegressionTest extends AbstractIntegrationTest {

    @Autowired
    private AlocacaoService alocacaoService;

    @Autowired
    private EnderecoEstoqueRepository enderecoRepository;

    @Autowired
    private PaleteRepository paleteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    private Produto produtoPadrao;

    @BeforeEach
    void setUp() {
        // Garante isolamento entre execuções de teste
        paleteRepository.deleteAll();

        String skuUnico = "SKU-MOT-" + UUID.randomUUID().toString().substring(0, 8);
        produtoPadrao = new Produto();
        produtoPadrao.setNome("Motor Trifásico Industrial");
        produtoPadrao.setCodigoSku(skuUnico);
        produtoPadrao.setPesoUnitarioKg(new BigDecimal("50.00"));
        produtoPadrao.setVolumeUnitarioM3(new BigDecimal("0.1000"));
        produtoPadrao.setCategoriaRisco("PADRAO");
        produtoPadrao = produtoRepository.save(produtoPadrao);
    }

    private EnderecoEstoque criarEndereco(String codigo, int nivel, String pesoMax, String volMax) {
        String codigoUnico = codigo + "-" + UUID.randomUUID().toString().substring(0, 4);
        EnderecoEstoque end = new EnderecoEstoque();
        end.setCodigoEndereco(codigoUnico);
        end.setRua("R01");
        end.setBloco("B01");
        end.setNivel(nivel);
        end.setPosicao(1);
        end.setCapacidadePesoKg(new BigDecimal(pesoMax));
        end.setCapacidadeVolumeM3(new BigDecimal(volMax));
        end.setOcupado(false);
        return enderecoRepository.save(end);
    }

    private Palete criarPalete(String lote, String pesoTotal, String volumeTotal) {
        String loteUnico = lote + "-" + UUID.randomUUID().toString().substring(0, 4);
        Palete p = new Palete();
        p.setCodigoLote(loteUnico);
        p.setProduto(produtoPadrao);
        p.setQuantidadeItens(10);
        p.setPesoTotalKg(new BigDecimal(pesoTotal));
        p.setVolumeTotalM3(new BigDecimal(volumeTotal));
        return paleteRepository.save(p);
    }

    @Test
    @DisplayName("1. Bloqueio por Excesso de Peso: Rejeita palete com peso superior à capacidade da estante")
    void deveRejeitarAlocacaoQuandoPesoExcederCapacidade() {
        EnderecoEstoque endereco = criarEndereco("R01-B01-N01-P90", 1, "500.00", "3.0000");
        Palete paletePesado = criarPalete("LOT-PESO-OVER", "650.00", "1.5000");

        assertThatThrownBy(() -> alocacaoService.alocarPalete(paletePesado.getId(), endereco.getId()))
                .isInstanceOf(CapacidadeExcedidaException.class)
                .hasMessageContaining("Peso");
    }

    @Test
    @DisplayName("2. Bloqueio por Excesso de Volume: Rejeita palete com cubagem superior à vaga")
    void deveRejeitarAlocacaoQuandoVolumeExcederCapacidade() {
        EnderecoEstoque endereco = criarEndereco("R01-B01-N01-P91", 1, "1500.00", "1.2000");
        Palete paleteVolumoso = criarPalete("LOT-VOL-OVER", "300.00", "2.0000");

        assertThatThrownBy(() -> alocacaoService.alocarPalete(paleteVolumoso.getId(), endereco.getId()))
                .isInstanceOf(CapacidadeExcedidaException.class)
                .hasMessageContaining("Volume");
    }

    @Test
    @DisplayName("3. Regra de Piso: Cargas pesadas (> 500kg) só podem ser alocadas no Nível 1 (Piso)")
    void deveRestringirCargasSuperioresA500KgAoNivelUm() {
        EnderecoEstoque nivelAlto = criarEndereco("R01-B01-N02-P92", 2, "1500.00", "3.0000");
        Palete paletePesado = criarPalete("LOT-PISO-RULE", "600.00", "1.5000");

        assertThatThrownBy(() -> alocacaoService.alocarPalete(paletePesado.getId(), nivelAlto.getId()))
                .isInstanceOf(CapacidadeExcedidaException.class)
                .hasMessageContaining("Piso");
    }

    @Test
    @DisplayName("4. Ciclo de Desalocação: Baixa deve liberar vaga e desvincular palete")
    void deveDesalocarPaleteComSucessoELiberarVaga() {
        EnderecoEstoque endereco = criarEndereco("R01-B01-N01-P93", 1, "1500.00", "3.0000");
        Palete palete = criarPalete("LOT-BAIXA-01", "400.00", "1.0000");

        alocacaoService.alocarPalete(palete.getId(), endereco.getId());

        EnderecoEstoque enderecoOcupado = enderecoRepository.findById(endereco.getId()).orElseThrow();
        assertThat(enderecoOcupado.getOcupado()).isTrue();

        alocacaoService.desalocarPalete(palete.getId());

        EnderecoEstoque enderecoLiberado = enderecoRepository.findById(endereco.getId()).orElseThrow();
        assertThat(enderecoLiberado.getOcupado()).isFalse();

        Palete paleteDesalocado = paleteRepository.findById(palete.getId()).orElseThrow();
        assertThat(paleteDesalocado.getEndereco()).isNull();
        assertThat(paleteDesalocado.getAlocadoEm()).isNull();
    }
}