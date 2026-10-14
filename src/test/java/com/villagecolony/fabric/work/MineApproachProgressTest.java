package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A regra de progresso útil do percurso até a boca — decisão do autor, 2026-10-14: um waypoint só
 * vale quando encurta a distância à boca de verdade, tolerando pequenas oscilações e o waypoint
 * repetido. Lógica pura, sem mundo.
 */
class MineApproachProgressTest {

    @Test
    void closerByTheMarginIsProgress() {
        double closest = 60.0;

        assertTrue(MineApproachPath.usefulProgress(closest - MineApproachPath.PROGRESS_MIN, closest),
                "encurtar pela margem conta como progresso");
        assertTrue(MineApproachPath.usefulProgress(40.0, closest), "bem mais perto é progresso");
    }

    @Test
    void aTinyOscillationIsNotProgress() {
        double closest = 60.0;

        assertFalse(MineApproachPath.usefulProgress(closest - 0.5, closest),
                "meia unidade abaixo da margem não é progresso");
        assertFalse(MineApproachPath.usefulProgress(closest, closest), "igual não é progresso");
        assertFalse(MineApproachPath.usefulProgress(closest + 5, closest), "mais longe não é progresso");
    }

    @Test
    void theFirstWaypointIsAlwaysProgress() {
        // Sem waypoint ainda, o "closest" começa no infinito — qualquer ponto finito avança.
        assertTrue(MineApproachPath.usefulProgress(95.0, Double.MAX_VALUE),
                "o primeiro waypoint de um episódio sempre avança");
    }
}
