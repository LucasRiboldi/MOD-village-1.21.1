package com.villagecolony.core.coordination;

import com.villagecolony.core.worker.model.ProfessionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ofício parado cede vaga (ADR-038 P1). */
class IdleYieldTest {

    @AfterEach
    void forget() {
        IdleYield.clearAll();
    }

    @Test
    void twoIdleWindowsInARowYieldOnePerson() {
        UUID colony = UUID.randomUUID();

        assertFalse(IdleYield.observe(colony, ProfessionType.SMELTER, 86, 3), "uma janela só não basta");
        assertTrue(IdleYield.observe(colony, ProfessionType.SMELTER, 80, 3), "duas janelas paradas não cederam");
        assertFalse(IdleYield.observe(colony, ProfessionType.SMELTER, 80, 2), "a conta não recomeçou depois de ceder");
    }

    @Test
    void aBusyWindowBreaksTheStreak() {
        UUID colony = UUID.randomUUID();

        IdleYield.observe(colony, ProfessionType.SMELTER, 86, 3);
        IdleYield.observe(colony, ProfessionType.SMELTER, 30, 3);

        assertFalse(IdleYield.observe(colony, ProfessionType.SMELTER, 86, 3));
    }

    @Test
    void theLastPersonOfATradeNeverYields() {
        UUID colony = UUID.randomUUID();

        IdleYield.observe(colony, ProfessionType.SHEPHERD, 90, 1);

        assertFalse(IdleYield.observe(colony, ProfessionType.SHEPHERD, 90, 1));
    }
}
