package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A obra com só peça sem apoio sobrando sai da frente em um minuto — playtest de 2026-10-03, 18 min parada. */
class WaitingWorkPatienceTest {

    @Test
    void onlyUnsupportedPiecesWaitOneMinute() {
        assertFalse(WaitingWork.ranOutOfPatience(true, WaitingWork.UNSUPPORTED_PATIENCE - 1));
        assertTrue(WaitingWork.ranOutOfPatience(true, WaitingWork.UNSUPPORTED_PATIENCE));
    }

    @Test
    void aBuildThatStillHasPiecesToPlaceKeepsTheLongClock() {
        assertFalse(WaitingWork.ranOutOfPatience(false, WaitingWork.UNSUPPORTED_PATIENCE * 2),
                "obra com peça a pôr não sai em dois minutos");
    }
}
