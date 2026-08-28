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
    COLLECT_WOOD(Capability.COLLECT_WOOD, true, true),

    /** Tirar pedra do mundo — pedregulho, ou arenito no deserto. */
    COLLECT_STONE(Capability.COLLECT_STONE, true, true),

    /** Tosquiar ovelha e trazer a lã. */
    COLLECT_WOOL(Capability.COLLECT_WOOL, true, true),

    /**
     * Colher lavoura madura, replantar, e trazer a comida — 2026-08-27.
     *
     * <p>O elo que faltava do fazendeiro. Ele tinha a capacidade
     * {@code MAINTAIN_FOOD} desde a Fase 7 e nenhuma tarefa a pedia:
     * capacidade sem tarefa é um aldeão com enxada e sem lavoura.
     */
    COLLECT_FOOD(Capability.MAINTAIN_FOOD, true, true),

    /** Fundir: areia em vidro, e o que mais a fornalha fizer. */
    SMELT_MATERIAL(Capability.SMELT_ITEMS, true, true),

    /** Transformar matéria-prima em material. */
    CRAFT_MATERIAL(Capability.CRAFT_ITEMS, true, true),

    /**
     * Erguer parte de uma expansão.
     *
     * <p>Único tipo que <b>não</b> é pedido de recurso. Ver
     * {@link #isResourceRequest}.
     */
    BUILD(Capability.BUILD_STRUCTURE, false, false);

    private final Capability required;

    private final boolean resourceRequest;

    private final boolean ownStorage;

    TaskType(Capability required, boolean resourceRequest, boolean ownStorage) {
        this.required = required;
        this.resourceRequest = resourceRequest;
        this.ownStorage = ownStorage;
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

    /**
     * Se o executor precisa ter baú próprio para sequer começar.
     *
     * <p>Colher e fabricar terminam guardando: {@code LumberjackWork} e
     * {@code ManufacturerWork} soltam a tarefa no primeiro tick quando
     * {@code STORAGES.of(worker)} vem vazio, porque não há onde pôr o que
     * o trabalho produz. Dar a tarefa a quem não tem baú é abrir e fechar
     * o mesmo trabalho todo ciclo — e, de fora, isso parece trabalho
     * acontecendo.
     *
     * <p>Construir não guarda nada: o material sai do baú da colônia por
     * {@code ChestWithdrawer} e vira bloco no mundo. Um construtor sem
     * baú próprio constrói igual, e exigir baú dele travaria a obra por
     * um motivo que não existe.
     *
     * <p>Ver a sessão de 2026-08-15: dois lenhadores e dois fabricantes
     * sem baú devolveram tarefa a cada ciclo por doze minutos seguidos —
     * dezenas de linhas "has no chest ... returned to the queue" e nenhum
     * recurso produzido, enquanto a tarefa deixava de chegar a quem tinha
     * baú para atendê-la.
     */
    public boolean needsOwnStorage() {
        return ownStorage;
    }
}
