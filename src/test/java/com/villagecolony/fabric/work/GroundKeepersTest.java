package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Quem arruma o chão da vila e o quê — Regra 242. */
@DisplayName("Arrumar o chão")
class GroundKeepersTest {

    private static final int Y = 64;

    private static ColonyPos at(int x, int z) {
        return new ColonyPos(x, Y, z);
    }

    @Test
    @DisplayName("só o caminho sem nenhuma marca do grafo volta ao solo")
    void onlyTheUnmarkedPathGoesBack() {
        Set<ColonyPos> line = new HashSet<>();

        for (int x = 0; x <= 10; x++) {
            line.add(at(x, 0));
        }

        RoadGraph graph = RoadGraph.from(line, at(0, 0), Set.of());
        Set<ColonyPos> inside = new HashSet<>(line);

        inside.add(at(5, 4));
        inside.add(at(3, 2));
        inside.add(at(7, 3));
        inside.add(at(9, 5));

        List<ColonyPos> back = PathTrim.unmarked(inside, graph, Set.of(com.villagecolony.core.construction.model.ColonyRoads.column(3, 2)),
                List.of(), List.of(at(7, 3)));

        assertEquals(Set.of(at(5, 4), at(9, 5)), new HashSet<>(back), "Regra 242: a linha, a porta ou o planejado voltou ao solo");
    }

    @Test
    @DisplayName("o vão entre as bases vai em rampa de degrau 1 de uma base à outra")
    void theGapRampsFromBaseToBase() {
        int previous = 64;

        for (int step = 1; step <= 4; step++) {
            int floor = BuildingGaps.rampAt(64, 68, step, 5);

            assertTrue(floor - previous >= 0 && floor - previous <= 1, "Regra 242: degrau maior que 1 na rampa: " + floor);
            previous = floor;
        }

        assertTrue(68 - previous <= 1, "Regra 242: a rampa não chegou à outra base: " + previous);
        assertEquals(64, BuildingGaps.rampAt(64, 64, 3, 5), "Regra 242: bases iguais deram rampa");
        assertEquals(8, BuildingGaps.MAX_GAP, "Regra 242: o vão vai até 8 blocos");
        assertEquals(4, BuildingGaps.LEVEL, "Regra 242: o corte e o aterro vão até 4");
    }

    @Test
    @DisplayName("o carpinteiro e o fundidor entram no rodízio de quem calça")
    void carpenterAndSmelterPave() {
        assertTrue(PathPaving.PAVERS.containsAll(List.of(Optional.of(ProfessionType.CARPENTER), Optional.of(ProfessionType.SMELTER))),
                "Regra 242: carpinteiro ou fundidor fora do rodízio do calçamento");
    }
}
