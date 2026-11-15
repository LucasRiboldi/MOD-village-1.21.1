package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * Regra 294: a sobra que os ociosos tiram da vila — o bloco solto (calombo de um bloco ou bloco no ar) e o toco de até
 * três toras naturais longe de construção. A tora encostada em construção e a fileira de blocos ficam.
 */
public class StrayBlocksGameTest implements FabricGameTest {

    /** Um bloco de terra sozinho sobre o chão é sobra; dois lado a lado não são. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "stray_blocks", tickLimit = 5)
    public void aLoneBlockIsStrayAndARowIsNot(TestContext context) {
        floor(context);
        context.setBlockState(new BlockPos(2, 2, 2), Blocks.DIRT);
        context.setBlockState(new BlockPos(5, 2, 5), Blocks.DIRT);
        context.setBlockState(new BlockPos(6, 2, 5), Blocks.DIRT);

        try {
            context.assertTrue(StrayBlocks.isLoneBlock(context.getWorld(), context.getAbsolutePos(new BlockPos(2, 2, 2))),
                    "o bloco solto não é sobra");
            context.assertFalse(StrayBlocks.isLoneBlock(context.getWorld(), context.getAbsolutePos(new BlockPos(5, 2, 5))),
                    "a fileira de blocos virou sobra");
        } finally {
            clearArena(context);
        }

        context.complete();
    }

    /** O toco de duas toras longe de construção é sobra; o encostado na pedra de obra e o descascado não são. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "stray_blocks", tickLimit = 5)
    public void aStumpIsStrayButNotNextToABuilding(TestContext context) {
        floor(context);
        context.setBlockState(new BlockPos(1, 2, 1), Blocks.OAK_LOG);
        context.setBlockState(new BlockPos(1, 3, 1), Blocks.OAK_LOG);
        context.setBlockState(new BlockPos(5, 2, 1), Blocks.OAK_LOG);
        context.setBlockState(new BlockPos(6, 2, 1), Blocks.COBBLESTONE);
        context.setBlockState(new BlockPos(1, 2, 6), Blocks.STRIPPED_OAK_LOG);

        try {
            context.assertTrue(StrayBlocks.stumpAt(context.getWorld(), context.getAbsolutePos(new BlockPos(1, 2, 1))).size() == 2,
                    "o toco de duas toras não é sobra");
            context.assertTrue(StrayBlocks.stumpAt(context.getWorld(), context.getAbsolutePos(new BlockPos(5, 2, 1))).isEmpty(),
                    "a tora encostada na construção virou sobra");
            context.assertTrue(StrayBlocks.stumpAt(context.getWorld(), context.getAbsolutePos(new BlockPos(1, 2, 6))).isEmpty(),
                    "a tora descascada virou sobra");
        } finally {
            clearArena(context);
        }

        context.complete();
    }

    /** Chão de grama em y 1, de 0 a 7. */
    private static void floor(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.DIRT);
                context.setBlockState(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }
    }

    private static void clearArena(TestContext context) {
        for (BlockPos at : BlockPos.iterate(0, 1, 0, 7, 7, 7)) {
            context.setBlockState(at.toImmutable(), Blocks.AIR);
        }
    }
}
