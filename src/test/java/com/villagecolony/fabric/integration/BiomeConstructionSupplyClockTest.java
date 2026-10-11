package com.villagecolony.fabric.integration;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * As esperas de muda sobrevivem a fechar o mundo.
 */
class BiomeConstructionSupplyClockTest {

    @BeforeAll
    static void bootMinecraft() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @AfterEach
    void forget() {
        BiomeConstructionSupply.clearAll();
    }

    @Test
    void aTreeRequestBecomesEligibleOnlyOnTheTwentiethWaitAndSurvivesSaving() {
        UUID colony = UUID.randomUUID();

        for (int wait = 1; wait < 20; wait++) {
            assertFalse(BiomeConstructionSupply.treeWaitReached(colony, TreeSpecies.OAK),
                    "a muda foi pedida antes da vigesima espera: " + wait);
        }

        Map<String, Integer> saved = BiomeConstructionSupply.treeWaitAttempts();
        BiomeConstructionSupply.clearAll();
        BiomeConstructionSupply.restoreTreeWaitAttempts(saved);

        assertTrue(BiomeConstructionSupply.treeWaitReached(colony, TreeSpecies.OAK),
                "a vigesima espera nao liberou a muda");
    }
}
