package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * A árvore em pé sobre o chão cortado sai inteira — autor, 2026-10-29, depois do jogo da 0.3.108 ("deixou partes
 * voando"; escolha: tirar inteira, tronco e folhas). A árvore vizinha, que não encosta nela, fica.
 */
public class VillageGroundGameTest implements FabricGameTest {

    private static void tree(ServerWorld world, BlockPos base, int height) {
        for (int dy = 0; dy < height; dy++) {
            world.setBlockState(base.up(dy), Blocks.OAK_LOG.getDefaultState(), Block.FORCE_STATE);
        }

        for (BlockPos leaf : BlockPos.iterate(base.add(-2, height - 2, -2), base.add(2, height, 2))) {
            if (world.getBlockState(leaf).isAir()) {
                world.setBlockState(leaf.toImmutable(), Blocks.OAK_LEAVES.getDefaultState(), Block.FORCE_STATE);
            }
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_ground", tickLimit = 5)
    public void aTreeOnCutGroundIsRemovedWhole(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos cut = context.getAbsolutePos(new BlockPos(2, 2, 2));
        BlockPos neighbour = context.getAbsolutePos(new BlockPos(9, 2, 2));

        tree(world, cut, 5);
        tree(world, neighbour, 5);

        try {
            int removed = VillageGround.removeTree(world, cut);

            context.assertTrue(removed == 5 + 5 * 5 * 3 - 2, "saíram " + removed + " blocos, não a árvore inteira");

            for (BlockPos at : BlockPos.iterate(cut.add(-2, 0, -2), cut.add(2, 5, 2))) {
                context.assertTrue(world.getBlockState(at).isAir(), "sobrou " + world.getBlockState(at).getBlock()
                        + " em " + at.toShortString());
            }

            context.assertTrue(world.getBlockState(neighbour).isOf(Blocks.OAK_LOG), "a árvore vizinha saiu junto");
            context.assertTrue(world.getBlockState(neighbour.add(0, 4, 0)).isOf(Blocks.OAK_LOG),
                    "o alto da árvore vizinha saiu junto");
        } finally {
            for (BlockPos at : BlockPos.iterate(cut.add(-2, 0, -2), neighbour.add(2, 5, 2))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.FORCE_STATE);
            }
        }

        context.complete();
    }

    /** O que não é árvore — o chão, a casa — nunca sai pela busca da árvore. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_ground", tickLimit = 5)
    public void onlyLogsAndLeavesLeave(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos cut = context.getAbsolutePos(new BlockPos(2, 2, 2));

        tree(world, cut, 4);
        world.setBlockState(cut.add(1, 0, 0), Blocks.OAK_PLANKS.getDefaultState(), Block.FORCE_STATE);
        world.setBlockState(cut.down(), Blocks.DIRT.getDefaultState(), Block.FORCE_STATE);

        try {
            VillageGround.removeTree(world, cut);

            context.assertTrue(world.getBlockState(cut.add(1, 0, 0)).isOf(Blocks.OAK_PLANKS), "a tábua encostada saiu");
            context.assertTrue(world.getBlockState(cut.down()).isOf(Blocks.DIRT), "o chão embaixo do tronco saiu");
            context.assertTrue(world.getBlockState(cut.up(2)).isAir(), "o tronco ficou");
        } finally {
            for (BlockPos at : BlockPos.iterate(cut.add(-2, -1, -2), cut.add(2, 4, 2))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.FORCE_STATE);
            }
        }

        context.complete();
    }
}
