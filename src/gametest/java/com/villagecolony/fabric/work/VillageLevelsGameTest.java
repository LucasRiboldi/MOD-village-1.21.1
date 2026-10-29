package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * As duas alturas da vila do deserto — pedido do autor, 2026-10-21: a roça cresce na areia e o
 * viveiro planta na areia da vila a céu aberto.
 */
public class VillageLevelsGameTest implements FabricGameTest {



    /**
     * A roça do deserto: borda de arenito e areia um abaixo; o canteiro 9×9 cabe na areia de fora, um abaixo da roça
     * (autor, 2026-10-28: canteiro retangular, {@link FarmPlots}).
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_levels")
    public void aNewPlotFitsOnTheSandBeyondTheBorder(TestContext context) {
        ServerWorld world = context.getWorld();
        java.util.UUID colonyId = java.util.UUID.randomUUID();
        BlockPos farm = context.getAbsolutePos(new BlockPos(1, 4, 1)).add(1_000_000, 0, 2_300_000);

        for (BlockPos pos : BlockPos.iterate(farm.add(-2, -2, -6), farm.add(13, 2, 6))) {
            world.getChunk(pos);
            world.setBlockState(pos, pos.getY() < farm.getY() ? Blocks.SAND.getDefaultState() : Blocks.AIR.getDefaultState());
        }

        world.setBlockState(farm, Blocks.FARMLAND.getDefaultState());
        world.setBlockState(farm.east(), Blocks.CUT_SANDSTONE.getDefaultState());

        try {
            Optional<BlockPos> plot = FarmPlots.next(world, colonyId, farm);

            context.assertTrue(plot.isPresent() && world.getBlockState(plot.get()).isOf(Blocks.SAND)
                            && plot.get().getY() == farm.getY() - 1,
                    "a areia além da borda, um abaixo da roça do deserto, não serviu de canteiro: " + plot);
        } finally {
            for (BlockPos pos : BlockPos.iterate(farm.add(-2, -2, -6), farm.add(13, 2, 6))) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
            FarmPlots.clearAll();
        }

        context.complete();
    }

    /** A areia da vila a céu aberto serve de viveiro perto do baú; debaixo de teto, não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_levels")
    public void theNurseryTakesOpenSandButNotSandUnderARoof(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos open = context.getAbsolutePos(new BlockPos(1, 4, 1)).add(1_000_000, 0, 2_400_000);
        BlockPos roofed = open.add(6, 0, 0);
        world.getChunk(open);

        world.setBlockState(open, Blocks.SAND.getDefaultState());
        world.setBlockState(roofed, Blocks.SAND.getDefaultState());
        world.setBlockState(roofed.up(4), Blocks.SMOOTH_SANDSTONE.getDefaultState());

        try {
            context.assertTrue(LumberjackNursery.isOpenNaturalGround(world, open),
                    "a areia a céu aberto não serviu de viveiro");
            context.assertFalse(LumberjackNursery.isOpenNaturalGround(world, roofed),
                    "controle: a areia debaixo do teto serviu de viveiro");
        } finally {
            for (BlockPos pos : BlockPos.iterate(open, roofed.up(4))) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
        }

        context.complete();
    }
}
