package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ligar ao ponto mais perto (Regra 224), ler todo caminho (Regra 224) e travar o trecho que ficou (Regra 225). */
@DisplayName("Grafo estável")
class GraphStabilityTest {

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

    /** Uma parede em x=45, de cima a baixo do mapa. */
    private static final PathTerrain WALL = new PathTerrain() {
        @Override
        public OptionalInt groundY(int x, int z, int nearY) {
            return x == 45 ? OptionalInt.empty() : OptionalInt.of(Y);
        }

        @Override
        public boolean blocked(int x, int z) {
            return x == 45;
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

    private static Set<ColonyPos> lineOf(RoadGraph graph) {
        Set<ColonyPos> line = new HashSet<>();

        graph.nodes().forEach(node -> line.add(node.position()));
        graph.edges().forEach(edge -> line.addAll(edge.columns()));

        return line;
    }

    private static boolean linked(RoadGraph graph, ColonyPos at) {
        return graph.edges().stream().anyMatch(edge -> edge.from().equals(at) || edge.to().equals(at));
    }

    @Test
    @DisplayName("a porta fora das quatro retas liga em L ao ponto mais perto, sem a busca cara")
    void aDoorOffTheFourRaysLinksInAnL() {
        ColonyPos door = at(50, 10);
        RoadLines.Result result = RoadLines.build(corridor(0, 40, 0, 0), Set.of(), at(0, 0), Set.of(door), Set.of(), FLAT, 0, 0);
        RoadGraph graph = result.graph().orElseThrow();

        assertTrue(linked(graph, door), "a porta ficou solta: " + graph.edges());
        assertTrue(lineOf(graph).size() <= 41 + 20, "o ramal deu volta: " + lineOf(graph).size());
    }

    @Test
    @DisplayName("todas as partes soltas ligam na mesma releitura")
    void everyLoosePartLinksInTheSameRead() {
        Set<ColonyPos> cells = corridor(0, 40, 0, 0);

        cells.addAll(corridor(46, 52, 0, 0));
        cells.addAll(corridor(0, 6, 8, 8));

        RoadLines.Result result = RoadLines.build(cells, Set.of(), at(0, 0), Set.of(), Set.of(), FLAT, 0, 0);

        assertEquals(0, result.isolated(), "partes soltas: " + result.isolated());
        assertTrue(lineOf(result.graph().orElseThrow()).contains(at(52, 0)), "a parte do leste ficou fora");
    }

    @Test
    @DisplayName("a reta não atravessa parede")
    void theStraightLinkDoesNotCrossAWall() {
        ColonyPos door = at(50, 10);
        RoadGraph graph = RoadLines.build(corridor(0, 40, 0, 0), Set.of(), at(0, 0), Set.of(door), Set.of(), WALL, 0, 0)
                .graph().orElseThrow();

        assertFalse(linked(graph, door), "a ligação passou pela parede: " + graph.edges());
    }

    @Test
    @DisplayName("o trecho fixo fica onde está, mesmo com a faixa pedindo outra linha")
    void aFixedStretchStaysPut() {
        Set<ColonyPos> band = corridor(0, 30, -1, 1);
        Set<ColonyPos> fixed = corridor(0, 30, -1, -1);

        RoadGraph free = RoadLines.build(band, Set.of(), at(0, 0), Set.of(), Set.of(), FLAT, 0, 0, Set.of())
                .graph().orElseThrow();
        RoadGraph held = RoadLines.build(band, Set.of(), at(0, 0), Set.of(), Set.of(), FLAT, 0, 0, fixed)
                .graph().orElseThrow();

        assertTrue(lineOf(free).contains(at(15, 0)), "sem trava a linha é a do meio: " + lineOf(free));
        assertTrue(lineOf(held).containsAll(fixed), "o trecho fixo saiu da linha: " + lineOf(held));
        assertFalse(lineOf(held).contains(at(15, 0)), "ficou uma linha paralela ao trecho fixo");
    }

    @Test
    @DisplayName("a porta nova liga ao trecho fixo, que ganha um nó no encontro")
    void aNewDoorLinksToAFixedStretch() {
        Set<ColonyPos> fixed = corridor(0, 40, 0, 0);
        ColonyPos door = at(20, 10);
        RoadGraph graph = RoadLines.build(fixed, Set.of(), at(0, 0), Set.of(door), Set.of(), FLAT, 0, 0, fixed)
                .graph().orElseThrow();

        assertTrue(linked(graph, door), "a porta nova ficou solta do trecho fixo: " + graph.edges());
        assertTrue(lineOf(graph).containsAll(fixed), "a ligação moveu o trecho fixo: " + lineOf(graph));
        assertTrue(graph.nodes().stream().anyMatch(node -> node.position().z() == 0 && node.position().x() > 0 && node.position().x() < 40),
                "o encontro com o trecho fixo não virou nó: " + graph.nodes());
    }

    @Test
    @DisplayName("a coluna fica fixa só depois dos dias da trava, e recomeça ao sair da linha")
    void aColumnSettlesAfterTheDaysAndRestartsWhenItLeaves() {
        long column = ColonyRoads.column(3, 4);
        Map<Long, Long> since = RoadSettling.track(Map.of(), Set.of(column), 100);

        assertTrue(RoadSettling.fixed(since, 100 + RoadSettling.DAYS - 1).isEmpty(), "travou cedo");
        assertEquals(Set.of(column), RoadSettling.fixed(RoadSettling.track(since, Set.of(column), 105), 100 + RoadSettling.DAYS),
                "não travou no dia certo");

        Map<Long, Long> left = RoadSettling.track(since, Set.of(), 105);
        Map<Long, Long> back = RoadSettling.track(left, Set.of(column), 106);

        assertTrue(RoadSettling.fixed(back, 100 + RoadSettling.DAYS).isEmpty(), "a volta guardou o dia antigo");
        assertEquals(10, RoadSettling.DAYS, "a trava é de 10 dias, pedido do autor");
    }

    @Test
    @DisplayName("fora da caixa só entra o caminho que encosta no de dentro")
    void onlyThePathTouchingTheInsideEnters() {
        Set<ColonyPos> inside = corridor(0, 10, 0, 0);
        Set<ColonyPos> outside = corridor(11, 20, 0, 0);

        outside.addAll(corridor(40, 45, 0, 0));

        Set<ColonyPos> touching = RoadCells.touching(inside, outside);

        assertTrue(touching.contains(at(20, 0)), "o caminho que segue para fora ficou de fora");
        assertFalse(touching.contains(at(42, 0)), "entrou caminho solto de fora");
    }

    @Test
    @DisplayName("acima do teto, ficam os mais perto do centro")
    void aboveTheCapTheNearestStay() {
        Set<ColonyPos> cells = corridor(0, 99, 0, 0);
        Set<ColonyPos> kept = RoadCells.nearest(cells, at(0, 0), 10);

        assertEquals(10, kept.size());
        assertTrue(kept.contains(at(9, 0)) && !kept.contains(at(10, 0)), "o corte não foi pela distância: " + kept);
    }
}
