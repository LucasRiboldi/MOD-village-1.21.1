package com.villagecolony.fabric.work;

import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Quem raspa a superfície esperando ramal não pega a pedra que é do poço planejado (B1). */
public class MineSurfaceGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_vein", tickLimit = 20)
    public void theSurfaceScrapingLeavesTheShaftCellsAlone(TestContext context) {
        BlockPos stone = context.getAbsolutePos(new BlockPos(3, 1, 3));
        BlockPos center = stone.up();

        context.setBlockState(new BlockPos(3, 1, 3), Blocks.STONE.getDefaultState());

        // O raio é global: encurtado e devolvido no mesmo tique, sem dar a vez a outro cenário.
        MineDigging.shortenSurfaceRadiusTo(1);

        try {
            Optional<BlockPos> free = MineVein.exposedStone(
                    context.getWorld(), UUID.randomUUID(), UUID.randomUUID(), center, Set.of());
            Optional<BlockPos> shaft = MineVein.exposedStone(context.getWorld(), UUID.randomUUID(), UUID.randomUUID(),
                    center, Set.of(MinecraftTypeAdapter.toColonyPos(stone)));

            context.assertTrue(free.equals(Optional.of(stone)),
                    "o cenário não expôs a pedra do centro: " + free);
            context.assertTrue(!shaft.equals(Optional.of(stone)),
                    "a pedra que é do poço foi entregue à raspagem da superfície");
        } finally {
            MineDigging.restoreSurfaceRadius();
        }

        context.complete();
    }
}
