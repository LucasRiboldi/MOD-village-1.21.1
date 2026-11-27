package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * Regra 378 (notas do jogo da 0.4.145, J5 🅱️: "descobrir por que esse tronco de árvore gigante está aqui" e "o
 * lenhador deve quebrar todas folhas da árvore que ele cortou"): depois de derrubar a árvore, o galho solto que ficou no
 * ar e a copa que ia apodrecer saem também.
 */
public class TreeLeftoversGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_leftovers", tickLimit = 20)
    public void theLooseBranchAndTheCanopyGoWithTheTree(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(new BlockPos(4, 2, 4));
        // Um galho que não encosta no tronco: dois blocos ao lado, no alto, só ligado pela copa.
        BlockPos branch = base.add(2, 4, 0);
        var leaf = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, false);

        try {
            for (BlockPos at : BlockPos.iterate(base.add(-3, -1, -3), base.add(3, -1, 3))) {
                world.setBlockState(at, Blocks.DIRT.getDefaultState());
            }
            for (int y = 0; y < 5; y++) {
                world.setBlockState(base.up(y), Blocks.OAK_LOG.getDefaultState());
            }
            for (BlockPos at : BlockPos.iterate(base.add(-2, 3, -2), base.add(2, 6, 2))) {
                if (world.getBlockState(at).isAir()) {
                    world.setBlockState(at, leaf);
                }
            }
            world.setBlockState(branch, Blocks.OAK_LOG.getDefaultState());

            TreeHarvester.Harvest harvest = TreeHarvester.fell(world, base);

            context.assertTrue(harvest.logs() > 0, "a árvore do teste não caiu");
            context.assertFalse(world.getBlockState(branch).isIn(BlockTags.LOGS),
                    "Regra 378: o galho solto ficou no ar");

            int leaves = 0;

            for (BlockPos at : BlockPos.iterate(base.add(-3, 0, -3), base.add(3, 7, 3))) {
                leaves += world.getBlockState(at).isIn(BlockTags.LEAVES) ? 1 : 0;
            }

            context.assertTrue(leaves == 0, "Regra 378: ficaram " + leaves + " folhas da copa");
        } finally {
            for (BlockPos at : BlockPos.iterate(base.add(-3, -1, -3), base.add(3, 7, 3))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }
        }

        context.complete();
    }
}
