package com.wms.engine.exception;

/**
 * Exceção de domínio de negócio lançada quando uma tentativa de alocação física falha
 * devido a restrições de capacidade estrutural na vaga de destino (ex: limite de peso
 * excedido ou volume incompatível do palete em relação à estante).
 */
public class CapacidadeExcedidaException extends RuntimeException {

    /**
     * Constrói a exceção com uma mensagem de erro detalhando a violação de limite.
     *
     * @param mensagem A descrição técnica ou de negócio informando qual capacidade foi excedida.
     */
    public CapacidadeExcedidaException(String mensagem) {
        super(mensagem);
    }
}