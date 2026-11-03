package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.data.save.MaeWorkStore;
import com.villagecolony.gametest.ColonyFixture;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.BuriedTreasureGenerator;
import net.minecraft.structure.StructurePiecesList;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A prova nasce na geração, nunca na carga de terreno legado — ADR-044. */
public class MaeSafetyGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_safety", tickLimit = 20)
    public void onlyGenerationSuppliesEvidence(TestContext context) {
        var world = context.getWorld();
        var chunk = world.getWorldChunk(context.getAbsolutePos(new BlockPos(1, 1, 1)));
        var before = chunk.getAttached(MaeGenerationEvidence.RECEIPT);
        try {
            chunk.removeAttached(MaeGenerationEvidence.RECEIPT);
            ServerChunkEvents.CHUNK_LOAD.invoker().onChunkLoad(world, chunk);
            context.assertTrue(chunk.getAttached(MaeGenerationEvidence.RECEIPT) == null,
                    "carregar chunk legado criou prova");
            ServerChunkEvents.CHUNK_GENERATE.invoker().onChunkGenerate(world, chunk);
            context.assertTrue(chunk.getAttached(MaeGenerationEvidence.RECEIPT) != null,
                    "geração não registrou evidência persistente");
        } finally {
            chunk.setAttached(MaeGenerationEvidence.RECEIPT, before);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_missing", tickLimit = 20)
    public void legacyChangedAndUnknownTerrainNeverGrantsPermission(TestContext context) {
        Fixture fixture = new Fixture(context);
        try {
            fixture.chunk.removeAttached(MaeGenerationEvidence.RECEIPT);
            refused(context, () -> fixture.reserve(), "missing_generation_evidence");
            fixture.capture();
            fixture.set(fixture.base, Blocks.STONE.getDefaultState());
            refused(context, () -> fixture.reserve(), "changed_generation_evidence");
            BlockPos unknown = new BlockPos(29_000_000, fixture.base.getY(), 29_000_000);
            context.assertFalse(context.getWorld().isChunkLoaded(unknown), "fixture exige chunk ausente");
            WorkArea area = new WorkArea(pos(unknown), pos(unknown.add(3, 3, 3)));
            refused(context, () -> fixture.writer.reserve(context.getWorld(), fixture.colony.colony().id(), area),
                    "unknown_chunk");
            context.assertFalse(context.getWorld().isChunkLoaded(unknown), "reserva carregou chunk");
        } finally {
            fixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_protected", tickLimit = 20)
    public void originalVillageFluidsBlockEntitiesAndExistingBuildingsStayUntouched(TestContext context) {
        Fixture fixture = new Fixture(context);
        var world = context.getWorld();
        var starts = new HashMap<>(fixture.chunk.getStructureStarts());
        try {
            for (var block : List.of(Blocks.CHEST, Blocks.WATER, Blocks.FARMLAND, Blocks.OAK_LOG)) {
                fixture.set(fixture.base, block.getDefaultState());
                fixture.capture();
                refused(context, () -> fixture.reserve(), "protected_volume");
            }
            fixture.set(fixture.base, Blocks.AIR.getDefaultState());
            var village = world.getRegistryManager().get(RegistryKeys.STRUCTURE)
                    .get(Identifier.ofVanilla("village_plains"));
            var start = new StructureStart(village, fixture.chunk.getPos(), 0,
                    new StructurePiecesList(List.of(new BuriedTreasureGenerator.Piece(fixture.base))));
            fixture.chunk.setStructureStart(village, start);
            fixture.capture();
            refused(context, () -> fixture.reserve(), "original_structure");
            context.assertFalse(VillageOrigin.isCurrent(start), "teste alterou origem da vila legada");
            fixture.chunk.setStructureStarts(starts);
            com.villagecolony.VillageColonyMod.BUILDINGS.register(fixture.lot());
            refused(context, () -> fixture.reserve(), "protected_volume");
        } finally {
            fixture.chunk.setStructureStarts(starts);
            fixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_permit", tickLimit = 20)
    public void reservationDoesNotApproveWorkAndOutOfLotChangesAreRejected(TestContext context) {
        Fixture fixture = new Fixture(context);
        try {
            fixture.capture();
            UUID region = fixture.reserve();
            context.assertTrue(fixture.writer.runSlice(context.getWorld(), region) == 0, "reserva executou sem aprovação");
            context.assertTrue(context.getWorld().getBlockState(fixture.base).isAir(), "reserva modificou terreno");
            refused(context, () -> fixture.reserve(), "overlapping_region");
            boolean rejected = false;
            try {
                fixture.writer.approve(context.getWorld(), region, fixture.lot(),
                        List.of(fixture.change(fixture.base.add(4, 0, 0))));
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            context.assertTrue(rejected, "posição fora do lote foi aprovada");
            fixture.writer.approve(context.getWorld(), region, fixture.lot(), List.of(fixture.change(fixture.base)));
            fixture.writer.pause(context.getWorld(), region);
            context.assertTrue(fixture.writer.runSlice(context.getWorld(), region) == 0, "pausa escreveu bloco");
            fixture.set(fixture.base.add(1, 0, 0), Blocks.STONE.getDefaultState());
            refused(context, () -> fixture.writer.resume(context.getWorld(), region), "world_changed_since_approval");
            context.assertTrue(context.getWorld().getBlockState(fixture.base).isAir(), "retomada reescreveu mundo divergente");
        } finally {
            fixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_reference", tickLimit = 20)
    public void remoteStructureReferenceNeverLoadsItsChunk(TestContext context) {
        Fixture fixture = new Fixture(context);
        var references = new HashMap<>(fixture.chunk.getStructureReferences());
        try {
            fixture.capture();
            var far = new ChunkPos(1_000_000, 1_000_000);
            var village = context.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE)
                    .get(Identifier.ofVanilla("village_plains"));
            var changed = new HashMap<>(references);
            changed.put(village, new LongOpenHashSet(new long[]{far.toLong()}));
            fixture.chunk.setStructureReferences(changed);
            refused(context, () -> fixture.reserve(), "unknown_structure_reference");
            context.assertTrue(context.getWorld().getChunkManager().getWorldChunk(far.x, far.z) == null,
                    "inspeção carregou origem estrutural remota");
        } finally {
            fixture.chunk.setStructureReferences(references);
            fixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_write", tickLimit = 40)
    public void approvedWritesRespectBudgetPauseAndPersistedProgress(TestContext context) {
        Fixture fixture = new Fixture(context);
        try {
            fixture.capture();
            UUID region = fixture.reserve();
            List<MaeWorldWriter.Change> changes = new ArrayList<>();
            for (int x = 0; x < 4; x++) {
                changes.add(fixture.change(fixture.base.add(x, 0, 0)));
            }
            fixture.writer.approve(context.getWorld(), region, fixture.lot(), changes);
            int writes = fixture.writer.runSlice(context.getWorld(), region);
            context.assertTrue(writes > 0 && writes <= MaeWorldWriter.MAX_WRITES_PER_TICK,
                    "obra válida não progrediu ou excedeu orçamento");
            NbtCompound root = new NbtCompound();
            fixture.store.write(root);
            var reloaded = new MaeWorkStore(() -> { });
            reloaded.read(root);
            fixture.writer = new MaeWorldWriter(reloaded);
            fixture.store = reloaded;
            int more = fixture.writer.runSlice(context.getWorld(), region);
            context.assertTrue(writes + more <= MaeWorldWriter.MAX_WRITES_PER_TICK,
                    "nova instância reiniciou orçamento no mesmo tick");
            fixture.writer.pause(context.getWorld(), region);
            context.waitAndRun(2, () -> finish(context, fixture, region));
        } catch (RuntimeException | AssertionError failure) {
            fixture.cleanup();
            throw failure;
        }
    }

    private static void finish(TestContext context, Fixture fixture, UUID region) {
        try {
            if (!"complete".equals(fixture.store.region(region).orElseThrow().getCompound("job").getString("state"))) {
                String state = fixture.store.region(region).orElseThrow().getCompound("job").getString("state");
                if ("paused".equals(state)) {
                    fixture.writer.resume(context.getWorld(), region);
                }
                fixture.writer.runSlice(context.getWorld(), region);
                context.waitAndRun(1, () -> finish(context, fixture, region));
                return;
            }
            var value = fixture.store.region(region).orElseThrow();
            context.assertTrue(value.getCompound("job").getInt("cursor") == 4, "progresso não persistiu");
            context.assertTrue(value.getList("history", 10).size() == 4, "histórico perdeu ou repetiu escrita");
            for (int x = 0; x < 4; x++) {
                context.assertTrue(context.getWorld().getBlockState(fixture.base.add(x, 0, 0)).isOf(Blocks.STONE),
                        "bloco aprovado não foi aplicado");
            }
            fixture.cleanup();
            context.complete();
        } catch (RuntimeException | AssertionError failure) {
            fixture.cleanup();
            throw failure;
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_boundary", tickLimit = 20)
    public void writerRevalidatesBoundsAndNewProtectionAtTheLastMoment(TestContext context) {
        Fixture fixture = new Fixture(context);
        try {
            fixture.capture();
            UUID region = fixture.reserve();
            fixture.writer.approve(context.getWorld(), region, fixture.lot(), List.of(fixture.change(fixture.base)));
            NbtCompound tampered = fixture.store.region(region).orElseThrow();
            tampered.getCompound("job").getList("plan", 10).getCompound(0)
                    .putLong("position", fixture.base.add(4, 0, 0).asLong());
            fixture.store.update(tampered);
            context.assertTrue(fixture.writer.runSlice(context.getWorld(), region) == 0,
                    "plano alterado escreveu fora do lote");
            context.assertTrue("blocked".equals(fixture.store.region(region).orElseThrow()
                    .getCompound("job").getString("state")), "escrita proibida não bloqueou obra");
            context.assertTrue(context.getWorld().getBlockState(fixture.base.add(4, 0, 0)).isAir(),
                    "escrita fora do lote alterou mundo");
        } finally {
            fixture.cleanup();
        }
        // A primeira recusa também consome o orçamento global do tick — ADR-044.
        context.waitAndRun(1, () -> recheckNewProtection(context));
    }

    private static void recheckNewProtection(TestContext context) {
        Fixture protectedFixture = new Fixture(context);
        try {
            protectedFixture.capture();
            UUID region = protectedFixture.reserve();
            protectedFixture.writer.approve(context.getWorld(), region, protectedFixture.lot(),
                    List.of(protectedFixture.change(protectedFixture.base)));
            com.villagecolony.VillageColonyMod.BUILDINGS.register(protectedFixture.lot());
            context.assertTrue(protectedFixture.writer.runSlice(context.getWorld(), region) == 0,
                    "proteção criada após aprovação foi ignorada");
            context.assertTrue("protected_volume".equals(protectedFixture.store.region(region).orElseThrow()
                    .getCompound("job").getString("reason")), "não revalidou proteção no limite da escrita");
        } finally {
            protectedFixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_pending", tickLimit = 20)
    public void unknownEvidenceAndUnconfirmedWriteCannotResume(TestContext context) {
        Fixture fixture = new Fixture(context);
        try {
            fixture.capture();
            var future = fixture.chunk.getAttached(MaeGenerationEvidence.RECEIPT).copy();
            future.putInt("version", 99);
            fixture.chunk.setAttached(MaeGenerationEvidence.RECEIPT, future);
            ServerChunkEvents.CHUNK_GENERATE.invoker().onChunkGenerate(context.getWorld(), fixture.chunk);
            context.assertTrue(future.equals(fixture.chunk.getAttached(MaeGenerationEvidence.RECEIPT)),
                    "evidência futura foi sobrescrita");
            refused(context, () -> fixture.reserve(), "missing_generation_evidence");
            fixture.capture();
            UUID region = fixture.reserve();
            fixture.writer.approve(context.getWorld(), region, fixture.lot(), List.of(fixture.change(fixture.base)));
            var persisted = fixture.store.region(region).orElseThrow();
            persisted.getCompound("job").putBoolean("pendingWrite", true);
            persisted.getCompound("job").putString("state", "blocked");
            fixture.store.update(persisted);
            refused(context, () -> fixture.writer.resume(context.getWorld(), region), "unconfirmed_or_nonresumable_job");
            context.assertTrue(context.getWorld().getBlockState(fixture.base).isAir(), "retomada alterou terreno");
            var nether = context.getWorld().getServer().getWorld(net.minecraft.world.World.NETHER);
            refused(context, () -> fixture.writer.reserve(nether, fixture.colony.colony().id(),
                    new WorkArea(pos(fixture.base), pos(fixture.base.add(3, 0, 0)))), "wrong_dimension");
        } finally {
            fixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_origin", tickLimit = 20)
    public void newVillageMarkerDoesNotProveExpansionUntouched(TestContext context) {
        Fixture fixture = new Fixture(context);
        var starts = new HashMap<>(fixture.chunk.getStructureStarts());
        try {
            var world = context.getWorld();
            var village = world.getRegistryManager().get(RegistryKeys.STRUCTURE)
                    .get(Identifier.ofVanilla("village_plains"));
            var start = new StructureStart(village, fixture.chunk.getPos(), 0,
                    new StructurePiecesList(List.of(new BuriedTreasureGenerator.Piece(fixture.base.add(30, 0, 0)))));
            VillageOrigin.markCreated(start, world.getRegistryManager());
            fixture.chunk.setStructureStart(village, start);
            context.assertTrue(VillageOrigin.isCurrent(start), "fixture não marcou vila nova");
            fixture.chunk.removeAttached(MaeGenerationEvidence.RECEIPT);
            refused(context, () -> fixture.reserve(), "missing_generation_evidence");
        } finally {
            fixture.chunk.setStructureStarts(starts);
            fixture.cleanup();
        }
        context.complete();
    }

    private static void refused(TestContext context, Runnable action, String reason) {
        try {
            action.run();
        } catch (IllegalStateException expected) {
            context.assertTrue(reason.equals(expected.getMessage()), "recusa errada: " + expected.getMessage());
            return;
        }
        context.assertTrue(false, "operação insegura aceita: " + reason);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_mine", tickLimit = 20)
    public void plannedMineCellsRemainProtectedEvenBeforeExcavation(TestContext context) {
        Fixture fixture = new Fixture(context);
        try {
            var shaft = com.villagecolony.core.construction.model.MineShaft.from(pos(fixture.base),
                    com.villagecolony.core.type.Side.EAST);
            var mine = com.villagecolony.core.construction.model.Mine.restore(fixture.colony.colony().id(), shaft, 0);
            WorkArea inspected = new WorkArea(pos(fixture.base.add(-3, -3, -3)), pos(fixture.base.add(7, 3, 3)));
            context.assertTrue(inspected.contains(shaft.positionAt(0)), "fixture exige célula planejada dentro do lote");
            com.villagecolony.VillageColonyMod.MINES.restore(mine);
            fixture.capture();
            refused(context, () -> fixture.reserve(), "protected_volume");
            context.assertTrue(context.getWorld().getBlockState(fixture.base).isAir(), "reserva escavou a mina");
        } finally {
            com.villagecolony.VillageColonyMod.MINES.removeOfColony(fixture.colony.colony().id());
            fixture.cleanup();
        }
        context.complete();
    }

    private static ColonyPos pos(BlockPos pos) {
        return new ColonyPos(pos.getX(), pos.getY(), pos.getZ());
    }

    private static final class Fixture {
        final TestContext context;
        final ColonyFixture colony;
        final WorldChunk chunk;
        final BlockPos base;
        final NbtCompound originalReceipt;
        final Map<BlockPos, BlockState> changed = new HashMap<>();
        MaeWorkStore store = new MaeWorkStore(() -> { });
        MaeWorldWriter writer = new MaeWorldWriter(store);

        Fixture(TestContext context) {
            this.context = context;
            // A cobertura de barreiras do GameTest não pertence ao terreno elegível — ADR-044.
            BlockPos origin = context.getAbsolutePos(new BlockPos(1, 40, 1));
            base = new BlockPos((origin.getX() & ~15) + 5, origin.getY(), (origin.getZ() & ~15) + 5);
            chunk = context.getWorld().getWorldChunk(base);
            originalReceipt = chunk.getAttached(MaeGenerationEvidence.RECEIPT);
            colony = ColonyFixture.colonyAt(context, base.subtract(context.getAbsolutePos(BlockPos.ORIGIN)));
            for (BlockPos pos : BlockPos.iterate(base.add(-3, -3, -3), base.add(7, 3, 3))) {
                BlockState state = context.getWorld().getBlockState(pos);
                context.assertTrue(MaeProtection.terrain(state) && BlockProtection.mayBreak(context.getWorld(), pos, state),
                        "fixture MAE contaminada em " + pos.toShortString() + ": " + state);
            }
        }

        Building lot() {
            return new Building(UUID.randomUUID(), colony.colony().id(), ResourceId.vanilla("village/plains/houses/plains_small_house_1"),
                    pos(base), pos(base.add(3, 0, 0)), false);
        }

        UUID reserve() {
            return writer.reserve(context.getWorld(), colony.colony().id(),
                    new WorkArea(pos(base.add(-3, -3, -3)), pos(base.add(7, 3, 3))));
        }

        MaeWorldWriter.Change change(BlockPos pos) {
            changed.putIfAbsent(pos, context.getWorld().getBlockState(pos));
            return new MaeWorldWriter.Change(pos, context.getWorld().getBlockState(pos), Blocks.STONE.getDefaultState());
        }

        void set(BlockPos pos, BlockState state) {
            changed.putIfAbsent(pos, context.getWorld().getBlockState(pos));
            context.getWorld().setBlockState(pos, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        }

        void capture() {
            chunk.removeAttached(MaeGenerationEvidence.RECEIPT);
            ServerChunkEvents.CHUNK_GENERATE.invoker().onChunkGenerate(context.getWorld(), chunk);
        }

        void cleanup() {
            changed.forEach((pos, state) -> context.getWorld().setBlockState(pos, state,
                    Block.NOTIFY_LISTENERS | Block.FORCE_STATE));
            chunk.setAttached(MaeGenerationEvidence.RECEIPT, originalReceipt);
            colony.cleanUp();
        }
    }
}
