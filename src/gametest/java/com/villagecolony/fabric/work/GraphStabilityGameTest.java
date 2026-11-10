package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.construction.model.RoadSettling;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** O caminho de fora da caixa que encosta no de dentro entra no grafo (Regra 224), e o trecho que ficou trava (Regra 225). */
public class GraphStabilityGameTest implements FabricGameTest {

    private static final int SIZE = 60;

    private static void chunks(ServerWorld world, BlockPos origin, boolean forced) {
        for (int x = origin.getX() >> 4; x <= (origin.getX() + SIZE) >> 4; x++) {
            for (int z = origin.getZ() >> 4; z <= (origin.getZ() + SIZE) >> 4; z++) {
                world.setChunkForced(x, z, forced);
            }
        }
    }

    private static void floor(ServerWorld world, BlockPos origin) {
        chunks(world, origin, true);

        // O caminho só conta com apoio embaixo (RoadGraphMigration.usable): o chão tem duas camadas.
        for (BlockPos at : BlockPos.iterate(origin.down(), origin.add(SIZE, 0, SIZE))) {
            world.setBlockState(at, at.getY() < origin.getY() ? Blocks.DIRT.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState(), 18);
        }
    }

    private static void clean(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin.down(), origin.add(SIZE, 4, SIZE))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }

        chunks(world, origin, false);
    }

    private static Set<ColonyPos> path(ServerWorld world, BlockPos origin, int fromX, int toX, int z) {
        Set<ColonyPos> cells = new HashSet<>();

        for (int x = fromX; x <= toX; x++) {
            BlockPos at = origin.add(x, 0, z);

            world.setBlockState(at, Blocks.DIRT_PATH.getDefaultState(), 18);
            cells.add(MinecraftTypeAdapter.toColonyPos(at));
        }

        return cells;
    }

    private static Set<ColonyPos> lineOf(RoadGraph graph) {
        Set<ColonyPos> line = new HashSet<>();

        graph.nodes().forEach(node -> line.add(node.position()));
        graph.edges().forEach(edge -> line.addAll(edge.columns()));

        return line;
    }

    /** A rua sai da caixa e segue 15 blocos: entra no grafo. Um caminho solto do lado de fora, não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_outside", tickLimit = 20)
    public void aPathLeavingTheBoxStaysInTheGraphButALooseOneOutsideDoesNot(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 60, 1));

        floor(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(10, 1, 20));
        Colony colony = fixture.colony();

        try {
            colony.measure(new VillageBounds(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 25,
                    origin.getY() + 8, origin.getZ() + 40));
            path(world, origin, 2, 40, 20);
            path(world, origin, 45, 55, 50);

            RoadGraphRefresh.scanNow(world, colony);

            RoadGraph graph = ColonySavedData.get(world.getServer()).roadGraphs().graph(colony.id()).orElseThrow();
            Set<ColonyPos> line = lineOf(graph);

            context.assertTrue(line.contains(MinecraftTypeAdapter.toColonyPos(origin.add(38, 0, 20))),
                    "Regra 224: a rua que sai da caixa foi cortada na borda: " + line.size());
            context.assertFalse(line.contains(MinecraftTypeAdapter.toColonyPos(origin.add(50, 0, 50))),
                    "Regra 224: entrou caminho solto de fora da caixa");
        } finally {
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** Dez dias sem mudar travam o trecho; a trava sobrevive a um grafo novo, e sai quando o caminho some do mundo. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_settle", tickLimit = 20)
    public void aStretchSettlesAfterTenDaysAndLetsGoWhenThePathIsGone(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 80, 1));

        floor(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(2, 1, 20));
        Colony colony = fixture.colony();

        try {
            Set<ColonyPos> paths = path(world, origin, 2, 40, 20);
            var store = ColonySavedData.get(world.getServer()).roadGraphs();

            for (long day = 0; day <= RoadSettling.DAYS; day++) {
                RoadGraphRefresh.publish(world, colony, paths, Set.of(), day);
            }

            long middle = ColonyRoads.column(origin.getX() + 20, origin.getZ() + 20);

            context.assertTrue(RoadSettling.fixed(store.settled(colony.id()), RoadSettling.DAYS).contains(middle),
                    "Regra 225: o trecho não travou em " + RoadSettling.DAYS + " dias: " + store.settled(colony.id()).size());
            context.assertTrue(RoadSettling.fixed(store.settled(colony.id()), RoadSettling.DAYS - 1).isEmpty(),
                    "Regra 225: travou antes dos " + RoadSettling.DAYS + " dias");

            paths.addAll(path(world, origin, 20, 20, 21));
            paths.addAll(path(world, origin, 20, 20, 22));
            paths.addAll(path(world, origin, 20, 20, 23));
            RoadGraphRefresh.publish(world, colony, paths, Set.of(), RoadSettling.DAYS + 1);

            context.assertTrue(store.settled(colony.id()).getOrDefault(middle, -1L) == 0L,
                    "Regra 225: o grafo novo apagou a trava: " + store.settled(colony.id()).get(middle));

            BlockPos gone = origin.add(30, 0, 20);
            long goneColumn = ColonyRoads.column(gone.getX(), gone.getZ());

            world.setBlockState(gone, Blocks.GRASS_BLOCK.getDefaultState(), 18);
            paths.remove(MinecraftTypeAdapter.toColonyPos(gone));
            RoadGraphRefresh.publish(world, colony, paths, Set.of(), RoadSettling.DAYS + 2);

            Map<Long, Long> since = store.settled(colony.id());

            context.assertTrue(since.getOrDefault(goneColumn, -1L) == RoadSettling.DAYS + 2,
                    "Regra 225: o trecho sem caminho continuou travado: " + since.get(goneColumn));
        } finally {
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }
}
