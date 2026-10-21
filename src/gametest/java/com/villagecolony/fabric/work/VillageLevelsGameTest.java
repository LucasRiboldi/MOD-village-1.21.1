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



    /** A roça do deserto: borda de arenito e areia um abaixo; com água perto, ali cabe canteiro. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_levels")
    public void aNewPlotFitsOnTheSandBeyondTheBorder(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos farm = context.getAbsolutePos(new BlockPos(1, 4, 1)).add(1_000_000, 0, 2_300_000);
        BlockPos dry = farm.add(0, 0, 40);
        world.getChunk(farm);
        world.getChunk(dry);

        for (BlockPos at : new BlockPos[] {farm, dry}) {
            for (BlockPos pos : BlockPos.iterate(at.add(-3, -2, -3), at.add(3, -1, 3))) {
                world.setBlockState(pos, Blocks.SAND.getDefaultState());
            }
            world.setBlockState(at, Blocks.FARMLAND.getDefaultState());
            world.setBlockState(at.down(), Blocks.SAND.getDefaultState());
            world.setBlockState(at.east(), Blocks.CUT_SANDSTONE.getDefaultState());
            world.setBlockState(at.east().down(), Blocks.SAND.getDefaultState());
        }
        world.setBlockState(farm.west(), Blocks.WATER.getDefaultState());
        world.setBlockState(farm.west().down(), Blocks.SAND.getDefaultState());

        try {
            Optional<BlockPos> plot = FarmerTill.nextPlot(world, farm);

            context.assertTrue(plot.isPresent() && world.getBlockState(plot.get()).isOf(Blocks.SAND),
                    "a areia em volta da roça do deserto não serviu de canteiro: " + plot);
            context.assertTrue(FarmerTill.nextPlot(world, dry).isEmpty(),
                    "controle: sem água perto, a areia virou canteiro");
        } finally {
            for (BlockPos at : new BlockPos[] {farm, dry}) {
                for (BlockPos pos : BlockPos.iterate(at.add(-3, -2, -3), at.add(3, 1, 3))) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
            }
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
