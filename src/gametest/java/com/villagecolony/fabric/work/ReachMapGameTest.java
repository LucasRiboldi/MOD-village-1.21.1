package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Set;
import java.util.UUID;

/**
 * M2, decisão do autor de 2026-10-08: medir o alcance a pé da vila antes do
 * nivelador. Um baú ao lado do caminho é alcançado; o de uma ilha colada ao
 * caminho, mas três blocos acima, não — o degrau é de um.
 */
public class ReachMapGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "reach_map")
    public void aChestThreeBlocksUpIsOutOfReachOnFoot(TestContext context) {
        ServerWorld world = context.getWorld();

        // Quarenta acima do piso, longe das outras arenas.
        for (int x = 0; x <= 6; x++) {
            world.setBlockState(context.getAbsolutePos(new BlockPos(x, 40, 3)), Blocks.GLASS.getDefaultState());
        }

        world.setBlockState(context.getAbsolutePos(new BlockPos(1, 43, 4)), Blocks.GLASS.getDefaultState());
        world.setBlockState(context.getAbsolutePos(new BlockPos(2, 43, 4)), Blocks.GLASS.getDefaultState());

        BlockPos nearChest = context.getAbsolutePos(new BlockPos(5, 41, 4));
        BlockPos islandChest = context.getAbsolutePos(new BlockPos(3, 44, 4));
        BlockPos start = context.getAbsolutePos(new BlockPos(0, 41, 3));
        BlockPos low = context.getAbsolutePos(new BlockPos(-2, 36, -2));
        BlockPos high = context.getAbsolutePos(new BlockPos(9, 48, 9));

        world.setBlockState(nearChest, Blocks.CHEST.getDefaultState());
        world.setBlockState(islandChest, Blocks.CHEST.getDefaultState());

        Set<BlockPos> seen = ReachMap.measure(world, start, new VillageBounds(
                low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ()));

        context.assertTrue(ReachMap.reaches(seen, nearChest, 1), "o baú ao lado do caminho ficou fora do alcance");
        context.assertTrue(!ReachMap.reaches(seen, islandChest, 1),
                "o baú da ilha três blocos acima foi dado como alcançável: " + seen.size() + " posições");
        context.complete();
    }

    /**
     * A mina só se muda para boca que se alcança a pé desde a vila — decisão do autor de 2026-10-22.
     * No deserto a boca nova ficou numa duna cuja entrada ninguém alcançava.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "reach_map")
    public void aMineMovesOnlyToAMouthTheVillageReachesOnFoot(TestContext context) {
        ServerWorld world = context.getWorld();

        // Um piso de vidro sob o centro da caixa (a medida recentra a colônia), e uma ilha do outro
        // lado de um vão de dois blocos.
        for (int x = 0; x <= 6; x++) {
            for (int z = 2; z <= 5; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, 40, z)), Blocks.GLASS.getDefaultState());
            }
        }

        world.setBlockState(context.getAbsolutePos(new BlockPos(3, 40, 8)), Blocks.GLASS.getDefaultState());

        BlockPos centre = context.getAbsolutePos(new BlockPos(3, 41, 3));
        BlockPos low = context.getAbsolutePos(new BlockPos(-2, 36, -2));
        BlockPos high = context.getAbsolutePos(new BlockPos(9, 48, 9));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(centre));

        colony.measure(new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ()));
        VillageColonyMod.COLONIES.register(colony);

        try {
            BlockPos besideThePath = context.getAbsolutePos(new BlockPos(6, 41, 3));
            BlockPos onTheIsland = context.getAbsolutePos(new BlockPos(3, 41, 8));

            context.assertTrue(MineTrouble.walkableFromTheVillage(world, colony.id(), besideThePath),
                    "a boca ao lado do caminho foi recusada");
            context.assertFalse(MineTrouble.walkableFromTheVillage(world, colony.id(), onTheIsland),
                    "a boca na ilha do outro lado do vão foi aceita: a mina se mudaria para onde ninguém chega");
        } finally {
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
