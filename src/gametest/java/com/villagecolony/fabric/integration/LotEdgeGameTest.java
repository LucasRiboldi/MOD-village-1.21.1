package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/** Medida do jogo da 0.4.84: o degrau entre a base do lote nivelado e o chão em volta (VC_LOT_EDGE). */
public class LotEdgeGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lot_edge", tickLimit = 20, skyAccess = true)
    public void aWallBesideTheLotIsMeasured(TestContext context) {
        // Chão em y 1; lote de 2 × 2 em (3, 3), com o anel de 3 blocos dentro da arena; muro de 3 blocos em x 2 (anel de 1).
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }
        for (int z = 3; z <= 4; z++) {
            for (int y = 2; y <= 4; y++) {
                context.setBlockState(new BlockPos(2, y, z), Blocks.DIRT.getDefaultState());
            }
        }

        context.waitAndRun(1, () -> {
            BlockPos origin = context.getAbsolutePos(new BlockPos(3, 2, 3));
            BlockPos far = context.getAbsolutePos(new BlockPos(4, 2, 4));
            int minX = Math.min(origin.getX(), far.getX());
            int minZ = Math.min(origin.getZ(), far.getZ());
            String report = LotTerraform.edgeReport(context.getWorld(), minX, minZ, 2, 2, origin.getY());

            context.assertTrue(report.startsWith("maxStep=3 steepRing1=2"), "o muro de 3 ao lado do lote não foi medido: " + report);
            context.complete();
        });
    }
}
