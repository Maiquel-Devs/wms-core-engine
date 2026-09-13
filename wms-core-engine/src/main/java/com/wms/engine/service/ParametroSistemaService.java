package com.wms.engine.service;

import com.wms.engine.model.ParametroSistema;
import com.wms.engine.repository.ParametroSistemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço responsável pelo gerenciamento de configurações dinâmicas globais (Parâmetros do Sistema).
 * Permite que administradores alterem comportamentos do WMS em tempo de execução
 * (ex: limiares de peso, chaves de API, prompts da IA) sem necessidade de reimplantação (deploy).
 */
@Service
public class ParametroSistemaService {

    private static final Logger log = LoggerFactory.getLogger(ParametroSistemaService.class);

    private final ParametroSistemaRepository repository;

    public ParametroSistemaService(ParametroSistemaRepository repository) {
        this.repository = repository;
    }

    /**
     * Retorna todos os parâmetros de sistema atualmente cadastrados no banco de dados.
     *
     * @return Lista de configurações dinâmicas.
     */
    @Transactional(readOnly = true)
    public List<ParametroSistema> listarTodos() {
        return repository.findAll();
    }

    /**
     * Busca o valor em texto de um parâmetro específico pela sua chave de identificação.
     * Retorna um valor de contingência (fallback) caso a chave não seja encontrada,
     * garantindo que a aplicação não quebre por falta de configuração.
     *
     * @param chave       A chave única do parâmetro (ex: "IA_PROMPT_ALOCACAO").
     * @param valorPadrao O valor padrão a ser retornado caso o parâmetro não exista na base.
     * @return O valor configurado ou o valor de fallback.
     */
    @Transactional(readOnly = true)
    public String obterValorPorChave(String chave, String valorPadrao) {
        return repository.findByChave(chave)
                .map(ParametroSistema::getValor)
                .orElse(valorPadrao);
    }

    /**
     * Insere ou atualiza o valor de um parâmetro no sistema (Upsert).
     * Aplica o método trim() para evitar que espaços em branco acidentais causem bugs
     * na leitura de configurações estritas (como chaves de API).
     *
     * @param chave     A chave de identificação do parâmetro.
     * @param novoValor O novo conteúdo em texto a ser associado à chave.
     */
    @Transactional
    public void atualizarValor(String chave, String novoValor) {
        ParametroSistema parametro = repository.findByChave(chave)
                .orElseGet(() -> {
                    ParametroSistema novo = new ParametroSistema();
                    novo.setChave(chave);
                    novo.setDescricao("Configuração dinâmica do sistema");
                    return novo;
                });

        parametro.setValor(novoValor != null ? novoValor.trim() : "");
        parametro.setAtualizadoEm(LocalDateTime.now());
        repository.save(parametro);

        log.info("[AUDIT] Parâmetro do sistema '{}' inserido/atualizado.", chave);
    }

    /**
     * Limpa (esvazia) o valor de um parâmetro existente sem deletar sua chave estrutural
     * do banco de dados.
     *
     * @param chave A chave do parâmetro a ser limpo.
     */
    @Transactional
    public void limparValor(String chave) {
        repository.findByChave(chave).ifPresent(parametro -> {
            parametro.setValor("");
            parametro.setAtualizadoEm(LocalDateTime.now());
            repository.save(parametro);
            log.info("[AUDIT] Valor do parâmetro do sistema '{}' foi limpo.", chave);
        });
    }

    /**
     * Remove fisicamente um parâmetro do sistema do banco de dados.
     *
     * @param chave A chave do parâmetro a ser excluído.
     */
    @Transactional
    public void excluirParametro(String chave) {
        repository.findByChave(chave).ifPresent(parametro -> {
            repository.delete(parametro);
            log.warn("[AUDIT] Parâmetro do sistema '{}' excluído permanentemente.", chave);
        });
    }
}