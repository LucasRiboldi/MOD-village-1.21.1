package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * As ligações do grafo planejado, por par de nós (colunas empacotadas, a menor primeiro), com o traçado e o estado de cada
 * uma — ADR-065 §6 e §8: ficam no save, e reabrir o mundo não muda o grafo. Também a última ponta de cada lado (Regra 320)
 * e quantas ligações ficaram na fila na última montagem.
 */
public final class NeighborhoodRoutes {

    final Map<List<Long>, RoadLink> byPair = new HashMap<>();

    /** Por lado, o caminho mais distante da última vez e o dia em que a ponta andou (Regra 320). */
    final Map<Integer, ColonyPos> lastEdge = new HashMap<>();

    final Map<Integer, Long> lastDay = new HashMap<>();

    /** Quantas ligações esperaram a vez na última montagem (ADR-065: fila não é falha). */
    int queued;

    /** Quantas ligações estão guardadas, com as que falharam. */
    public int size() {
        return byPair.size();
    }

    /** As ligações, só para ler e gravar. */
    public Map<List<Long>, RoadLink> links() {
        return Collections.unmodifiableMap(byPair);
    }

    /** Por lado, o último caminho mais distante. */
    public Map<Integer, ColonyPos> tipEdges() {
        return Collections.unmodifiableMap(lastEdge);
    }

    /** Por lado, o dia em que a ponta andou. */
    public Map<Integer, Long> tipDays() {
        return Collections.unmodifiableMap(lastDay);
    }

    /** Quantas ligações esperaram a vez na última montagem. */
    public int queued() {
        return queued;
    }

    /** As ligações lidas do save. */
    public static NeighborhoodRoutes restore(Map<List<Long>, RoadLink> links, Map<Integer, ColonyPos> tipEdges,
                                             Map<Integer, Long> tipDays) {
        NeighborhoodRoutes routes = new NeighborhoodRoutes();

        routes.byPair.putAll(links);
        routes.lastEdge.putAll(tipEdges);
        routes.lastDay.putAll(tipDays);

        return routes;
    }

    /** O mesmo conteúdo guardado: ligações e pontas (a fila não conta, porque não vai para o save). */
    public boolean sameAs(NeighborhoodRoutes other) {
        return other != null && byPair.equals(other.byPair) && lastEdge.equals(other.lastEdge) && lastDay.equals(other.lastDay);
    }
}
