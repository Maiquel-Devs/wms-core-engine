package com.wms.engine.service;

import com.wms.engine.client.AiAllocationClient;
import com.wms.engine.dto.ia.AlocacaoDecisaoDTO;
import com.wms.engine.exception.CapacidadeExcedidaException;
import com.wms.engine.exception.EnderecoOcupadoException;
import com.wms.engine.model.EnderecoEstoque;
import com.wms.engine.model.Palete;
import com.wms.engine.model.Produto;
import com.wms.engine.model.StatusPalete;
import com.wms.engine.repository.EnderecoEstoqueRepository;
import com.wms.engine.repository.PaleteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlocacaoServiceTest {

    @Mock
    private EnderecoEstoqueRepository enderecoRepository;

    @Mock
    private PaleteRepository paleteRepository;

    @Mock
    private AiAllocationClient aiClient;

    @InjectMocks
    private AlocacaoService alocacaoService;

    private Palete paletePesado;
    private EnderecoEstoque vagaPiso;
    private EnderecoEstoque vagaNivelSuperior;

    @BeforeEach
    void setUp() {
        Produto produto = new Produto();
        produto.setNome("Motor Trifásico");

        paletePesado = new Palete();
        paletePesado.setId(1L);
        paletePesado.setCodigoLote("LT-UNIT-01");
        paletePesado.setProduto(produto);
        paletePesado.setPesoTotalKg(new BigDecimal("650.00")); // > 500kg
        paletePesado.setVolumeTotalM3(new BigDecimal("1.2000"));
        paletePesado.setStatus(StatusPalete.RECEBIDO_DOCA);

        vagaPiso = new EnderecoEstoque();
        vagaPiso.setId(10L);
        vagaPiso.setCodigoEndereco("R01-B01-N01-P01");
        vagaPiso.setNivel(1);
        vagaPiso.setCapacidadePesoKg(new BigDecimal("1500.00"));
        vagaPiso.setCapacidadeVolumeM3(new BigDecimal("2.0000"));
        vagaPiso.setOcupado(false);

        vagaNivelSuperior = new EnderecoEstoque();
        vagaNivelSuperior.setId(20L);
        vagaNivelSuperior.setCodigoEndereco("R01-B01-N02-P01");
        vagaNivelSuperior.setNivel(2);
        vagaNivelSuperior.setCapacidadePesoKg(new BigDecimal("800.00"));
        vagaNivelSuperior.setCapacidadeVolumeM3(new BigDecimal("2.0000"));
        vagaNivelSuperior.setOcupado(false);
    }

    @Test
    @DisplayName("Deve barrar com CapacidadeExcedidaException carga > 500kg se for alocada em nível superior ao solo")
    void naoDevePermitirCargaPesadaAcimaDoPiso() {
        when(paleteRepository.findById(1L)).thenReturn(Optional.of(paletePesado));
        when(enderecoRepository.findById(20L)).thenReturn(Optional.of(vagaNivelSuperior));

        CapacidadeExcedidaException exception = assertThrows(
                CapacidadeExcedidaException.class,
                () -> alocacaoService.alocarPalete(1L, 20L)
        );

        assertTrue(exception.getMessage().contains("Nível 1 (Piso)"));
        verify(paleteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve impedir alocação em vaga que já está ocupada")
    void naoDeveAlocarEmVagaOcupada() {
        vagaPiso.setOcupado(true);

        when(paleteRepository.findById(1L)).thenReturn(Optional.of(paletePesado));
        when(enderecoRepository.findById(10L)).thenReturn(Optional.of(vagaPiso));

        assertThrows(
                EnderecoOcupadoException.class,
                () -> alocacaoService.alocarPalete(1L, 10L)
        );

        verify(paleteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve alocar palete no solo com sucesso e atualizar status para ARMAZENADO")
    void deveAlocarComSucessoNoSolo() {
        when(paleteRepository.findById(1L)).thenReturn(Optional.of(paletePesado));
        when(enderecoRepository.findById(10L)).thenReturn(Optional.of(vagaPiso));
        when(paleteRepository.save(any(Palete.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Palete paleteAlocado = alocacaoService.alocarPalete(1L, 10L);

        assertEquals(StatusPalete.ARMAZENADO, paleteAlocado.getStatus());
        assertEquals(vagaPiso, paleteAlocado.getEndereco());
        assertTrue(vagaPiso.getOcupado());
        assertNotNull(paleteAlocado.getAlocadoEm());
        verify(enderecoRepository, times(1)).save(vagaPiso);
        verify(paleteRepository, times(1)).save(paletePesado);
    }

    @Test
    @DisplayName("Deve ativar fallback heurístico determinístico quando IA falhar ou retornar vaga inexistente")
    void deveAcionarFallbackHeuristicoSeIaFalhar() {
        when(paleteRepository.findById(1L)).thenReturn(Optional.of(paletePesado));
        when(enderecoRepository.buscarVagasDisponiveis(any(), any(), eq(1)))
                .thenReturn(List.of(vagaPiso));

        // Simula erro de API da IA (ex: timeout ou 429)
        when(aiClient.obterSugestaoAlocacao(anyString())).thenThrow(new RuntimeException("API indisponível"));

        AlocacaoDecisaoDTO decisao = alocacaoService.sugerirVagaInteligente(1L);

        assertNotNull(decisao);
        assertEquals("R01-B01-N01-P01", decisao.vagaSugerida());
        assertEquals("MOTOR_HEURISTICO_LOCAL", decisao.origemDecisao());
        assertTrue(decisao.justificativa().contains("Alocação determinística"));
    }
}