package com.villagecolony.core.storage.model;

import java.util.Comparator;

/**
 * A ordem em que pedidos de suprimento disputam estoque escasso —
 * decisão 4B, 2026-09-24.
 *
 * <p><b>O campo {@link #rank()} é a ordem que vale</b>, não a posição
 * da constante: número mais baixo vence (até 2026-09-29 era o
 * {@code ordinal()}; folga de dez para um nível novo entrar sem
 * renumerar): uma obra ativa nunca perde material para o objetivo de
 * estoque geral da colônia. Ver {@link WarehouseIndex#reserveBatch}, o
 * único método que de fato aplica esta ordem — {@link WarehouseIndex#reserve}
 * sozinho é <em>greedy</em> por chamada.
 */
public enum SupplyPriority {
    /** Uma obra em andamento, com tarefa aberta esperando este material. */
    ACTIVE_CONSTRUCTION(10),

    /** Uma profissão com trabalho aberto que precisa da ferramenta ou do material. */
    ACTIVE_PROFESSION_WORK(20),

    /** Um baú perto da capacidade, aliviado por escoar o excesso. */
    CAPACITY_RELIEF(30),

    /** Meta geral de estoque da colônia, sem trabalho aberto esperando. */
    STOCK_OBJECTIVE(40);

    /** Do pedido que mais pesa para o que menos pesa. */
    public static final Comparator<SupplyPriority> FIRST_SERVED =
            Comparator.comparingInt(SupplyPriority::rank);

    private final int rank;

    SupplyPriority(int rank) {
        this.rank = rank;
    }

    /** Menor é servido primeiro. Só para ordenar — não é persistido. */
    public int rank() {
        return rank;
    }
}
