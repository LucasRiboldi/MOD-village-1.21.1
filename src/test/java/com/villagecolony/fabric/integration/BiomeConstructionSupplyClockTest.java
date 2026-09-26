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
 * O relógio de "rota atrasada" sobrevive a fechar o mundo — decisão do autor,
 * 2026-09-26.
 *
 * <p>Na sessão de 26-09 a obra esperou o tapete verde por 4min30s e o autor
 * saiu 30 s antes de os 10 ciclos da ADR-022 fecharem. O relógio vivia só em
 * memória: ao reabrir, a espera recomeçava do zero.
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
    void aWaitStartedBeforeSavingIsStillRunningAfterLoading() {
        UUID colony = UUID.randomUUID();
        long started = 1_000L;

        assertFalse(BiomeConstructionSupply.routeIsOverdue(colony, Items.GREEN_CARPET, started));

        Map<String, Long> saved = BiomeConstructionSupply.waits();

        BiomeConstructionSupply.clearAll();
        BiomeConstructionSupply.restore(saved);

        assertTrue(BiomeConstructionSupply.routeIsOverdue(
                        colony, Items.GREEN_CARPET, started + BiomeConstructionSupply.OVERDUE_TICKS),
                "o relógio recomeçou ao carregar: a espera de antes de fechar o mundo se perdeu");
    }
}
