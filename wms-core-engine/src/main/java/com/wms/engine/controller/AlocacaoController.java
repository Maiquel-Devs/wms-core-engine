package com.wms.engine.controller;

import com.wms.engine.client.AiAllocationClient;
import com.wms.engine.dto.NovoPaleteDTO;
import com.wms.engine.dto.ia.AlocacaoDecisaoDTO;
import com.wms.engine.exception.CapacidadeExcedidaException;
import com.wms.engine.model.EnderecoEstoque;
import com.wms.engine.model.Palete;
import com.wms.engine.model.Produto;
import com.wms.engine.model.StatusPalete;
import com.wms.engine.repository.EnderecoEstoqueRepository;
import com.wms.engine.repository.PaleteRepository;
import com.wms.engine.repository.ProdutoRepository;
import com.wms.engine.service.AlocacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controlador principal responsável pelo painel operacional do armazém,
 * recebimento de mercadorias na doca, alocação física e expedição.
 */
@Controller
@RequestMapping("/")
public class AlocacaoController {

    private static final Logger log = LoggerFactory.getLogger(AlocacaoController.class);

    // Constantes para evitar magic strings no roteamento e nas mensagens da interface
    private static final String REDIRECT_HOME = "redirect:/";
    private static final String VIEW_PAINEL = "painel";
    private static final String ATTR_SUCESSO = "sucesso";
    private static final String ATTR_ERRO = "erro";
    private static final String ATTR_INFO = "info";

    private static final String MOCK_CONTEXTO_IA = """
            Temos um palete recém-chegado na doca:
            - Produto: Tambores de Solvente Industrial (Inflamável / Químico)
            - Peso total: 850 kg
            
            Vagas disponíveis no galpão:
            1. Vaga A1 (Nível 1, solo) - Suporta até 1000 kg.
            2. Vaga B3 (Nível 3, altura elevada) - Suporta até 500 kg.
            3. Vaga C1 (Nível 1, baias reforçadas para inflamáveis) - Suporta até 1200 kg.
            
            Analise e retorne estritamente um JSON com o seguinte formato:
            {"vagaSugerida": "C1", "origemDecisao": "IA_LOGISTICA", "justificativa": "sua justificativa técnica"}
            """;

    private final AlocacaoService alocacaoService;
    private final PaleteRepository paleteRepository;
    private final EnderecoEstoqueRepository enderecoRepository;
    private final ProdutoRepository produtoRepository;
    private final AiAllocationClient aiClient;

    public AlocacaoController(AlocacaoService alocacaoService,
                              PaleteRepository paleteRepository,
                              EnderecoEstoqueRepository enderecoRepository,
                              ProdutoRepository produtoRepository,
                              AiAllocationClient aiClient) {
        this.alocacaoService = alocacaoService;
        this.paleteRepository = paleteRepository;
        this.enderecoRepository = enderecoRepository;
        this.produtoRepository = produtoRepository;
        this.aiClient = aiClient;
    }

    /**
     * Endpoint técnico para validar a comunicação pura e formatação de resposta com o provedor de IA.
     */
    @GetMapping("/api/alocacao/teste-ia")
    @ResponseBody
    public ResponseEntity<String> testarIntegracaoIA() {
        try {
            String respostaIA = aiClient.obterSugestaoAlocacao(MOCK_CONTEXTO_IA);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE + "; charset=UTF-8")
                    .body(respostaIA);

        } catch (Exception e) {
            log.error("Falha no teste direto de integração com a IA: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("{\"erro\": \"Falha ao comunicar com o serviço de IA.\"}");
        }
    }

    /**
     * Carrega o painel operacional contendo as métricas físicas, status da doca e mapa de estantes.
     */
    @GetMapping
    public String exibirPainel(Model model) {
        List<EnderecoEstoque> enderecos = enderecoRepository.findAll(Sort.by("codigoEndereco"));
        List<Palete> paletesPendentes = paleteRepository.findByStatus(StatusPalete.RECEBIDO_DOCA);
        List<Palete> paletesAlocados = paleteRepository.findByStatus(StatusPalete.ARMAZENADO);
        List<Produto> produtos = produtoRepository.findAll();

        model.addAttribute("enderecos", enderecos);
        model.addAttribute("paletesPendentes", paletesPendentes);
        model.addAttribute("paletesAlocados", paletesAlocados);
        model.addAttribute("produtos", produtos);

        // Garante que o objeto do formulário exista no modelo para o modal de recebimento
        if (!model.containsAttribute("novoPalete")) {
            model.addAttribute("novoPalete", new NovoPaleteDTO());
        }

        return VIEW_PAINEL;
    }

