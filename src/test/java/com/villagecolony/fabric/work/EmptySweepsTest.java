package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O raio varrido inteiro e vazio — F-1, 2026-10-02: o fundidor varreu atrás de
 * areia 38 vezes em 20 minutos sem achar.
 */
class EmptySweepsTest {

    private static final UUID COLONY = UUID.randomUUID();

    @BeforeEach
    void forget() {
        EmptySweeps.clearAll();
    }

    @Test
    void theWaitGrowsOneMinuteAtATimeUpToFive() {
        assertEquals(1_200, EmptySweeps.memoryFor(1));
        assertEquals(2_400, EmptySweeps.memoryFor(2));
        assertEquals(3_600, EmptySweeps.memoryFor(3));
        assertEquals(4_800, EmptySweeps.memoryFor(4));
        assertEquals(6_000, EmptySweeps.memoryFor(5));
        assertEquals(6_000, EmptySweeps.memoryFor(9), "o castigo para de crescer em cinco minutos");
    }

    @Test
    void anEmptySweepHoldsTheResourceOffForItsWait() {
        assertEquals(1, EmptySweeps.foundNothing(COLONY, ResourceType.SAND, 0));

        assertTrue(EmptySweeps.isWaiting(COLONY, ResourceType.SAND, 1_199));
        assertFalse(EmptySweeps.isWaiting(COLONY, ResourceType.SAND, 1_200));
        assertFalse(EmptySweeps.isWaiting(COLONY, ResourceType.DIRT, 10), "só o recurso que faltou");
        assertFalse(EmptySweeps.isWaiting(UUID.randomUUID(), ResourceType.SAND, 10), "só a colônia que varreu");
    }

    @Test
    void anotherEmptySweepWaitsLongerAndFindingItStartsOver() {
        EmptySweeps.foundNothing(COLONY, ResourceType.SAND, 0);

        assertEquals(2, EmptySweeps.foundNothing(COLONY, ResourceType.SAND, 1_200), "a contagem é a que a entrega lê");
        assertTrue(EmptySweeps.isWaiting(COLONY, ResourceType.SAND, 1_200 + 2_399));

        EmptySweeps.found(COLONY, ResourceType.SAND);

        assertFalse(EmptySweeps.isWaiting(COLONY, ResourceType.SAND, 1_201), "achou: a espera acaba");

        assertEquals(1, EmptySweeps.foundNothing(COLONY, ResourceType.SAND, 20_000), "e a contagem recomeça");

        assertFalse(EmptySweeps.isWaiting(COLONY, ResourceType.SAND, 21_200), "e a próxima volta ao primeiro castigo");
    }
}
