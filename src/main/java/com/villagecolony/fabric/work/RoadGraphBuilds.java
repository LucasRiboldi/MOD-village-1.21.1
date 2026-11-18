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
    private static final Map<UUID, com.villagecolony.core.construction.model.NeighborhoodRoads.Routes> ROUTES = new HashMap<>();

    /** Quantos quadrados a última montagem refez, por colônia; -1 no modo 1. */
    private static final Map<UUID, Integer> LAST_DIRTY = new HashMap<>();

    private RoadGraphBuilds() {
    }

    /** O que a releitura leu, igual para os três modos. */
    record Input(UUID colonyId, Set<ColonyPos> cells, Set<ColonyPos> planned, ColonyPos center, Set<ColonyPos> fronts,
                 Set<ColonyPos> farmRings, PathTerrain terrain, int maxLinks, int turn, Set<ColonyPos> fixed,
                 Set<ColonyPos> retired, Optional<RoadGraph> previous) {
    }

    /** O grafo pelo modo; a trava de dias (Regras 225 e 306) só vale no modo atual. */
    static RoadLines.Result build(RoadGraphMode mode, Input in) {
        if (mode == RoadGraphMode.ATUAL) {
            LAST_DIRTY.remove(in.colonyId());
        }

        return switch (mode) {
            case ATUAL -> RoadLines.build(in.cells(), in.planned(), in.center(), in.fronts(), in.farmRings(), in.terrain(),
                    in.maxLinks(), in.turn(), in.fixed(), in.retired());
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
                in.terrain(), in.maxLinks(), in.turn(), frozen, in.retired());

        // O quadrado limpo sai igual ao grafo anterior: a montagem só vale nos refeitos (Regra 319).
        return RoadRegions.settle(built, in.previous().orElse(null), dirty, in.center(), in.fronts(), in.cells());
    }

    /**
     * Modo 3: vizinhança relativa entre o centro e as entradas, cada ligação por A*; a ligação guardada só é refeita
     * quando um quadrado sujo a cruza (Regra 320).
     */
    private static RoadLines.Result neighborhood(Input in) {
        Set<Long> dirty = dirty(in);

        return com.villagecolony.core.construction.model.NeighborhoodRoads.build(in.cells(), in.center(), in.fronts(),
                in.terrain(), ROUTES.computeIfAbsent(in.colonyId(),
                        ignored -> new com.villagecolony.core.construction.model.NeighborhoodRoads.Routes()), dirty);
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

    static void clearAll() {
        PRINTS.clear();
        LAST_DIRTY.clear();
        ROUTES.clear();
    }
}
