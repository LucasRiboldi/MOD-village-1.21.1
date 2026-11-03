package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Contratos de exclusão e prova encadeada sem relaxar a geração — MAE Fase 2. */
public final class MaeContractGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_unknown_lock", tickLimit = 20)
    public void unknownCompletedJobKeepsSectionLockedAfterReload(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            fixture.capture();
            UUID first = fixture.writer.reserve(context.getWorld(), fixture.colony.colony().id(),
                    area(fixture.base.add(-3, -3, -3), fixture.base.add(1, 3, 3)));
            UUID second = fixture.writer.reserve(context.getWorld(), fixture.colony.colony().id(),
                    area(fixture.base.add(2, -3, -3), fixture.base.add(7, 3, 3)));
            var region = fixture.store.region(first).orElseThrow();
            var job = surfaceJob(fixture.base);
            job.putString("type", "future_operation");
            job.putInt("contract", 99);
            job.putString("state", "complete");
            region.put("job", job);
            fixture.store.update(region);
            var disk = new net.minecraft.nbt.NbtCompound();
            fixture.store.write(disk);
            fixture.store.read(disk);
            refused(context, () -> MaeSectionLocks.check(context.getWorld(), fixture.store, second,
                    MaeProtection.margin(area(fixture.base.east(4), fixture.base.east(4)))), "shared_section_busy");
            context.assertTrue(fixture.store.region(first).orElseThrow().equals(region), "consulta reescreveu contrato futuro");
        } finally { fixture.cleanup(); }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_publication_roles", tickLimit = 20)
    public void extendingRoadPreservesExistingEntrance(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            ground(fixture);
            fixture.set(fixture.base, Blocks.DIRT_PATH.getDefaultState());
            publishFixtureRoad(context, fixture);
            var job = surfaceJob(fixture.base.east());
            job.putLong("hub", fixture.base.asLong());
            var graph = MaeRoadExecution.publicationGraph(context.getWorld(), fixture.colony.colony().id(), job);
            context.assertTrue(graph.nodes().stream().anyMatch(node ->
                    node.position().equals(MinecraftTypeAdapter.toColonyPos(fixture.base))
                    && node.roles().contains(RoadGraph.Role.ENTRANCE)), "publicação apagou entrada existente");
        } finally {
            SweepState.ROADS.remove(fixture.colony.colony().id());
            fixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_final_duplicate", tickLimit = 20)
    public void finalTerrainRejectsDuplicateColumns(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            ground(fixture);
            var job = surfaceJob(fixture.base, fixture.base);
            refused(context, () -> MaeTerrainExecution.finish(context.getWorld(), job), "invalid_terrain_surface");
        } finally { fixture.cleanup(); }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_final_disconnected", tickLimit = 20)
    public void finalTerrainRejectsDisconnectedCirculation(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            ground(fixture);
            var job = surfaceJob(fixture.base, fixture.base.east(2));
            refused(context, () -> MaeTerrainExecution.finish(context.getWorld(), job), "final_disconnected_surface");
        } finally { fixture.cleanup(); }
        context.complete();
    }

    private static net.minecraft.nbt.NbtCompound surfaceJob(BlockPos... positions) {
        var job = new net.minecraft.nbt.NbtCompound();
        job.put("area", MaeWorldWriter.encode(area(positions[0].add(-1, -2, -1), positions[0].add(4, 3, 1))));
        job.putLong("entry", positions[0].asLong());
        var surfaces = new net.minecraft.nbt.NbtList();
        for (BlockPos at : positions) {
            var value = new net.minecraft.nbt.NbtCompound();
            value.putLong("position", at.asLong());
            surfaces.add(value);
        }
        job.put("surfaces", surfaces);
        return job;
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sections", tickLimit = 20)
    public void distinctRegionsCannotApproveConcurrentJobsInTheSameSection(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            fixture.set(fixture.base.west(), Blocks.AIR.getDefaultState());
            fixture.set(fixture.base.east(4), Blocks.AIR.getDefaultState());
            fixture.capture();
            UUID first = fixture.writer.reserve(context.getWorld(), fixture.colony.colony().id(),
                    area(fixture.base.add(-3, -3, -3), fixture.base.add(1, 3, 3)));
            UUID second = fixture.writer.reserve(context.getWorld(), fixture.colony.colony().id(),
                    area(fixture.base.add(2, -3, -3), fixture.base.add(7, 3, 3)));
            fixture.writer.approve(context.getWorld(), first, pointLot(fixture, fixture.base.west()),
                    List.of(fixture.change(fixture.base.west())));
            fixture.writer.pause(context.getWorld(), first);
            refused(context, () -> fixture.writer.approve(context.getWorld(), second,
                    pointLot(fixture, fixture.base.east(4)), List.of(fixture.change(fixture.base.east(4)))),
                    "shared_section_busy");
            context.assertFalse(fixture.store.region(second).orElseThrow().contains("job"), "segunda aprovação persistiu trabalho");
            context.assertTrue(context.getWorld().getBlockState(fixture.base.east(4)).isAir(), "recusa escreveu terreno");
        } finally {
            fixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_generation_anchor", tickLimit = 20)
    public void generationRoadAcceptsOnlyItsOwnConfirmedNeighbourChanges(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            ground(fixture);
            fixture.set(fixture.base, Blocks.DIRT_PATH.getDefaultState());
            fixture.capture();
            UUID region = fixture.writer.reserveTerrain(context.getWorld(), fixture.colony.colony().id(),
                    area(fixture.base.add(-3, -6, -3), fixture.base.add(7, 6, 3)));
            publishFixtureRoad(context, fixture);
            var work = area(fixture.base.south().add(0, -2, 0), fixture.base.south().add(1, 3, 0));
            var readOnly = fixture.writer.roadContext(context.getWorld(), region, work,
                    MinecraftTypeAdapter.toColonyPos(fixture.base));
            var data = fixture.store.region(region).orElseThrow();
            // Simula uma mudança própria confirmada em outro alvo, sem recapturar o recibo.
            BlockPos neighbour = fixture.base.south();
            var expected = MaeGenerationEvidence.after(context.getWorld(), MaeProtection.margin(work), neighbour,
                    Blocks.STONE.getDefaultState());
            fixture.set(neighbour, Blocks.STONE.getDefaultState());
            data.put("ownedSections", expected);
            fixture.store.update(data);
            MaeRoadEvidence.revalidate(context.getWorld(), data, readOnly);
            fixture.set(fixture.base.east(2), Blocks.STONE.getDefaultState());
            refused(context, () -> MaeRoadEvidence.revalidate(context.getWorld(), data, readOnly), "changed_generation_evidence");
        } finally {
            SweepState.ROADS.remove(fixture.colony.colony().id());
            fixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_anchor_cells", tickLimit = 20)
    public void duplicateReadOnlySnapshotDoesNotAuthorizeMissingCells(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            ground(fixture);
            fixture.set(fixture.base, Blocks.DIRT_PATH.getDefaultState());
            fixture.capture();
            UUID region = fixture.writer.reserveTerrain(context.getWorld(), fixture.colony.colony().id(),
                    area(fixture.base.add(-3, -6, -3), fixture.base.add(7, 6, 3)));
            publishFixtureRoad(context, fixture);
            var work = area(fixture.base.south().add(0, -2, 0), fixture.base.south().add(1, 3, 0));
            var readOnly = fixture.writer.roadContext(context.getWorld(), region, work,
                    MinecraftTypeAdapter.toColonyPos(fixture.base));
            var cells = readOnly.getList("cells", 10);
            cells.set(1, cells.getCompound(0).copy());
            refused(context, () -> MaeRoadEvidence.revalidate(context.getWorld(), fixture.store.region(region).orElseThrow(), readOnly),
                    "invalid_anchor_cells");
        } finally {
            SweepState.ROADS.remove(fixture.colony.colony().id());
            fixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_anchor_identity", tickLimit = 20)
    public void anchorRequiresOwnGraphIndexHeightAndUnchangedPhysicalContext(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        UUID foreign = UUID.randomUUID();
        UUID colony = fixture.colony.colony().id();
        try {
            ground(fixture);
            fixture.set(fixture.base, Blocks.DIRT_PATH.getDefaultState());
            fixture.capture();
            UUID region = fixture.writer.reserveTerrain(context.getWorld(), colony,
                    area(fixture.base.add(-3, -6, -3), fixture.base.add(7, 6, 3)));
            var work = area(fixture.base.south().add(0, -2, 0), fixture.base.south().add(1, 3, 0));
            var anchor = MinecraftTypeAdapter.toColonyPos(fixture.base);
            var before = fixture.store.region(region).orElseThrow();
            refused(context, () -> fixture.writer.roadContext(context.getWorld(), region, work, anchor),
                    "missing_road_graph");
            publishFixtureRoad(context, fixture);
            var graphStore = ColonySavedData.get(context.getWorld().getServer()).roadGraphs();
            var originalGraph = graphStore.graph(colony).orElseThrow();
            var originalIndex = SweepState.ROADS.remove(colony);
            refused(context, () -> fixture.writer.roadContext(context.getWorld(), region, work, anchor),
                    "anchor_index_diverged");
            SweepState.ROADS.put(colony, originalIndex);
            refused(context, () -> fixture.writer.roadContext(context.getWorld(), region, work,
                    MinecraftTypeAdapter.toColonyPos(fixture.base.up())), "anchor_not_primary");
            var readOnly = fixture.writer.roadContext(context.getWorld(), region, work, anchor);
            SweepState.ROADS.put(foreign, new ColonyRoads(foreign, anchor,
                    List.of(ColonyRoads.column(anchor.x(), anchor.z()))));
            refused(context, () -> MaeRoadEvidence.revalidate(context.getWorld(), before, readOnly),
                    "anchor_index_diverged");
            SweepState.ROADS.remove(foreign);
            var other = MinecraftTypeAdapter.toColonyPos(fixture.base.east(2));
            graphStore.update(colony, RoadGraph.from(Set.of(other), other, Set.of(other)));
            refused(context, () -> MaeRoadEvidence.revalidate(context.getWorld(), before, readOnly),
                    "anchor_not_primary");
            graphStore.update(colony, originalGraph);
            fixture.set(fixture.base, Blocks.AIR.getDefaultState());
            refused(context, () -> MaeRoadEvidence.revalidate(context.getWorld(), before, readOnly),
                    "invalid_road_floor");
            fixture.set(fixture.base, Blocks.DIRT_PATH.getDefaultState());
            fixture.set(fixture.base.up(), Blocks.STONE.getDefaultState());
            refused(context, () -> MaeRoadEvidence.revalidate(context.getWorld(), before, readOnly),
                    "road_clearance");
            fixture.set(fixture.base.up(), Blocks.AIR.getDefaultState());
            MaeRoadEvidence.revalidate(context.getWorld(), before, readOnly);
            context.assertTrue(before.equals(fixture.store.region(region).orElseThrow()),
                    "consulta ou recusa alterou reserva, plano ou diário");
            context.assertTrue(originalGraph.equals(graphStore.graph(colony).orElseThrow())
                    && originalIndex.equals(SweepState.ROADS.get(colony)), "consulta alterou rede existente");
        } finally {
            SweepState.ROADS.remove(foreign);
            SweepState.ROADS.remove(colony);
            fixture.cleanup();
        }
        context.complete();
    }

    private static void publishFixtureRoad(TestContext context, MaeSafetyGameTest.Fixture fixture) {
        var at = MinecraftTypeAdapter.toColonyPos(fixture.base);
        UUID colony = fixture.colony.colony().id();
        ColonySavedData.get(context.getWorld().getServer()).roadGraphs().update(colony, RoadGraph.from(Set.of(at), at, Set.of(at)));
        SweepState.ROADS.put(colony, new ColonyRoads(colony, at, List.of(ColonyRoads.column(at.x(), at.z()))));
    }

    private static void ground(MaeSafetyGameTest.Fixture fixture) {
        for (BlockPos at : BlockPos.iterate(fixture.base.add(-2, -4, -2), fixture.base.add(5, 0, 2))) fixture.set(at, Blocks.DIRT.getDefaultState());
        for (BlockPos at : BlockPos.iterate(fixture.base.up(), fixture.base.add(5, 3, 2))) fixture.set(at, Blocks.AIR.getDefaultState());
    }

    private static Building pointLot(MaeSafetyGameTest.Fixture fixture, BlockPos at) {
        var point = MinecraftTypeAdapter.toColonyPos(at);
        return new Building(UUID.randomUUID(), fixture.colony.colony().id(), fixture.lot().blueprint(), point, point, false);
    }

    private static WorkArea area(BlockPos min, BlockPos max) {
        return new WorkArea(MinecraftTypeAdapter.toColonyPos(min), MinecraftTypeAdapter.toColonyPos(max));
    }

    private static void refused(TestContext context, Runnable action, String expected) {
        try {
            action.run();
        } catch (IllegalStateException failure) {
            context.assertTrue(expected.equals(failure.getMessage()), "recusa incorreta: " + failure.getMessage());
            return;
        }
        context.assertTrue(false, "operação deveria recusar: " + expected);
    }
}
