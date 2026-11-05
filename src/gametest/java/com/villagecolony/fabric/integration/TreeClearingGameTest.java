package com.villagecolony.fabric.integration;

import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** A zona da obra limpa (Regra 173) e a coluna aberta do caminho (Regra 178). */
public class TreeClearingGameTest implements FabricGameTest {

    private static void forceChunks(ServerWorld world, BlockPos origin, boolean force) {
        for (int x = origin.getX() >> 4; x <= (origin.getX() + 30) >> 4; x++) {
            world.setChunkForced(x, origin.getZ() >> 4, force);
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_clearing", tickLimit = 20)
    public void theZoneTakesTheTreeUpToTheReach(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(new BlockPos(1, 110, 1));
        int reach = TreeClearing.REACH;

        forceChunks(world, base, true);

        try {
            // Um tronco na zona, e um galho longo com folha em cima, que passa do alcance.
            for (int y = 0; y < 3; y++) {
                world.setBlockState(base.up(y), Blocks.OAK_LOG.getDefaultState(), 18);
            }

            for (int x = 0; x <= reach + 4; x++) {
                world.setBlockState(base.add(x, 3, 0), Blocks.OAK_LOG.getDefaultState(), 18);
                world.setBlockState(base.add(x, 4, 0), Blocks.OAK_LEAVES.getDefaultState(), 18);
            }

            // Uma árvore separada, que não encosta em nada.
            BlockPos other = base.add(0, 0, 8);

            world.setBlockState(other, Blocks.OAK_LOG.getDefaultState(), 18);
            world.setBlockState(other.up(), Blocks.OAK_LEAVES.getDefaultState(), 18);

            int cleared = TreeClearing.sweep(world, base.getX(), base.getZ(), base.getX(), base.getZ(), base.getY(), List.of());

            context.assertTrue(cleared > 0, "Regra 173: nada saiu");
            context.assertTrue(world.getBlockState(base).isAir(), "Regra 173: o tronco da zona ficou");
            context.assertTrue(world.getBlockState(base.add(reach, 3, 0)).isAir(), "Regra 173: o galho dentro do alcance ficou");
            context.assertTrue(world.getBlockState(base.add(reach, 4, 0)).isAir(), "Regra 173: a folha dentro do alcance ficou");
            context.assertTrue(world.getBlockState(base.add(reach + 1, 3, 0)).isOf(Blocks.OAK_LOG), "Regra 173: a busca passou do alcance");
            context.assertTrue(world.getBlockState(other).isOf(Blocks.OAK_LOG), "Regra 173: a árvore que não encosta saiu");
            context.assertTrue(TreeClearing.MAX_BLOCKS == 2_000, "Regra 173: o teto é de 2.000 blocos");
        } finally {
            for (BlockPos at : BlockPos.iterate(base, base.add(reach + 5, 5, 8))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }

            forceChunks(world, base, false);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_clearing_column", tickLimit = 20)
    public void thePathColumnOpensOnlyThroughNature(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos floor = context.getAbsolutePos(new BlockPos(1, 118, 1));
        BlockPos planks = floor.add(2, 0, 0);
        BlockPos hill = floor.add(4, 0, 0);
        ColonyFixture fixture = ColonyFixture.colonyAtWorld(floor.up());

        try {
            for (BlockPos at : List.of(floor, planks, hill)) {
                world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
            }

            world.setBlockState(floor.up(), Blocks.OAK_LOG.getDefaultState(), 18);
            world.setBlockState(floor.up(2), Blocks.DIRT.getDefaultState(), 18);
            world.setBlockState(floor.up(3), Blocks.OAK_LEAVES.getDefaultState(), 18);
            world.setBlockState(planks.up(), Blocks.OAK_PLANKS.getDefaultState(), 18);

            // Chão logo acima da altura do corte: abriria um túnel no morro.
            world.setBlockState(hill.up(TreeClearing.HEADROOM + 1), Blocks.DIRT.getDefaultState(), 18);

            WorldPathTerrain terrain = new WorldPathTerrain(world, fixture.colony().id());

            context.assertTrue(terrain.canCut(floor.getX(), floor.getZ(), floor.getY()), "Regra 178: árvore e chão natural barraram o corte");
            context.assertFalse(terrain.canCut(planks.getX(), planks.getZ(), planks.getY()), "Regra 178: a tábua foi aceita no corte");
            context.assertFalse(terrain.canCut(hill.getX(), hill.getZ(), hill.getY()), "Regra 178: o morro virou túnel");

            TreeClearing.openColumn(world, floor.getX(), floor.getZ(), floor.getY(), List.of());

            for (int up = 1; up <= 3; up++) {
                context.assertTrue(world.getBlockState(floor.up(up)).isAir(), "Regra 178: a coluna ficou fechada na altura " + up);
            }

            context.assertTrue(world.getBlockState(floor).isOf(Blocks.GRASS_BLOCK), "Regra 178: o piso saiu junto");
        } finally {
            for (BlockPos at : BlockPos.iterate(floor, floor.add(4, TreeClearing.HEADROOM + 2, 0))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }

            fixture.cleanUp();
        }

        context.complete();
    }
}
