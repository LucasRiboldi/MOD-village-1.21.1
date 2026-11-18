package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * As regiões do modo 2 do grafo — ADR-057: quadrados de {@value #SIZE} × {@value #SIZE} com impressão digital. O quadrado
 * cuja impressão não mudou é limpo, e as linhas do grafo anterior que passam por ele entram fixas na montagem; só o sujo
 * se ajeita. Puro: o chão vem por {@link PathTerrain}.
 */
public final class RoadRegions {

    /** O lado do quadrado, em blocos: um chunk. */
    public static final int SIZE = 16;

    private RoadRegions() {
    }

    /** O quadrado desta coluna, empacotado. */
    public static long regionOf(int x, int z) {
        return ColonyRoads.column(Math.floorDiv(x, SIZE), Math.floorDiv(z, SIZE));
    }

    /**
     * A impressão digital de cada quadrado que tem alguma coisa: os blocos de caminho, o planejado, as entradas e a altura
     * do chão em cada uma dessas colunas e nas do grafo anterior. Mudou qualquer um, mudou a impressão.
     */
    public static Map<Long, Integer> fingerprints(Collection<ColonyPos> cells, Collection<ColonyPos> planned,
                                                  Collection<ColonyPos> fronts, Collection<ColonyPos> previous,
                                                  PathTerrain terrain) {
        Map<Long, Integer> prints = new HashMap<>();

        mix(prints, cells, 1, terrain);
        mix(prints, planned, 2, terrain);
        mix(prints, fronts, 3, terrain);
        mix(prints, previous, 4, terrain);

        return prints;
    }

    private static void mix(Map<Long, Integer> prints, Collection<ColonyPos> cells, int kind, PathTerrain terrain) {
        for (ColonyPos at : cells) {
            OptionalInt ground = terrain.groundY(at.x(), at.z(), at.y());
            int part = 31 * (31 * (31 * at.x() + at.z()) + at.y()) + kind * 7 + (ground.isPresent() ? ground.getAsInt() : -9999) * 13;

            // Soma: a ordem das colunas não muda a impressão.
            prints.merge(regionOf(at.x(), at.z()), part * 0x9E3779B1, Integer::sum);
        }
    }

    /** Os quadrados sujos: impressão nova, sumida ou diferente, mais os que um bloco mudado marcou. */
    public static Set<Long> dirty(Map<Long, Integer> before, Map<Long, Integer> now, Set<Long> marked) {
        Set<Long> dirty = new HashSet<>(marked);

        now.forEach((region, print) -> {
            if (!print.equals(before.get(region))) {
                dirty.add(region);
            }
        });
        before.keySet().stream().filter(region -> !now.containsKey(region)).forEach(dirty::add);

        return dirty;
    }

    /** As colunas do grafo anterior que ficam fixas: as de nó e de aresta nos quadrados limpos. */
    public static Set<ColonyPos> frozen(RoadGraph previous, Set<Long> dirty) {
        Set<ColonyPos> frozen = new HashSet<>();

        previous.nodes().forEach(node -> keep(node.position(), dirty, frozen));
        previous.edges().forEach(edge -> edge.columns().forEach(at -> keep(at, dirty, frozen)));

        return frozen;
    }

    /** Todas as colunas do grafo, para a impressão digital. */
    public static Set<ColonyPos> columnsOf(RoadGraph graph) {
        Set<ColonyPos> columns = new HashSet<>();

        graph.nodes().forEach(node -> columns.add(node.position()));
        graph.edges().forEach(edge -> columns.addAll(edge.columns()));

        return columns;
    }

    private static void keep(ColonyPos at, Set<Long> dirty, Set<ColonyPos> frozen) {
        if (!dirty.contains(regionOf(at.x(), at.z()))) {
            frozen.add(at);
        }
    }
}
