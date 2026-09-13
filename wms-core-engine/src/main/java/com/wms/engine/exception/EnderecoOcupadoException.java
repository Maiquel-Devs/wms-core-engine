package com.wms.engine.exception;

/**
 * Exceção de domínio de negócio lançada quando há uma tentativa de alocar um palete
 * em um endereço físico (vaga/estante) que já se encontra ocupado ou indisponível
 * para novos armazenamentos.
 */
public class EnderecoOcupadoException extends RuntimeException {

    /**
     * Constrói a exceção com uma mensagem de erro detalhando o conflito de ocupação.
     *
     * @param mensagem A descrição técnica ou de negócio informando qual endereço já está ocupado.
     */
    public EnderecoOcupadoException(String mensagem) {
        super(mensagem);
    }
}