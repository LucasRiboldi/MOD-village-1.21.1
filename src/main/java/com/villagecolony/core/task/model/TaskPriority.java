package com.villagecolony.core.task.model;

/**
 * Quanto uma tarefa importa perto das outras.
 *
 * <p>Simulation-Loop.md §"Prioridade das Demandas": sobrevivência antes
 * de produção, produção antes de construção. Uma colônia com fome não
 * ergue casa.
 *
 * <p>A ordem de declaração é a ordem de urgência, da maior para a menor,
 * para que a ordenação natural do enum já sirva. Trocar a ordem aqui
 * muda o comportamento da colônia — é por isso que ela está declarada
 * de propósito, e não em ordem alfabética.
 */
public enum TaskPriority {

    /** Comida e recursos básicos. Sem isto a vila encolhe. */
    SURVIVAL,

    /** Coletar e transformar matéria-prima. */
    PRODUCTION,

    /** Erguer o que a colônia planejou. */
    CONSTRUCTION;

    /** Se esta prioridade vem antes da outra. */
    public boolean isHigherThan(TaskPriority other) {
        return ordinal() < other.ordinal();
    }
}
