package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** O grafo a publicar: a rede, e toda entrada já como nó, mesmo sem ligação ainda — Regra 214. O centro é sempre da rede. */
final class RoadLinesGraph {

    /** Até quantos blocos do centro um encontro de linhas vira o nó central. */
    private static final int HUB_SNAP = 3;

    private RoadLinesGraph() {
    }

    static RoadGraph of(Map<Long, Integer> columns, Map<Long, Integer> fronts, ColonyPos center, Set<Long> doors) {
        Set<ColonyPos> positions = new HashSet<>();
        Set<ColonyPos> entrances = new HashSet<>();

        columns.forEach((column, y) -> {
            ColonyPos at = new ColonyPos(ColonyRoads.xOf(column), y, ColonyRoads.zOf(column));

            positions.add(at);

            if (doors.contains(column)) {
                entrances.add(at);
            }
        });

        Set<ColonyPos> network = new HashSet<>(positions);

        fronts.forEach((column, y) -> {
            if (!columns.containsKey(column)) {
                ColonyPos at = new ColonyPos(ColonyRoads.xOf(column), y, ColonyRoads.zOf(column));

                positions.add(at);
                entrances.add(at);
            }
        });

        ColonyPos hub = network.stream()
                .min(Comparator.comparingInt((ColonyPos at) -> Math.abs(at.x() - center.x()) + Math.abs(at.z() - center.z()))
                        .thenComparingInt(ColonyPos::x).thenComparingInt(ColonyPos::z))
                .orElse(center);

        return RoadGraph.from(positions, junctionNear(columns, hub, network), entrances);
    }

    /** O encontro de linhas a até {@value #HUB_SNAP} blocos do centro vira o nó central: dois nós não ficam colados. */
    private static ColonyPos junctionNear(Map<Long, Integer> columns, ColonyPos hub, Set<ColonyPos> positions) {
        return positions.stream()
                .filter(at -> Math.abs(at.x() - hub.x()) + Math.abs(at.z() - hub.z()) <= HUB_SNAP
                        && RoadLines.cardinalDegree(columns, at.x(), at.z()) >= 3)
                .min(Comparator.comparingInt((ColonyPos at) -> Math.abs(at.x() - hub.x()) + Math.abs(at.z() - hub.z()))
                        .thenComparingInt(ColonyPos::x).thenComparingInt(ColonyPos::z))
                .orElse(hub);
    }
}
