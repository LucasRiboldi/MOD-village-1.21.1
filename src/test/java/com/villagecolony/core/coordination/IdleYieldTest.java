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

        assertFalse(IdleYield.observe(colony, ProfessionType.SMELTER, 16, 3), "uma janela só não basta");
        assertTrue(IdleYield.observe(colony, ProfessionType.SMELTER, 16, 3), "duas janelas paradas não cederam");
        assertFalse(IdleYield.observe(colony, ProfessionType.SMELTER, 16, 2), "a conta não recomeçou depois de ceder");
    }

    @Test
    void aBusyWindowBreaksTheStreak() {
        UUID colony = UUID.randomUUID();

        IdleYield.observe(colony, ProfessionType.SMELTER, 16, 3);
        IdleYield.observe(colony, ProfessionType.SMELTER, 15, 3);

        assertFalse(IdleYield.observe(colony, ProfessionType.SMELTER, 16, 3));
    }

    @Test
    void fifteenPercentIdleDoesNotYield() {
        UUID colony = UUID.randomUUID();

        IdleYield.observe(colony, ProfessionType.SMELTER, 15, 3);

        assertFalse(IdleYield.observe(colony, ProfessionType.SMELTER, 15, 3));
    }

    @Test
    void theLastPersonOfATradeDoesNotYieldWhenNoOtherTradeIsShort() {
        UUID colony = UUID.randomUUID();

        IdleYield.observe(colony, ProfessionType.SHEPHERD, 90, 1);

        assertFalse(IdleYield.observe(colony, ProfessionType.SHEPHERD, 90, 1));
    }

    /** Vaga por demanda — P5c, 2026-10-11: o pastor parado vai aonde a obra tem falta. */
    @Test
    void theLastPersonYieldsToATradeTheBuildIsShortOf() {
        UUID colony = UUID.randomUUID();
        java.util.List<ProfessionType> minerShort = java.util.List.of(ProfessionType.MINER);

        IdleYield.observe(colony, ProfessionType.SHEPHERD, 68, 1, minerShort);

        assertTrue(IdleYield.observe(colony, ProfessionType.SHEPHERD, 68, 1, minerShort),
                "o mineiro tem falta para a obra e o pastor estava parado: devia ceder");
    }

    @Test
    void theLastPersonKeepsATradeTheBuildIsShortOfAndTheBuilderNeverGoes() {
        UUID colony = UUID.randomUUID();
        java.util.List<ProfessionType> bothShort = java.util.List.of(ProfessionType.MINER, ProfessionType.SHEPHERD);
        java.util.List<ProfessionType> minerShort = java.util.List.of(ProfessionType.MINER);

        IdleYield.observe(colony, ProfessionType.SHEPHERD, 68, 1, bothShort);
        assertFalse(IdleYield.observe(colony, ProfessionType.SHEPHERD, 68, 1, bothShort),
                "a obra também espera lã: o único pastor fica");

        IdleYield.observe(colony, ProfessionType.BUILDER, 81, 1, minerShort);
        assertFalse(IdleYield.observe(colony, ProfessionType.BUILDER, 81, 1, minerShort),
                "sem o único construtor a obra para");
    }
}
