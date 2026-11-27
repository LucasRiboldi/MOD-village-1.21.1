package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra 367 (notas do jogo da 0.4.143, H1 🅲: "nó de entrada não ligado ao grafo", seis vezes): a entrada de construção
 * se liga em toda releitura, sem esperar a vez da busca cara.
 */
class RoadLinesDoorsTest {

    private static final int Y = 64;

    /** Três portas, cada uma dentro de um "U" de colunas bloqueadas aberto para longe da rua: só a busca cara liga. */
    private static final Set<Long> WALLS = new HashSet<>();

    static {
        for (int cup : new int[] {5, 25, 45}) {
            for (int x = cup - 3; x <= cup + 3; x++) {
                WALLS.add(ColonyRoads.column(x, 8));
            }
            for (int z = 8; z <= 12; z++) {
                WALLS.add(ColonyRoads.column(cup - 3, z));
                WALLS.add(ColonyRoads.column(cup + 3, z));
            }
        }
    }

    private static final PathTerrain CUPS = new PathTerrain() {
        @Override
        public OptionalInt groundY(int x, int z, int nearY) {
            return OptionalInt.of(Y);
        }

        @Override
        public boolean blocked(int x, int z) {
            return WALLS.contains(ColonyRoads.column(x, z));
        }
    };

    @Test
    @DisplayName("Regra 367: toda porta se liga na mesma releitura, mesmo com a vez da busca cara gasta")
    void everyDoorLinksWithoutWaitingItsTurn() {
        Set<ColonyPos> road = new HashSet<>();

        for (int x = 0; x <= 50; x++) {
            road.add(new ColonyPos(x, Y, 0));
        }

        Set<ColonyPos> doors = Set.of(new ColonyPos(5, Y, 10), new ColonyPos(25, Y, 10), new ColonyPos(45, Y, 10));
        RoadLines.Result result = RoadLines.build(road, Set.of(), new ColonyPos(25, Y, 0), doors, Set.of(), CUPS, 1, 0);

        assertTrue(result.loose().stream().noneMatch(entry -> entry.startsWith("door@")),
                "Regra 367: porta esperando a vez: " + result.loose());
    }
}
