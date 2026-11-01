package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.Identifier;
import net.minecraft.world.chunk.ChunkStatus;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A migração lê o mundo sem reconstruir ruas ou carregar chunks — ADR-043. */
public class RoadGraphMigrationGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_graph_migration")
    public void removedAndUnsupportedRoadsStayRemoved(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(4_000_000, 30, 4_000_000);
        try {
            for (int x = 0; x < 4; x++) {
                world.getChunk(base.add(x, 0, 0));
                world.setBlockState(base.add(x, -1, 0), Blocks.STONE.getDefaultState());
                world.setBlockState(base.add(x, 0, 0), Blocks.DIRT_PATH.getDefaultState());
            }
            world.setBlockState(base.add(1, 0, 0), Blocks.AIR.getDefaultState());
            world.setBlockState(base.add(3, -1, 0), Blocks.AIR.getDefaultState());
            ColonyRoads legacy = roads(base, List.of(column(base), column(base.add(1, 0, 0)),
                    column(base.add(2, 0, 0)), column(base.add(3, 0, 0))));
            var result = RoadGraphMigration.inspect(world, legacy, Set.of());
            context.assertTrue(result.pending().isEmpty(), "chunk carregado ficou pendente");
            context.assertTrue(result.missing().equals(Set.of(column(base.add(1, 0, 0)),
                    column(base.add(3, 0, 0)))), "rua removida ou sem apoio foi aceita: " + result);
            var graph = result.graph().orElseThrow();
            context.assertTrue(graph.nodes().size() == 2 && graph.edges().isEmpty(),
                    "a migração inventou ligação sobre rua removida");
            context.assertTrue(result.equals(RoadGraphMigration.inspect(world, legacy, Set.of())),
                    "repetir a leitura mudou o resultado");
            context.assertTrue(world.getBlockState(base.add(1, 0, 0)).isAir(), "rua removida reapareceu");
            context.assertTrue(world.getBlockState(base.add(3, -1, 0)).isAir(), "migração criou apoio");
            context.assertTrue(world.getBlockState(base.add(3, 0, 0)).isOf(Blocks.DIRT_PATH),
                    "migração apagou rua sem apoio");
            context.assertTrue(legacy.columns().size() == 4, "índice legado foi truncado");
        } finally {
            for (int x = 0; x < 4; x++) {
                world.setBlockState(base.add(x, 0, 0), Blocks.AIR.getDefaultState());
                world.setBlockState(base.add(x, -1, 0), Blocks.AIR.getDefaultState());
            }
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_graph_migration")
    public void anUnavailableChunkDefersTheWholeGraphWithoutLoadingIt(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos unknown = context.getAbsolutePos(BlockPos.ORIGIN).add(7_000_000, 30, 7_000_000);
        context.assertTrue(!world.isChunkLoaded(unknown), "cenário exige chunk indisponível");
        ColonyRoads legacy = roads(unknown, List.of(column(unknown)));
        var result = RoadGraphMigration.inspect(world, legacy, Set.of());
        context.assertTrue(result.graph().isEmpty() && result.missing().isEmpty(),
                "chunk desconhecido virou apagamento ou grafo completo");
        context.assertTrue(result.pending().equals(Set.of(column(unknown))), "coluna pendente foi perdida");
        context.assertTrue(!world.isChunkLoaded(unknown), "inspeção carregou chunk");
        context.assertTrue(legacy.columns().equals(List.of(column(unknown))), "índice legado mudou");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_graph_migration")
    public void aConfirmedComponentKeepsItsHeightAndEntrance(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(5_000_000, 30, 5_000_000);
        try {
            for (int x = 0; x <= 2; x++) {
                world.getChunk(base.add(x, 0, 0));
                world.setBlockState(base.add(x, -1, 0), Blocks.STONE.getDefaultState());
                world.setBlockState(base.add(x, 0, 0), Blocks.DIRT_PATH.getDefaultState());
            }
            ColonyPos entrance = MinecraftTypeAdapter.toColonyPos(base.add(2, 0, 0));
            ColonyRoads legacy = roads(base, List.of(column(base), column(base.east()), column(base.east(2))));
            var result = RoadGraphMigration.inspect(world, legacy, Set.of(entrance));
            RoadGraph graph = result.graph().orElseThrow();
            context.assertTrue(result.missing().isEmpty() && result.pending().isEmpty(), "rua íntegra recusada");
            context.assertTrue(graph.edges().size() == 1 && graph.edges().getFirst().columns().size() == 3,
                    "geometria confirmada perdida");
            context.assertTrue(graph.edges().getFirst().roadClass() == RoadGraph.RoadClass.PRIMARY,
                    "componente do centro não foi classificado");
            context.assertTrue(graph.nodes().stream().anyMatch(node -> node.position().equals(entrance)
                    && node.roles().contains(RoadGraph.Role.ENTRANCE)), "entrada confirmada perdida");
            BlockPos unknown = base.add(4096, 0, 0);
            context.assertTrue(!world.isChunkLoaded(unknown), "cenário parcial exige chunk desconhecido");
            ColonyRoads partial = roads(base, List.of(column(base), column(unknown)));
            var deferred = RoadGraphMigration.inspect(world, partial, Set.of());
            context.assertTrue(deferred.graph().isEmpty() && deferred.pending().contains(column(unknown)),
                    "grafo parcial foi publicado");
            context.assertTrue(world.getBlockState(base).isOf(Blocks.DIRT_PATH), "revalidação alterou rua");
            context.assertTrue(!world.isChunkLoaded(unknown), "revalidação carregou chunk desconhecido");
        } finally {
            for (int x = 0; x <= 2; x++) {
                world.setBlockState(base.add(x, 0, 0), Blocks.AIR.getDefaultState());
                world.setBlockState(base.add(x, -1, 0), Blocks.AIR.getDefaultState());
            }
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_graph_migration")
    public void aRoadUnderARoofKeepsItsPhysicalHeight(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(4_100_000, 30, 4_100_000);
        world.getChunk(base);
        var saved = saveColumn(world, base, -1, 6);
        try {
            clearColumn(world, base, -1, 6);
            world.setBlockState(base.down(), Blocks.STONE.getDefaultState());
            world.setBlockState(base, Blocks.DIRT_PATH.getDefaultState());
            world.setBlockState(base.up(4), Blocks.STONE.getDefaultState());
            var result = RoadGraphMigration.inspect(world, roads(base, List.of(column(base))), Set.of());
            context.assertTrue(result.pending().isEmpty() && result.missing().isEmpty()
                            && result.graph().isPresent(), "teto fez rua física desaparecer: " + result);
            ColonyPos expected = MinecraftTypeAdapter.toColonyPos(base);
            context.assertTrue(result.graph().orElseThrow().nodes().stream()
                    .anyMatch(node -> node.position().equals(expected)), "cota sob o teto foi perdida");
            context.assertTrue(world.getBlockState(base.up(4)).isOf(Blocks.STONE), "inspeção removeu teto");
        } finally {
            saved.forEach(world::setBlockState);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_graph_migration")
    public void twoEligibleFloorsDeferTheGraphInsteadOfChoosingAHeight(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(4_200_000, 30, 4_200_000);
        world.getChunk(base);
        var saved = saveColumn(world, base, -1, 6);
        try {
            clearColumn(world, base, -1, 6);
            for (BlockPos ground : List.of(base, base.up(4))) {
                world.setBlockState(ground.down(), Blocks.STONE.getDefaultState());
                world.setBlockState(ground, Blocks.DIRT_PATH.getDefaultState());
            }
            var result = RoadGraphMigration.inspect(world, roads(base, List.of(column(base))), Set.of());
            context.assertTrue(result.graph().isEmpty() && result.missing().isEmpty()
                            && result.pending().equals(Set.of(column(base))),
                    "dois pisos foram resolvidos sem prova de cota: " + result);
        } finally {
            saved.forEach(world::setBlockState);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_graph_migration")
    public void aRoofBeyondTheSearchDepthDefersInsteadOfRemovingTheRoad(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(4_300_000, 30, 4_300_000);
        world.getChunk(base);
        var saved = saveColumn(world, base, -1, 18);
        try {
            clearColumn(world, base, -1, 18);
            world.setBlockState(base.down(), Blocks.STONE.getDefaultState());
            world.setBlockState(base, Blocks.DIRT_PATH.getDefaultState());
            world.setBlockState(base.up(16), Blocks.STONE.getDefaultState());
            var result = RoadGraphMigration.inspect(world, roads(base, List.of(column(base))), Set.of());
            context.assertTrue(result.graph().isEmpty() && result.missing().isEmpty()
                            && result.pending().equals(Set.of(column(base))),
                    "rua fora da janela vertical foi declarada ausente: " + result);
        } finally {
            saved.forEach(world::setBlockState);
        }
        context.complete();
    }

    /** Piso superior não resolve a cota legada fora da janela de inspeção — ADR-043. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_graph_migration")
    public void anEligibleUpperFloorDoesNotHideALegacyRoadBelowTheSearchWindow(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(BlockPos.ORIGIN);
        BlockPos lower = new BlockPos(origin.getX() + 4_500_000, 64, origin.getZ() + 4_500_000);
        BlockPos upper = lower.up(16);
        world.getChunk(lower);
        var saved = saveColumn(world, lower, -1, 18);
        try {
            clearColumn(world, lower, -1, 18);
            for (BlockPos ground : List.of(lower, upper)) {
                world.setBlockState(ground.down(), Blocks.STONE.getDefaultState());
                world.setBlockState(ground, Blocks.DIRT_PATH.getDefaultState());
                context.assertTrue(world.getBlockState(ground).isOf(Blocks.DIRT_PATH)
                                && world.getBlockState(ground.down()).isSideSolidFullSquare(world,
                                        ground.down(), net.minecraft.util.math.Direction.UP)
                                && world.getBlockState(ground.up()).isAir()
                                && world.getBlockState(ground.up(2)).isAir(),
                        "fixture exige dois pisos apoiados e passagem livre: " + ground.toShortString());
            }
            ColonyRoads legacy = roads(lower, List.of(column(lower)));
            context.assertTrue(legacy.from().y() == 64 && upper.getY() == 80
                            && world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                                    lower.getX(), lower.getZ()) - 1 == 80,
                    "fixture exige origem 64 e topo 80; janela 68..80 exclui a rua inferior");
            var result = RoadGraphMigration.inspect(world, legacy, Set.of());
            context.assertTrue(result.graph().isEmpty() && result.missing().isEmpty()
                            && result.pending().equals(Set.of(column(lower))),
                    "piso superior ocultou rua legada abaixo da janela: " + result);
        } finally {
            saved.forEach(world::setBlockState);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_graph_migration")
    public void legacyBiomePavingDoesNotResolveAnUnloadedStructureReference(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(4_400_000, 30, 4_400_000);
        var chunk = world.getChunk(base);
        // Referência sintética fora do halo já gerado: isola a proibição de carregar chunks — ADR-043.
        ChunkPos remote = new ChunkPos(chunk.getPos().x + 32, chunk.getPos().z + 32);
        var referencesBefore = new HashMap<>(chunk.getStructureReferences());
        var saved = saveColumn(world, base, -1, 2);
        try {
            clearColumn(world, base, -1, 2);
            world.setBlockState(base.down(), Blocks.STONE.getDefaultState());
            world.setBlockState(base, Blocks.SMOOTH_SANDSTONE.getDefaultState());
            context.assertFalse(RoadIndex.isLaidByAColony(base), "fixture já está no índice global");
            context.assertFalse(world.isChunkLoaded(remote.x, remote.z), "referência exige chunk indisponível");
            context.assertTrue(world.getChunkManager().getChunk(remote.x, remote.z,
                    ChunkStatus.STRUCTURE_STARTS, false) == null, "start remoto já estava disponível");
            var village = world.getRegistryManager().get(RegistryKeys.STRUCTURE)
                    .get(Identifier.ofVanilla("village_desert"));
            var references = new HashMap<>(referencesBefore);
            var remoteOnly = new LongOpenHashSet();
            remoteOnly.add(remote.toLong());
            references.put(village, remoteOnly);
            chunk.setStructureReferences(references);
            var result = RoadGraphMigration.inspect(world, roads(base, List.of(column(base))), Set.of());
            context.assertTrue(world.getChunkManager().getChunk(remote.x, remote.z,
                    ChunkStatus.STRUCTURE_STARTS, false) == null, "inspeção carregou chunk até STRUCTURE_STARTS");
            context.assertFalse(world.isChunkLoaded(remote.x, remote.z), "inspeção carregou referência estrutural");
            context.assertTrue(result.graph().isPresent() && result.pending().isEmpty()
                            && result.missing().isEmpty(), "material de bioma indexado foi recusado: " + result);
            context.assertTrue(result.graph().orElseThrow().nodes().stream().anyMatch(node -> node.position()
                    .equals(MinecraftTypeAdapter.toColonyPos(base))), "rua de bioma perdeu a cota física");
            context.assertFalse(RoadIndex.isLaidByAColony(base), "inspeção alterou índice global");
        } finally {
            chunk.setStructureReferences(referencesBefore);
            saved.forEach(world::setBlockState);
        }
        context.complete();
    }

    private static java.util.Map<BlockPos, BlockState> saveColumn(ServerWorld world, BlockPos base,
                                                                int minY, int maxY) {
        var saved = new HashMap<BlockPos, BlockState>();
        for (int y = minY; y <= maxY; y++) {
            BlockPos at = base.up(y);
            saved.put(at, world.getBlockState(at));
        }
        return saved;
    }

    private static void clearColumn(ServerWorld world, BlockPos base, int minY, int maxY) {
        for (int y = minY; y <= maxY; y++) world.setBlockState(base.up(y), Blocks.AIR.getDefaultState());
    }

    private static ColonyRoads roads(BlockPos center, List<Long> columns) {
        ColonyPos from = MinecraftTypeAdapter.toColonyPos(center);
        return new ColonyRoads(UUID.randomUUID(), from, columns);
    }

    private static long column(BlockPos at) {
        return ColonyRoads.column(at.getX(), at.getZ());
    }
}
