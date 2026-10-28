package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * A água que a picareta solta na escada sai, e a escada fica aberta; só a face de fora é tapada — o
 * mineiro não fecha o caminho de volta (pedido do autor, 2026-10-27).
 */
public class MineSealGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_seal", tickLimit = 20)
    public void waterInTheStairIsDrainedAndOnlyTheOutsideIsSealed(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos entry = context.getAbsolutePos(BlockPos.ORIGIN).add(4_300_000, 40, 4_300_000);
        MineShaft shaft = MineShaft.from(MinecraftTypeAdapter.toColonyPos(entry), Side.EAST);
        UUID colonyId = UUID.randomUUID();
        BlockPos dug = MinecraftTypeAdapter.toBlockPos(shaft.positionAt(0));
        BlockPos stair = MinecraftTypeAdapter.toBlockPos(shaft.positionAt(1));
        BlockPos outside = null;

        for (net.minecraft.util.math.Direction face : net.minecraft.util.math.Direction.values()) {
            BlockPos at = dug.offset(face);
            if (!shaft.plannedCells().contains(MinecraftTypeAdapter.toColonyPos(at))) {
                outside = at;
                break;
            }
        }

        VillageColonyMod.MINES.restore(Mine.restore(colonyId, shaft, 1));

        try {
            context.assertTrue(dug.isWithinDistance(stair, 1.01), "o teste precisa de uma célula da escada vizinha");
            context.assertTrue(outside != null, "o teste precisa de uma face de fora da mina");
            world.getChunk(dug);
            world.setBlockState(dug, Blocks.AIR.getDefaultState());
            world.setBlockState(stair, Blocks.WATER.getDefaultState());
            world.setBlockState(outside, Blocks.WATER.getDefaultState());

            MineFlooding.seal(world, dug);

            context.assertTrue(world.getBlockState(stair).getFluidState().isEmpty()
                            && world.getBlockState(stair).getCollisionShape(world, stair).isEmpty(),
                    "a célula da escada devia ficar aberta e seca, veio " + world.getBlockState(stair));
            context.assertTrue(!world.getBlockState(outside).getCollisionShape(world, outside).isEmpty(),
                    "a face de fora devia ser tapada, veio " + world.getBlockState(outside));
        } finally {
            VillageColonyMod.MINES.removeOfColony(colonyId);
            for (BlockPos at : BlockPos.iterate(dug.add(-2, -2, -2), dug.add(2, 2, 2))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }
        }

        context.complete();
    }
}
