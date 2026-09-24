package com.villagecolony.core.storage.model;

/**
 * A ordem em que pedidos de suprimento disputam estoque escasso —
 * decisão 4B, 2026-09-24.
 *
 * <p><b>A ordem declarada é a ordem que vale.</b> {@code ordinal()} mais
 * baixo vence: uma obra ativa nunca perde material para o objetivo de
 * estoque geral da colônia. Ver {@link WarehouseIndex#reserveBatch}, o
 * único método que de fato aplica esta ordem — {@link WarehouseIndex#reserve}
 * sozinho é <em>greedy</em> por chamada.
 */
public enum SupplyPriority {
    /** Uma obra em andamento, com tarefa aberta esperando este material. */
    ACTIVE_CONSTRUCTION,

    /** Uma profissão com trabalho aberto que precisa da ferramenta ou do material. */
    ACTIVE_PROFESSION_WORK,

    /** Um baú perto da capacidade, aliviado por escoar o excesso. */
    CAPACITY_RELIEF,

    /** Meta geral de estoque da colônia, sem trabalho aberto esperando. */
    STOCK_OBJECTIVE
}
