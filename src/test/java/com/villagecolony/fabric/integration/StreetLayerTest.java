package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Planta sem porta nem encaixe: a camada 0 esparsa é subterrânea, e a rua
 * fica nela — o {@code barn_majest} tem 5 peças na camada 0 e 85 na 1.
 */
class StreetLayerTest {

    private static final ResourceId STONE = ResourceId.vanilla("tuff_bricks");

    private static List<BlueprintBlock> layers(int bottom, int above) {
        List<BlueprintBlock> blocks = new ArrayList<>();

        for (int i = 0; i < bottom; i++) {
            blocks.add(new BlueprintBlock(new ColonyPos(i, 0, 0), STONE));
        }

        for (int i = 0; i < above; i++) {
            blocks.add(new BlueprintBlock(new ColonyPos(i, 1, 1), STONE));
        }

        return blocks;
    }

    @Test
    void aSparseBottomLayerIsBelowTheStreet() {
        assertEquals(OptionalInt.of(0), StreetLayer.sunkenBase(layers(5, 85)));
    }

    @Test
    void aFullBottomLayerStaysOnTopOfTheGround() {
        // As plantas da casa grande e da pequena: a camada 0 é piso.
        assertEquals(OptionalInt.empty(), StreetLayer.sunkenBase(layers(36, 26)));
        assertEquals(OptionalInt.empty(), StreetLayer.sunkenBase(layers(24, 19)));
    }

    @Test
    void halfIsStillAFloor() {
        assertEquals(OptionalInt.empty(), StreetLayer.sunkenBase(layers(10, 20)));
        assertEquals(OptionalInt.of(0), StreetLayer.sunkenBase(layers(9, 20)));
    }

    @Test
    void aSingleLayerPlanHasNothingBelowTheStreet() {
        assertEquals(OptionalInt.empty(), StreetLayer.sunkenBase(layers(12, 0)));
    }
}
