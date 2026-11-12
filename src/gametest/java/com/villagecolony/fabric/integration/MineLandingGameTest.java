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

    /** O arco novo surge de tora de pinheiro descascada, com o baú à direita de quem entra, e é reconhecido pela boca. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_landing", tickLimit = 5)
    public void theArchStandsAndIsKnownByTheMouth(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos mouth = context.getAbsolutePos(new BlockPos(3, 2, 3));

        for (BlockPos at : BlockPos.iterate(mouth.add(-3, -1, -3), mouth.add(4, -1, 3))) {
            world.setBlockState(at, Blocks.STONE.getDefaultState());
        }

        context.assertTrue(MineGate.standingAtMouth(world, mouth, Direction.SOUTH).isEmpty(), "arco reconhecido sem arco");
        MineGate.raise(world, mouth, Direction.SOUTH);

        // Para o sul, as três pistas vão da boca para o leste e a direita de quem entra é o oeste.
        for (int lane = 0; lane < 3; lane++) {
            context.assertTrue(world.getBlockState(mouth.east(lane)).isAir(), "o vão está fechado na pista " + lane);
        }
        context.assertTrue(world.getBlockState(mouth.west(1)).isOf(Blocks.STRIPPED_SPRUCE_LOG), "falta o pilar da direita");
        context.assertTrue(world.getBlockState(mouth.east(3)).isOf(Blocks.STRIPPED_SPRUCE_LOG), "falta o pilar da esquerda");
        context.assertTrue(world.getBlockState(mouth.west(3)).isOf(Blocks.CHEST), "o baú não está à direita, no chão");
        context.assertTrue(world.getBlockState(mouth.west(2).up(3)).isOf(Blocks.LANTERN), "falta o lampião da direita");
        context.assertTrue(world.getBlockState(mouth.east(4).up(3)).isOf(Blocks.LANTERN), "falta o lampião da esquerda");
        context.assertTrue(MineGate.standingAtMouth(world, mouth, Direction.SOUTH).isPresent(), "o arco não foi reconhecido");

        world.setBlockState(mouth.west(2).up(4), Blocks.AIR.getDefaultState());
        context.assertTrue(MineGate.standingAtMouth(world, mouth, Direction.SOUTH).isPresent(),
                "uma peça tirada apagou o arco");

        for (BlockPos at : BlockPos.iterate(mouth.add(-3, -1, -3), mouth.add(4, 5, 3))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState());
        }

        context.complete();
    }

    /** Tronco sem folha natural perto do alto não é árvore: o lenhador não o leva (Regra 256). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_landing", tickLimit = 5)
    public void aLogWithoutNaturalLeavesIsNotATree(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos bare = context.getAbsolutePos(new BlockPos(2, 2, 2));
        BlockPos tree = context.getAbsolutePos(new BlockPos(8, 2, 2));
        BlockPos placed = context.getAbsolutePos(new BlockPos(14, 2, 2));

        for (BlockPos base : java.util.List.of(bare, tree, placed)) {
            world.setBlockState(base, Blocks.OAK_LOG.getDefaultState());
            world.setBlockState(base.up(), Blocks.OAK_LOG.getDefaultState());
        }

        world.setBlockState(tree.up(2), Blocks.OAK_LEAVES.getDefaultState());
        world.setBlockState(placed.up(2), Blocks.OAK_LEAVES.getDefaultState()
                .with(net.minecraft.block.LeavesBlock.PERSISTENT, true));

        context.assertFalse(TreeScanner.hasNaturalLeaves(world, bare), "tronco solto contou como árvore");
        context.assertTrue(TreeScanner.hasNaturalLeaves(world, tree), "a árvore com folha não contou");
        context.assertFalse(TreeScanner.hasNaturalLeaves(world, placed), "folha posta por alguém contou como árvore");
        context.complete();
    }
}
