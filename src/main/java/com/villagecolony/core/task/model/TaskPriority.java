package com.villagecolony.core.task.model;

import java.util.Comparator;

/**
 * Quanto uma tarefa importa perto das outras.
 *
 * <p>Simulation-Loop.md §"Prioridade das Demandas": sobrevivência antes
 * de produção, produção antes de construção. Uma colônia com fome não
 * ergue casa.
 *
 * <p>A urgência é o campo {@link #urgency()}, <b>não</b> a ordem de
 * declaração: menor número, mais urgente. Até 2026-09-29 valia o
 * {@code ordinal()}, e reordenar as constantes mudava a colônia em
 * silêncio. Os números têm folga de dez para que um nível novo entre
 * entre dois sem renumerar os outros; {@code TaskPriorityTest} fixa a
 * sequência e a unicidade.
 */
public enum TaskPriority {

    /** Comida e recursos básicos. Sem isto a vila encolhe. */
    SURVIVAL(10),

    /** Materiais que destravam a obra aberta têm precedência sobre estoque. */
    CONSTRUCTION_MATERIAL(20),

    /** Coletar e transformar matéria-prima. */
    PRODUCTION(30),

    /** Erguer o que a colônia planejou. */
    CONSTRUCTION(40);

    /** Da mais urgente para a menos urgente. */
    public static final Comparator<TaskPriority> MOST_URGENT_FIRST =
            Comparator.comparingInt(TaskPriority::urgency);

    private final int urgency;

    TaskPriority(int urgency) {
        this.urgency = urgency;
    }

    /** Menor é mais urgente. Só para ordenar — não é persistido. */
    public int urgency() {
        return urgency;
    }

    /** Se esta prioridade vem antes da outra. */
    public boolean isHigherThan(TaskPriority other) {
        return urgency < other.urgency;
    }
}
