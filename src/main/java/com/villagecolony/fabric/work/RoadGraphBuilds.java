package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.PathTerrain;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.construction.model.RoadGraphMode;
import com.villagecolony.core.construction.model.RoadLines;
import com.villagecolony.core.construction.model.RoadRegions;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.RoadRegionEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A montagem do grafo pelo modo ligado — ADR-057. Os três modos entregam o mesmo {@link RoadLines.Result}; quem usa o grafo
 * (calçamento, sobra, lote, pontas, poste) não muda.
 */
final class RoadGraphBuilds {

    static {
        ServerMemory.register(RoadGraphBuilds.class, RoadGraphBuilds::clearAll);
    }

    /** Modos 2 e 3: a impressão aceita de cada quadrado e o que mudou na releitura anterior, por colônia (Regra 319). */
    private static final Map<UUID, RoadRegions.Memory> PRINTS = new HashMap<>();

    /** Modo 3: as ligações traçadas, por colônia. */
    private static final Map<UUID, com.villagecolony.core.construction.model.NeighborhoodRoutes> ROUTES = new HashMap<>();

    /** A conta de releituras iguais de cada aresta, por colônia (Regra 322). */
    private static final Map<UUID, com.villagecolony.core.construction.model.RoadStability.Memory> STABILITY = new HashMap<>();

    /** Quantas ligações esperaram a vez na última montagem do modo 3, por colônia. */
    private static final Map<UUID, Integer> LAST_QUEUED = new HashMap<>();

    /** Quantos quadrados a última montagem refez, por colônia; -1 no modo 1. */
    private static final Map<UUID, Integer> LAST_DIRTY = new HashMap<>();

    private RoadGraphBuilds() {
    }

    /** O que a releitura leu, igual para os três modos. */
    record Input(UUID colonyId, Set<ColonyPos> cells, Set<ColonyPos> planned, ColonyPos center, Set<ColonyPos> fronts,
                 Set<ColonyPos> farmRings, PathTerrain terrain, int maxLinks, int turn, Set<ColonyPos> fixed,
                 Set<ColonyPos> retired, Optional<RoadGraph> previous, java.util.List<Integer> limit, long today,
                 Set<ColonyPos> far, com.villagecolony.data.save.RoadGraphStore store) {
    }

    /** O grafo pelo modo; a trava de dias (Regras 225 e 306) só vale no modo atual. */
    static RoadLines.Result build(RoadGraphMode mode, Input in) {
        if (mode == RoadGraphMode.ATUAL) {
            LAST_DIRTY.remove(in.colonyId());
        }

        return switch (mode) {
            case ATUAL -> RoadLines.build(in.cells(), in.planned(), in.center(), in.fronts(), in.farmRings(), in.terrain(),
                    in.maxLinks(), in.turn(), in.fixed(), in.retired(), in.far());
            case REGIOES -> regions(in);
            case VIZINHANCA -> neighborhood(in);
        };
    }

    /**
     * Modo 2: os quadrados cuja impressão digital mudou, ou que um bloco mudado marcou, são sujos; as linhas do grafo
     * anterior nos quadrados limpos entram fixas, e só os sujos se ajeitam em volta.
     */
    private static RoadLines.Result regions(Input in) {
        Set<Long> dirty = dirty(in);
        Set<ColonyPos> frozen = in.previous().map(graph -> RoadRegions.frozen(graph, dirty)).orElse(Set.of());
        RoadLines.Result built = RoadLines.build(in.cells(), in.planned(), in.center(), in.fronts(), in.farmRings(),
                in.terrain(), in.maxLinks(), in.turn(), frozen, in.retired(), in.far());

        // O quadrado limpo sai igual ao grafo anterior: a montagem só vale nos refeitos (Regra 319).
        return RoadRegions.settle(built, in.previous().orElse(null), dirty, in.center(), in.fronts(), in.cells());
    }

