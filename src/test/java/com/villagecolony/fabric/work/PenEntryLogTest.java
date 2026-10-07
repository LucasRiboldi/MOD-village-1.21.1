package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A linha da entrada no curral responde à pergunta do estudo de 01-10, §8-A. */
class PenEntryLogTest {

    @Test
    void theLineSaysWhereItWasGoingAndWhereItCameFrom() {
        assertEquals("Worker 1a2b3c4d entered a pen at 10, 64, -3 standing on hay_block"
                        + " — path to 12, 64, -3, work target none, walk target 12, 64, -3,"
                        + " task none; last seen free at 9, 65, -3",
                PenEntryLog.line("Worker", "1a2b3c4d", "10, 64, -3", "12, 64, -3", "none",
                        "12, 64, -3", "none", "9, 65, -3", "hay_block"));
    }
}
