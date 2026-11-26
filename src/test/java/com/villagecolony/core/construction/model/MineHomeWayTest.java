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

/**
 * Regra 386 (nota do jogo da 0.4.148, K5 🅱️: "o caminho do arco da mina até a casa do mineiro tem que ter um caminho que
 * una os dois e esse caminho deve ser unido até o grafo mais perto existente"): a porta da casa do mineiro e a entrada da
 * mina ficam ligadas por um caminho, e esse trecho se liga à linha mais perto, mesmo além dos 64 blocos das outras portas.
 */
class MineHomeWayTest {

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

    /** Um degrau de três blocos na coluna x = 10: a reta que o cruza não serve. */
    private static final PathTerrain CLIFF = new PathTerrain() {
        @Override
        public OptionalInt groundY(int x, int z, int nearY) {
            return OptionalInt.of(x >= 10 ? Y + 3 : Y);
        }

        @Override
        public boolean blocked(int x, int z) {
            return false;
        }

        @Override
        public boolean canCut(int x, int z, int floorY) {
            return false;
        }
    };

    private static Set<ColonyPos> road() {
        Set<ColonyPos> road = new HashSet<>();

        for (int x = 0; x <= 50; x++) {
            road.add(new ColonyPos(x, Y, 0));
        }

        return road;
    }

    @Test
    @DisplayName("Regra 386: a porta da casa e a entrada da mina ficam ligadas, coluna a coluna")
    void theHouseAndTheMineAreJoined() {
        ColonyPos house = new ColonyPos(30, Y, 86);
        ColonyPos mine = new ColonyPos(25, Y, 80);
        List<ColonyPos> way = MineHomeWay.between(house, mine, FLAT);

        assertEquals(11, way.size(), "o caminho em L dá 11 passos da porta até a entrada da mina: " + way);
        assertEquals(mine, way.getLast(), "o caminho não chega à entrada da mina");

        ColonyPos before = house;

        for (ColonyPos at : way) {
            assertEquals(1, Math.abs(at.x() - before.x()) + Math.abs(at.z() - before.z()), "o caminho pulou de " + before
                    + " para " + at);
            before = at;
        }
    }

    @Test
    @DisplayName("Regra 386: sem chão andável entre as duas, não há caminho inventado")
    void noWayOverACliff() {
        assertTrue(MineHomeWay.between(new ColonyPos(5, Y, 5), new ColonyPos(15, Y + 3, 5), CLIFF).isEmpty(),
                "o caminho subiu três blocos de uma vez");
    }

    @Test
    @DisplayName("Regra 386: a mina a 80 blocos da rua se liga à linha mais perto")
    void theFarMineReachesTheLine() {
        ColonyPos house = new ColonyPos(30, Y, 86);
        ColonyPos mine = new ColonyPos(25, Y, 80);
        Set<ColonyPos> planned = new HashSet<>(MineHomeWay.between(house, mine, FLAT));

        RoadLines.Result alone = RoadLines.build(road(), planned, new ColonyPos(25, Y, 0), Set.of(house, mine), Set.of(),
                FLAT, 1, 0, Set.of(), Set.of());

        assertFalse(alone.loose().isEmpty(), "sem a Regra 386 a mina a 80 blocos já se ligava; o teste não mede nada");

        RoadLines.Result result = RoadLines.build(road(), planned, new ColonyPos(25, Y, 0), Set.of(house, mine), Set.of(),
                FLAT, 1, 0, Set.of(), Set.of(), Set.of(mine));

        assertTrue(result.loose().isEmpty(), "Regra 386: a mina ficou solta: " + result.loose());

        RoadGraph graph = result.graph().orElseThrow();

        assertTrue(onTheLine(graph, house), "Regra 386: a porta da casa do mineiro ficou fora da linha");
        assertTrue(onTheLine(graph, mine), "Regra 386: a entrada da mina ficou fora da linha");
    }

    private static boolean onTheLine(RoadGraph graph, ColonyPos at) {
        return graph.nodes().stream().anyMatch(node -> node.position().x() == at.x() && node.position().z() == at.z())
                || graph.edges().stream().anyMatch(edge -> edge.columns().stream()
                        .anyMatch(column -> column.x() == at.x() && column.z() == at.z()));
    }
}
