package com.villagecolony.fabric.event;

import com.villagecolony.core.colony.service.VillageDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A mesma tarefa não volta ao mesmo trabalhador entre ciclos — E49, 2026-09-30.
 *
 * <p><b>O que o playtest de 30-09 mediu:</b> seis fundidores da vila foco
 * soltaram a tarefa de vidro por falta de areia <b>6.324 vezes em 18
 * minutos</b>, uma por segundo cada. A entrega entre ciclos (F10) devolvia a
 * tarefa recém-solta ao mesmo aldeão no segundo seguinte, e ele a soltava de
 * novo. Antes do F10 a volta era a cada ciclo de 30 s.
 */
class IdleHandoutsTest {

    private static final long WINDOW = VillageDetector.CYCLE_TICKS;

    private final IdleHandouts handouts = new IdleHandouts();
    private final UUID worker = UUID.randomUUID();
    private final UUID task = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handouts.clear();
    }

    @Test
    void anythingMayBeHandedTheFirstTime() {
        assertTrue(handouts.mayHand(worker, task, 1_000));
    }

    @Test
    void theSameTaskDoesNotGoBackToTheSameWorkerBeforeACycle() {
        handouts.handed(worker, task, 1_000);

        assertFalse(handouts.mayHand(worker, task, 1_020),
                "a tarefa que ele acabou de soltar voltou a ele um segundo depois");
        assertFalse(handouts.mayHand(worker, task, 1_000 + WINDOW - 1));
    }

    @Test
    void afterACycleTheSamePairMayMeetAgain() {
        handouts.handed(worker, task, 1_000);

        assertTrue(handouts.mayHand(worker, task, 1_000 + WINDOW));
    }

    @Test
    void anotherTaskStillGoesToHimAtOnce() {
        handouts.handed(worker, task, 1_000);

        assertTrue(handouts.mayHand(worker, UUID.randomUUID(), 1_020),
                "quem terminou uma tarefa tem de receber a próxima sem esperar o ciclo");
    }

    @Test
    void theSameTaskStillGoesToAnotherWorkerAtOnce() {
        handouts.handed(worker, task, 1_000);

        assertTrue(handouts.mayHand(UUID.randomUUID(), task, 1_020));
    }

    @Test
    void aClockThatWentBackDoesNotLockThePairForever() {
        handouts.handed(worker, task, 50_000);

        assertTrue(handouts.mayHand(worker, task, 10),
                "o /time set volta o relógio do mundo; o par não pode ficar preso");
    }

    @Test
    void oldHandoutsAreForgottenSoTheMemoryHasACeiling() {
        for (int i = 0; i < 100; i++) {
            handouts.handed(UUID.randomUUID(), UUID.randomUUID(), 1_000);
        }
        handouts.handed(worker, task, 1_000 + WINDOW);

        handouts.forgetExpired(1_000 + WINDOW);

        assertEquals(1, handouts.size());
    }
}
