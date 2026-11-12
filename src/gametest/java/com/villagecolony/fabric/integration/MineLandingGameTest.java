package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** A tocha do patamar das dobras da mina fica na parede, presa na rocha de fora — Regra 254. */
public class MineLandingGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_landing", tickLimit = 5)
    public void theLandingTorchHangsOnTheRockBehindIt(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos open = context.getAbsolutePos(new BlockPos(2, 2, 2));
        BlockPos noWall = context.getAbsolutePos(new BlockPos(5, 2, 2));
        BlockPos taken = context.getAbsolutePos(new BlockPos(8, 2, 2));

        world.setBlockState(open.south(), Blocks.STONE.getDefaultState());
        world.setBlockState(taken.south(), Blocks.STONE.getDefaultState());
        world.setBlockState(taken, Blocks.COBBLESTONE.getDefaultState());

        context.assertTrue(MineLighting.placeWallTorch(world, open, Direction.SOUTH), "a tocha não foi posta");
        context.assertTrue(world.getBlockState(open).isOf(Blocks.WALL_TORCH), "não é tocha de parede");
        context.assertTrue(world.getBlockState(open).get(WallTorchBlock.FACING) == Direction.NORTH,
                "a tocha não aponta para dentro do patamar");
        context.assertFalse(MineLighting.placeWallTorch(world, noWall, Direction.SOUTH), "tocha sem rocha atrás");
        context.assertFalse(MineLighting.placeWallTorch(world, taken, Direction.SOUTH), "tocha no lugar de um bloco");
        context.complete();
    }
}
