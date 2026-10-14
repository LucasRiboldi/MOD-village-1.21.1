package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * O fazendeiro alarga a roça só na borda dela, com água perto e longe da rua —
 * regra do autor, 2026-10-11 ({@link FarmerTill}).
 */
public class FarmerTillGameTest implements FabricGameTest {

    private static final int Y = 50;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_sowing", tickLimit = 20)
    public void theFarmerTillsBesideTheFieldNearWaterAndAwayFromTheRoad(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos farmland = context.getAbsolutePos(new BlockPos(3, Y, 3));
        BlockPos grass = farmland.east();
        BlockPos water = context.getAbsolutePos(new BlockPos(3, Y, 6));

        try {
            for (int x = 0; x <= 8; x++) {
                for (int z = 0; z <= 8; z++) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)), Blocks.STONE.getDefaultState());
                }
            }

            world.setBlockState(farmland, Blocks.FARMLAND.getDefaultState());
            world.setBlockState(grass, Blocks.GRASS_BLOCK.getDefaultState());

            context.assertTrue(FarmerTill.nextPlot(world, farmland.up()).isEmpty(),
                    "sem água perto, a terra não vira canteiro");

            world.setBlockState(water, Blocks.WATER.getDefaultState());
            Optional<BlockPos> plot = FarmerTill.nextPlot(world, farmland.up());

            context.assertTrue(plot.equals(Optional.of(grass)),
                    "a grama ao lado da roça, com água perto, devia virar canteiro: " + plot);

            world.setBlockState(grass.east(), Blocks.DIRT_PATH.getDefaultState());
            context.assertTrue(FarmerTill.nextPlot(world, farmland.up()).isEmpty(),
                    "a grama encostada na rua não vira canteiro");
        } finally {
            for (int x = 0; x <= 8; x++) {
                for (int z = 0; z <= 8; z++) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)), Blocks.AIR.getDefaultState());
                }
            }
        }

        context.complete();
    }
}