    /**
     * Registra a entrada de um novo palete/lote na doca com validação de unicidade.
     */
    @PostMapping("/paletes/receber")
    public String receberPaleteNaDoca(@ModelAttribute("novoPalete") NovoPaleteDTO dto,
                                      RedirectAttributes redirectAttributes) {
        try {
            String codigoLoteFormatado = dto.getCodigoLote() != null ? dto.getCodigoLote().trim().toUpperCase() : "";

            // Cláusula de guarda: Código do lote em branco
            if (codigoLoteFormatado.isBlank()) {
                redirectAttributes.addFlashAttribute(ATTR_ERRO, "O código do lote é obrigatório.");
                return REDIRECT_HOME;
            }

            // Cláusula de guarda: Prevenção contra lote duplicado
            if (paleteRepository.existsByCodigoLote(codigoLoteFormatado)) {
                redirectAttributes.addFlashAttribute(ATTR_ERRO, "Já existe um palete cadastrado com o lote: " + codigoLoteFormatado);
                return REDIRECT_HOME;
            }

            Produto produto = produtoRepository.findById(dto.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto informado não foi encontrado."));

            Palete palete = new Palete();
            palete.setCodigoLote(codigoLoteFormatado);
            palete.setProduto(produto);
            palete.setQuantidadeItens(dto.getQuantidadeItens());
            palete.setPesoTotalKg(dto.getPesoTotalKg());
            palete.setVolumeTotalM3(dto.getVolumeTotalM3());
            palete.setStatus(StatusPalete.RECEBIDO_DOCA);

            paleteRepository.save(palete);
            redirectAttributes.addFlashAttribute(ATTR_SUCESSO, "Lote " + palete.getCodigoLote() + " recebido na doca com sucesso!");

        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERRO, "Não foi possível cadastrar: o código de lote informado já está em uso.");
        } catch (Exception e) {
            log.error("Erro inesperado ao registrar lote na doca: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ATTR_ERRO, "Erro ao dar entrada no lote: " + e.getMessage());
        }

        return REDIRECT_HOME;
    }

    /**
     * Aciona a sugestão inteligente de vaga para um palete pendente (IA com Fallback Heurístico).
     */
    @GetMapping("/paletes/{id}/sugerir")
    public String sugerirVagaParaPalete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            AlocacaoDecisaoDTO sugestao = alocacaoService.sugerirVagaInteligente(id);

            String iconeOrigem = "IA_LOGISTICA".equalsIgnoreCase(sugestao.origemDecisao())
                    ? "🤖 [IA Logística]"
                    : "⚡ [Motor Heurístico - Contingência]";

            redirectAttributes.addFlashAttribute(ATTR_INFO,
                    String.format("%s Posição recomendada: %s. Justificativa: %s",
                            iconeOrigem, sugestao.vagaSugerida(), sugestao.justificativa()
                    ));

        } catch (CapacidadeExcedidaException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERRO, "Alerta Estrutural: " + e.getMessage());
        } catch (Exception e) {
            log.error("Falha ao sugerir vaga para o palete ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ATTR_ERRO, "Não foi possível sugerir vaga: " + e.getMessage());
        }

        return REDIRECT_HOME;
    }

    /**
     * Efetiva a alocação física de um palete em uma estante com controle de concorrência.
     */
    @PostMapping("/paletes/alocar")
    public String alocarPaleteManual(@RequestParam("paleteId") Long paleteId,
                                     @RequestParam("enderecoId") Long enderecoId,
                                     RedirectAttributes redirectAttributes) {
        try {
            alocacaoService.alocarPalete(paleteId, enderecoId);
            redirectAttributes.addFlashAttribute(ATTR_SUCESSO, "Palete alocado na posição física com sucesso!");

        } catch (Exception e) {
            log.error("Falha ao alocar palete ID {} no endereço ID {}: {}", paleteId, enderecoId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ATTR_ERRO, "Falha na alocação: " + e.getMessage());
        }

        return REDIRECT_HOME;
    }

    /**
     * Realiza a expedição de um palete, liberando sua vaga física e finalizando seu ciclo de vida.
     */
    @PostMapping("/paletes/{id}/desalocar")
    public String desalocarPalete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            alocacaoService.desalocarPalete(id);
            redirectAttributes.addFlashAttribute(ATTR_SUCESSO, "Palete expedido com sucesso e vaga física liberada!");

        } catch (Exception e) {
            log.error("Falha ao expedir o palete ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ATTR_ERRO, "Falha ao dar baixa no palete: " + e.getMessage());
        }

        return REDIRECT_HOME;
    }

    /**
     * Exclui de forma segura um palete que ainda está pendente na doca de recebimento.
     */
    @PostMapping("/paletes/{id}/excluir")
    public String excluirPaleteDaDoca(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            alocacaoService.excluirPaleteDaDoca(id);
            redirectAttributes.addFlashAttribute(ATTR_SUCESSO, "Palete removido da doca com sucesso!");

        } catch (Exception e) {
            log.error("Falha ao excluir o palete pendente ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ATTR_ERRO, "Não foi possível excluir o palete: " + e.getMessage());
        }

        return REDIRECT_HOME;
    }
}