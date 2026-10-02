package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O ponto onde a descida trava — F-1, 2026-10-02: na sessão das 01:36 três
 * mineiros congelaram sete vezes em {@code -241, 10, 379}, cada vez atrás de uma
 * pedra diferente lá embaixo ({@code y=-15}, {@code y=-49}).
 */
class MineDescentTest {

    private static final BlockPos STUCK = new BlockPos(-241, 10, 379);

    private static final BlockPos DEEP = new BlockPos(-241, -15, 378);

    private static final BlockPos DEEPER_AND_ASIDE = new BlockPos(-251, -48, 391);

    @BeforeEach
    void forget() {
        MineDescent.clearAll();
    }

    @Test
    void oneFreezeIsNotEnough() {
        MineDescent.stoppedAt(0, STUCK, DEEP);

        assertFalse(MineDescent.blocksAt(10, DEEP), "uma parada pode ser azar de navegação");
    }

    @Test
    void theSecondFreezeAtTheSameSpotSkipsEveryStoneBelowIt() {
        MineDescent.stoppedAt(0, STUCK, DEEP);
        MineDescent.stoppedAt(100, STUCK.add(1, 0, 0), DEEPER_AND_ASIDE);

        assertTrue(MineDescent.blocksAt(200, DEEP), "a pedra da primeira vez");
        assertTrue(MineDescent.blocksAt(200, DEEPER_AND_ASIDE), "outra pedra, outro ramal, abaixo do mesmo ponto");
        assertFalse(MineDescent.blocksAt(200, STUCK.up(5)), "acima do ponto ele passou: nada muda");
        assertFalse(MineDescent.blocksAt(200, STUCK.add(MineDescent.RADIUS + 4, -20, 0)), "longe para o lado, outra mina");
    }

    @Test
    void theSkipExpiresAndGrowsIfItHappensAgain() {
        MineDescent.stoppedAt(0, STUCK, DEEP);
        MineDescent.stoppedAt(0, STUCK, DEEP);

        long first = MineDescent.memoryFor(MineDescent.TIMES_TO_BLOCK);

        assertEquals(MineMarks.memoryFor(1), first);
        assertTrue(MineDescent.blocksAt(first - 1, DEEP));
        assertFalse(MineDescent.blocksAt(first, DEEP), "o castigo vence");

        MineDescent.stoppedAt(first, STUCK, DEEP);

        assertTrue(MineDescent.blocksAt(first + first, DEEP), "travou de novo: o prazo dobrou");
    }

    @Test
    void aStoneAtTheMinersOwnLevelIsNotADescent() {
        MineDescent.stoppedAt(0, STUCK, STUCK.add(3, -1, 0));
        MineDescent.stoppedAt(0, STUCK, STUCK.add(3, -1, 0));

        assertFalse(MineDescent.blocksAt(10, DEEP), "desistir de pedra ao lado não é descida travada");
    }
}
