package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * As regiões do modo 2 do grafo — ADR-057, Regra 319: quadrados de {@value #SIZE} × {@value #SIZE} com impressão digital
 * do que vem de fora do grafo (caminho, planejado, entradas e chão). O quadrado só é refeito quando a impressão fica
 * diferente da última aceita em duas releituras seguidas, ou na releitura depois de um bloco marcado; o quadrado limpo sai
 * igual ao grafo anterior. Puro: o chão vem por {@link PathTerrain}.
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

    /** O que um quadrado precisa lembrar entre releituras: a impressão aceita e o que mudou na releitura anterior. */
    public static final class Memory {

        private final Map<Long, Integer> accepted = new HashMap<>();

        private final Set<Long> pending = new HashSet<>();

        private final Set<Long> pendingMarked = new HashSet<>();
    }

    /**
     * A impressão digital de cada quadrado que tem alguma coisa: os blocos de caminho, o planejado, as entradas e a altura
     * do chão nessas colunas. O grafo não entra: ele muda porque foi refeito, e entrar faria o quadrado se sujar sozinho.
     */
    public static Map<Long, Integer> fingerprints(Collection<ColonyPos> cells, Collection<ColonyPos> planned,
                                                  Collection<ColonyPos> fronts, PathTerrain terrain) {
        Map<Long, Integer> prints = new HashMap<>();

        mix(prints, cells, 1, terrain);
        mix(prints, planned, 2, terrain);
        mix(prints, fronts, 3, terrain);

        return prints;
    }

    /**
     * Os quadrados a refazer agora. Mudou em relação à impressão aceita nesta releitura e na anterior, ou um bloco foi
     * marcado na anterior: refaz, e a impressão de agora vira a aceita. Mudou só agora: espera a próxima. Voltou ao que era:
     * esquece. Na primeira montagem, tudo.
     */
    public static Set<Long> confirm(Memory memory, Map<Long, Integer> now, Set<Long> marked, boolean firstBuild) {
        if (firstBuild) {
            memory.accepted.clear();
            memory.accepted.putAll(now);
            memory.pending.clear();
            memory.pendingMarked.clear();

            return Set.copyOf(now.keySet());
        }

        Set<Long> changed = new HashSet<>(dirty(memory.accepted, now, Set.of()));
        Set<Long> rebuild = new HashSet<>(memory.pendingMarked);

        changed.stream().filter(memory.pending::contains).forEach(rebuild::add);

        for (long region : rebuild) {
            Integer print = now.get(region);

            if (print == null) {
                memory.accepted.remove(region);
            } else {
                memory.accepted.put(region, print);
            }
        }

        memory.pending.clear();
        changed.stream().filter(region -> !rebuild.contains(region)).forEach(memory.pending::add);
        memory.pendingMarked.clear();
        marked.stream().filter(region -> !rebuild.contains(region)).forEach(memory.pendingMarked::add);

        return rebuild;
    }

    /**
     * O grafo com o quadrado limpo igual ao anterior: as colunas da montagem nos quadrados refeitos e as do grafo anterior
     * nos outros, remontadas num grafo só. Sem grafo anterior, a montagem fica como está.
     */
    public static RoadLines.Result settle(RoadLines.Result built, @org.jspecify.annotations.Nullable RoadGraph previous,
                                          Set<Long> rebuilt, ColonyPos center, Set<ColonyPos> fronts, Set<ColonyPos> cells) {
        if (previous == null || built.graph().isEmpty()) {
            return built;
        }

        Map<Long, Integer> columns = new HashMap<>();

        for (ColonyPos at : columnsOf(built.graph().get())) {
            if (rebuilt.contains(regionOf(at.x(), at.z()))) {
                columns.put(ColonyRoads.column(at.x(), at.z()), at.y());
            }
        }

        for (ColonyPos at : columnsOf(previous)) {
            if (!rebuilt.contains(regionOf(at.x(), at.z()))) {
                columns.put(ColonyRoads.column(at.x(), at.z()), at.y());
            }
        }

        Map<Long, Integer> toLay = new HashMap<>();
        Set<Long> doors = new HashSet<>();
        Map<Long, Integer> physical = new HashMap<>();

        fronts.forEach(at -> {
            toLay.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at.y());
            doors.add(ColonyRoads.column(at.x(), at.z()));
        });
        cells.forEach(at -> physical.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at.y()));

        return new RoadLines.Result(java.util.Optional.of(RoadLinesGraph.of(columns, toLay, center, doors)),
                RoadLines.toPave(columns, toLay, physical), built.isolated(), built.surplus(), built.loose());
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
