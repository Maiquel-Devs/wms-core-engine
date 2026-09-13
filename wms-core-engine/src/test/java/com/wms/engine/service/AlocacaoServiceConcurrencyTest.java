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

/**
 * Suíte de testes de integração para validar a segurança estrutural e transacional
 * do motor de alocação em cenários de alta concorrência (Race Conditions).
 */
public class AlocacaoServiceConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private AlocacaoService alocacaoService;

    @Autowired
    private EnderecoEstoqueRepository enderecoRepository;

    @Autowired
    private PaleteRepository paleteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Simula o cenário crítico onde dois operadores (ou rotinas automáticas) tentam
     * alocar paletes diferentes na mesma vaga física exatamente no mesmo milissegundo.
     * O sistema deve permitir a primeira transação e rejeitar a segunda usando
     * Lock Otimista (JPA @Version / ObjectOptimisticLockingFailureException).
     */
    @Test
    @DisplayName("Deve permitir apenas 1 alocação e rejeitar colisões simultâneas na mesma vaga via Lock Otimista")
    void deveGarantirIsolamentoEmAlocacoesSimultaneas() throws InterruptedException {

        // ==========================================
        // 1. ARRANGE (Preparação do Banco de Dados)
        // ==========================================

        // Produto base atendendo às restrições do banco (NOT NULL)
        Produto produto = new Produto();
        produto.setNome("Compressor de Ar Industrial");
        produto.setCodigoSku("SKU-COMP-999");
        produto.setPesoUnitarioKg(new BigDecimal("40.00"));
        produto.setVolumeUnitarioM3(new BigDecimal("0.1600"));
        produto.setCategoriaRisco("PADRAO");
        produto = produtoRepository.save(produto);

        // Palete A (Concorrente 1)
        Palete paleteA = new Palete();
        paleteA.setCodigoLote("LOTE-CONC-A");
        paleteA.setProduto(produto);
        paleteA.setQuantidadeItens(5);
        paleteA.setPesoTotalKg(new BigDecimal("200.00"));
        paleteA.setVolumeTotalM3(new BigDecimal("0.8000"));
        paleteA = paleteRepository.save(paleteA);

        // Palete B (Concorrente 2)
        Palete paleteB = new Palete();
        paleteB.setCodigoLote("LOTE-CONC-B");
        paleteB.setProduto(produto);
        paleteB.setQuantidadeItens(5);
        paleteB.setPesoTotalKg(new BigDecimal("200.00"));
        paleteB.setVolumeTotalM3(new BigDecimal("0.8000"));
        paleteB = paleteRepository.save(paleteB);

        // Vaga física disputada no armazém
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

        // ==========================================
        // 2. ACT (Disparo Simultâneo de Threads)
        // ==========================================

        int threadsCount = 2;
        // Latch para garantir que as threads nasçam e fiquem em repouso
        CountDownLatch latchPronto = new CountDownLatch(threadsCount);
        // Latch para funcionar como o "tiro de largada", liberando ambas simultaneamente
        CountDownLatch latchLargada = new CountDownLatch(1);

        AtomicInteger sucessos = new AtomicInteger(0);
        AtomicInteger falhas = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newFixedThreadPool(threadsCount)) {

            // Thread 1: Operador tentando alocar Palete A
            executor.submit(() -> {
                latchPronto.countDown();
                try {
                    latchLargada.await(); // Aguarda o tiro de largada
                    alocacaoService.alocarPalete(paleteAId, enderecoId);
                    sucessos.incrementAndGet();
                } catch (Exception e) {
                    falhas.incrementAndGet();
                }
            });

            // Thread 2: Inteligência Artificial tentando alocar Palete B
            executor.submit(() -> {
                latchPronto.countDown();
                try {
                    latchLargada.await(); // Aguarda o tiro de largada
                    alocacaoService.alocarPalete(paleteBId, enderecoId);
                    sucessos.incrementAndGet();
                } catch (Exception e) {
                    falhas.incrementAndGet();
                }
            });

            // Aguarda as threads se prepararem e dispara ambas ao mesmo tempo
            latchPronto.await();
            latchLargada.countDown();

            executor.shutdown();
            boolean finalizado = executor.awaitTermination(5, TimeUnit.SECONDS);

            assertThat(finalizado)
                    .as("O pool de threads deveria ter finalizado a execução dentro do tempo limite (5s)")
                    .isTrue();
        }

        // ==========================================
        // 3. ASSERT (Validação da Regra de Negócio)
        // ==========================================

        assertThat(sucessos.get())
                .as("Apenas 1 operação de alocação deve ser bem-sucedida (O primeiro a comitar)")
                .isEqualTo(1);

        assertThat(falhas.get())
                .as("A segunda operação deve obrigatoriamente falhar e ser barrada pelo Lock Otimista")
                .isEqualTo(1);

        EnderecoEstoque enderecoAtualizado = enderecoRepository.findById(enderecoId).orElseThrow();

        assertThat(enderecoAtualizado.getOcupado())
                .as("A vaga física deve estar marcada como OCUPADA após a transação vitoriosa")
                .isTrue();

        assertThat(enderecoAtualizado.getVersion())
                .as("O controle de concorrência do Hibernate (Version) deve ter sido incrementado")
                .isGreaterThan(0L);
    }
}