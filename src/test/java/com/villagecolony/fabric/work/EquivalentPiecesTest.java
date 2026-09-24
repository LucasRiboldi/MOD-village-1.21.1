package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A peça equivalente pelo nome — N3, 2026-09-24. */
class EquivalentPiecesTest {

    @Test
    void aGraniteStairIsAnyStoneStairStartingWithCobblestone() {
        List<String> family = EquivalentPieces.familyOf("granite_stairs");

        assertEquals("cobblestone_stairs", family.get(0));
        assertTrue(family.contains("sandstone_stairs"), "no deserto a pedra da vila é o arenito");
        assertTrue(family.stream().allMatch(name -> name.endsWith("_stairs")),
                "a escada de pedra trocou de peça: " + family);
    }

    @Test
    void aWallStaysAWallAndASlabStaysASlab() {
        assertTrue(EquivalentPieces.familyOf("diorite_wall").contains("cobblestone_wall"));
        assertTrue(EquivalentPieces.familyOf("smooth_stone_slab").contains("stone_slab"));
        assertTrue(EquivalentPieces.familyOf("smooth_stone_slab").stream()
                .allMatch(name -> name.endsWith("_slab")));
    }

    @Test
    void aColouredPaneFallsBackToClearGlassLast() {
        List<String> family = EquivalentPieces.familyOf("yellow_stained_glass_pane");

        assertEquals("glass_pane", family.get(family.size() - 1));
        assertEquals(17, family.size(), "dezesseis cores e a vidraça sem cor");
    }

    @Test
    void aGlazedTerracottaFallsBackToPlainTerracotta() {
        List<String> family = EquivalentPieces.familyOf("orange_glazed_terracotta");

        assertTrue(family.contains("white_glazed_terracotta"));
        assertEquals("terracotta", family.get(family.size() - 1));
    }

    @Test
    void mossyStoneFallsBackToPlainStone() {
        assertEquals(List.of("cobblestone", "stone_bricks"),
                EquivalentPieces.familyOf("mossy_cobblestone"));
    }

    @Test
    void tallPlantsTradeOnlyWithTallPlants() {
        assertEquals(List.of("tall_grass", "large_fern"), EquivalentPieces.familyOf("large_fern"));
    }

    @Test
    void aWorkstationHasNoEquivalentAndStillComesReadyMade() {
        assertEquals(List.of(), EquivalentPieces.familyOf("lectern"));
        assertEquals(List.of(), EquivalentPieces.familyOf("oak_stairs"));
        assertEquals(List.of(), EquivalentPieces.familyOf("cobblestone"));
    }
}
