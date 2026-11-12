package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A troca de blocos pelo bioma da vila — Regra 262: madeira pela da vila, pedregulho e arenito pela pedra da paleta. */
class BiomeBlockSwapTest {

    private static final BiomeBlockSwap TAIGA = BiomeBlockSwap.of(VillagePalette.ofWood("taiga"),
            ResourceId.vanilla("spruce_planks"));

    private static final BiomeBlockSwap DESERT = BiomeBlockSwap.of(VillagePalette.ofSandstone(),
            ResourceId.vanilla("oak_planks"));

    private static ResourceId id(String path) {
        return ResourceId.vanilla(path);
    }

    @Test
    void everyWoodBecomesTheVillageWoodInTheSameShape() {
        assertEquals(id("spruce_fence"), TAIGA.apply(id("oak_fence")));
        assertEquals(id("spruce_fence_gate"), TAIGA.apply(id("dark_oak_fence_gate")));
        assertEquals(id("stripped_spruce_log"), TAIGA.apply(id("stripped_acacia_log")));
        assertEquals(id("oak_log"), DESERT.apply(id("spruce_log")));
        assertEquals(id("stripped_oak_log"), DESERT.apply(id("stripped_spruce_log")));
    }

    @Test
    void cobblestoneAndSandstoneTradePlacesInTheSameShape() {
        assertEquals(id("sandstone"), DESERT.apply(id("cobblestone")));
        assertEquals(id("sandstone_slab"), DESERT.apply(id("cobblestone_slab")));
        assertEquals(id("sandstone_wall"), DESERT.apply(id("cobblestone_wall")));
        assertEquals(id("cobblestone_stairs"), TAIGA.apply(id("sandstone_stairs")));
    }

    @Test
    void theRestOfTheFileStaysAsItIs() {
        for (String path : List.of("chest", "lantern", "grass_block", "farmland", "mossy_cobblestone", "cut_sandstone",
                "tuff_bricks", "deepslate_tiles", "torch")) {
            assertEquals(id(path), DESERT.apply(id(path)), path);
        }

        ResourceId modBlock = new ResourceId("villagecolony", "oak_bench");
        assertEquals(modBlock, TAIGA.apply(modBlock));
    }

    @Test
    void theSamePieceOfAnotherWoodPlaysTheSameRole() {
        assertTrue(BiomeBlockSwap.sameRole(id("oak_fence"), id("spruce_fence")));
        assertTrue(BiomeBlockSwap.sameRole(id("stripped_spruce_log"), id("stripped_oak_log")));
        assertTrue(BiomeBlockSwap.sameRole(id("sandstone_slab"), id("cobblestone_slab")));
        assertFalse(BiomeBlockSwap.sameRole(id("oak_fence"), id("oak_fence_gate")));
        assertFalse(BiomeBlockSwap.sameRole(id("oak_log"), id("stripped_oak_log")));
        assertFalse(BiomeBlockSwap.sameRole(id("chest"), id("barrel")));
    }

    @Test
    void theBlueprintKeepsOrderStatesAndStreetLayer() {
        BlueprintBlock fence = new BlueprintBlock(new ColonyPos(0, 1, 0), id("oak_fence"), false, Optional.empty(),
                Map.of("north", "true"));
        BlueprintBlock grass = new BlueprintBlock(new ColonyPos(0, 0, 0), id("grass_block"));
        Blueprint plan = Blueprint.of(new ResourceId("villagecolony", "colony/animal_pen"), List.of(fence, grass))
                .withStreetLayer(0);

        Blueprint swapped = TAIGA.apply(plan, any -> true);

        assertEquals(List.of(id("spruce_fence"), id("grass_block")),
                swapped.blocks().stream().map(BlueprintBlock::block).toList());
        assertEquals(Map.of("north", "true"), swapped.blocks().getFirst().states());
        assertEquals(0, swapped.streetLayer());
        assertEquals(plan.id(), swapped.id());
        assertSame(plan, TAIGA.apply(plan, any -> false), "nome que o jogo não tem fica como estava");
    }
}
