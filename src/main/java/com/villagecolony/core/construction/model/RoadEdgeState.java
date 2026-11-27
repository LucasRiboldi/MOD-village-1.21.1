package com.villagecolony.core.construction.model;

/**
 * O estado de uma ligação do grafo planejado — ADR-065 §6. A ligação na fila (ainda não traçada por falta de vez) não é
 * guardada: ela só aparece na contagem {@code queued=} do log.
 */
public enum RoadEdgeState {
    /** Traçado feito, ainda pode mudar. */
    PLANNED,
    /** O traçado ficou igual em {@link RoadStability#READS} montagens seguidas; só um evento o muda. */
    LOCKED,
    /** Toda coluna do traçado já é caminho no mundo. */
    PAVED,
    /** Sem traçado; só é tentada de novo quando um bloco muda perto dela. */
    FAILED
}
