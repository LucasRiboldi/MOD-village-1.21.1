package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A peça de lado volta à fila quatro vezes e surge na quinta — Regra 113. */
class PieceRescueTest {

    @Test
    void thePieceComesBackFourTimesAndThenSurges() {
        PieceRescue rescue = new PieceRescue();
        ColonyPos at = new ColonyPos(1, 2, 3);

        for (int time = 1; time <= PieceRescue.TRIES; time++) {
            assertEquals(PieceRescue.Verdict.RETRY, rescue.next(at), "tentativa " + time);
            assertEquals(time, rescue.triesAt(at));
        }

        assertEquals(PieceRescue.Verdict.SURGE, rescue.next(at));
        assertEquals(PieceRescue.TRIES, rescue.triesAt(at));
    }

    @Test
    void eachPositionCountsItsOwnTries() {
        PieceRescue rescue = new PieceRescue();
        ColonyPos first = new ColonyPos(0, 0, 0);
        ColonyPos second = new ColonyPos(0, 1, 0);

        for (int time = 0; time < PieceRescue.TRIES; time++) {
            rescue.next(first);
        }

        assertEquals(PieceRescue.Verdict.RETRY, rescue.next(second));
        assertEquals(0, rescue.triesAt(new ColonyPos(9, 9, 9)));
        assertEquals(4, PieceRescue.TRIES, "quatro tentativas — decisão do autor, 2026-10-22");
    }
}
