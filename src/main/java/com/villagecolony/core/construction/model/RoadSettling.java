package com.villagecolony.core.construction.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O trecho que ficou — Regras 225 e 306 (autor, 2026-11-10 e 2026-11-17, ADR-050): a aresta inteira entre dois nós rosa
 * que passa {@value #DAYS} dias de jogo sem sair do grafo fica fixa. A releitura não a move, e o resto do traçado se ajeita em volta. Sai da linha, ou some o caminho
 * dela no mundo, e a conta recomeça. Puro: a coluna é {@link ColonyRoads#column}, e o dia, o do relógio do mundo.
 */
public final class RoadSettling {

    /** Quantos dias de jogo sem mudar fazem o trecho fixo: longo de propósito, para ser a versão final (autor). */
    public static final int DAYS = 10;

    private RoadSettling() {
    }

    /** As colunas na linha desde {@value #DAYS} dias ou mais — a idade de cada coluna, sem a aresta. */
    static Set<Long> settledColumns(Map<Long, Long> since, long today) {
        Set<Long> fixed = new HashSet<>();

        since.forEach((column, day) -> {
            if (today - day >= DAYS) {
                fixed.add(column);
            }
        });

        return fixed;
    }

    /**
     * As colunas fixas — Regra 306 (autor, 2026-11-17): só a aresta inteira entre dois nós rosa (junção ou centro), com o
     * nó das pontas, quando toda coluna dela está na linha há {@value #DAYS} dias. A aresta que chega a nó azul (ponta) ou
     * branco (entrada) nunca fixa, para o grafo sempre poder crescer a vila; nenhum pedaço de aresta fixa sozinho, nem um
     * lado de curva em L, que não tem nó.
     */
    public static Set<Long> fixed(Map<Long, Long> since, long today, java.util.Optional<RoadGraph> graph) {
        Set<Long> fixed = new HashSet<>();

        if (graph.isEmpty()) {
            return fixed;
        }

        Set<Long> old = settledColumns(since, today);
        Map<Long, RoadGraph.Node> nodes = new java.util.HashMap<>();

        graph.get().nodes().forEach(node -> nodes.put(ColonyRoads.column(node.position().x(), node.position().z()), node));

        for (RoadGraph.Edge edge : graph.get().edges()) {
            List<Long> columns = new java.util.ArrayList<>();

            columns.add(ColonyRoads.column(edge.from().x(), edge.from().z()));
            columns.add(ColonyRoads.column(edge.to().x(), edge.to().z()));
            edge.columns().forEach(at -> columns.add(ColonyRoads.column(at.x(), at.z())));

            if (isPink(nodes.get(columns.get(0))) && isPink(nodes.get(columns.get(1))) && old.containsAll(columns)) {
                fixed.addAll(columns);
            }
        }

        return fixed;
    }

    /** Nó rosa: junção, nó de anel ou centro; ponta (azul) e entrada (branco) não seguram aresta fixa. */
    private static boolean isPink(RoadGraph.@org.jspecify.annotations.Nullable Node node) {
        return node != null && !node.roles().contains(RoadGraph.Role.ENDPOINT) && !node.roles().contains(RoadGraph.Role.ENTRANCE)
                && (node.roles().contains(RoadGraph.Role.JUNCTION) || node.roles().contains(RoadGraph.Role.HUB)
                        || node.roles().contains(RoadGraph.Role.CYCLE_ANCHOR));
    }

    /**
     * O dia de entrada de cada coluna, depois desta releitura: a que segue na linha guarda o dia, a nova começa hoje e a que
     * saiu é esquecida.
     */
    public static Map<Long, Long> track(Map<Long, Long> since, Set<Long> line, long today) {
        Map<Long, Long> next = new HashMap<>();

        for (long column : line) {
            next.put(column, since.getOrDefault(column, today));
        }

        return next;
    }
}
