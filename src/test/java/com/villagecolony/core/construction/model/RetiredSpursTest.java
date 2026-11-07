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

/** Sem entrada não há ramal, e o bloco de nós não existe — Regra 223. */
@DisplayName("RetiredSpurs")
class RetiredSpursTest {

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

    private static RoadGraph graphOf(Set<ColonyPos> cells) {
        return RoadGraph.from(cells, at(0, 0), Set.of());
    }

    @Test
    @DisplayName("o ramal longo sai inteiro, até a linha, e a linha fica")
    void aLongSpurLeavesUpToTheLine() {
        Set<ColonyPos> line = corridor(0, 40, 0, 0);
        Set<ColonyPos> physical = new HashSet<>(line);

        physical.addAll(corridor(20, 20, 1, 12));

        List<ColonyPos> spur = RetiredSpurs.of(physical, graphOf(line), Set.of(at(20, 12)));

        assertEquals(12, spur.size(), "o ramal de 12 blocos, além dos 3 da Regra 186: " + spur);
        assertTrue(spur.stream().noneMatch(line::contains), "a linha não sai");
    }

    @Test
    @DisplayName("coluna que deixou de ser entrada mas está na linha não leva nada")
    void aRetiredColumnOnTheLineTakesNothing() {
        Set<ColonyPos> line = corridor(0, 40, 0, 0);
        Set<ColonyPos> physical = new HashSet<>(line);

        physical.addAll(corridor(20, 20, 1, 5));

        assertTrue(RetiredSpurs.of(physical, graphOf(line), Set.of(at(20, 0))).isEmpty());
    }

    @Test
    @DisplayName("trecho do tamanho de uma rua fica: pode ser do jogador ou da vila gerada")
    void aStreetSizedStretchStays() {
        Set<ColonyPos> line = corridor(0, 40, 0, 0);
        Set<ColonyPos> physical = new HashSet<>(line);

        physical.addAll(corridor(20, 20, 1, RetiredSpurs.MAX_CELLS + 1));

        assertTrue(RetiredSpurs.of(physical, graphOf(line), Set.of(at(20, RetiredSpurs.MAX_CELLS + 1))).isEmpty());
    }

    @Test
    @DisplayName("caminho que não encosta no ramal fica")
    void anUnrelatedPathStays() {
        Set<ColonyPos> line = corridor(0, 40, 0, 0);
        Set<ColonyPos> physical = new HashSet<>(line);

        physical.addAll(corridor(20, 20, 1, 3));
        physical.addAll(corridor(30, 30, 1, 3));

        List<ColonyPos> spur = RetiredSpurs.of(physical, graphOf(line), Set.of(at(20, 3)));

        assertFalse(spur.contains(at(30, 2)), "o outro ramal saiu: " + spur);
    }

    @Test
    @DisplayName("a quina tem linha perto: a rua que passa ao lado conta, o ramal da própria quina não")
    void aCornerSeesTheRoadBesideItButNotItsOwnSpur() {
        Set<ColonyPos> cells = corridor(0, 40, 0, 0);

        cells.addAll(corridor(20, 20, 1, 8));

        RoadGraph beside = RoadGraph.from(cells, at(0, 0), Set.of(at(20, 8)));

        assertTrue(beside.hasLineNear(at(21, 1), 2), "a rua passa a 1 bloco");
        assertFalse(beside.hasLineNear(at(20, 8), 2), "só o ramal da própria quina está perto");
        assertFalse(beside.hasLineNear(at(35, 8), 2), "longe de tudo");
    }

    /** Imagem 1 do autor: duas ruas desencontradas em 1 bloco, com o chão entre elas todo calçado. */
    private static Set<ColonyPos> theBlockOfTheImage() {
        Set<ColonyPos> cells = new HashSet<>();

        cells.addAll(corridor(115, 115, -278, -267));
        cells.addAll(corridor(114, 114, -267, -258));
        cells.addAll(corridor(105, 124, -267, -267));
        cells.addAll(corridor(114, 115, -270, -267));

        return cells;
    }

    private static int squares(RoadGraph graph) {
        Set<ColonyPos> line = new HashSet<>();

        graph.nodes().forEach(node -> line.add(node.position()));
        graph.edges().forEach(edge -> line.addAll(edge.columns()));

        return (int) line.stream().filter(cell -> line.contains(at(cell.x() + 1, cell.z()))
                && line.contains(at(cell.x(), cell.z() + 1)) && line.contains(at(cell.x() + 1, cell.z() + 1))).count();
    }

