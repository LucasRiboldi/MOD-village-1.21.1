package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.PatienceClock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A obra com peça sem apoio tenta se recompor antes de ceder o lote. */
class WaitingWorkPatienceTest {

    @Test
    void onlyUnsupportedPiecesKeepTheFullRescueWindow() {
        assertFalse(WaitingWork.ranOutOfPatience(true, WaitingWork.UNSUPPORTED_PATIENCE),
                "peça sem apoio não pode liberar a obra antes da revisão de apoio e suprimento");
        assertFalse(WaitingWork.ranOutOfPatience(true, PatienceClock.TICKS - 1));
        assertTrue(WaitingWork.ranOutOfPatience(true, PatienceClock.TICKS));
    }

    @Test
    void aBuildThatStillHasPiecesToPlaceKeepsTheLongClock() {
        assertFalse(WaitingWork.ranOutOfPatience(false, WaitingWork.UNSUPPORTED_PATIENCE * 2),
                "obra com peça a pôr não sai em dois minutos");
    }
}
