package com.villagecolony.core.telemetry.model;

/**
 * A categoria ampla do alvo de um evento de traço — decisão 7B,
 * 2026-09-24.
 *
 * <p><b>Categoria, não coordenada.</b> {@code ActivityTraceEvent} não
 * guarda posição alguma — a Task 7 é explícita sobre isso, e é a mesma
 * régua de {@code ActivityLog}: "as linhas não carregam UUID, coordenada,
 * nome de bloco ou texto livre". O que o traço registra é <i>que tipo</i>
 * de coisa o trabalhador perseguia, não onde.
 */
public enum TargetKind {
    STONE,
    WOOD,
    CROP,
    ANIMAL,
    ORE,
    CONSTRUCTION,
    STORAGE,
    MINE,

    /** Nenhum alvo — coordenação, espera sem trabalho aberto. */
    NONE,

    UNKNOWN
}
