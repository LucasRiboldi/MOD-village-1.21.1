package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/** Medida do jogo da 0.4.84: o alisamento diz por que recusou cada coluna (VC_SMOOTH). */
public class SmoothRefusalGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smooth_refusal", tickLimit = 20)
    public void theRefusedColumnSaysWhy(TestContext context) {
        for (int x = 0; x <= 4; x++) {
            context.setBlockState(new BlockPos(x, 0, 1), Blocks.STONE.getDefaultState());
            context.setBlockState(new BlockPos(x, 1, 1), Blocks.GRASS_BLOCK.getDefaultState());
        }
        // Água em cima da coluna 3: o corte não pode abrir o lugar para ela.
        context.setBlockState(new BlockPos(3, 2, 1), Blocks.WATER.getDefaultState());

        context.waitAndRun(1, () -> {
            BlockPos clean = context.getAbsolutePos(new BlockPos(1, 1, 1));
            BlockPos wet = context.getAbsolutePos(new BlockPos(3, 1, 1));
            String none = GroundSmoothing.refusal(context.getWorld(), clean.getX(), clean.getZ(), clean.getY(), clean.getY() + 1);
            String water = GroundSmoothing.refusal(context.getWorld(), wet.getX(), wet.getZ(), wet.getY(), wet.getY() + 1);

            context.assertTrue(none == null, "a coluna limpa foi recusada: " + none);
            context.assertTrue("fluid:water".equals(water), "a coluna com água não disse o motivo: " + water);
            context.complete();
        });
    }
}
