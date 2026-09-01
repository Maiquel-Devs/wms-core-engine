package com.wms.engine.service;

import com.wms.engine.model.ParametroSistema;
import com.wms.engine.repository.ParametroSistemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParametroSistemaService {

    private final ParametroSistemaRepository repository;

    public ParametroSistemaService(ParametroSistemaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ParametroSistema> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public String obterValorPorChave(String chave, String valorPadrao) {
        return repository.findByChave(chave)
                .map(ParametroSistema::getValor)
                .orElse(valorPadrao);
    }

    @Transactional
    public void atualizarValor(String chave, String novoValor) {
        ParametroSistema parametro = repository.findByChave(chave)
                .orElseGet(() -> {
                    ParametroSistema novo = new ParametroSistema();
                    novo.setChave(chave);
                    novo.setDescricao("Configuração do sistema");
                    return novo;
                });

        parametro.setValor(novoValor != null ? novoValor.trim() : "");
        parametro.setAtualizadoEm(LocalDateTime.now());
        repository.save(parametro);
    }

    @Transactional
    public void limparValor(String chave) {
        repository.findByChave(chave).ifPresent(parametro -> {
            parametro.setValor("");
            parametro.setAtualizadoEm(LocalDateTime.now());
            repository.save(parametro);
        });
    }

    @Transactional
    public void excluirParametro(String chave) {
        repository.findByChave(chave).ifPresent(repository::delete);
    }
}