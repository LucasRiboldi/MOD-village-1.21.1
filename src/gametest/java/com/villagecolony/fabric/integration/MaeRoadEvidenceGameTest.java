package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import java.util.List;
import java.util.UUID;

/** Rua só entra na rede depois da confirmação física e da publicação — MAE Fase 2. */
public final class MaeRoadEvidenceGameTest implements FabricGameTest {
    private long maxSurveyNanos;
    private long maxWriterNanos;
    private int totalReads;
    private int maxReads;
    private int totalWrites;
    private int maxWrites;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_road_chain", tickLimit = 400)
    public void roadIsInvisibleToNetworkUntilEveryPlacementIsConfirmed(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            for (BlockPos at : BlockPos.iterate(fixture.base.add(-2, -3, -2), fixture.base.add(5, 0, 2))) {
                fixture.set(at, Blocks.DIRT.getDefaultState());
            }
            for (BlockPos at : BlockPos.iterate(fixture.base.up(), fixture.base.add(5, 3, 2))) {
                fixture.set(at, Blocks.AIR.getDefaultState());
            }
            fixture.capture();
            UUID region = fixture.writer.reserveTerrain(context.getWorld(), fixture.colony.colony().id(),
                    new WorkArea(MinecraftTypeAdapter.toColonyPos(fixture.base.add(-3, -8, -3)),
                            MinecraftTypeAdapter.toColonyPos(fixture.base.add(7, 8, 3))));
            List<BlockPos> route = List.of(fixture.base, fixture.base.east(), fixture.base.east(2), fixture.base.east(3));
            var prepared = MaeRoadPreparation.prepare(context.getWorld(), fixture.writer, region, route, fixture.base);
            fixture.writer.approveRoad(context.getWorld(), prepared);
            context.assertTrue(RoadIndex.roadsOf(fixture.colony.colony().id()).isEmpty(), "aprovação publicou rua");
            finish(context, fixture, region, false);
        } catch (RuntimeException | AssertionError failure) {
            fixture.cleanup();
            throw failure;
        }
    }

    private void finish(TestContext context, MaeSafetyGameTest.Fixture fixture, UUID region, boolean extension) {
        try {
            writeSlice(context, fixture, region);
            var data = fixture.store.region(region).orElseThrow();
            var job = data.getCompound("job");
            context.assertFalse("blocked".equals(job.getString("state")), "rua bloqueada: " + job.getString("reason"));
            if (!"complete".equals(job.getString("state"))) {
                int published = RoadIndex.roadsOf(fixture.colony.colony().id()).map(index -> index.columns().size()).orElse(0);
                context.assertTrue(published == (extension ? 4 : 0), "trecho parcial publicado");
                context.waitAndRun(1, () -> finish(context, fixture, region, extension));
                return;
            }
            context.assertTrue(job.getInt("cursor") == (extension ? 2 : 4), "pavimentação incompleta");
            context.assertTrue(data.getList("history", 10).size() == (extension ? 8 : 5), "publicação não entrou no orçamento");
            context.assertTrue(RoadIndex.roadsOf(fixture.colony.colony().id()).orElseThrow().columns().size() == (extension ? 6 : 4),
                    "índice não recebeu trecho confirmado");
            context.assertTrue(com.villagecolony.data.save.ColonySavedData.get(context.getWorld().getServer())
                    .roadGraphs().graph(fixture.colony.colony().id()).orElseThrow().edges().size() == 1,
                    "grafo não recebeu rua contínua");
            context.assertTrue(fixture.writer.runSlice(context.getWorld(), region) == 0, "publicação repetida");
            if (!extension) {
                var prepared = MaeRoadPreparation.prepare(context.getWorld(), fixture.writer, region,
                        List.of(fixture.base.east(4), fixture.base.east(5)), fixture.base, fixture.base.east(3));
                fixture.writer.approveRoad(context.getWorld(), prepared);
                var disk = new net.minecraft.nbt.NbtCompound();
                fixture.store.write(disk);
                fixture.store = new com.villagecolony.data.save.MaeWorkStore(() -> { });
                fixture.store.read(disk);
                fixture.writer = new MaeWorldWriter(fixture.store);
                context.waitAndRun(1, () -> finish(context, fixture, region, true));
                return;
            }
            var beforeGraph = com.villagecolony.data.save.ColonySavedData.get(context.getWorld().getServer())
                    .roadGraphs().graph(fixture.colony.colony().id()).orElseThrow();
            var floor = fixture.base.south().down();
            var lot = new com.villagecolony.core.construction.model.Building(UUID.randomUUID(), fixture.colony.colony().id(),
                    fixture.lot().blueprint(), MinecraftTypeAdapter.toColonyPos(floor),
                    MinecraftTypeAdapter.toColonyPos(floor.east(3)), false);
            var surfaces = List.of(floor, floor.east(), floor.east(2), floor.east(3)).stream()
                    .map(MinecraftTypeAdapter::toColonyPos).toList();
            var proposal = new MaeTerrainPreparation.Proposal(lot, surfaces, surfaces.getFirst(),
                    MinecraftTypeAdapter.toColonyPos(fixture.base), 1);
            var survey = MaeTerrainPreparation.begin(context.getWorld(), fixture.writer, region, List.of(proposal),
                    fixture.colony.colony().center());
            finishTerrain(context, fixture, region, survey, beforeGraph, false);
        } catch (RuntimeException | AssertionError failure) {
            fixture.cleanup();
            throw failure;
        }
    }

    private void finishTerrain(TestContext context, MaeSafetyGameTest.Fixture fixture, UUID region,
                                      MaeTerrainPreparation.Survey survey,
                                      com.villagecolony.core.construction.model.RoadGraph graph, boolean approved) {
        try {
            if (!survey.complete()) {
                long started = System.nanoTime();
                int reads = survey.runSlice();
                maxSurveyNanos = Math.max(maxSurveyNanos, System.nanoTime() - started);
                totalReads += reads;
                maxReads = Math.max(maxReads, reads);
                context.assertTrue(reads <= MaeTerrainPreparation.MAX_READS_PER_TICK, "levantamento excedeu orçamento de leituras");
            }
            if (survey.complete() && !approved) {
                context.assertTrue("planned_access_to_anchor".equals(survey.result().terrain().reason()), "âncora não conectada");
                context.assertTrue(fixture.writer.approveTerrain(context.getWorld(), survey.result()), "terreno não aprovado");
                approved = true;
            }
            if (approved) writeSlice(context, fixture, region);
            var job = fixture.store.region(region).orElseThrow().getCompound("job");
            context.assertFalse("blocked".equals(job.getString("state")), "terreno bloqueado: " + job.getString("reason"));
            if (!approved || !"complete".equals(job.getString("state"))) {
                boolean next = approved;
                context.waitAndRun(1, () -> finishTerrain(context, fixture, region, survey, graph, next));
                return;
            }
            context.assertTrue(com.villagecolony.data.save.ColonySavedData.get(context.getWorld().getServer())
                    .roadGraphs().graph(fixture.colony.colony().id()).orElseThrow().equals(graph), "terreno alterou grafo");
            for (int x = 0; x < 4; x++) {
                context.assertTrue(context.getWorld().getBlockState(fixture.base.east(x)).isOf(Blocks.DIRT_PATH), "terreno alterou rua");
                context.assertTrue(context.getWorld().getBlockState(fixture.base.south().east(x)).isAir(), "corte não chegou à cota");
            }
            var villager = net.minecraft.entity.EntityType.VILLAGER.create(context.getWorld());
            context.assertTrue(villager != null, "aldeão não criado");
            BlockPos start = fixture.base.south().down();
            villager.refreshPositionAndAngles(start.getX() + 0.5, start.getY() + 1.01, start.getZ() + 0.5, 0, 0);
            context.getWorld().spawnEntity(villager);
            var targets = List.of(start, start.east(), start.east(2), start.east(3), fixture.base.east(3),
                    fixture.base.east(2), fixture.base.east(), fixture.base, start);
            walk(context, fixture, villager, targets, 0, 0);
        } catch (RuntimeException | AssertionError failure) {
            SweepState.ROADS.remove(fixture.colony.colony().id());
            fixture.cleanup();
            throw failure;
        }
    }

    private void walk(TestContext context, MaeSafetyGameTest.Fixture fixture,
                             net.minecraft.entity.passive.VillagerEntity villager,
                             List<BlockPos> targets, int index, int elapsed) {
        try {
            context.assertTrue(elapsed < 240, "aldeão não percorreu todas as superfícies: alvo=" + index);
            BlockPos floor = targets.get(index);
            BlockPos feet = floor.up();
            boolean arrived = villager.isOnGround() && Math.abs(villager.getY() - feet.getY()) < 0.2
                    && Math.hypot(villager.getX() - feet.getX() - 0.5, villager.getZ() - feet.getZ() - 0.5) < 0.35;
            if (arrived && index + 1 == targets.size()) {
                org.slf4j.LoggerFactory.getLogger(MaeRoadEvidenceGameTest.class).info(
                        "VC_MAE_METRICS reads={} maxReadsPerSlice={} surveyMaxNs={} writes={} maxWritesPerSlice={} writerMaxNs={}",
                        totalReads, maxReads, maxSurveyNanos, totalWrites, maxWrites, maxWriterNanos);
                villager.discard();
                SweepState.ROADS.remove(fixture.colony.colony().id());
                context.getWorld().getEntitiesByClass(net.minecraft.entity.ItemEntity.class,
                        new net.minecraft.util.math.Box(fixture.base).expand(8), e -> true).forEach(net.minecraft.entity.Entity::discard);
                fixture.cleanup();
                context.complete();
                return;
            }
            int next = arrived ? index + 1 : index;
            BlockPos target = targets.get(next).up();
            // Só o destino é controlado; navegação e salto continuam Vanilla — gate local MAE Fase 2.
            villager.getBrain().remember(net.minecraft.entity.ai.brain.MemoryModuleType.WALK_TARGET,
                    new net.minecraft.entity.ai.brain.WalkTarget(target, 0.8f, 0));
            villager.getNavigation().startMovingTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.8);
            context.waitAndRun(1, () -> walk(context, fixture, villager, targets, next, elapsed + 1));
        } catch (RuntimeException | AssertionError failure) {
            villager.discard();
            SweepState.ROADS.remove(fixture.colony.colony().id());
            fixture.cleanup();
            throw failure;
        }
    }

    private void writeSlice(TestContext context, MaeSafetyGameTest.Fixture fixture, UUID region) {
        long started = System.nanoTime();
        int writes = fixture.writer.runSlice(context.getWorld(), region);
        maxWriterNanos = Math.max(maxWriterNanos, System.nanoTime() - started);
        totalWrites += writes;
        maxWrites = Math.max(maxWrites, writes);
        context.assertTrue(writes <= 4, "writer excedeu orçamento de escritas");
    }
}
