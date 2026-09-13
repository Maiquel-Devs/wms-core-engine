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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Suíte de testes unitários isolados para a classe {@link AlocacaoService}.
 * Utiliza o Mockito para simular o comportamento dos repositórios e clientes externos,
 * validando as regras de negócio puras de alocação, validações de piso e fallbacks de IA.
 */
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
        paletePesado.setPesoTotalKg(new BigDecimal("650.00")); // Acima do limite de 500kg
        paletePesado.setVolumeTotalM3(new BigDecimal("1.2000"));
        paletePesado.setStatus(StatusPalete.RECEBIDO_DOCA);

        vagaPiso = new EnderecoEstoque();
        vagaPiso.setId(10L);
        vagaPiso.setCodigoEndereco("R01-B01-N01-P01");
        vagaPiso.setNivel(1); // Nível 1 (Piso)
        vagaPiso.setCapacidadePesoKg(new BigDecimal("1500.00"));
        vagaPiso.setCapacidadeVolumeM3(new BigDecimal("2.0000"));
        vagaPiso.setOcupado(false);

        vagaNivelSuperior = new EnderecoEstoque();
        vagaNivelSuperior.setId(20L);
        vagaNivelSuperior.setCodigoEndereco("R01-B01-N02-P01");
        vagaNivelSuperior.setNivel(2); // Nível superior
        vagaNivelSuperior.setCapacidadePesoKg(new BigDecimal("800.00"));
        vagaNivelSuperior.setCapacidadeVolumeM3(new BigDecimal("2.0000"));
        vagaNivelSuperior.setOcupado(false);
    }

    @Test
    @DisplayName("Deve barrar com CapacidadeExcedidaException carga > 500kg se for alocada em nível superior ao solo")
    void naoDevePermitirCargaPesadaAcimaDoPiso() {
        // Arrange
        when(paleteRepository.findById(1L)).thenReturn(Optional.of(paletePesado));
        when(enderecoRepository.findById(20L)).thenReturn(Optional.of(vagaNivelSuperior));

        // Act & Assert
        assertThatThrownBy(() -> alocacaoService.alocarPalete(1L, 20L))
                .as("Cargas superiores a 500kg não podem ser direcionadas para andares superiores")
                .isInstanceOf(CapacidadeExcedidaException.class)
                .hasMessageContaining("Nível 1 (Piso)");

        verify(paleteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve impedir alocação em vaga que já está ocupada")
    void naoDeveAlocarEmVagaOcupada() {
        // Arrange
        vagaPiso.setOcupado(true);

        when(paleteRepository.findById(1L)).thenReturn(Optional.of(paletePesado));
        when(enderecoRepository.findById(10L)).thenReturn(Optional.of(vagaPiso));

        // Act & Assert
        assertThatThrownBy(() -> alocacaoService.alocarPalete(1L, 10L))
                .as("O sistema deve impedir conflitos de endereçamento em vagas já ocupadas")
                .isInstanceOf(EnderecoOcupadoException.class);

        verify(paleteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve alocar palete no solo com sucesso e atualizar status para ARMAZENADO")
    void deveAlocarComSucessoNoSolo() {
        // Arrange
        when(paleteRepository.findById(1L)).thenReturn(Optional.of(paletePesado));
        when(enderecoRepository.findById(10L)).thenReturn(Optional.of(vagaPiso));
        when(paleteRepository.save(any(Palete.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Palete paleteAlocado = alocacaoService.alocarPalete(1L, 10L);

        // Assert
        assertThat(paleteAlocado.getStatus())
                .as("O status do palete deve transicionar para ARMAZENADO")
                .isEqualTo(StatusPalete.ARMAZENADO);

        assertThat(paleteAlocado.getEndereco())
                .as("O palete deve estar vinculado ao endereço físico correto")
                .isEqualTo(vagaPiso);

        assertThat(vagaPiso.getOcupado())
                .as("A vaga física deve ser marcada como ocupada")
                .isTrue();

        assertThat(paleteAlocado.getAlocadoEm())
                .as("O timestamp de alocação deve ser gerado")
                .isNotNull();

        verify(enderecoRepository, times(1)).save(vagaPiso);
        verify(paleteRepository, times(1)).save(paletePesado);
    }

    @Test
    @DisplayName("Deve ativar fallback heurístico determinístico quando IA falhar ou retornar vaga inexistente")
    void deveAcionarFallbackHeuristicoSeIaFalhar() {
        // Arrange
        when(paleteRepository.findById(1L)).thenReturn(Optional.of(paletePesado));
        when(enderecoRepository.buscarVagasDisponiveis(any(), any(), eq(1)))
                .thenReturn(List.of(vagaPiso));

        // Simula falha catastrófica ou timeout na API externa de LLM
        when(aiClient.obterSugestaoAlocacao(anyString())).thenThrow(new RuntimeException("API indisponível"));

        // Act
        AlocacaoDecisaoDTO decisao = alocacaoService.sugerirVagaInteligente(1L);

        // Assert
        assertThat(decisao)
                .as("O serviço deve retornar uma decisão de contingência e nunca retornar nulo")
                .isNotNull();

        assertThat(decisao.vagaSugerida())
                .as("A vaga sugerida pelo fallback deve corresponder à melhor vaga física real encontrada")
                .isEqualTo("R01-B01-N01-P01");

        assertThat(decisao.origemDecisao())
                .as("A origem da decisão informada no DTO deve indicar claramente o motor local")
                .isEqualTo("MOTOR_HEURISTICO_LOCAL");

        assertThat(decisao.justificativa())
                .as("A justificativa técnica deve detalhar o acionamento do algoritmo determinístico")
                .contains("Alocação determinística");
    }
}