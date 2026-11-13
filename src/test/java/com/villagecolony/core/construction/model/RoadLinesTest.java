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

/** O grafo como mapa de trilhos (Regra 177), a frente de porta (Regra 178), a rua de um bloco (Regra 186) e a plantação sem anel (Regra 208). */
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

    private static RoadLines.Result build(Set<ColonyPos> cells, ColonyPos center, Set<ColonyPos> entrances, Set<ColonyPos> farmRings) {
        return RoadLines.build(cells, Set.of(), center, entrances, farmRings, FLAT, 0, 0);
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
    @DisplayName("Regras 177 e 224: vão maior que 2 blocos não fecha a faixa, mas liga em reta e o vão vai para calçar")
    void aWiderGapIsLinkedStraightAndPaved() {
        Set<ColonyPos> cells = corridor(0, 10, 0, 0);

        cells.addAll(corridor(20, 30, 0, 0));

        RoadLines.Result result = build(cells, at(5, 0), Set.of(), Set.of());

        assertEquals(0, result.isolated());
        assertTrue(result.toPave().stream().anyMatch(step -> step.x() == 15 && step.z() == 0), "o vão não foi para calçar");
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

    /** A rua em z=0 e o anel da plantação ligado a ela por dois ramais, nas duas quinas de cima. */
    private static Set<ColonyPos> streetWithAnOldRing(Set<ColonyPos> ring) {
        Set<ColonyPos> cells = corridor(-20, 20, 0, 0);

        cells.addAll(ring);
        cells.addAll(corridor(2, 2, 1, 5));
        cells.addAll(corridor(10, 10, 1, 5));

        return cells;
    }

    private static Set<ColonyPos> ring(int minX, int maxX, int minZ, int maxZ) {
        Set<ColonyPos> ring = new HashSet<>();

        for (int x = minX; x <= maxX; x++) {
            ring.add(at(x, minZ));
            ring.add(at(x, maxZ));
        }

        for (int z = minZ; z <= maxZ; z++) {
            ring.add(at(minX, z));
            ring.add(at(maxX, z));
        }

        return ring;
    }

    @Test
    @DisplayName("Regra 208: o anel antigo da plantação sai do grafo e vira sobra, mesmo longe da linha")
    void anOldFarmRingLeavesTheGraph() {
        Set<ColonyPos> ring = ring(2, 10, 6, 12);
        RoadLines.Result result = build(streetWithAnOldRing(ring), at(0, 0), Set.of(), ring);
        Set<ColonyPos> columns = columnsOf(result);

        assertFalse(columns.contains(at(6, 12)), "o lado de baixo do anel ficou no grafo: " + columns);
        assertFalse(columns.contains(at(10, 12)), "a quina ficou no grafo: " + columns);
        assertFalse(columns.contains(at(6, 6)), "o lado de cima do anel ficou no grafo: " + columns);
        assertTrue(columns.containsAll(List.of(at(-20, 0), at(20, 0))), "a rua saiu junto");
        assertTrue(result.surplus().containsAll(List.of(at(6, 12), at(10, 12))), "o anel longe da linha não virou sobra: " + result.surplus());
    }

    @Test
    @DisplayName("Regra 208: a entrada da plantação fica, com o ramal até a rua")
    void theFarmEntranceKeepsItsBranch() {
        ColonyPos door = at(2, 6);
        Set<ColonyPos> ring = ring(2, 10, 6, 12);
        RoadLines.Result result = build(streetWithAnOldRing(ring), at(0, 0), Set.of(door), ring);
        Set<ColonyPos> columns = columnsOf(result);

        assertTrue(result.graph().orElseThrow().nodes().stream().anyMatch(node -> node.position().equals(door)
                && node.roles().contains(RoadGraph.Role.ENTRANCE)), "a quina de entrada saiu do grafo");
        assertTrue(columns.containsAll(List.of(at(2, 1), at(2, 5))), "a entrada perdeu o ramal: " + columns);
        assertFalse(columns.contains(at(6, 12)), "o resto do anel ficou no grafo: " + columns);
    }

    @Test
    @DisplayName("Regra 208: o trecho colado na plantação que é a única ligação da rede fica")
    void aRingSideThatIsTheOnlyLinkStays() {
        Set<ColonyPos> columns = columnsOf(build(corridor(-20, 40, 0, 0), at(0, 0), Set.of(), ring(5, 15, 0, 6)));

        assertTrue(columns.containsAll(List.of(at(10, 0), at(40, 0))), "a rede partiu: " + columns);
    }

    @Test
    @DisplayName("Regras 206 e 229: toco que não é a ponta do lado dele sai do grafo, e o caminho dele vira sobra")
    void aStubInsideTheVillageLeavesTheGraph() {
        Set<ColonyPos> cells = corridor(-20, 20, 0, 0);

        // Do lado leste, mais perto do centro que a ponta (20, 0).
        cells.addAll(corridor(12, 12, 1, 3));

        RoadLines.Result result = build(cells, at(0, 0), Set.of(), Set.of());
        Set<ColonyPos> columns = columnsOf(result);

        assertFalse(columns.contains(at(12, 3)), "o toco ficou no grafo: " + columns);
        assertFalse(columns.contains(at(12, 1)), "o trecho morto ficou no grafo: " + columns);
        assertTrue(columns.containsAll(List.of(at(-20, 0), at(20, 0))), "as pontas dos lados saíram");
        assertTrue(result.surplus().contains(at(12, 1)), "o caminho do toco não virou sobra: " + result.surplus());
    }

    @Test
    @DisplayName("Regra 229: o toco que é o único do lado sul vira a ponta sul")
    void theOnlyStubOnASideIsThatSidesTip() {
        Set<ColonyPos> cells = corridor(-20, 20, 0, 0);

        cells.addAll(corridor(5, 5, 1, 8));

        assertTrue(columnsOf(build(cells, at(0, 0), Set.of(), Set.of())).contains(at(5, 8)), "a ponta sul saiu do grafo");
    }

    @Test
    @DisplayName("Regras 206 e 229: no máximo uma ponta por lado da vila, a mais distante")
    void atMostOneTipPerSide() {
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
        assertEquals(tips.size(), tips.stream().map(node -> RoadSides.of(node.position().x(), node.position().z(), at(0, 0))).distinct().count(),
                "duas pontas no mesmo lado: " + tips);
        assertTrue(tips.stream().anyMatch(node -> node.position().equals(at(20, 0))), "a ponta leste não é a mais distante: " + tips);
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

    @Test
    @DisplayName("toda entrada é nó do grafo, mesmo longe demais para ligar agora")
    void anEntranceFarFromTheNetworkIsStillInTheGraph() {
        ColonyPos far = at(500, 500);
        RoadLines.Result result = build(corridor(0, 20, 0, 0), at(10, 0), Set.of(far), Set.of());
        RoadGraph graph = result.graph().orElseThrow();

        assertTrue(graph.nodes().stream().anyMatch(node -> node.position().equals(far) && node.roles().contains(RoadGraph.Role.ENTRANCE)),
                "Regra 214: a entrada sem ligação sumiu do grafo: " + graph.nodes());
        assertEquals(1, graph.nodes().stream().filter(node -> node.roles().contains(RoadGraph.Role.HUB)).count(), "um nó central só");
        assertTrue(graph.nodes().stream().noneMatch(node -> node.position().equals(far) && node.roles().contains(RoadGraph.Role.HUB)),
                "o centro saiu da rede e foi para a entrada solta");
    }

    @Test
    @DisplayName("o centro é da rede, mesmo com uma entrada solta mais perto dele")
    void theHubStaysOnTheNetworkWhenAnEntranceIsCloser() {
        ColonyPos center = at(100, 100);
        ColonyPos door = at(100, 99);
        RoadLines.Result result = build(corridor(0, 20, 0, 0), center, Set.of(door), Set.of());
        RoadGraph graph = result.graph().orElseThrow();

        assertTrue(graph.nodes().stream().anyMatch(node -> node.position().equals(door) && node.roles().contains(RoadGraph.Role.ENTRANCE)
                && !node.roles().contains(RoadGraph.Role.HUB)), "Regra 214: a entrada solta virou centro: " + graph.nodes());
    }

    /** Duas partes da rua separadas por um riacho de {@code wet} colunas, de x=21 em diante. */
    private static PathTerrain withWater(int wet, boolean water) {
        return new PathTerrain() {
            @Override
            public OptionalInt groundY(int x, int z, int nearY) {
                return !water && z >= -2 && z <= 2 && x >= 21 && x < 21 + wet ? OptionalInt.empty() : OptionalInt.of(Y);
            }

            @Override
            public boolean blocked(int x, int z) {
                return Math.abs(z) > 2;
            }
        };
    }

    @Test
    @DisplayName("Regra 264: sem saída a seco, a ligação não atravessa a água e a parte fica solta")
    void aLinkNeverCrossesWater() {
        Set<ColonyPos> cells = corridor(0, 20, 0, 0);

        cells.addAll(corridor(24, 40, 0, 0));

        RoadLines.Result result = RoadLines.build(cells, Set.of(), at(10, 0), Set.of(), Set.of(), withWater(3, false), 1, 0);

        assertFalse(columnsOf(result).contains(at(22, 0)), "Regra 264: a linha cruzou o riacho: " + result.toPave());
        assertEquals(1, result.isolated(), "Regra 264: a parte do outro lado devia ficar solta");
    }

    @Test
    @DisplayName("Regra 264: coluna traçada antes sobre a água sai da linha na releitura")
    void aPlannedColumnOverWaterLeavesTheLine() {
        Set<ColonyPos> cells = corridor(0, 20, 0, 0);
        Set<ColonyPos> planned = Set.of(at(21, 0), at(22, 0), at(23, 0));

        cells.addAll(corridor(24, 40, 0, 0));

        RoadLines.Result result = RoadLines.build(cells, planned, at(10, 0), Set.of(), Set.of(), withWater(3, false), 1, 0);

        assertFalse(columnsOf(result).contains(at(22, 0)), "Regra 264: a coluna planejada sobre a água ficou: " + result.toPave());
    }
}