    /** O mesmo lugar, com o desnível do save: a rua oeste e a rua sul a 62, o resto a 63. */
    private static Set<ColonyPos> theBlockOfTheSave() {
        Set<ColonyPos> cells = new HashSet<>();

        for (int z = -278; z <= -268; z++) {
            cells.add(new ColonyPos(115, 63, z));
        }

        for (int z = -270; z <= -268; z++) {
            cells.add(new ColonyPos(114, 63, z));
        }

        for (int x = 106; x <= 114; x++) {
            cells.add(new ColonyPos(x, 62, -267));
        }

        for (int x = 115; x <= 124; x++) {
            cells.add(new ColonyPos(x, 63, -267));
        }

        for (int z = -266; z <= -258; z++) {
            cells.add(new ColonyPos(114, 62, z));
        }

        cells.add(new ColonyPos(118, 63, -266));

        return cells;
    }

    private static final PathTerrain SAVE = new PathTerrain() {
        @Override
        public OptionalInt groundY(int x, int z, int nearY) {
            return OptionalInt.of(z >= -267 && x <= 114 ? 62 : 63);
        }

        @Override
        public boolean blocked(int x, int z) {
            return false;
        }
    };

    @Test
    @DisplayName("com o desnível do save e sem o poste, restam só os encontros e a porta")
    void theBlockOfTheSaveBecomesTwoJunctions() {
        ColonyPos door = new ColonyPos(118, 63, -266);
        RoadGraph graph = RoadLines.build(theBlockOfTheSave(), Set.of(), new ColonyPos(124, 63, -267), Set.of(door), Set.of(), SAVE, 0, 0)
                .graph().orElseThrow();

        assertEquals(0, squares(graph), "ainda há quadrado de nós: " + graph.nodes());

        Set<ColonyPos> junctions = new HashSet<>();

        graph.nodes().stream().filter(node -> node.roles().contains(RoadGraph.Role.JUNCTION)
                && node.position().x() < 118).forEach(node -> junctions.add(node.position()));

        assertTrue(!junctions.isEmpty() && junctions.size() <= 2, "encontros demais: " + graph.nodes());
        assertTrue(graph.nodes().stream().anyMatch(node -> node.position().equals(door) && node.roles().contains(RoadGraph.Role.ENTRANCE)),
                "a porta sumiu: " + graph.nodes());
    }

    @Test
    @DisplayName("com o desnível do save e uma porta encostada no bloco, ele também não volta")
    void aDoorBesideTheBlockOfTheSaveDoesNotBringItBack() {
        Set<ColonyPos> cells = theBlockOfTheSave();

        cells.add(new ColonyPos(113, 63, -270));

        RoadGraph graph = RoadLines.build(cells, Set.of(), new ColonyPos(124, 63, -267), Set.of(new ColonyPos(113, 63, -270)), Set.of(), SAVE, 0, 0)
                .graph().orElseThrow();

        assertEquals(0, squares(graph), "a porta trouxe o quadrado de volta: " + graph.nodes());
    }

    @Test
    @DisplayName("quadrado que sobrou nas linhas sai, sem separar a rede nem tirar porta")
    void squaresLeaveWithoutSplittingTheNetwork() {
        java.util.Map<Long, Integer> columns = new java.util.HashMap<>();

        for (ColonyPos cell : theBlockOfTheImage()) {
            columns.put(ColonyRoads.column(cell.x(), cell.z()), Y);
        }

        columns.put(ColonyRoads.column(113, -270), Y);

        long door = ColonyRoads.column(113, -270);

        RoadSquares.thin(columns, Set.of(door));

        Set<ColonyPos> left = new HashSet<>();

        columns.keySet().forEach(column -> left.add(at(ColonyRoads.xOf(column), ColonyRoads.zOf(column))));

        assertEquals(1, RoadLines.components(columns).size(), "a rede se partiu: " + left);
        assertTrue(columns.containsKey(door), "a porta saiu");
        assertTrue(left.stream().noneMatch(cell -> left.contains(at(cell.x() + 1, cell.z()))
                && left.contains(at(cell.x(), cell.z() + 1)) && left.contains(at(cell.x() + 1, cell.z() + 1))), "ainda há quadrado: " + left);
    }

    @Test
    @DisplayName("sem o poste, o bloco de nós vira dois encontros de verdade")
    void theBlockBecomesTwoRealJunctions() {
        RoadGraph graph = RoadLines.build(theBlockOfTheImage(), Set.of(), at(124, -267), Set.of(), Set.of(), FLAT, 0, 0)
                .graph().orElseThrow();

        assertEquals(0, squares(graph), "ainda há quadrado de nós: " + graph.nodes());
        assertTrue(graph.nodes().stream().filter(node -> node.roles().contains(RoadGraph.Role.JUNCTION)).count() <= 2,
                "encontros demais: " + graph.nodes());
    }

    @Test
    @DisplayName("com uma porta encostada no bloco, ele também não volta")
    void aDoorBesideTheBlockDoesNotBringItBack() {
        RoadGraph graph = RoadLines.build(theBlockOfTheImage(), Set.of(), at(124, -267), Set.of(at(113, -270)), Set.of(), FLAT, 0, 0)
                .graph().orElseThrow();

        assertEquals(0, squares(graph), "a porta trouxe o quadrado de volta: " + graph.nodes());
    }
}
