package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O resultado parcial impede que uma peça sem apoio pareça concluída. */
class ConstructionOutcomeTest {

    @Test
    void anUnsupportedPieceReleasesTheBuilderWithoutCountingAsPlaced() {
        ColonyPos position = new ColonyPos(12, 64, -3);

        ConstructionOutcome outcome = ConstructionOutcome.skipped(position, SkipReason.UNSUPPORTED);

        assertEquals(0, outcome.placedCount());
        assertTrue(outcome.releasesProjectSlot());
        assertEquals(Optional.of(SkipReason.UNSUPPORTED), outcome.reason());
    }
}
