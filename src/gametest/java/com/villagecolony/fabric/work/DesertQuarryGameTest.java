package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.VillageBounds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/** A pedreira de superfície do deserto — Regra 236: fora da caixa, uma camada por vez, longe da água. */
public class DesertQuarryGameTest implements FabricGameTest {

    private static final int SIZE = 24;

    private static void forceChunks(ServerWorld world, BlockPos origin, boolean force) {
        for (int x = origin.getX() >> 4; x <= (origin.getX() + SIZE) >> 4; x++) {
            for (int z = origin.getZ() >> 4; z <= (origin.getZ() + SIZE) >> 4; z++) {
                world.setChunkForced(x, z, force);
            }
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "desert_quarry", tickLimit = 20)
    public void theQuarryShavesSandOutsideTheBox(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 102, 1));
        int y = origin.getY();
        VillageBounds box = new VillageBounds(origin.getX() + 10, y, origin.getZ() + 10, origin.getX() + 12, y + 5, origin.getZ() + 12);

        forceChunks(world, origin, true);

        try {
            for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 0, SIZE))) {
                world.setBlockState(at, Blocks.SAND.getDefaultState(), 18);
            }

            Optional<BlockPos> first = DesertQuarry.pick(world, UUID.randomUUID(), box, y);

            context.assertTrue(first.isPresent(), "Regra 236: a areia fora da caixa não serviu");
            BlockPos at = first.orElseThrow();
            boolean outside = at.getX() < box.minX() || at.getX() > box.maxX() || at.getZ() < box.minZ() || at.getZ() > box.maxZ();
            context.assertTrue(outside && at.getY() == y, "Regra 236: a pedreira não ficou fora da caixa, no topo: " + at);

            BlockPos column = origin.add(4, 0, 4);
            context.assertTrue(DesertQuarry.shavable(world, column, y).isPresent(), "Regra 236: o chão plano de areia não serviu");
            context.assertTrue(DesertQuarry.shavable(world, column, y + DesertQuarry.DEPTH).isEmpty(),
                    "Regra 236: a pedreira desceu além do fundo");

            world.setBlockState(column.east().up(), Blocks.SAND.getDefaultState(), 18);
            context.assertTrue(DesertQuarry.shavable(world, column, y).isEmpty(), "Regra 236: a pedreira cavou abaixo da vizinha");
            world.setBlockState(column.east().up(), Blocks.AIR.getDefaultState(), 18);

            world.setBlockState(column.west(), Blocks.WATER.getDefaultState(), 18);
            context.assertTrue(DesertQuarry.shavable(world, column, y).isEmpty(), "Regra 236: a pedreira cavou junto da água");
        } finally {
            for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 2, SIZE))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }

            forceChunks(world, origin, false);
        }

        context.complete();
    }
}
