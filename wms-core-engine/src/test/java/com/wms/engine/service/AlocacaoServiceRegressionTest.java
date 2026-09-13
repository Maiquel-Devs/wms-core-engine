package com.wms.engine.service;

import com.wms.engine.AbstractIntegrationTest;
import com.wms.engine.exception.CapacidadeExcedidaException;
import com.wms.engine.model.EnderecoEstoque;
import com.wms.engine.model.Palete;
import com.wms.engine.model.Produto;
import com.wms.engine.model.StatusPalete;
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

/**
 * Suíte de testes de integração (Regressão) para o motor de alocação.
 * Valida o cumprimento rigoroso das regras de negócio físicas (peso, volume e nível de piso)
 * e o ciclo de vida completo de uma carga (armazenamento e expedição).
 */
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
        // Garante isolamento entre execuções limpando paletes anteriores
        paleteRepository.deleteAll();

        // Sufixo randômico para evitar violação de Unique Constraint em execuções paralelas
        String skuUnico = String.format("SKU-MOT-%s", UUID.randomUUID().toString().substring(0, 8));

        produtoPadrao = new Produto();
        produtoPadrao.setNome("Motor Trifásico Industrial");
        produtoPadrao.setCodigoSku(skuUnico);
        produtoPadrao.setPesoUnitarioKg(new BigDecimal("50.00"));
        produtoPadrao.setVolumeUnitarioM3(new BigDecimal("0.1000"));
        produtoPadrao.setCategoriaRisco("PADRAO");
        produtoPadrao = produtoRepository.save(produtoPadrao);
    }

    // ==========================================
    // MÉTODOS AUXILIARES (HELPERS)
    // ==========================================

    private EnderecoEstoque criarEndereco(String codigo, int nivel, String pesoMax, String volMax) {
        String codigoUnico = String.format("%s-%s", codigo, UUID.randomUUID().toString().substring(0, 4));

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
        String loteUnico = String.format("%s-%s", lote, UUID.randomUUID().toString().substring(0, 4));

        Palete p = new Palete();
        p.setCodigoLote(loteUnico);
        p.setProduto(produtoPadrao);
        p.setQuantidadeItens(10);
        p.setPesoTotalKg(new BigDecimal(pesoTotal));
        p.setVolumeTotalM3(new BigDecimal(volumeTotal));
        p.setStatus(StatusPalete.RECEBIDO_DOCA); // Fixando o status inicial explícito
        return paleteRepository.save(p);
    }

    // ==========================================
    // CASOS DE TESTE
    // ==========================================

    @Test
    @DisplayName("1. Bloqueio por Excesso de Peso: Rejeita palete com peso superior à capacidade da estante")
    void deveRejeitarAlocacaoQuandoPesoExcederCapacidade() {
        // Arrange
        EnderecoEstoque endereco = criarEndereco("R01-B01-N01-P90", 1, "500.00", "3.0000");
        Palete paletePesado = criarPalete("LOT-PESO-OVER", "650.00", "1.5000");

        // Act & Assert
        assertThatThrownBy(() -> alocacaoService.alocarPalete(paletePesado.getId(), endereco.getId()))
                .as("Não deve permitir alocação de carga física mais pesada que o limite da estante")
                .isInstanceOf(CapacidadeExcedidaException.class)
                .hasMessageContaining("Peso");
    }

    @Test
    @DisplayName("2. Bloqueio por Excesso de Volume: Rejeita palete com cubagem superior à vaga")
    void deveRejeitarAlocacaoQuandoVolumeExcederCapacidade() {
        // Arrange
        EnderecoEstoque endereco = criarEndereco("R01-B01-N01-P91", 1, "1500.00", "1.2000");
        Palete paleteVolumoso = criarPalete("LOT-VOL-OVER", "300.00", "2.0000");

        // Act & Assert
        assertThatThrownBy(() -> alocacaoService.alocarPalete(paleteVolumoso.getId(), endereco.getId()))
                .as("Não deve permitir alocação de carga com volume cúbico superior ao da vaga")
                .isInstanceOf(CapacidadeExcedidaException.class)
                .hasMessageContaining("Volume");
    }

    @Test
    @DisplayName("3. Regra de Piso: Cargas pesadas (> 500kg) só podem ser alocadas no Nível 1 (Piso)")
    void deveRestringirCargasSuperioresA500KgAoNivelUm() {
        // Arrange
        EnderecoEstoque nivelAlto = criarEndereco("R01-B01-N02-P92", 2, "1500.00", "3.0000");
        Palete paletePesado = criarPalete("LOT-PISO-RULE", "600.00", "1.5000");

        // Act & Assert
        assertThatThrownBy(() -> alocacaoService.alocarPalete(paletePesado.getId(), nivelAlto.getId()))
                .as("O motor heurístico deve barrar cargas acima de 500kg em andares superiores da estante")
                .isInstanceOf(CapacidadeExcedidaException.class)
                .hasMessageContaining("Piso");
    }

    @Test
    @DisplayName("4. Ciclo de Desalocação: Baixa deve liberar vaga e transicionar palete para EXPEDIDO")
    void deveDesalocarPaleteComSucessoELiberarVaga() {
        // Arrange
        EnderecoEstoque endereco = criarEndereco("R01-B01-N01-P93", 1, "1500.00", "3.0000");
        Palete palete = criarPalete("LOT-BAIXA-01", "400.00", "1.0000");

        alocacaoService.alocarPalete(palete.getId(), endereco.getId());

        // Verificação intermediária: a vaga ficou ocupada?
        EnderecoEstoque enderecoOcupado = enderecoRepository.findById(endereco.getId()).orElseThrow();
        assertThat(enderecoOcupado.getOcupado()).isTrue();

        // Act
        alocacaoService.desalocarPalete(palete.getId());

        // Assert
        EnderecoEstoque enderecoLiberado = enderecoRepository.findById(endereco.getId()).orElseThrow();
        assertThat(enderecoLiberado.getOcupado())
                .as("A vaga física na estante deve ser liberada (ocupado = false) após a baixa")
                .isFalse();

        Palete paleteDesalocado = paleteRepository.findById(palete.getId()).orElseThrow();

        assertThat(paleteDesalocado.getEndereco())
                .as("O vínculo físico do palete com a estante deve ser desfeito")
                .isNull();

        assertThat(paleteDesalocado.getStatus())
                .as("O ciclo de vida do palete deve terminar como EXPEDIDO")
                .isEqualTo(StatusPalete.EXPEDIDO);

        assertThat(paleteDesalocado.getExpedidoEm())
                .as("O timestamp de expedição (saída) deve ser gerado pelo sistema para auditoria")
                .isNotNull();
    }
}