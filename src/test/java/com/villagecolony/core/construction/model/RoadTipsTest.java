package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A ponta azul fica na extremidade do grafo e aponta para fora — Regra 231. */
@DisplayName("Pontas na extremidade")
class RoadTipsTest {

    private static final int Y = 64;
    private static final ColonyPos CENTER = new ColonyPos(0, Y, 0);

    private static void line(Map<Long, Integer> columns, int fromX, int fromZ, int toX, int toZ) {
        for (int x = Math.min(fromX, toX); x <= Math.max(fromX, toX); x++) {
            for (int z = Math.min(fromZ, toZ); z <= Math.max(fromZ, toZ); z++) {
                columns.put(ColonyRoads.column(x, z), Y);
            }
        }
    }

    /** Um anel de 41 por 41 em volta do centro, cortado ao meio por uma rua leste-oeste. */
    private static Map<Long, Integer> ring() {
        Map<Long, Integer> columns = new HashMap<>();

        line(columns, -20, -20, 20, -20);
        line(columns, -20, 20, 20, 20);
        line(columns, -20, -20, -20, 20);
        line(columns, 20, -20, 20, 20);
        line(columns, -20, 0, 20, 0);

        return columns;
    }

    private static boolean has(Map<Long, Integer> columns, int x, int z) {
        return columns.containsKey(ColonyRoads.column(x, z));
    }

    @Test
    @DisplayName("a ponta entre linhas, longe da extremidade do lado dela, sai")
    void aTipBetweenLinesIsCut() {
        Map<Long, Integer> columns = ring();

        line(columns, 10, 8, 10, 19);
        RoadTips.prune(columns, CENTER, Set.of(), Set.of());

        assertFalse(has(columns, 10, 8), "a ponta no meio da vila ficou");
        assertFalse(has(columns, 10, 19), "o trecho morto até o anel ficou");
        assertTrue(has(columns, 10, 20), "o anel perdeu coluna");
    }

    @Test
    @DisplayName("a ponta na extremidade que aponta para fora fica, e a que corre de lado sai")
    void theTipAtTheEdgePointingOutwardStays() {
        Map<Long, Integer> columns = ring();

        line(columns, 10, 21, 10, 25);
        line(columns, -15, 21, -15, 26);
        line(columns, -19, 26, -16, 26);
        RoadTips.prune(columns, CENTER, Set.of(), Set.of());

        assertTrue(has(columns, 10, 25), "a ponta que aponta para fora saiu");
        assertFalse(has(columns, -19, 26), "ficou a ponta que corre de lado");
    }
}
