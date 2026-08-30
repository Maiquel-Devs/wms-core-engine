package com.wms.engine.exception;

public class CapacidadeExcedidaException extends RuntimeException {
    public CapacidadeExcedidaException(String mensagem) {
        super(mensagem);
    }
}