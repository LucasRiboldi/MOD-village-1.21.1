package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O artesão vai até a bancada antes de fabricar, mas com salvaguarda — regra do autor, 2026-10-15,
 * reconciliando com a de 2026-10-11 ("fabrica onde está; a caminhada até a bancada soltava a tarefa").
 *
 * <p>Ele só insiste em caminhar enquanto <b>não chegou</b> e o <b>orçamento</b> de ida não estourou;
 * chegando, ou estourando o prazo (bancada inalcançável, ex.: na montanha), fabrica onde está — nunca
 * larga a tarefa por não alcançar a estação. Lógica pura, no padrão de {@link MinerRerouteTest}.
 */
class BenchApproachTest {

    @Test
    void craftsAtTheBenchWhenItHasArrived() {
        assertTrue(CraftStation.craftHereNow(true, 0, CraftStation.APPROACH_BUDGET),
                "chegou à bancada: fabrica nela");
    }

    @Test
    void walksWhileAwayAndWithinBudget() {
        assertFalse(CraftStation.craftHereNow(false, 10, CraftStation.APPROACH_BUDGET),
                "longe e dentro do prazo: continua indo até a bancada");
    }

    @Test
    void craftsHereWhenTheBudgetRunsOut() {
        assertTrue(CraftStation.craftHereNow(false, CraftStation.APPROACH_BUDGET, CraftStation.APPROACH_BUDGET),
                "prazo estourado (bancada inalcançável): fabrica onde está, não larga a tarefa");
    }

    @Test
    void craftsHereWhenThereIsNoBenchToWalkTo() {
        // ticks = -1 é o código de "não há bancada" — fabrica onde está, sem andar.
        assertTrue(CraftStation.craftHereNow(false, -1, CraftStation.APPROACH_BUDGET),
                "sem bancada ao alcance: fabrica onde está (trabalho normal)");
    }
}
