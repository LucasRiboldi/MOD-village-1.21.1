package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Só se calça a aresta estável — Regra 322 (autor, 2026-11-18, para todos os modos do grafo): a aresta igual em
 * {@value #READS} releituras seguidas é estável, e só as colunas dela (com os nós das pontas) podem ser calçadas. A aresta que
 * muda recomeça a conta. Puro.
 */
public final class RoadStability {

    /** Quantas releituras seguidas a aresta precisa ficar igual para ser calçada. */
    public static final int READS = 3;

    /** As arestas vistas na última releitura e há quantas releituras cada uma está igual. */
    public static final class Memory {

        private Map<List<ColonyPos>, Integer> seen = new HashMap<>();
    }

    private RoadStability() {
    }

    /** Conta mais uma releitura deste grafo e devolve as colunas das arestas estáveis. */
    public static Set<Long> stableColumns(Memory memory, RoadGraph graph) {
        Map<List<ColonyPos>, Integer> now = new HashMap<>();
        Set<Long> stable = new HashSet<>();

        for (RoadGraph.Edge edge : graph.edges()) {
            List<ColonyPos> key = List.copyOf(edge.columns());
            int reads = memory.seen.getOrDefault(key, 0) + 1;

            now.put(key, reads);

            if (reads >= READS) {
                add(stable, edge.from());
                add(stable, edge.to());
                edge.columns().forEach(at -> add(stable, at));
            }
        }

        memory.seen = now;

        return stable;
    }

    private static void add(Set<Long> into, ColonyPos at) {
        into.add(ColonyRoads.column(at.x(), at.z()));
    }
}
