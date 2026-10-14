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
    private static final ResourceId GRASS = ResourceId.vanilla("grass_block");
    private static final ResourceId FENCE = ResourceId.vanilla("oak_fence");

    private static List<BlueprintBlock> layers(int bottom, int above) {
        return layers(bottom, STONE, above, STONE);
    }

    private static List<BlueprintBlock> layers(int bottom, ResourceId bottomBlock, int above, ResourceId aboveBlock) {
        List<BlueprintBlock> blocks = new ArrayList<>();

        for (int i = 0; i < bottom; i++) {
            blocks.add(new BlueprintBlock(new ColonyPos(i, 0, 0), bottomBlock));
        }

        for (int i = 0; i < above; i++) {
            blocks.add(new BlueprintBlock(new ColonyPos(i, 1, 1), aboveBlock));
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

    /** O curral: a camada 0 é só grama (o chão de pisar) e vai no nível da rua, não abaixo. */
    @Test
    void aSoilBottomLayerIsTheStreetLevel() {
        // animal_pen: 30 de grama embaixo, 21 de cerca em cima — o sunkenBase não pega (grama densa).
        assertEquals(OptionalInt.of(0), StreetLayer.soilBase(layers(30, GRASS, 21, FENCE)));
    }

    /** Piso construído na camada 0 não é solo: segue como antes (sem nivelar pela camada 0). */
    @Test
    void aBuiltFloorIsNotSoil() {
        assertEquals(OptionalInt.empty(), StreetLayer.soilBase(layers(30, STONE, 21, FENCE)));
    }

    /** Camada 0 de grama misturada com piso construído não conta como solo puro. */
    @Test
    void aMixedBottomLayerIsNotPureSoil() {
        List<BlueprintBlock> blocks = layers(10, GRASS, 21, FENCE);
        blocks.add(new BlueprintBlock(new ColonyPos(99, 0, 0), STONE));

        assertEquals(OptionalInt.empty(), StreetLayer.soilBase(blocks));
    }
}
