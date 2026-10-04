package com.villagecolony.fabric.work;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A espera antes do fundidor adiantar uma obra deve ser curta e explícita. */
class SmelterFallbackTest {

    @AfterEach
    void clearMemory() {
        SmelterFallback.clearAll();
    }

    @Test
    void startsHelpingTheBuildAfterTwoMisses() {
        UUID task = UUID.randomUUID();

        assertEquals(1, SmelterFallback.missed(task));
        assertFalse(SmelterFallback.isDue(task));

        assertEquals(2, SmelterFallback.missed(task));
        assertTrue(SmelterFallback.isDue(task));
    }
}
