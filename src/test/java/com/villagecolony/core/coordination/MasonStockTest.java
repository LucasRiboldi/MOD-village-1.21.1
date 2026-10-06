package com.villagecolony.core.coordination;

import com.villagecolony.core.type.Production;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O pedreiro não fica parado: mantém peças da pedra da vila — ADR-036 item 8. */
class MasonStockTest {

    @Test
    void aPlainsVillageKeepsCobblestoneSlabsAndStoneBricks() {
        assertEquals(List.of(ResourceType.COBBLESTONE_SLAB, ResourceType.STONE_BRICKS),
                MasonStock.piecesFor(ResourceType.COBBLESTONE));
    }

    @Test
    void aDesertVillageKeepsSandstoneStairsSlabsAndCutSandstone() {
        assertEquals(List.of(ResourceType.SANDSTONE_STAIRS, ResourceType.SANDSTONE_SLAB,
                        ResourceType.CUT_SANDSTONE),
                MasonStock.piecesFor(ResourceType.SANDSTONE));
    }

    @Test
    void everyPieceIsMasonWork() {
        for (ResourceType stone : List.of(ResourceType.COBBLESTONE, ResourceType.SANDSTONE)) {
            for (ResourceType piece : MasonStock.piecesFor(stone)) {
                assertEquals(Production.CRAFTED_STONE, piece.production(), piece.name());
            }
        }
    }

    @Test
    void theFloorIsAddedWithoutLoweringABiggerGoal() {
        Map<ResourceType, Integer> goals = new EnumMap<>(ResourceType.class);
        goals.put(ResourceType.STONE_BRICKS, 40);

        MasonStock.addTo(goals, ResourceType.COBBLESTONE);

        assertEquals(40, goals.get(ResourceType.STONE_BRICKS), "a meta maior da obra foi rebaixada");
        assertEquals(MasonStock.FLOOR, goals.get(ResourceType.COBBLESTONE_SLAB));
    }

    @Test
    void aStoneWithoutPiecesAddsNothing() {
        Map<ResourceType, Integer> goals = new EnumMap<>(ResourceType.class);

        MasonStock.addTo(goals, ResourceType.WHEAT);

        assertTrue(goals.isEmpty());
    }
}
