package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.PathTerrain;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.construction.model.RoadGraphMode;
import com.villagecolony.core.construction.model.RoadLines;
import com.villagecolony.core.type.ColonyPos;

import java.util.Optional;
import java.util.Set;

/**
 * A montagem do grafo pelo modo ligado — ADR-057. Os três modos entregam o mesmo {@link RoadLines.Result}; quem usa o grafo
 * (calçamento, sobra, lote, pontas, poste) não muda.
 */
final class RoadGraphBuilds {

    private RoadGraphBuilds() {
    }

    /** O que a releitura leu, igual para os três modos. */
    record Input(Set<ColonyPos> cells, Set<ColonyPos> planned, ColonyPos center, Set<ColonyPos> fronts,
                 Set<ColonyPos> farmRings, PathTerrain terrain, int maxLinks, int turn, Set<ColonyPos> fixed,
                 Set<ColonyPos> retired, Optional<RoadGraph> previous) {
    }

    /** O grafo pelo modo; a trava de dias (Regras 225 e 306) só vale no modo atual. */
    static RoadLines.Result build(RoadGraphMode mode, Input in) {
        return switch (mode) {
            case ATUAL -> RoadLines.build(in.cells(), in.planned(), in.center(), in.fronts(), in.farmRings(), in.terrain(),
                    in.maxLinks(), in.turn(), in.fixed(), in.retired());
            case REGIOES, VIZINHANCA -> RoadLines.build(in.cells(), in.planned(), in.center(), in.fronts(), in.farmRings(),
                    in.terrain(), in.maxLinks(), in.turn(), Set.of(), in.retired());
        };
    }
}
