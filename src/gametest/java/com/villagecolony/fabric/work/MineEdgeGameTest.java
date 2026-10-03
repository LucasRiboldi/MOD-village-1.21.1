package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * A mina nova longe da água — A-4, pedido do autor de 2026-10-02. A boca vence
 * pela distância da água mais perto; esta é a régua.
 */
public final class MineEdgeGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_edge", tickLimit = 20)
    public void theWaterDistanceIsTheRingOfTheNearestWater(TestContext context) {
        BlockPos mouth = new BlockPos(8, 2, 8);

        for (int x = 0; x <= 16; x++) {
            for (int z = 0; z <= 16; z++) {
                context.setBlockState(new BlockPos(x, 2, z), Blocks.AIR.getDefaultState());
            }
        }

        context.assertTrue(MineEdge.waterDistance(context.getWorld(), context.getAbsolutePos(mouth))
                        == MineEdge.WATER_LOOK,
                "sem água em volta a distância devia ser o teto");

        context.setBlockState(mouth.add(5, 0, 2), Blocks.WATER.getDefaultState());

        int distance = MineEdge.waterDistance(context.getWorld(), context.getAbsolutePos(mouth));

        context.setBlockState(mouth.add(5, 0, 2), Blocks.AIR.getDefaultState());
        context.assertTrue(distance == 5, "a água a cinco blocos devia dar 5, deu " + distance);
        context.complete();
    }
}
