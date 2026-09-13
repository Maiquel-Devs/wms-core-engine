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
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/")
public class AlocacaoController {

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
     * Endpoint técnico para validar comunicação pura e resposta com o provedor de IA
     */
    @GetMapping("/api/alocacao/teste-ia")
    @ResponseBody
    public ResponseEntity<String> testarIntegracaoIA() {
        String contextoArmazem = """
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

        try {
            String respostaIA = aiClient.obterSugestaoAlocacao(contextoArmazem);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(respostaIA);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("{\"erro\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Carrega o painel operacional com as métricas físicas, doca e mapa de estantes
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

        if (!model.containsAttribute("novoPalete")) {
            model.addAttribute("novoPalete", new NovoPaleteDTO());
        }

        return "painel";
    }

    /**
     * Dá entrada em novos lotes/paletes na doca de recebimento
     */
    @PostMapping("/paletes/receber")
    public String receberPaleteNaDoca(@ModelAttribute("novoPalete") NovoPaleteDTO dto,
                                      RedirectAttributes redirectAttributes) {
        try {
            Produto produto = produtoRepository.findById(dto.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto informado não foi encontrado."));

            Palete palete = new Palete();
            palete.setCodigoLote(dto.getCodigoLote());
            palete.setProduto(produto);
            palete.setQuantidadeItens(dto.getQuantidadeItens());
            palete.setPesoTotalKg(dto.getPesoTotalKg());
            palete.setVolumeTotalM3(dto.getVolumeTotalM3());
            palete.setStatus(StatusPalete.RECEBIDO_DOCA);

            paleteRepository.save(palete);
            redirectAttributes.addFlashAttribute("sucesso", "Lote " + palete.getCodigoLote() + " recebido na doca com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao dar entrada no lote: " + e.getMessage());
        }
        return "redirect:/";
    }

    /**
     * Aciona a sugestão inteligente da vaga para o palete da doca (IA com Fallback Heurístico)
     */
    @GetMapping("/paletes/{id}/sugerir")
    public String sugerirVagaParaPalete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            AlocacaoDecisaoDTO sugestao = alocacaoService.sugerirVagaInteligente(id);

            String iconeOrigem = "IA_LOGISTICA".equalsIgnoreCase(sugestao.origemDecisao())
                    ? "🤖 [IA Logística]"
                    : "⚡ [Motor Heurístico - Contingência]";

            redirectAttributes.addFlashAttribute("info",
                    String.format("%s Posição recomendada: %s. Justificativa: %s",
                            iconeOrigem,
                            sugestao.vagaSugerida(),
                            sugestao.justificativa()
                    ));
        } catch (CapacidadeExcedidaException e) {
            redirectAttributes.addFlashAttribute("erro", "Alerta Estrutural: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Não foi possível sugerir vaga: " + e.getMessage());
        }
        return "redirect:/";
    }

    /**
     * Realiza a alocação do palete na vaga física com controle de concorrência
     */
    @PostMapping("/paletes/alocar")
    public String alocarPaleteManual(@RequestParam("paleteId") Long paleteId,
                                     @RequestParam("enderecoId") Long enderecoId,
                                     RedirectAttributes redirectAttributes) {
        try {
            alocacaoService.alocarPalete(paleteId, enderecoId);
            redirectAttributes.addFlashAttribute("sucesso", "Palete alocado na posição física com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Falha na alocação: " + e.getMessage());
        }
        return "redirect:/";
    }

    /**
     * Realiza a baixa por expedição liberando o endereço e finalizando o ciclo do lote
     */
    @PostMapping("/paletes/{id}/desalocar")
    public String desalocarPalete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            alocacaoService.desalocarPalete(id);
            redirectAttributes.addFlashAttribute("sucesso", "Palete expedido com sucesso e vaga física liberada!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Falha ao dar baixa no palete: " + e.getMessage());
        }
        return "redirect:/";
    }
}