package com.villagecolony.core.coordination;

import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.worker.model.ProfessionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Quem fabrica cada peça, pelo material — regra do autor, 2026-10-11 (itens do relatório de cobertura). */
class CraftFamilyTest {

    @Test
    void woodPiecesStayWithTheCarpenter() {
        for (String path : List.of("oak_stairs", "spruce_door", "acacia_fence", "chest", "barrel", "ladder", "torch",
                "crafting_table", "bookshelf", "lectern", "loom", "campfire", "redstone_torch", "smithing_table")) {
            assertEquals(TaskType.CRAFT_WOOD_MATERIAL, CraftFamily.of(path), path);
        }
    }

    @Test
    void stoneClayAndFurnacesGoToTheMason() {
        for (String path : List.of("cobblestone_stairs", "sandstone_wall", "bricks", "white_terracotta", "furnace",
                "blast_furnace", "smoker", "grindstone", "stonecutter", "flower_pot", "clay")) {
            assertEquals(TaskType.CRAFT_STONE_MATERIAL, CraftFamily.of(path), path);
        }
    }

    @Test
    void metalGlassIceAndSnowGoToTheSmelter() {
        for (String path : List.of("iron_bars", "iron_block", "iron_nugget", "lantern", "water_cauldron", "glass_pane",
                "coal_block", "packed_ice", "blue_ice", "snow_block")) {
            assertEquals(TaskType.CRAFT_METAL_MATERIAL, CraftFamily.of(path), path);
        }
    }

    @Test
    void woolBedsAndDyesGoToTheShepherd() {
        for (String path : List.of("red_wool", "blue_carpet", "white_bed", "brown_wall_banner", "lime_dye", "leather")) {
            assertEquals(TaskType.CRAFT_TEXTILE_MATERIAL, CraftFamily.of(path), path);
        }
    }

    @Test
    void paperBooksBoneMealAndHayGoToTheFarmer() {
        for (String path : List.of("paper", "book", "bone_meal", "bone_block", "hay_block", "jack_o_lantern")) {
            assertEquals(TaskType.CRAFT_FARM_MATERIAL, CraftFamily.of(path), path);
        }
    }

    @Test
    void everyFamilyHasItsCraftsman() {
        assertEquals(ProfessionType.CARPENTER, CraftFamily.craftsmanOf(TaskType.CRAFT_WOOD_MATERIAL));
        assertEquals(ProfessionType.MASON, CraftFamily.craftsmanOf(TaskType.CRAFT_STONE_MATERIAL));
        assertEquals(ProfessionType.SMELTER, CraftFamily.craftsmanOf(TaskType.CRAFT_METAL_MATERIAL));
        assertEquals(ProfessionType.SHEPHERD, CraftFamily.craftsmanOf(TaskType.CRAFT_TEXTILE_MATERIAL));
        assertEquals(ProfessionType.FARMER, CraftFamily.craftsmanOf(TaskType.CRAFT_FARM_MATERIAL));
    }
}
