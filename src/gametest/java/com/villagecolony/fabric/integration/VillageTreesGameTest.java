package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/** O lenhador lembra as árvores e mudas da vila (ADR-038 P3a). */
public class VillageTreesGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_trees", tickLimit = 20)
    public void theNearestKnownTreeWinsAndASaplingIsNeverATarget(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colony = UUID.randomUUID();
        BlockPos near = context.getAbsolutePos(new BlockPos(2, 1, 2));
        BlockPos far = context.getAbsolutePos(new BlockPos(7, 1, 7));
        BlockPos sapling = context.getAbsolutePos(new BlockPos(1, 1, 6));
        BlockPos lumberjack = context.getAbsolutePos(new BlockPos(1, 1, 1));

        world.setBlockState(near, Blocks.OAK_LOG.getDefaultState());
        world.setBlockState(far, Blocks.OAK_LOG.getDefaultState());
        world.setBlockState(sapling, Blocks.OAK_SAPLING.getDefaultState());
        VillageTrees.rememberTree(colony, far);
        VillageTrees.rememberTree(colony, near);
        VillageTrees.rememberSapling(colony, sapling);

        context.assertTrue(VillageTrees.nearest(world, colony, lumberjack, pos -> true).equals(Optional.of(near)),
                "não foi à árvore mais perto");
        context.assertTrue(VillageTrees.nearest(world, colony, lumberjack, pos -> !pos.equals(near) && !pos.equals(far))
                        .isEmpty(), "a muda virou alvo");

        // A árvore perto foi derrubada; a muda cresceu.
        world.setBlockState(near, Blocks.AIR.getDefaultState());
        world.setBlockState(sapling, Blocks.OAK_LOG.getDefaultState());

        context.assertTrue(VillageTrees.nearest(world, colony, lumberjack, pos -> true).equals(Optional.of(sapling)),
                "a muda crescida não virou árvore, ou o tronco derrubado ficou na memória");
        context.assertTrue(VillageTrees.knownSaplings(colony) == 0 && VillageTrees.knownTrees(colony) == 2,
                "a memória não se arrumou: " + VillageTrees.knownTrees(colony) + " árvores, "
                        + VillageTrees.knownSaplings(colony) + " mudas");
        context.complete();
    }
}
