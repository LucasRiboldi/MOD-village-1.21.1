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
    void theWaitIsFiveThenTenThenTwentyMinutes() {
        assertEquals(6_000, EmptySweeps.memoryFor(1));
        assertEquals(12_000, EmptySweeps.memoryFor(2));
        assertEquals(24_000, EmptySweeps.memoryFor(3));
        assertEquals(24_000, EmptySweeps.memoryFor(9), "o castigo para de crescer em vinte minutos");
    }

    @Test
    void anEmptySweepHoldsTheResourceOffForItsWait() {
        EmptySweeps.foundNothing(COLONY, ResourceType.SAND, 0);

        assertTrue(EmptySweeps.isWaiting(COLONY, ResourceType.SAND, 5_999));
        assertFalse(EmptySweeps.isWaiting(COLONY, ResourceType.SAND, 6_000));
        assertFalse(EmptySweeps.isWaiting(COLONY, ResourceType.DIRT, 10), "só o recurso que faltou");
        assertFalse(EmptySweeps.isWaiting(UUID.randomUUID(), ResourceType.SAND, 10), "só a colônia que varreu");
    }

    @Test
    void anotherEmptySweepWaitsLongerAndFindingItStartsOver() {
        EmptySweeps.foundNothing(COLONY, ResourceType.SAND, 0);
        EmptySweeps.foundNothing(COLONY, ResourceType.SAND, 6_000);

        assertTrue(EmptySweeps.isWaiting(COLONY, ResourceType.SAND, 6_000 + 11_999));

        EmptySweeps.found(COLONY, ResourceType.SAND);

        assertFalse(EmptySweeps.isWaiting(COLONY, ResourceType.SAND, 6_001), "achou: a espera acaba");

        EmptySweeps.foundNothing(COLONY, ResourceType.SAND, 20_000);

        assertFalse(EmptySweeps.isWaiting(COLONY, ResourceType.SAND, 26_000), "e a próxima volta ao primeiro castigo");
    }
}
