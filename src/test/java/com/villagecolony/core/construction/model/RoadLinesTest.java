package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O grafo como mapa de trilhos (Regra 177), a frente de porta (Regra 178), a quina fixa (Regra 183) e a rua de um bloco (Regra 186). */
@DisplayName("RoadLines")
class RoadLinesTest {

    private static final int Y = 64;

    private static final PathTerrain FLAT = new PathTerrain() {
        @Override
        public OptionalInt groundY(int x, int z, int nearY) {
            return OptionalInt.of(Y);
        }

        @Override
        public boolean blocked(int x, int z) {
            return false;
        }
    };

    private static ColonyPos at(int x, int z) {
        return new ColonyPos(x, Y, z);
    }

    private static Set<ColonyPos> corridor(int fromX, int toX, int fromZ, int toZ) {
        Set<ColonyPos> cells = new HashSet<>();

        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                cells.add(at(x, z));
            }
        }

        return cells;
    }

    private static RoadLines.Result build(Set<ColonyPos> cells, ColonyPos center, Set<ColonyPos> entrances, Set<ColonyPos> anchors) {
        return RoadLines.build(cells, Set.of(), center, entrances, anchors, FLAT, 0, 0);
    }

    private static Set<ColonyPos> columnsOf(RoadLines.Result result) {
        Set<ColonyPos> columns = new HashSet<>();
        RoadGraph graph = result.graph().orElseThrow();

        graph.nodes().forEach(node -> columns.add(node.position()));
        graph.edges().forEach(edge -> columns.addAll(edge.columns()));

        return columns;
    }

    @Test
    @DisplayName("sem caminho não há grafo")
    void noPathNoGraph() {
        assertTrue(build(Set.of(), at(0, 0), Set.of(), Set.of()).graph().isEmpty());
    }

    @Test
    @DisplayName("Regra 177: o corredor largo vira uma linha só, no bloco central")
    void aWideCorridorIsOneCentralLine() {
        Set<ColonyPos> columns = columnsOf(build(corridor(0, 20, -1, 1), at(10, 0), Set.of(), Set.of()));

        assertFalse(columns.isEmpty());
        assertTrue(columns.stream().allMatch(column -> column.z() == 0), "a linha saiu do centro da faixa: " + columns);
    }

    @Test
    @DisplayName("Regra 177: vão de até 2 blocos ainda liga, sem nenhuma busca")
    void aGapOfTwoStillLinks() {
        Set<ColonyPos> cells = corridor(0, 10, 0, 0);

        cells.addAll(corridor(13, 23, 0, 0));

        RoadLines.Result result = build(cells, at(5, 0), Set.of(), Set.of());
        Set<ColonyPos> columns = columnsOf(result);

        assertEquals(0, result.isolated(), "o vão de 2 deixou uma parte isolada");
        assertTrue(columns.containsAll(List.of(at(11, 0), at(12, 0))), "a linha não atravessou o vão");
        assertTrue(result.toPave().stream().anyMatch(step -> step.x() == 11), "o vão não foi mandado calçar");
    }

    @Test
    @DisplayName("Regra 177: vão maior que 2 blocos não liga sozinho")
    void aWiderGapStaysApart() {
        Set<ColonyPos> cells = corridor(0, 10, 0, 0);

        cells.addAll(corridor(20, 30, 0, 0));

        assertEquals(1, build(cells, at(5, 0), Set.of(), Set.of()).isolated());
    }

    @Test
    @DisplayName("Regra 177: nó só onde linhas se encontram, nunca nó ao lado de nó")
    void aCrossHasOneJunctionAndNoNeighbouringNodes() {
        Set<ColonyPos> cells = corridor(-10, 10, 0, 0);

        cells.addAll(corridor(0, 0, -10, 10));

        RoadGraph graph = build(cells, at(0, 0), Set.of(), Set.of()).graph().orElseThrow();
        List<RoadGraph.Node> nodes = List.copyOf(graph.nodes());

        assertEquals(1, nodes.stream().filter(node -> node.roles().contains(RoadGraph.Role.JUNCTION)).count());

        for (RoadGraph.Node a : nodes) {
            for (RoadGraph.Node b : nodes) {
                int distance = Math.abs(a.position().x() - b.position().x()) + Math.abs(a.position().z() - b.position().z());

                assertTrue(a == b || distance > 1, "nó ao lado de nó: " + a.position() + " e " + b.position());
            }
        }
    }

    @Test
    @DisplayName("Regra 177: entrada de estrutura tem ramal até a rede")
    void anEntranceGetsABranchToTheNetwork() {
        ColonyPos door = at(5, 4);
        RoadLines.Result result = build(corridor(0, 20, 0, 0), at(10, 0), Set.of(door), Set.of());
        RoadGraph graph = result.graph().orElseThrow();

        assertEquals(0, result.isolated());
        assertTrue(graph.nodes().stream().anyMatch(node -> node.position().equals(door)
                && node.roles().contains(RoadGraph.Role.ENTRANCE)), "a porta não virou entrada do grafo");
        assertTrue(columnsOf(result).containsAll(List.of(at(5, 1), at(5, 2), at(5, 3))), "o ramal não chegou à linha");
    }

    @Test
    @DisplayName("Regra 178: o primeiro bloco fora da porta é mandado calçar")
    void theBlockInFrontOfADoorIsPaved() {
        RoadLines.Result result = build(corridor(0, 20, 0, 0), at(10, 0), Set.of(at(5, 4)), Set.of());

        assertTrue(result.toPave().stream().anyMatch(step -> step.x() == 5 && step.z() == 4), "a frente da porta ficou sem caminho");
    }

    @Test
    @DisplayName("Regra 183: a linha passa pela quina fixa, mesmo fora do centro da faixa")
    void theLinePassesThroughAnAnchor() {
        ColonyPos corner = at(5, 1);

        assertTrue(columnsOf(build(corridor(0, 20, -1, 1), at(10, 0), Set.of(), Set.of(corner))).contains(corner));
    }

    @Test
    @DisplayName("Regra 206: toco no meio da vila sai do grafo, e o caminho dele vira sobra")
    void aStubInsideTheVillageLeavesTheGraph() {
        Set<ColonyPos> cells = corridor(-20, 20, 0, 0);

        cells.addAll(corridor(5, 5, 1, 8));

        RoadLines.Result result = build(cells, at(0, 0), Set.of(), Set.of());
        Set<ColonyPos> columns = columnsOf(result);

        assertFalse(columns.contains(at(5, 8)), "o toco ficou no grafo: " + columns);
        assertFalse(columns.contains(at(5, 1)), "o trecho morto ficou no grafo: " + columns);
        assertTrue(columns.containsAll(List.of(at(-20, 0), at(20, 0))), "as pontas dos cantos saíram");
        assertTrue(result.surplus().contains(at(5, 1)), "o caminho do toco não virou sobra: " + result.surplus());
    }

    @Test
    @DisplayName("Regra 206: no máximo uma ponta por quadrante, a mais distante")
    void atMostOneTipPerQuadrant() {
        Set<ColonyPos> cells = corridor(-20, 20, 0, 0);

        cells.addAll(corridor(6, 6, 1, 8));
        cells.addAll(corridor(12, 12, 1, 3));
        cells.addAll(corridor(-6, -6, 1, 8));
        cells.addAll(corridor(6, 6, -8, -1));
        cells.addAll(corridor(-6, -6, -8, -1));

        RoadGraph graph = build(cells, at(0, 0), Set.of(), Set.of()).graph().orElseThrow();
        List<RoadGraph.Node> tips = graph.nodes().stream()
                .filter(node -> node.roles().contains(RoadGraph.Role.ENDPOINT) && !node.roles().contains(RoadGraph.Role.HUB))
                .toList();

        assertTrue(tips.size() <= 4, "mais de 4 pontas: " + tips);
        assertEquals(tips.size(), tips.stream().map(node -> (node.position().x() >= 0 ? 1 : 0) + (node.position().z() >= 0 ? 2 : 0)).distinct().count(),
                "duas pontas no mesmo quadrante: " + tips);
    }

    @Test
    @DisplayName("Regra 206: a entrada no fim de um trecho fica")
    void anEntranceAtTheEndOfAStubStays() {
        ColonyPos door = at(5, 3);
        Set<ColonyPos> cells = corridor(-20, 20, 0, 0);

        cells.addAll(corridor(5, 5, 1, 3));

        RoadGraph graph = build(cells, at(0, 0), Set.of(door), Set.of()).graph().orElseThrow();

        assertTrue(graph.nodes().stream().anyMatch(node -> node.position().equals(door)
                && node.roles().contains(RoadGraph.Role.ENTRANCE)), "a entrada saiu junto com o toco");
    }

    @Test
    @DisplayName("Regra 206: linha toda num quadrante não some até o centro")
    void aLineInOneQuadrantKeepsItsHub() {
        Set<ColonyPos> columns = columnsOf(build(corridor(5, 20, 5, 5), at(0, 0), Set.of(), Set.of()));

        assertTrue(columns.containsAll(List.of(at(5, 5), at(20, 5))), "a linha foi cortada: " + columns);
    }

    @Test
    @DisplayName("Regra 186: a sobra ao lado da linha volta ao solo, e a frente de porta fica")
    void theSurplusBesideTheLineGoesAwayButTheDoorStays() {
        ColonyPos door = at(10, 1);
        RoadLines.Result result = build(corridor(0, 20, -1, 1), at(10, 0), Set.of(door), Set.of());

        assertTrue(result.surplus().contains(at(5, 1)) && result.surplus().contains(at(5, -1)), "a sobra não saiu: " + result.surplus());
        assertFalse(result.surplus().contains(door), "a frente da porta foi tratada como sobra");
        assertFalse(result.surplus().contains(at(5, 0)), "a própria linha foi tratada como sobra");
    }
}
