package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.NeighborhoodRoutes;
import com.villagecolony.core.construction.model.RoadEdgeState;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.construction.model.RoadGraphMode;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.data.save.GraphModeSavedData;
import com.villagecolony.data.save.RoadGraphStore;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Regra 388 (ADR-065 §8, etapa 1): as ligações do grafo planejado ficam no save. Reabrir o mundo, que apaga a memória do
 * servidor, não refaz as ligações: elas voltam travadas, e o grafo fica igual.
 */
public class PlannedGraphReopenGameTest implements FabricGameTest {

    private static final int SIZE = 40;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "planned_graph_reopen", tickLimit = 20)
    public void reopeningTheWorldKeepsThePlannedLinks(TestContext context) {
        ServerWorld world = context.getWorld();
        // Longe da bateria: a plataforma de 40 não pode invadir a arena vizinha.
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 96, 1)).add(2_800_000, 0, 2_800_000);
        GraphModeSavedData modes = GraphModeSavedData.get(world.getServer());
        RoadGraphMode before = modes.mode();

        chunks(world, origin, true);

        for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 0, SIZE))) {
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
        }

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 20));
        Colony colony = fixture.colony();
        RoadGraphStore store = ColonySavedData.get(world.getServer()).roadGraphs();

        try {
            modes.set(RoadGraphMode.VIZINHANCA);

            // Uma boca de mina ao sul do centro: a entrada que a malha liga pelo braço sul (Regra 389).
            Set<ColonyPos> paths = new HashSet<>();
            BlockPos mouth = origin.add(20, 1, 34);

            paths.add(MinecraftTypeAdapter.toColonyPos(origin.add(20, 0, 21)));
            world.setBlockState(origin.add(20, 0, 21), Blocks.DIRT_PATH.getDefaultState(), 18);
            com.villagecolony.VillageColonyMod.MINES.open(colony.id(), com.villagecolony.core.construction.model.MineShaft.from(
                    MinecraftTypeAdapter.toColonyPos(mouth), com.villagecolony.core.type.Side.SOUTH));
            com.villagecolony.fabric.integration.MineMouth.furnish(world, mouth, net.minecraft.util.math.Direction.SOUTH, false);

            for (int read = 0; read < 4; read++) {
                RoadGraphRefresh.publish(world, colony, paths);
            }

            NeighborhoodRoutes kept = store.routes(colony.id());
            Optional<RoadGraph> graph = store.graph(colony.id());

            context.assertTrue(!kept.links().isEmpty() && kept.links().values().stream()
                            .allMatch(link -> link.state() == RoadEdgeState.LOCKED),
                    "Regra 388: as ligações não travaram depois de 4 leituras: " + kept.links());

            // Reabrir o mundo: a memória do servidor some, o save fica.
            RoadGraphBuilds.clearAll();
            RoadGraphRefresh.publish(world, colony, paths);

            context.assertTrue(kept.sameAs(store.routes(colony.id())),
                    "Regra 388: reabrir o mundo refez as ligações: " + store.routes(colony.id()).links());
            context.assertTrue(graph.equals(store.graph(colony.id())), "Regra 388: reabrir o mundo mudou o grafo");
        } finally {
            com.villagecolony.VillageColonyMod.MINES.removeOfColony(colony.id());
            modes.set(before);
            RoadGraphBuilds.clearAll();
            fixture.cleanUp();

            for (BlockPos at : BlockPos.iterate(origin.add(0, -12, 0), origin.add(SIZE, 9, SIZE))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }

            chunks(world, origin, false);
        }

        context.complete();
    }

    private static void chunks(ServerWorld world, BlockPos origin, boolean forced) {
        for (int x = origin.getX() >> 4; x <= (origin.getX() + SIZE) >> 4; x++) {
            for (int z = origin.getZ() >> 4; z <= (origin.getZ() + SIZE) >> 4; z++) {
                world.setChunkForced(x, z, forced);
            }
        }
    }
}
