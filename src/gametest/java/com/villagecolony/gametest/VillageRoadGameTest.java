package com.villagecolony.gametest;

import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.VillageRoad;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/**
 * Materiais oficiais de pavimentacao da colonia no P0.7.
 *
 * <p>O material identifica uma possivel pavimentacao; a classificacao como
 * estrada tambem exige a reserva espacial {@code ROAD_AREA}.
 */
public class VillageRoadGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_road",
            tickLimit = 40)
    public void eachStyleUsesTheColonyRoadMaterial(TestContext context) {
        ServerWorld world = context.getWorld();

        ResourceId plains = VillageRoad.pavingFor(world, "plains").orElse(null);

        context.assertTrue(
                ResourceId.vanilla("dirt_path").equals(plains),
                "a planície devia calçar com dirt_path, e calça com " + plains);

        for (String style : new String[] {"desert", "savanna", "taiga", "snowy"}) {
            ResourceId paving = VillageRoad.pavingFor(world, style).orElse(null);

            context.assertTrue(
                    ResourceId.vanilla("dirt_path").equals(paving),
                    "o estilo " + style + " calça com " + paving);
        }

        context.complete();
    }

    /**
     * A pergunta que a busca de lote faz, e o que ela precisa recusar.
     *
     * <p>A classificacao espacial pertence ao scanner. Este teste cobre
     * somente o conjunto estrito de materiais permitidos.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_road",
            tickLimit = 40)
    public void officialRoadMaterialsAreRecognized(TestContext context) {
        ServerWorld world = context.getWorld();

        context.assertTrue(
                VillageRoad.isPaving(world, Blocks.DIRT_PATH.getDefaultState()),
                "caminho de terra não foi reconhecido como rua");

        for (var paving : new net.minecraft.block.Block[] {
                Blocks.DIRT_PATH, Blocks.GRAVEL, Blocks.TERRACOTTA}) {
            context.assertTrue(
                    VillageRoad.isPaving(world, paving.getDefaultState()),
                    paving + " não foi reconhecido como material oficial de rua");
        }

        for (var ground : new net.minecraft.block.Block[] {
                Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.SAND, Blocks.STONE,
                Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE}) {

            context.assertTrue(
                    !VillageRoad.isPaving(world, ground.getDefaultState()),
                    ground + " foi tomado por rua, e a colônia construiria em cima do nada");
        }

        context.complete();
    }
}
