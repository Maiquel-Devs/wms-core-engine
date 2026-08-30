package com.wms.engine.controller;

import com.wms.engine.dto.AlocacaoRequestDTO;
import com.wms.engine.dto.PaleteRecebimentoDTO;
import com.wms.engine.model.EnderecoEstoque;
import com.wms.engine.model.Palete;
import com.wms.engine.model.Produto;
import com.wms.engine.repository.EnderecoEstoqueRepository;
import com.wms.engine.repository.PaleteRepository;
import com.wms.engine.repository.ProdutoRepository;
import com.wms.engine.service.AlocacaoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/")
public class AlocacaoController {

    private final AlocacaoService alocacaoService;
    private final EnderecoEstoqueRepository enderecoRepository;
    private final PaleteRepository paleteRepository;
    private final ProdutoRepository produtoRepository;

    public AlocacaoController(AlocacaoService alocacaoService,
                              EnderecoEstoqueRepository enderecoRepository,
                              PaleteRepository paleteRepository,
                              ProdutoRepository produtoRepository) {
        this.alocacaoService = alocacaoService;
        this.enderecoRepository = enderecoRepository;
        this.paleteRepository = paleteRepository;
        this.produtoRepository = produtoRepository;
    }

    // Painel Operacional: Grid do armazém e paletes pendentes
    @GetMapping
    public String painelArmazem(Model model) {
        List<EnderecoEstoque> enderecos = enderecoRepository.findAll();
        List<Palete> pendentes = paleteRepository.findByEnderecoIsNull();
        List<Palete> alocados = paleteRepository.findByEnderecoIsNotNull();
        List<Produto> produtos = produtoRepository.findAll();

        model.addAttribute("enderecos", enderecos);
        model.addAttribute("paletesPendentes", pendentes);
        model.addAttribute("paletesAlocados", alocados);
        model.addAttribute("produtos", produtos);
        model.addAttribute("novoPalete", new PaleteRecebimentoDTO("", null, 1, null, null));

        return "painel";
    }

    // Recebimento de novo lote/palete no galpão
    @PostMapping("/paletes/receber")
    public String receberPalete(@Valid @ModelAttribute("novoPalete") PaleteRecebimentoDTO dto,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("erro", "Dados inválidos para recebimento do lote.");
            return "redirect:/";
        }

        Produto produto = produtoRepository.findById(dto.produtoId())
                .orElseThrow(() -> new IllegalArgumentException("Produto inválido"));

        Palete palete = new Palete();
        palete.setCodigoLote(dto.codigoLote());
        palete.setProduto(produto);
        palete.setQuantidadeItens(dto.quantidadeItens());
        palete.setPesoTotalKg(dto.pesoTotalKg());
        palete.setVolumeTotalM3(dto.volumeTotalM3());

        paleteRepository.save(palete);
        redirectAttributes.addFlashAttribute("sucesso", "Lote " + palete.getCodigoLote() + " recebido na doca!");
        return "redirect:/";
    }

    // Sugestão determinística de alocação por algoritmo
    @GetMapping("/paletes/{id}/sugerir")
    public String sugerirVaga(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Palete palete = paleteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Palete não encontrado"));

        try {
            EnderecoEstoque sugestao = alocacaoService.sugerirMelhorEndereco(palete);
            redirectAttributes.addFlashAttribute("info",
                    "Melhor vaga para o lote " + palete.getCodigoLote() + ": " + sugestao.getCodigoEndereco() +
                            " (Nível " + sugestao.getNivel() + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }

        return "redirect:/";
    }

    // Execução da alocação física no endereço
    @PostMapping("/paletes/alocar")
    public String alocarPalete(@ModelAttribute AlocacaoRequestDTO dto, RedirectAttributes redirectAttributes) {
        try {
            alocacaoService.alocarPalete(dto.paleteId(), dto.enderecoId());
            redirectAttributes.addFlashAttribute("sucesso", "Palete alocado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Falha na alocação: " + e.getMessage());
        }
        return "redirect:/";
    }

    // Desalocar (Remoção / Expedição)
    @PostMapping("/paletes/{id}/desalocar")
    public String desalocarPalete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            alocacaoService.desalocarPalete(id);
            redirectAttributes.addFlashAttribute("sucesso", "Endereço desocupado e palete liberado!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao desocupar: " + e.getMessage());
        }
        return "redirect:/";
    }
}