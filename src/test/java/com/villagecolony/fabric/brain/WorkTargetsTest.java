package com.villagecolony.fabric.brain;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O destino do aldeão, e com que folga ele conta como chegado —
 * 2026-08-29.
 *
 * <p><b>A folga não era do destino, e precisava ser.</b> Ela vivia como
 * uma constante só dentro do {@code GoToWorkTargetTask}, valendo dois
 * blocos para todo mundo. Dois é certo para o lenhador — o destino dele
 * <b>é</b> a árvore, e parar dois antes é parar dentro do braço — e é
 * fatal para o mineiro, cujo destino já é o lugar exato de ficar de pé.
 *
 * <p>A sessão de 2026-08-28, 23:19, mostrou a conta: o mineiro parou a
 * exatamente dois blocos do lugar escolhido e ficou 4,2 da pedra, com
 * braço de 4. Quem sabe de quanto precisa é quem põe o destino.
 */
class WorkTargetsTest {

    private final UUID villager = UUID.randomUUID();

    private static final BlockPos SOMEWHERE = new BlockPos(758, 44, 878);

    @BeforeEach
    void forgetTheLastSession() {
        WorkTargets.clearAll();
    }

    /** Sem destino não há folga a perguntar, e a resposta é a de casa. */
    @Test
    void aVillagerWithoutADestinationHasTheDefaultSlack() {
        assertTrue(WorkTargets.of(villager).isEmpty());
        assertEquals(WorkTargets.DEFAULT_ARRIVAL, WorkTargets.arrivalOf(villager));
    }

    /** Quem não escolhe folga fica com a de casa, que é a do lenhador. */
    @Test
    void thePlainDestinationKeepsTheDefaultSlack() {
        WorkTargets.set(villager, SOMEWHERE);

        assertEquals(SOMEWHERE, WorkTargets.of(villager).orElseThrow());
        assertEquals(WorkTargets.DEFAULT_ARRIVAL, WorkTargets.arrivalOf(villager));
    }

    /** Quem precisa chegar mais perto diz de quanto precisa. */
    @Test
    void aDestinationCanAskToBeReachedCloser() {
        WorkTargets.set(villager, SOMEWHERE, 1);

        assertEquals(SOMEWHERE, WorkTargets.of(villager).orElseThrow());
        assertEquals(1, WorkTargets.arrivalOf(villager));
    }

    /** Repor um destino sem folga não guarda a folga do anterior. */
    @Test
    void aNewDestinationDoesNotInheritTheOldSlack() {
        WorkTargets.set(villager, SOMEWHERE, 1);
        WorkTargets.set(villager, SOMEWHERE.up());

        assertEquals(WorkTargets.DEFAULT_ARRIVAL, WorkTargets.arrivalOf(villager));
    }

    /** Cedido de volta ao Vanilla, ele não guarda folga nenhuma. */
    @Test
    void givingTheVillagerBackForgetsTheSlackToo() {
        WorkTargets.set(villager, SOMEWHERE, 1);

        WorkTargets.clear(villager);

        assertTrue(WorkTargets.of(villager).isEmpty());
        assertEquals(WorkTargets.DEFAULT_ARRIVAL, WorkTargets.arrivalOf(villager));
    }
}
