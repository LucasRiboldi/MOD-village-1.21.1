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

    /** Modo 2: a última impressão digital de cada quadrado, por colônia. */
    private static final Map<UUID, Map<Long, Integer>> PRINTS = new HashMap<>();

    /** Quantos quadrados a última montagem refez, por colônia; -1 fora do modo 2. */
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
        if (mode != RoadGraphMode.REGIOES) {
            LAST_DIRTY.remove(in.colonyId());
        }

        return switch (mode) {
            case ATUAL -> RoadLines.build(in.cells(), in.planned(), in.center(), in.fronts(), in.farmRings(), in.terrain(),
                    in.maxLinks(), in.turn(), in.fixed(), in.retired());
            case REGIOES -> regions(in);
            case VIZINHANCA -> RoadLines.build(in.cells(), in.planned(), in.center(), in.fronts(), in.farmRings(),
                    in.terrain(), in.maxLinks(), in.turn(), Set.of(), in.retired());
        };
    }

    /**
     * Modo 2: os quadrados cuja impressão digital mudou, ou que um bloco mudado marcou, são sujos; as linhas do grafo
     * anterior nos quadrados limpos entram fixas, e só os sujos se ajeitam em volta.
     */
    private static RoadLines.Result regions(Input in) {
        Set<ColonyPos> previous = in.previous().map(RoadRegions::columnsOf).orElse(Set.of());
        Map<Long, Integer> prints = RoadRegions.fingerprints(in.cells(), in.planned(), in.fronts(), previous, in.terrain());
        Set<Long> dirty = RoadRegions.dirty(PRINTS.getOrDefault(in.colonyId(), Map.of()), prints,
                RoadRegionEvents.drain(in.colonyId()));
        Set<ColonyPos> frozen = in.previous().map(graph -> RoadRegions.frozen(graph, dirty)).orElse(Set.of());

        PRINTS.put(in.colonyId(), prints);
        LAST_DIRTY.put(in.colonyId(), dirty.size());

        return RoadLines.build(in.cells(), in.planned(), in.center(), in.fronts(), in.farmRings(), in.terrain(),
                in.maxLinks(), in.turn(), frozen, in.retired());
    }

    /** Quantos quadrados a última montagem desta colônia refez no modo 2; -1 nos outros modos. */
    static int lastDirty(UUID colonyId) {
        return LAST_DIRTY.getOrDefault(colonyId, -1);
    }

    static void clearAll() {
        PRINTS.clear();
        LAST_DIRTY.clear();
    }
}
