package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A ida do artesão até a bancada, acompanhada ao longo de uma tarefa — regra do autor de 2026-10-15,
 * com a salvaguarda que reconcilia com a de 2026-10-11 ("fabrica onde está").
 *
 * <p>O teste anterior cobria só a regra de um tique e passava com o defeito G1 da varredura de
 * 2026-10-20: o chamador zerava o contador ao desistir, e o artesão voltava a caminhar a cada 200
 * tiques. Aqui o mesmo {@link CraftStation.Approach} é levado por tiques seguidos, como na tarefa.
 */
class BenchApproachTest {

    private static final BlockPos BENCH = new BlockPos(10, 64, 10);

    @Test
    void withoutABenchItCraftsWhereItIsFromTheFirstTick() {
        CraftStation.Approach approach = new CraftStation.Approach(null);

        assertTrue(approach.craftHere(false), "sem bancada: trabalho normal, no lugar");
    }

    @Test
    void itWalksWhileAwayAndCraftsOnceItArrives() {
        CraftStation.Approach approach = new CraftStation.Approach(BENCH);

        assertFalse(approach.craftHere(false), "longe e dentro do prazo: caminha");
        assertFalse(approach.craftHere(false), "ainda longe: segue caminhando");
        assertTrue(approach.craftHere(true), "chegou: fabrica na bancada");
    }

    @Test
    void onceTheBudgetRunsOutItKeepsCraftingHereUntilTheTaskEnds() {
        CraftStation.Approach approach = new CraftStation.Approach(BENCH);

        for (int tick = 1; tick < CraftStation.APPROACH_BUDGET; tick++) {
            assertFalse(approach.craftHere(false), "dentro do prazo, tique " + tick + ": caminha");
        }

        assertTrue(approach.craftHere(false), "prazo estourado: desiste da bancada e fabrica no lugar");

        // O defeito G1: depois de desistir, ele voltava a caminhar. Agora fica no lugar até o fim.
        for (int tick = 0; tick < 3 * CraftStation.APPROACH_BUDGET; tick++) {
            assertTrue(approach.craftHere(false), "depois de desistir, tique " + tick + ": não volta a caminhar");
        }
    }
}
