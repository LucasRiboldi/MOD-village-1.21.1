package com.villagecolony.fabric.integration;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * As tentativas de suprir uma peça sem profissão sobrevivem a fechar o mundo.
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
    void theFailedAttemptCountSurvivesSavingAndTheDeliveryClearsIt() {
        UUID colony = UUID.randomUUID();

        // Desde 2026-09-30 (F5) a primeira falta sem rota já libera a peça.
        assertTrue(BiomeConstructionSupply.failedProfessionAttempt(colony, Items.BREWING_STAND),
                "a primeira falta sem rota não liberou a peça");

        Map<String, Integer> saved = BiomeConstructionSupply.failedProfessionAttempts();

        BiomeConstructionSupply.clearAll();
        BiomeConstructionSupply.restoreFailedProfessionAttempts(saved);

        assertEquals(saved, BiomeConstructionSupply.failedProfessionAttempts(),
                "a contagem de faltas se perdeu ao carregar o mundo");

        BiomeConstructionSupply.routeDelivered(colony, Items.BREWING_STAND);

        assertTrue(BiomeConstructionSupply.failedProfessionAttempts().isEmpty(),
                "a entrega não apagou a contagem de faltas");
    }
}
