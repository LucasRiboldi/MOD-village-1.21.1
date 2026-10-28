package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * A roça conhecida responde primeiro, mesmo com a varredura do raio no meio da volta — Regra 121.
 * No jogo da 0.3.93 a planície grande passou 622 ciclos em "still sweeping" com lavoura madura
 * conhecida esperando, e a vila parou de crescer.
 */
public class CropRegistryGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "crop_registry", tickLimit = 20)
    public void theKnownFarmAnswersWhileTheRadiusIsStillBeingSwept(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        // Longe de tudo: nenhum outro teste tem roça aqui, e os chunks em volta não estão carregados.
        BlockPos center = context.getAbsolutePos(BlockPos.ORIGIN).add(2_000_000, 2, 2_000_000);
        BlockPos farmland = center.add(40, -1, 0);

        try {
            world.getChunk(farmland);
            world.setBlockState(farmland, Blocks.FARMLAND.getDefaultState());
            world.setBlockState(farmland.up(), Blocks.WHEAT.getDefaultState().with(CropBlock.AGE, CropBlock.MAX_AGE));

            CropPatch.Field first = CropPatch.survey(world, colonyId, center, 64);

            context.assertTrue(first.incomplete(), "o raio de 64 devia ficar no meio da volta numa passagem");

            com.villagecolony.fabric.integration.KnownFarmland.register(colonyId, farmland);

            Optional<BlockPos> ripe = CropPatch.survey(world, colonyId, center, 64).ripe();

            context.assertTrue(ripe.map(farmland.up()::equals).orElse(false),
                    "com a varredura no meio da volta, a roça conhecida madura não foi achada: " + ripe);
        } finally {
            world.setBlockState(farmland.up(), Blocks.AIR.getDefaultState());
            world.setBlockState(farmland, Blocks.AIR.getDefaultState());
            CropPatch.forget(colonyId);
            RingSweep.forget(colonyId);
        }

        context.complete();
    }
}
