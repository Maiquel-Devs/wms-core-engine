package com.wms.engine.service;

import com.wms.engine.AbstractIntegrationTest;
import com.wms.engine.model.EnderecoEstoque;
import com.wms.engine.model.Palete;
import com.wms.engine.model.Produto;
import com.wms.engine.repository.EnderecoEstoqueRepository;
import com.wms.engine.repository.PaleteRepository;
import com.wms.engine.repository.ProdutoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class AlocacaoServiceConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private AlocacaoService alocacaoService;

    @Autowired
    private EnderecoEstoqueRepository enderecoRepository;

    @Autowired
    private PaleteRepository paleteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Test
    @DisplayName("Deve permitir apenas 1 alocação e rejeitar colisões simultâneas na mesma vaga via Lock Otimista")
    void deveGarantirIsolamentoEmAlocacoesSimultaneas() throws InterruptedException {
        // 1. Arrange: Cria produto completo atendendo as restrições NOT NULL
        Produto produto = new Produto();
        produto.setNome("Compressor de Ar Industrial");
        produto.setCodigoSku("SKU-COMP-999");
        produto.setPesoUnitarioKg(new BigDecimal("40.00"));
        produto.setVolumeUnitarioM3(new BigDecimal("0.1600"));
        produto.setCategoriaRisco("PADRAO");
        produto = produtoRepository.save(produto);

        // Palete A
        Palete paleteA = new Palete();
        paleteA.setCodigoLote("LOTE-CONC-A");
        paleteA.setProduto(produto);
        paleteA.setQuantidadeItens(5);
        paleteA.setPesoTotalKg(new BigDecimal("200.00"));
        paleteA.setVolumeTotalM3(new BigDecimal("0.8000"));
        paleteA = paleteRepository.save(paleteA);

        // Palete B
        Palete paleteB = new Palete();
        paleteB.setCodigoLote("LOTE-CONC-B");
        paleteB.setProduto(produto);
        paleteB.setQuantidadeItens(5);
        paleteB.setPesoTotalKg(new BigDecimal("200.00"));
        paleteB.setVolumeTotalM3(new BigDecimal("0.8000"));
        paleteB = paleteRepository.save(paleteB);

        // Vaga física
        EnderecoEstoque endereco = new EnderecoEstoque();
        endereco.setCodigoEndereco("TEST-R99-B01-N02-P01");
        endereco.setRua("R99");
        endereco.setBloco("B01");
        endereco.setNivel(2);
        endereco.setPosicao(1);
        endereco.setCapacidadePesoKg(new BigDecimal("800.00"));
        endereco.setCapacidadeVolumeM3(new BigDecimal("2.5000"));
        endereco.setOcupado(false);
        endereco = enderecoRepository.save(endereco);

        final Long enderecoId = endereco.getId();
        final Long paleteAId = paleteA.getId();
        final Long paleteBId = paleteB.getId();

        // 2. Act: Dispara threads simultâneas disputando a mesma vaga
        int threadsCount = 2;
        CountDownLatch latchPronto = new CountDownLatch(threadsCount);
        CountDownLatch latchLargada = new CountDownLatch(1);

        AtomicInteger sucessos = new AtomicInteger(0);
        AtomicInteger falhas = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newFixedThreadPool(threadsCount)) {
            executor.submit(() -> {
                latchPronto.countDown();
                try {
                    latchLargada.await();
                    alocacaoService.alocarPalete(paleteAId, enderecoId);
                    sucessos.incrementAndGet();
                } catch (Exception e) {
                    falhas.incrementAndGet();
                }
            });

            executor.submit(() -> {
                latchPronto.countDown();
                try {
                    latchLargada.await();
                    alocacaoService.alocarPalete(paleteBId, enderecoId);
                    sucessos.incrementAndGet();
                } catch (Exception e) {
                    falhas.incrementAndGet();
                }
            });

            latchPronto.await();
            latchLargada.countDown();

            executor.shutdown();
            boolean finalizado = executor.awaitTermination(5, TimeUnit.SECONDS);
            assertThat(finalizado).isTrue();
        }

        // 3. Assert: Apenas 1 vence, 1 falha e a integridade da vaga é mantida
        assertThat(sucessos.get()).isEqualTo(1);
        assertThat(falhas.get()).isEqualTo(1);

        EnderecoEstoque enderecoAtualizado = enderecoRepository.findById(enderecoId).orElseThrow();
        assertThat(enderecoAtualizado.getOcupado()).isTrue();
        assertThat(enderecoAtualizado.getVersion()).isGreaterThan(0L);
    }
}