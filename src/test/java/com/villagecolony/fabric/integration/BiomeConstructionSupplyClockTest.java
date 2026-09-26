package com.villagecolony.fabric.integration;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

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
    void theThirdFailedProfessionAttemptSurvivesSaving() {
        UUID colony = UUID.randomUUID();

        assertFalse(BiomeConstructionSupply.failedProfessionAttempt(colony, Items.BREWING_STAND));
        assertFalse(BiomeConstructionSupply.failedProfessionAttempt(colony, Items.BREWING_STAND));

        Map<String, Integer> saved = BiomeConstructionSupply.failedProfessionAttempts();

        BiomeConstructionSupply.clearAll();
        BiomeConstructionSupply.restoreFailedProfessionAttempts(saved);

        assertTrue(BiomeConstructionSupply.failedProfessionAttempt(colony, Items.BREWING_STAND),
                "a terceira tentativa recomeçou ao carregar o mundo");

        BiomeConstructionSupply.routeDelivered(colony, Items.BREWING_STAND);

        assertFalse(BiomeConstructionSupply.failedProfessionAttempt(colony, Items.BREWING_STAND),
                "a entrega não reiniciou a contagem de tentativas");
    }
}