    /**
     * Modo 3: vizinhança relativa entre o centro e as entradas, cada ligação por A*; a ligação guardada só é refeita
     * quando um quadrado sujo a cruza (Regra 320).
     */
    private static RoadLines.Result neighborhood(Input in) {
        Set<Long> dirty = dirty(in);
        // As ligações vêm do save na primeira montagem depois de abrir o mundo, e voltam para ele (Regra 388, ADR-065 §8).
        com.villagecolony.core.construction.model.NeighborhoodRoutes routes = ROUTES.computeIfAbsent(in.colonyId(),
                ignored -> in.store().routes(in.colonyId()));
        RoadLines.Result result = com.villagecolony.core.construction.model.NeighborhoodRoads.build(in.cells(), in.center(),
                in.fronts(), in.terrain(), routes, dirty, in.limit().stream().mapToInt(Integer::intValue).toArray(), in.today(),
                com.villagecolony.fabric.integration.StreetGrowth.take(in.colonyId()));

        in.store().keepRoutes(in.colonyId(), routes);
        LAST_QUEUED.put(in.colonyId(), routes.queued());

        return result;
    }

    /** Quantas ligações esperaram a vez na última montagem do modo 3; a fila não é porta solta (Regra 388). */
    static int lastQueued(UUID colonyId) {
        return LAST_QUEUED.getOrDefault(colonyId, 0);
    }

    /**
     * Os quadrados a refazer nesta montagem: mudados em duas releituras seguidas, ou marcados por bloco na anterior
     * (Regra 319). Sem grafo anterior, todos.
     */
    private static Set<Long> dirty(Input in) {
        RoadRegions.Memory memory = PRINTS.computeIfAbsent(in.colonyId(), ignored -> new RoadRegions.Memory());
        Map<Long, Integer> prints = RoadRegions.fingerprints(in.cells(), in.planned(), in.fronts(), in.terrain());
        Set<Long> dirty = RoadRegions.confirm(memory, prints, RoadRegionEvents.drain(in.colonyId()),
                in.previous().isEmpty());

        LAST_DIRTY.put(in.colonyId(), dirty.size());

        return dirty;
    }

    /** Quantos quadrados a última montagem desta colônia achou sujos nos modos 2 e 3; -1 no modo 1. */
    static int lastDirty(UUID colonyId) {
        return LAST_DIRTY.getOrDefault(colonyId, -1);
    }

    /**
     * Só o que é da aresta estável vai para quem calça (Regra 322, todos os modos): a aresta igual em
     * {@link com.villagecolony.core.construction.model.RoadStability#READS} releituras seguidas.
     */
    static java.util.List<com.villagecolony.core.construction.model.PathStep> stableSteps(UUID colonyId,
            Optional<RoadGraph> graph, java.util.List<com.villagecolony.core.construction.model.PathStep> toPave) {
        if (graph.isEmpty()) {
            return java.util.List.of();
        }

        com.villagecolony.core.construction.model.NeighborhoodRoutes routes = ROUTES.get(colonyId);

        if (routes != null) {
            return lockedSteps(routes, toPave);
        }

        Set<Long> stable = com.villagecolony.core.construction.model.RoadStability.stableColumns(STABILITY.computeIfAbsent(
                colonyId, ignored -> new com.villagecolony.core.construction.model.RoadStability.Memory()), graph.get());

        return toPave.stream()
                .filter(step -> stable.contains(com.villagecolony.core.construction.model.ColonyRoads.column(step.x(), step.z())))
                .toList();
    }

    /**
     * No modo 3, só a ligação travada ou já calçada vai para quem calça — Regra 390 (ADR-065 etapa 2, L2 do jogo da 0.4.150:
     * "só calça caminhos que já estão travados definitivamente").
     */
    static java.util.List<com.villagecolony.core.construction.model.PathStep> lockedSteps(
            com.villagecolony.core.construction.model.NeighborhoodRoutes routes,
            java.util.List<com.villagecolony.core.construction.model.PathStep> toPave) {
        Set<Long> locked = new java.util.HashSet<>();

        routes.links().values().stream()
                .filter(link -> link.state() == com.villagecolony.core.construction.model.RoadEdgeState.LOCKED
                        || link.state() == com.villagecolony.core.construction.model.RoadEdgeState.PAVED)
                .forEach(link -> link.columns().forEach(at -> locked.add(
                        com.villagecolony.core.construction.model.ColonyRoads.column(at.x(), at.z()))));

        return toPave.stream()
                .filter(step -> locked.contains(com.villagecolony.core.construction.model.ColonyRoads.column(step.x(), step.z())))
                .toList();
    }

    static void clearAll() {
        STABILITY.clear();
        PRINTS.clear();
        LAST_DIRTY.clear();
        LAST_QUEUED.clear();
        ROUTES.clear();
    }
}
