package com.villagecolony.core.colony.model;

import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O raio-x imutável de uma colônia — decisão 11A, 2026-09-24.
 */
class VillageInventoryTest {

    private static final ResourceId IRON = ResourceId.vanilla("iron_ingot");

    private static VillageInventory sample(ChestCoverage coverage) {
        return new VillageInventory(
                UUID.randomUUID(),
                4,
                6,
                Map.of(ColonyProfession.MINER, 2, ColonyProfession.LUMBERJACK, 1),
                3,
                1,
                coverage,
                Map.of(IRON, 5));
    }

    @Test
    void countOfMissingProfessionIsZero() {
        VillageInventory inventory = sample(new ChestCoverage(4, 0));

        assertEquals(0, inventory.countOf(ColonyProfession.SHEPHERD));
        assertEquals(2, inventory.countOf(ColonyProfession.MINER));
    }

    @Test
    void observedAmountOfUnseenResourceIsZero() {
        VillageInventory inventory = sample(new ChestCoverage(4, 0));

        assertEquals(0, inventory.observedAmountOf(ResourceId.vanilla("oak_log")));
        assertEquals(5, inventory.observedAmountOf(IRON));
    }

    @Test
    void inventoryKeepsPartialChestCoverageSeparateFromStock() {
        VillageInventory inventory = sample(new ChestCoverage(3, 1));

        assertTrue(inventory.chestCoverage().isPartial());
        // O estoque observado continua o que foi lido, mesmo parcial —
        // a fotografia não inventa o que não alcançou.
        assertEquals(5, inventory.observedAmountOf(IRON));
    }

    @Test
    void professionsMapIsUnmodifiable() {
        VillageInventory inventory = sample(new ChestCoverage(4, 0));

        assertThrows(UnsupportedOperationException.class,
                () -> inventory.professions().put(ColonyProfession.BUILDER, 1));
    }

    @Test
    void negativeAdultsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new VillageInventory(
                UUID.randomUUID(), -1, 0, Map.of(), 0, 0,
                new ChestCoverage(0, 0), Map.of()));
    }

    @Test
    void chestCoverageKnowsTotalKnownChests() {
        ChestCoverage coverage = new ChestCoverage(5, 2);

        assertEquals(7, coverage.chestsKnown());
        assertTrue(coverage.isPartial());
    }

    @Test
    void fullChestCoverageIsNotPartial() {
        ChestCoverage coverage = new ChestCoverage(8, 0);

        assertTrue(!coverage.isPartial());
    }
}
