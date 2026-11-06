package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageRoad;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** O chão que o grafo pede: Regras 180, 183 a 186 e 208. Tudo em coordenada do mundo: a arena pode vir girada. */
public class RoadRulesGameTest implements FabricGameTest {

    private static final int SIZE = 24;

    private static void forceChunks(ServerWorld world, BlockPos origin, boolean force) {
        for (int x = origin.getX() >> 4; x <= (origin.getX() + SIZE) >> 4; x++) {
            for (int z = origin.getZ() >> 4; z <= (origin.getZ() + SIZE) >> 4; z++) {
                world.setChunkForced(x, z, force);
            }
        }
    }

    /** Um chão de grama no alto, fora das outras arenas, de {@code SIZE} por {@code SIZE}. */
    private static void platform(ServerWorld world, BlockPos origin) {
        forceChunks(world, origin, true);

        for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 0, SIZE))) {
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
        }
    }

    private static void clean(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 4, SIZE))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }

        forceChunks(world, origin, false);
    }

    private static ColonyPos cell(BlockPos at) {
        return MinecraftTypeAdapter.toColonyPos(at);
    }

    private static Building farmAt(UUID colonyId, BlockPos min, BlockPos max) {
        return new Building(UUID.randomUUID(), colonyId, ResourceId.vanilla("village/plains/test_farm"), cell(min), cell(max), true);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_covered", tickLimit = 20)
    public void aCoveredPathIsGroundAgain(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 90, 1));
        BlockPos covered = origin.add(1, 0, 1);
        BlockPos open = origin.add(3, 0, 1);

        platform(world, origin);

        try {
            world.setBlockState(covered, Blocks.DIRT_PATH.getDefaultState(), 18);
            world.setBlockState(covered.up(), Blocks.STONE.getDefaultState(), 18);
            world.setBlockState(open, Blocks.DIRT_PATH.getDefaultState(), 18);
            world.setBlockState(open.up(), Blocks.SHORT_GRASS.getDefaultState(), 18);

            RoadGraphRefresh.coverPath(world, covered.up(), world.getBlockState(covered.up()));
            RoadGraphRefresh.coverPath(world, open.up(), world.getBlockState(open.up()));

            context.assertTrue(world.getBlockState(covered).isOf(Blocks.DIRT), "Regra 180: caminho coberto continuou caminho");
            context.assertTrue(world.getBlockState(open).isOf(Blocks.DIRT_PATH), "Regra 180: mato sem colisão tirou o caminho");
        } finally {
            clean(world, origin);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_rings", tickLimit = 20)
    public void aFarmHasNoRingAndItsNearestCornerIsTheEntrance(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 94, 1));
        BlockPos min = origin.add(6, 1, 6);
        BlockPos max = origin.add(8, 2, 8);

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(1, 1, 1));
        Colony colony = fixture.colony();
        Building farm = farmAt(colony.id(), min, max);

        try {
            VillageColonyMod.BUILDINGS.register(farm);

            WorldPathTerrain terrain = new WorldPathTerrain(world, colony.id());
            Set<ColonyPos> expected = new HashSet<>();

            for (int x = 5; x <= 9; x++) {
                for (int z = 5; z <= 9; z++) {
                    if (x == 5 || x == 9 || z == 5 || z == 9) {
                        expected.add(cell(origin.add(x, 0, z)));
                    }
                }
            }

            context.assertTrue(RoadShape.rings(world, colony, terrain).isEmpty(), "Regra 208: a plantação ainda pede anel");
            context.assertTrue(RoadShape.farmRings(world, colony, terrain).equals(expected), "Regra 208: o chão colado na plantação está errado");
            context.assertTrue(RoadShape.ringOf(world, terrain, new BlockBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ())).equals(expected),
                    "Regra 183: o anel não é a volta da caixa");

            context.assertTrue(RoadShape.farmEntrances(world, colony, terrain).equals(Set.of(cell(origin.add(5, 0, 5)))),
                    "Regra 184: a entrada não é a quina mais perto do centro");
            context.assertTrue(RoadShape.CORNER_AFTER_READS == 3, "Regra 184: a quina vira entrada na terceira releitura");
        } finally {
            VillageColonyMod.BUILDINGS.remove(farm.id());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** A rua, um ramal e o anel que uma versão anterior calçou: na terceira releitura só sobra a rua e a entrada da plantação. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_old_ring", tickLimit = 20)
    public void anOldFarmRingLeavesTheGraphAndTheGround(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 106, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(11, 1, 2));
        Colony colony = fixture.colony();
        Building farm = farmAt(colony.id(), origin.add(8, 1, 8), origin.add(14, 2, 14));
        Set<ColonyPos> paths = new HashSet<>();

        try {
            VillageColonyMod.BUILDINGS.register(farm);

            for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 0, SIZE))) {
                int x = at.getX() - origin.getX();
                int z = at.getZ() - origin.getZ();
                boolean street = z == 2 && x >= 1 && x <= 22;
                boolean branch = x == 11 && z >= 3 && z <= 6;
                boolean ring = x >= 7 && x <= 15 && z >= 7 && z <= 15 && (x == 7 || x == 15 || z == 7 || z == 15);

                if (street || branch || ring) {
                    world.setBlockState(at, Blocks.DIRT_PATH.getDefaultState(), 18);
                    paths.add(cell(at.toImmutable()));
                }
            }

            for (int read = 0; read < RoadShape.CORNER_AFTER_READS; read++) {
                RoadGraphRefresh.publish(world, colony, paths);
            }

            RoadGraph graph = ColonySavedData.get(world.getServer()).roadGraphs().graph(colony.id()).orElseThrow();
            Set<ColonyPos> columns = new HashSet<>();

            graph.nodes().forEach(node -> columns.add(node.position()));
            graph.edges().forEach(edge -> columns.addAll(edge.columns()));

            ColonyPos entrance = cell(origin.add(7, 0, 7));

            context.assertFalse(columns.contains(cell(origin.add(15, 0, 15))), "Regra 208: a quina de longe ficou no grafo");
            context.assertFalse(columns.contains(cell(origin.add(11, 0, 15))), "Regra 208: o lado de baixo do anel ficou no grafo");
            context.assertTrue(graph.nodes().stream().anyMatch(node -> node.position().equals(entrance)
                    && node.roles().contains(RoadGraph.Role.ENTRANCE)), "Regra 184: a plantação ficou sem entrada");
            context.assertTrue(columns.contains(cell(origin.add(1, 0, 2))) && columns.contains(cell(origin.add(22, 0, 2))), "Regra 208: a rua saiu junto");
            context.assertTrue(graph.nodes().stream().filter(node -> node.roles().contains(RoadGraph.Role.ENTRANCE)).count() == 1,
                    "Regra 208: a plantação ficou com mais de uma entrada: " + graph.nodes());
            context.assertTrue(graph.nodes().stream().noneMatch(node -> node.roles().contains(RoadGraph.Role.ENDPOINT)
                    && !node.roles().contains(RoadGraph.Role.HUB) && !node.roles().contains(RoadGraph.Role.ENTRANCE)
                    && node.position().z() != origin.getZ() + 2), "Regra 206: sobrou ponta azul fora da rua: " + graph.nodes());

            for (int pass = 0; pass < 4; pass++) {
                PathTrim.pass(world, colony);
            }

            context.assertFalse(VillageRoad.isPathBlock(world.getBlockState(origin.add(15, 0, 15))), "Regra 208: o anel continuou caminho no chão");
        } finally {
            VillageColonyMod.BUILDINGS.remove(farm.id());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_tips", tickLimit = 20)
    public void onlyTheFarTipGrowsOncePerDay(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 98, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(4, 0, 4));
        Colony colony = fixture.colony();
        long time = world.getTimeOfDay();

        try {
            // A ponta perto fica a 3 do centro e a longe a 10: 3 está abaixo de 75% de 10.
            Set<ColonyPos> line = new HashSet<>();

            for (int x = 1; x <= 14; x++) {
                line.add(cell(origin.add(x, 0, 4)));
                world.setBlockState(origin.add(x, 0, 4), Blocks.DIRT_PATH.getDefaultState(), 18);
            }

            RoadGraph graph = RoadGraph.from(line, colony.center(), Set.of());
            WorldPathTerrain terrain = new WorldPathTerrain(world, colony.id());

            RoadShape.growTips(world, colony, graph, terrain);
            context.assertTrue(RoadShape.pendingGrowth(colony.id(), Set.of(), Set.of()).isEmpty(), "Regra 185: cresceu na primeira leitura");

            RoadShape.growTips(world, colony, graph, terrain);
            context.assertTrue(RoadShape.pendingGrowth(colony.id(), Set.of(), Set.of()).isEmpty(), "Regra 185: cresceu duas vezes no mesmo dia");

            world.setTimeOfDay(time + 24_000);
            RoadShape.growTips(world, colony, graph, terrain);

            ColonyPos next = cell(origin.add(15, 0, 4));

            context.assertTrue(RoadShape.pendingGrowth(colony.id(), Set.of(), Set.of()).equals(Set.of(next)),
                    "Regra 185: não cresceu só a ponta longe: " + RoadShape.pendingGrowth(colony.id(), Set.of(), Set.of()));
            context.assertTrue(RoadShape.pendingGrowth(colony.id(), Set.of(next), Set.of()).isEmpty(), "Regra 185: o que foi calçado continuou pendente");
        } finally {
            world.setTimeOfDay(time);
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_trim", tickLimit = 20)
    public void theSurplusPathReturnsToTheBiomeWithinTheBudget(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 102, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(1, 1, 1));
        Colony colony = fixture.colony();
        List<ColonyPos> surplus = new ArrayList<>();

        try {
            for (int x = 0; x < 20; x++) {
                world.setBlockState(origin.add(x, 0, 2), Blocks.DIRT_PATH.getDefaultState(), 18);
                surplus.add(cell(origin.add(x, 0, 2)));
            }

            PathTrim.offer(colony.id(), surplus);

            context.assertTrue(PathTrim.pass(world, colony) == 16, "Regra 186: a passada do pedreiro não respeitou o orçamento");
            context.assertTrue(PathTrim.pass(world, colony) == 4, "Regra 186: a segunda passada não terminou o que sobrou");

            for (int x = 0; x < 20; x++) {
                context.assertFalse(VillageRoad.isPathBlock(world.getBlockState(origin.add(x, 0, 2))), "Regra 186: sobrou caminho em " + x);
                context.assertFalse(world.getBlockState(origin.add(x, 0, 2)).isAir(), "Regra 186: abriu buraco em " + x);
            }
        } finally {
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }
}
