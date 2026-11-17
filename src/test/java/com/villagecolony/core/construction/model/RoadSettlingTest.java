package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra 306: só fica fixa a aresta inteira entre dois nós rosa (junção ou centro), com todas as colunas na linha há
 * {@value RoadSettling#DAYS} dias; a que chega a nó azul (ponta) ou branco (entrada) nunca fixa, e nenhum pedaço fixa sozinho.
 */
class RoadSettlingTest {

    private static final long TODAY = 100;

    private static ColonyPos at(int x, int z) {
        return new ColonyPos(x, 64, z);
    }

    private static RoadGraph.Edge edge(ColonyPos from, ColonyPos to) {
        List<ColonyPos> columns = new ArrayList<>();
        int dx = Integer.signum(to.x() - from.x());
        int dz = Integer.signum(to.z() - from.z());

        for (ColonyPos c = from; !c.equals(to); c = at(c.x() + dx, c.z() + dz)) {
            columns.add(c);
        }

        columns.add(to);

        return new RoadGraph.Edge(from, to, columns, RoadGraph.RoadClass.PRIMARY);
    }

    private static RoadGraph graph() {
        return new RoadGraph(List.of(
                new RoadGraph.Node(at(0, 0), Set.of(RoadGraph.Role.JUNCTION)),
                new RoadGraph.Node(at(10, 0), Set.of(RoadGraph.Role.JUNCTION)),
                new RoadGraph.Node(at(10, 10), Set.of(RoadGraph.Role.ENDPOINT)),
                new RoadGraph.Node(at(0, 10), Set.of(RoadGraph.Role.ENTRANCE))),
                List.of(edge(at(0, 0), at(10, 0)), edge(at(10, 0), at(10, 10)), edge(at(0, 0), at(0, 10))));
    }

    private static Map<Long, Long> allOld(RoadGraph graph) {
        Map<Long, Long> since = new HashMap<>();

        graph.edges().forEach(edge -> edge.columns().forEach(c -> since.put(ColonyRoads.column(c.x(), c.z()), 0L)));

        return since;
    }

    @Test
    void onlyAWholePinkToPinkEdgeIsFixed() {
        RoadGraph graph = graph();
        Set<Long> fixed = RoadSettling.fixed(allOld(graph), TODAY, Optional.of(graph));

        assertTrue(fixed.contains(ColonyRoads.column(5, 0)), "a aresta de rosa a rosa não fixou");
        assertFalse(fixed.contains(ColonyRoads.column(10, 5)), "Regra 306: a linha até o nó azul fixou");
        assertFalse(fixed.contains(ColonyRoads.column(0, 5)), "Regra 306: a linha até a entrada fixou");
    }

    @Test
    void oneNewColumnKeepsTheWholeEdgeFree() {
        RoadGraph graph = graph();
        Map<Long, Long> since = allOld(graph);
        since.put(ColonyRoads.column(7, 0), TODAY - 1);

        Set<Long> fixed = RoadSettling.fixed(since, TODAY, Optional.of(graph));

        assertFalse(fixed.contains(ColonyRoads.column(2, 0)), "Regra 306: só um pedaço da aresta fixou");
    }
}
