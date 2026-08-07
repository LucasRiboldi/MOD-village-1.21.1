package com.villagecolony.core.task.model;

import com.villagecolony.core.type.Capability;

/**
 * O que uma tarefa manda fazer.
 *
 * <p>Cada tipo declara a {@link Capability} que exige. É assim que a
 * colônia acha quem pode executá-la sem que a tarefa conheça profissão
 * alguma — Profession-System.md §"Compatibilidade de Tarefas" e
 * §"Regras de Arquitetura".
 *
 * <p>Três tipos no MVP, um por elo da cadeia produtiva. A demanda vira
 * um deles em Simulation-Loop.md §5.
 */
public enum TaskType {

    /** Derrubar árvore e trazer a madeira. */
    COLLECT_WOOD(Capability.COLLECT_WOOD),

    /** Transformar matéria-prima em material. */
    CRAFT_MATERIAL(Capability.CRAFT_ITEMS),

    /** Erguer parte de uma expansão. */
    BUILD(Capability.BUILD_STRUCTURE);

    private final Capability required;

    TaskType(Capability required) {
        this.required = required;
    }

    /** A capacidade que o executor precisa ter. */
    public Capability required() {
        return required;
    }
}
