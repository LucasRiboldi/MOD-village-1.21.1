package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/** As ruas medidas como rede de caminhos conectados — item 4 do autor, 2026-10-13. */
public class RoadNetworkGameTest implements FabricGameTest {

    /** Alto o bastante para o caminho da prova ser o topo de cada coluna. */
    private static final int HIGH = 40;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_network", tickLimit = 20)
    public void theNetworkFollowsConnectedPathsAndLeavesStrayOnesOut(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(0, HIGH, 0));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(origin.up()));
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        BlockPos from = origin.add(-2, -1, -6);
        BlockPos to = origin.add(40, 3, 6);

        colony.measure(new VillageBounds(origin.getX() - 4, origin.getY() - 4, origin.getZ() - 4,
                origin.getX() + 20, origin.getY() + 6, origin.getZ() + 4));
        VillageColonyMod.COLONIES.register(colony);

        try {
            for (BlockPos at : BlockPos.iterate(from, to)) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 2);
            }

            // Rua de 0 a 24 em x, com um degrau em x = 12; e um caminho solto em z = 5, longe do centro.
            for (int x = 0; x <= 24; x++) {
                int y = x >= 12 ? 1 : 0;

                world.setBlockState(origin.add(x, y - 1, 0), Blocks.STONE.getDefaultState(), 2);
                world.setBlockState(origin.add(x, y, 0), Blocks.DIRT_PATH.getDefaultState(), 2);
            }

            for (int x = 36; x <= 39; x++) {
                world.setBlockState(origin.add(x, 0, 5), Blocks.DIRT_PATH.getDefaultState(), 2);
            }

            Optional<ColonyRoads> roads = RoadNetwork.trace(world, colony);

            context.assertTrue(roads.isPresent(), "a rede de caminhos não foi traçada");

            BlockPos far = origin.add(24, 1, 0);
            BlockPos stray = origin.add(37, 0, 5);

            context.assertTrue(roads.get().columns().contains(ColonyRoads.column(far.getX(), far.getZ())),
                    "a rede parou no degrau e não chegou à ponta da rua");
            context.assertFalse(roads.get().columns().contains(ColonyRoads.column(stray.getX(), stray.getZ())),
                    "o caminho solto, sem ligação com a vila, entrou na rede");
        } finally {
            for (BlockPos at : BlockPos.iterate(from, to)) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 2);
            }

            owned.cleanUp();
        }

        context.complete();
    }
}
