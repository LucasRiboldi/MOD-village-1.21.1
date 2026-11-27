package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.NeighborhoodRoutes;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.construction.model.RoadEdgeState;
import com.villagecolony.core.construction.model.RoadLink;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regra 390 (ADR-065 etapa 2, L2 do jogo da 0.4.150: "só calça caminhos que já estão travados definitivamente"): no modo
 * 3, quem calça só recebe as colunas da ligação travada ou já calçada.
 */
class LockedStepsTest {

    private static ColonyPos at(int x) {
        return new ColonyPos(x, 64, 0);
    }

    private static PathStep step(int x) {
        return new PathStep(x, 64, 0, Side.EAST, 0, 0);
    }

    @Test
    @DisplayName("Regra 390: a ligação planejada não vai para quem calça; a travada vai")
    void onlyLockedLinksArePaved() {
        NeighborhoodRoutes routes = NeighborhoodRoutes.restore(Map.of(
                List.of(1L, 2L), new RoadLink(List.of(at(0), at(1)), RoadEdgeState.PLANNED, 1, ""),
                List.of(3L, 4L), new RoadLink(List.of(at(5), at(6)), RoadEdgeState.LOCKED, 3, ""),
                List.of(5L, 6L), new RoadLink(List.of(at(8)), RoadEdgeState.PAVED, 3, "")), Map.of(), Map.of());

        List<PathStep> paved = RoadGraphBuilds.lockedSteps(routes, List.of(step(0), step(1), step(5), step(6), step(8)));

        assertEquals(List.of(step(5), step(6), step(8)), paved, "Regra 390: o calçador recebeu coluna de ligação planejada");
    }
}
