package com.villagecolony.fabric.work;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A frase de parada repetida não enche o log — 6.391 linhas iguais do fundidor desde 30-09. */
class StopLogTest {

    private static final UUID SMELTER = UUID.randomUUID();

    @BeforeEach
    void forget() {
        StopLog.clearAll();
    }

    @Test
    void theSameReasonIsWrittenOnceAndThenEveryHundred() {
        assertTrue(StopLog.stopped("Smelter", SMELTER, "no sand"));

        for (int i = 1; i < StopLog.EVERY; i++) {
            assertFalse(StopLog.stopped("Smelter", SMELTER, "no sand"), "repetição " + i + " saiu no log");
        }

        assertTrue(StopLog.stopped("Smelter", SMELTER, "no sand"), "a centésima repetição devia sair");
    }

    @Test
    void aNewReasonIsAlwaysWritten() {
        StopLog.stopped("Smelter", SMELTER, "no sand");
        StopLog.stopped("Smelter", SMELTER, "no sand");

        assertTrue(StopLog.stopped("Smelter", SMELTER, "no room in the chest"));
        assertTrue(StopLog.stopped("Smelter", UUID.randomUUID(), "no room in the chest"), "outro fundidor, outra conta");
    }
}
