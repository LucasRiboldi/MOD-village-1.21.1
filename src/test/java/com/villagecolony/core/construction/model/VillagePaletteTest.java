package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * De que uma vila é feita — a Regra 20 dita por inteiro, 2026-08-20.
 */
class VillagePaletteTest {

    private static ResourceId vanilla(String path) {
        return new ResourceId(ResourceId.VANILLA, path);
    }

    @Test
    void theDoorComesFromTheSpeciesOfThePlanks() {
        assertEquals(
                vanilla("spruce_door"),
                VillagePalette.ofWood(vanilla("spruce_planks")).door().orElseThrow());

        assertEquals(
                vanilla("acacia_door"),
                VillagePalette.ofWood(vanilla("acacia_planks")).door().orElseThrow());
    }

    @Test
    void aWoodenVillageWallsInItsOwnSpecies() {
        assertEquals(
                vanilla("acacia_planks"),
                VillagePalette.ofWood(vanilla("acacia_planks")).wall());
    }

    /**
     * O deserto não tem porta, e é decisão e não esquecimento.
     *
     * <p>A porta sai de tábua, tábua sai de tronco, e o deserto não tem
     * tronco. Exigi-la deixaria a casa em espera para sempre — que é o
     * travamento que a Regra 13 corrigiu.
     */
    @Test
    void theDesertHasNoDoorToMake() {
        assertFalse(VillagePalette.ofSandstone().hasDoor());
    }

    /** No deserto a pedra é a parede: é ela que faz a vila construir. */
    @Test
    void theDesertWallsInTheStoneItMines() {
        VillagePalette desert = VillagePalette.ofSandstone();

        assertEquals(VillagePalette.SANDSTONE, desert.wall());
        assertEquals(VillagePalette.SANDSTONE, desert.stone());
    }

    @Test
    void everyVillageShearsAndSmeltsTheSameThings() {
        for (VillagePalette palette : new VillagePalette[] {
                VillagePalette.ofWood(vanilla("oak_planks")),
                VillagePalette.ofSandstone()}) {

            assertEquals(VillagePalette.GLASS, palette.glass());
            assertEquals(VillagePalette.WOOL, palette.wool());
        }
    }

    /**
     * A cabana do deserto sobe sem uma tábua, e sem porta.
     *
     * <p>É o que fecha o limite conhecido do §"deserto": até 2026-08-20
     * a vila de deserto nascia, contratava, contava recurso e não
     * construía nunca.
     */
    @Test
    void theDesertHutIsAllStoneAndHasNoDoor() {
        Blueprint hut = ColonyHut.blueprint(VillagePalette.ofSandstone(), Side.NORTH);

        assertTrue(
                hut.blocks().stream()
                        .filter(block -> !block.furniture())
                        .allMatch(block -> VillagePalette.SANDSTONE.equals(block.block())),
                "a cabana do deserto tem bloco que não é arenito");

        assertFalse(
                hut.blocks().stream().anyMatch(block -> block.block().path().endsWith("_door")),
                "a cabana do deserto ganhou uma porta que ninguém ali sabe fazer");
    }

    /** E a de madeira continua com a porta dela. */
    @Test
    void theWoodenHutStillGetsItsDoor() {
        Blueprint hut = ColonyHut.blueprint(
                VillagePalette.ofWood(ColonyHut.OAK_PLANKS), Side.NORTH);

        assertTrue(
                hut.blocks().stream()
                        .anyMatch(block -> vanilla("oak_door").equals(block.block())),
                "a cabana de carvalho perdeu a porta");
    }
}
