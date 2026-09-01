package com.wms.engine.controller;

import com.wms.engine.service.ParametroSistemaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/configuracoes")
public class AdminConfiguracaoController {

    private final ParametroSistemaService parametroService;

    public AdminConfiguracaoController(ParametroSistemaService parametroService) {
        this.parametroService = parametroService;
    }

    @GetMapping
    public String exibirConfiguracoes(Model model) {
        model.addAttribute("parametros", parametroService.listarTodos());
        model.addAttribute("aiApiKey", parametroService.obterValorPorChave("AI_API_KEY", ""));
        model.addAttribute("aiProvider", parametroService.obterValorPorChave("AI_PROVIDER", "MISTRAL"));
        return "admin/configuracoes";
    }

    @PostMapping("/ai")
    public String salvarConfiguracaoIA(@RequestParam(value = "apiKey", required = false) String apiKey,
                                       @RequestParam(value = "provider", defaultValue = "MISTRAL") String provider,
                                       RedirectAttributes redirectAttributes) {
        try {
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                parametroService.atualizarValor("AI_API_KEY", apiKey.trim());
            }
            parametroService.atualizarValor("AI_PROVIDER", provider);
            redirectAttributes.addFlashAttribute("sucesso", "Configurações do Motor de IA salvas com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar parâmetros: " + e.getMessage());
        }
        return "redirect:/admin/configuracoes";
    }

    @PostMapping("/ai/limpar")
    public String limparConfiguracaoIA(RedirectAttributes redirectAttributes) {
        try {
            parametroService.limparValor("AI_API_KEY");
            redirectAttributes.addFlashAttribute("sucesso", "Chave de API de IA desvinculada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao limpar chave: " + e.getMessage());
        }
        return "redirect:/admin/configuracoes";
    }

    @PostMapping("/excluir")
    public String excluirParametro(@RequestParam("chave") String chave, RedirectAttributes redirectAttributes) {
        try {
            parametroService.excluirParametro(chave);
            redirectAttributes.addFlashAttribute("sucesso", "Parâmetro " + chave + " excluído com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao excluir parâmetro: " + e.getMessage());
        }
        return "redirect:/admin/configuracoes";
    }
}