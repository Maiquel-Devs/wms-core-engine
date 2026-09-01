package com.wms.engine.controller;

import com.wms.engine.dto.NovoPaleteDTO;
import com.wms.engine.model.EnderecoEstoque;
import com.wms.engine.model.Palete;
import com.wms.engine.model.Produto;
import com.wms.engine.repository.EnderecoEstoqueRepository;
import com.wms.engine.repository.PaleteRepository;
import com.wms.engine.repository.ProdutoRepository;
import com.wms.engine.service.AlocacaoService;
import org.springframework.data.domain.Sort;
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

    public AlocacaoController(AlocacaoService alocacaoService,
                              PaleteRepository paleteRepository,
                              EnderecoEstoqueRepository enderecoRepository,
                              ProdutoRepository produtoRepository) {
        this.alocacaoService = alocacaoService;
        this.paleteRepository = paleteRepository;
        this.enderecoRepository = enderecoRepository;
        this.produtoRepository = produtoRepository;
    }

    /**
     * Carrega o painel operacional com as métricas físicas, doca e mapa de estantes
     */
    @GetMapping
    public String exibirPainel(Model model) {
        List<EnderecoEstoque> enderecos = enderecoRepository.findAll(Sort.by("codigoEndereco"));
        List<Palete> paletesPendentes = paleteRepository.findByEnderecoIsNull();
        List<Palete> paletesAlocados = paleteRepository.findByEnderecoIsNotNull();
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

            paleteRepository.save(palete);
            redirectAttributes.addFlashAttribute("sucesso", "Lote " + palete.getCodigoLote() + " recebido na doca com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao dar entrada no lote: " + e.getMessage());
        }
        return "redirect:/";
    }

    /**
     * Executa a rota do botão 'Sugerir Vaga' acionando o algoritmo determinístico
     */
    @GetMapping("/paletes/{id}/sugerir")
    public String sugerirVagaParaPalete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            EnderecoEstoque enderecoSugerido = alocacaoService.sugerirVaga(id);
            redirectAttributes.addFlashAttribute("info",
                    String.format("Sugestão da Engenharia: A melhor posição calculada para o palete é [%s] (Nível %d - Capacidade: %s kg / %s m³).",
                            enderecoSugerido.getCodigoEndereco(),
                            enderecoSugerido.getNivel(),
                            enderecoSugerido.getCapacidadePesoKg(),
                            enderecoSugerido.getCapacidadeVolumeM3()
                    ));
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
     * Realiza a baixa do palete liberando o endereço no galpão
     */
    @PostMapping("/paletes/{id}/desalocar")
    public String desalocarPalete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            alocacaoService.desalocarPalete(id);
            redirectAttributes.addFlashAttribute("sucesso", "Palete retirado da prateleira e vaga liberada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Falha ao dar baixa no palete: " + e.getMessage());
        }
        return "redirect:/";
    }
}