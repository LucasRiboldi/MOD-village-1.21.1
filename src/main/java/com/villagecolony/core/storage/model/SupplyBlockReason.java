package com.villagecolony.core.storage.model;

/**
 * Por que uma reserva de {@link WarehouseIndex} foi recusada — decisão
 * 4B, 2026-09-24.
 */
public enum SupplyBlockReason {
    /**
     * A fotografia não cobriu todo o baú reconhecido — algum estava em
     * chunk descarregado. Recusar aqui, em vez de tratar o que faltou
     * como "zero", é o mesmo princípio de {@code IdleReason.COUNT_PARTIAL}:
     * uma contagem incompleta não decide nada.
     */
    SNAPSHOT_INCOMPLETE,

    /** A colônia tem menos do recurso do que o pedido soma. */
    INSUFFICIENT_PHYSICAL_STOCK,

    /** Nenhum baú reconhecido tem espaço livre para o grupo do pedido. */
    NO_CAPACITY
}
