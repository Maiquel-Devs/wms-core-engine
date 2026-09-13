package com.wms.engine.model;

/**
 * Enum que define os estados possíveis do ciclo de vida de um palete no sistema WMS.
 * Utilizado para rastrear a movimentação física da carga desde a sua entrada até a saída do armazém.
 */
public enum StatusPalete {

    /**
     * O palete deu entrada no armazém e está fisicamente na área da doca,
     * aguardando triagem e alocação em uma vaga nas estantes.
     */
    RECEBIDO_DOCA,

    /**
     * O palete foi alocado com sucesso em um endereço físico (vaga/estante)
     * dentro do galpão e faz parte do estoque ativo.
     */
    ARMAZENADO,

    /**
     * O palete foi retirado do estoque, liberando a vaga física, e despachado
     * no caminhão, finalizando seu ciclo operacional no armazém.
     */
    EXPEDIDO
}