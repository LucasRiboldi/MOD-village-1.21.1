package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.HashMap;
import java.util.Map;

/**
 * A peça sem tipo próprio pede o ingrediente de superfície dela — autor, 2026-10-20: o bloco de neve e
 * o gelo azul tinham rota e nenhuma tarefa (NO_TASK no registro de faltas).
 */
public class SurfaceIngredientNeedsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "surface_needs")
    public void aSnowBlockAsksForSnowballs(TestContext context) {
        Map<ResourceType, Integer> wanted = new HashMap<>();

        WorkMaterialRecipeNeeds.addSmeltedIngredients(context.getWorld(),
                MinecraftTypeAdapter.toResourceId(Blocks.SNOW_BLOCK), 2, wanted);

        context.assertTrue(wanted.getOrDefault(ResourceType.SNOWBALL, 0) == 8,
                "dois blocos de neve deviam pedir 8 bolas de neve: " + wanted);
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "surface_needs")
    public void blueIceAsksForIceThroughPackedIce(TestContext context) {
        Map<ResourceType, Integer> wanted = new HashMap<>();

        WorkMaterialRecipeNeeds.addSmeltedIngredients(context.getWorld(),
                MinecraftTypeAdapter.toResourceId(Blocks.BLUE_ICE), 1, wanted);

        context.assertTrue(wanted.getOrDefault(ResourceType.ICE, 0) == 81,
                "um gelo azul (9 gelos compactados de 9 gelos) devia pedir 81 gelos: " + wanted);
        context.complete();
    }
}
