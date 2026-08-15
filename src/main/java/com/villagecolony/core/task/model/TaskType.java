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
    COLLECT_WOOD(Capability.COLLECT_WOOD, true),

    /** Transformar matéria-prima em material. */
    CRAFT_MATERIAL(Capability.CRAFT_ITEMS, true),

    /**
     * Erguer parte de uma expansão.
     *
     * <p>Único tipo que <b>não</b> é pedido de recurso. Ver
     * {@link #isResourceRequest}.
     */
    BUILD(Capability.BUILD_STRUCTURE, false);

    private final Capability required;

    private final boolean resourceRequest;

    TaskType(Capability required, boolean resourceRequest) {
        this.required = required;
        this.resourceRequest = resourceRequest;
    }

    /** A capacidade que o executor precisa ter. */
    public Capability required() {
        return required;
    }

    /**
     * Se esta tarefa nasce de uma falta de recurso, e por isso pertence
     * ao ciclo da colônia.
     *
     * <p>{@code ColonyCycle} abre tarefa quando falta recurso e a retira
     * quando a falta acaba. Isso vale para colher e fabricar: a tarefa
     * <em>é</em> o pedido, e um pedido sem motivo não deve ficar na fila.
     *
     * <p>Não vale para construir. A obra não é uma falta — é um projeto
     * aberto precisando de mão, e quem a abre e a encerra é
     * {@code ConstructionPlanner}, pela vida do projeto. Sem esta
     * distinção o ciclo faria duas coisas erradas com a tarefa de obra,
     * as duas em silêncio:
     *
     * <ul>
     *   <li><b>cancelá-la</b> — o {@code targetResource} de uma tarefa de
     *       obra é nominal, e assim que aquele recurso deixasse de faltar
     *       ela sairia da fila sem que a casa tivesse subido um bloco;
     *   <li><b>contá-la como pedido</b> — ela ocuparia a vaga de um
     *       pedido de tábua de verdade, e a colônia deixaria de fabricar
     *       o que a obra consome.
     * </ul>
     *
     * <p>Ver a entrada de 2026-08-15 no Development Log: a tarefa de obra
     * nunca chegou a existir em jogo, e foi este o primeiro lugar onde ela
     * teria morrido se tivesse existido.
     */
    public boolean isResourceRequest() {
        return resourceRequest;
    }
}
