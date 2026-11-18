package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regra 315: o alvo que a conferência recusou fica de lado por 30 segundos, só ele e só para quem o recusou. */
class FarmerSetAsideTest {

    private final UUID farmer = UUID.randomUUID();

    private final BlockPos plot = new BlockPos(309, 63, -2607);

    @AfterEach
    void forget() {
        FarmerSetAside.clearAll();
    }

    @Test
    void aRefusedTargetStaysAsideUntilTheTimeRunsOut() {
        FarmerSetAside.refuse(farmer, plot, 1_000);

        assertTrue(FarmerSetAside.isSetAside(farmer, plot, 1_000 + FarmerSetAside.TICKS - 1));
        assertFalse(FarmerSetAside.isSetAside(farmer, plot, 1_000 + FarmerSetAside.TICKS));
    }

    @Test
    void onlyThatBlockAndOnlyForThatFarmer() {
        FarmerSetAside.refuse(farmer, plot, 1_000);

        assertFalse(FarmerSetAside.isSetAside(farmer, plot.east(), 1_001));
        assertFalse(FarmerSetAside.isSetAside(UUID.randomUUID(), plot, 1_001));
    }
}
