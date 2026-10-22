package com.villagecolony.core.coordination;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A lista declarada do que pode surgir — Regra 111. */
class SupplyExceptionTest {

    @Test
    void everyExceptionNamesTheRuleThatAuthorisesItAndWhatItMakes() {
        for (SupplyException exception : SupplyException.values()) {
            assertTrue(exception.rule() > 0, exception + " sem regra");
            assertFalse(exception.what().isBlank(), exception + " sem descrição");
        }

        assertEquals(SupplyException.values().length,
                Arrays.stream(SupplyException.values()).map(SupplyException::rule).distinct().count(),
                "duas exceções com a mesma regra");
    }

    @Test
    void theRuleLeadsBackToItsException() {
        assertEquals(Optional.of(SupplyException.MISSING_PIECE), SupplyException.ofRule(51));
        assertEquals(Optional.of(SupplyException.FOUNDING_ANIMALS), SupplyException.ofRule(102));
        assertEquals(Optional.empty(), SupplyException.ofRule(74), "a cama perto do trabalho não faz surgir nada");
        assertEquals(51, SupplyException.MISSING_PIECE.rule());
    }
}
