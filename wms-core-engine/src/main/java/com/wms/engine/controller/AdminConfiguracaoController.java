package com.wms.engine.controller;

import com.wms.engine.service.ParametroSistemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador responsável pelo painel de configurações administrativas.
 * Gerencia os parâmetros do sistema, incluindo configurações e chaves da IA.
 * Nota: O acesso a este controlador é restrito a perfis ADMIN via SecurityConfig.
 */
@Controller
@RequestMapping("/admin/configuracoes")
public class AdminConfiguracaoController {

    private static final Logger log = LoggerFactory.getLogger(AdminConfiguracaoController.class);

    // Constantes para evitar "magic strings" e erros de digitação nas chaves do banco
    private static final String PARAM_AI_API_KEY = "AI_API_KEY";
    private static final String PARAM_AI_PROVIDER = "AI_PROVIDER";
    private static final String DEFAULT_PROVIDER = "MISTRAL";

    private final ParametroSistemaService parametroService;

    public AdminConfiguracaoController(ParametroSistemaService parametroService) {
        this.parametroService = parametroService;
    }

    /**
     * Renderiza o painel de configurações com os parâmetros atuais do sistema.
     */
    @GetMapping
    public String exibirConfiguracoes(Model model) {
        model.addAttribute("parametros", parametroService.listarTodos());
        model.addAttribute("aiApiKey", parametroService.obterValorPorChave(PARAM_AI_API_KEY, ""));
        model.addAttribute("aiProvider", parametroService.obterValorPorChave(PARAM_AI_PROVIDER, DEFAULT_PROVIDER));

        return "admin/configuracoes";
    }

    /**
     * Salva ou atualiza os parâmetros de configuração da IA (Provedor e Chave de API).
     */
    @PostMapping("/ai")
    public String salvarConfiguracaoIA(@RequestParam(value = "apiKey", required = false) String apiKey,
                                       @RequestParam(value = "provider", defaultValue = DEFAULT_PROVIDER) String provider,
                                       RedirectAttributes redirectAttributes) {
        try {
            // Cláusula de guarda: Só atualiza a chave se uma string válida for fornecida
            if (apiKey != null && !apiKey.isBlank()) {
                parametroService.atualizarValor(PARAM_AI_API_KEY, apiKey.trim());
            }

            parametroService.atualizarValor(PARAM_AI_PROVIDER, provider);
            redirectAttributes.addFlashAttribute("sucesso", "Configurações do Motor de IA salvas com sucesso!");

        } catch (Exception e) {
            log.error("Falha ao salvar parâmetros de configuração da IA: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar parâmetros: " + e.getMessage());
        }

        return "redirect:/admin/configuracoes";
    }

    /**
     * Limpa a Chave de API da IA do banco de dados por motivos de segurança.
     * Isso força o sistema a utilizar o motor heurístico local de contingência.
     */
    @PostMapping("/ai/limpar")
    public String limparConfiguracaoIA(RedirectAttributes redirectAttributes) {
        try {
            parametroService.limparValor(PARAM_AI_API_KEY);
            redirectAttributes.addFlashAttribute("sucesso", "Chave de API de IA desvinculada com sucesso!");

        } catch (Exception e) {
            log.error("Falha ao limpar a Chave de API da IA: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("erro", "Erro ao limpar chave: " + e.getMessage());
        }

        return "redirect:/admin/configuracoes";
    }

    /**
     * Exclui um parâmetro de configuração específico do sistema com base na sua chave.
     */
    @PostMapping("/excluir")
    public String excluirParametro(@RequestParam("chave") String chave, RedirectAttributes redirectAttributes) {
        try {
            parametroService.excluirParametro(chave);
            redirectAttributes.addFlashAttribute("sucesso", "Parâmetro " + chave + " excluído com sucesso!");

        } catch (Exception e) {
            log.error("Falha ao excluir o parâmetro do sistema [{}]: {}", chave, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("erro", "Erro ao excluir parâmetro: " + e.getMessage());
        }

        return "redirect:/admin/configuracoes";
    }
}