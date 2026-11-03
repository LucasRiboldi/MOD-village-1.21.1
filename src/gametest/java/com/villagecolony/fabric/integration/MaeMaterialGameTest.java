package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.data.save.MaeWorkStore;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.util.List;
import java.util.UUID;

/** Materiais observados e falhas injetadas no diário, sem repetir efeitos incertos — ADR-045. */
public final class MaeMaterialGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_pending_write", tickLimit = 20)
    public void pendingWriteBeforePhysicalChangeRemainsBlockedAfterReload(TestContext context) {
        interrupted(context, false);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_pending_publication", tickLimit = 20)
    public void pendingPublicationAfterPhysicalChangeNeverRepublishesOrDelivers(TestContext context) {
        interrupted(context, true);
    }

    private static void interrupted(TestContext context, boolean changed) {
        var f = new MaeSafetyGameTest.Fixture(context);
        try {
            ground(f);
            UUID id = reserve(f);
            var prepared = MaeRoadPreparation.prepare(context.getWorld(), f.writer, id, List.of(f.base, f.base.east()), f.base);
            f.writer.approveRoad(context.getWorld(), prepared);
            var region = f.store.region(id).orElseThrow();
            var job = region.getCompound("job");
            var record = job.getList("plan", 10).getCompound(0).copy();
            record.putString("materials", "pending");
            record.putString("result", changed ? "applied" : "pending");
            region.getList("history", 10).add(record);
            job.putBoolean(changed ? "pendingPublication" : "pendingWrite", true);
            if (changed) f.set(f.base, MaeWorldWriter.decodeState(record.getCompound("after")));
            region.put("job", job);
            f.store.update(region);
            reload(f);
            var expected = context.getWorld().getBlockState(f.base);
            context.waitAndRun(1, () -> {
                try {
                    context.assertTrue(f.writer.runSlice(context.getWorld(), id) == 0, "interrupção repetiu escrita");
                    var blocked = f.store.region(id).orElseThrow();
                    context.assertTrue("blocked".equals(blocked.getCompound("job").getString("state")), "incerteza não ficou bloqueada");
                    refused(context, () -> f.writer.resume(context.getWorld(), id), "unconfirmed_or_nonresumable_job");
                    context.assertTrue(expected.equals(context.getWorld().getBlockState(f.base)), "retomada alterou mundo incerto");
                    context.assertTrue(blocked.getList("history", 10).size() == 1, "repetiu intenção");
                    context.assertTrue(RoadIndex.roadsOf(f.colony.colony().id()).isEmpty(), "publicou rua incompleta");
                    context.assertTrue(Supplies.sessionCount(f.colony.colony().id(), SupplyException.SHAPED_GROUND) == 0
                            && drops(context, f).isEmpty(), "repetiu material incerto");
                } finally { f.cleanup(); }
                context.complete();
            });
        } catch (RuntimeException | AssertionError failure) { f.cleanup(); throw failure; }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_history_budget", tickLimit = 20)
    public void roadReservesPublicationSlotBeforeAnyWrite(TestContext context) {
        var f = new MaeSafetyGameTest.Fixture(context);
        try {
            ground(f);
            UUID id = reserve(f);
            var prepared = MaeRoadPreparation.prepare(context.getWorld(), f.writer, id, List.of(f.base, f.base.east()), f.base);
            var region = f.store.region(id).orElseThrow();
            var history = new NbtList();
            for (int i = 0; i < 1022; i++) history.add(new NbtCompound());
            region.put("history", history);
            f.store.update(region);
            refused(context, () -> f.writer.approveRoad(context.getWorld(), prepared), "history_budget_exceeded");
            context.assertFalse(f.store.region(id).orElseThrow().contains("job"), "recusa deixou aprovação");
            context.assertTrue(context.getWorld().getBlockState(f.base).isOf(Blocks.DIRT), "recusa escreveu rua");
            history.remove(0);
            region.put("history", history);
            f.store.update(region);
            f.writer.approveRoad(context.getWorld(), prepared);
            context.assertTrue(f.store.region(id).orElseThrow().contains("job"), "teto exato de 1024 foi recusado");
        } finally { f.cleanup(); }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_fill_materials", tickLimit = 200)
    public void fillPreservesNamedChestSpillsWhenPublicChestIsFullAndCountsActualSupply(TestContext context) {
        var f = new MaeSafetyGameTest.Fixture(context);
        context.waitAndRun(20, () -> startFill(context, f));
    }

    private static void startFill(TestContext context, MaeSafetyGameTest.Fixture f) {
        try {
            ground(f);
            BlockPos publicAt = f.base.south(5), namedAt = f.base.south(7);
            f.set(publicAt, Blocks.CHEST.getDefaultState());
            f.set(namedAt, Blocks.CHEST.getDefaultState());
            var publicChest = (ChestBlockEntity) context.getWorld().getBlockEntity(publicAt);
            var namedChest = (ChestBlockEntity) context.getWorld().getBlockEntity(namedAt);
            var namedData = namedChest.createNbt(context.getWorld().getRegistryManager());
            namedData.putString("CustomName", Text.Serialization.toJsonString(Text.literal("Reserva do jogador"),
                    context.getWorld().getRegistryManager()));
            namedChest.read(namedData, context.getWorld().getRegistryManager());
            for (int i = 0; i < publicChest.size(); i++) publicChest.setStack(i, new ItemStack(Items.COBBLESTONE, 64));
            f.colony.colony().measure(new VillageBounds(f.base.getX() - 6, f.base.getY() - 6, f.base.getZ() - 6,
                    f.base.getX() + 10, f.base.getY() + 10, f.base.getZ() + 10));
            for (int x = 0; x < 4; x++) f.set(f.base.add(x, 1, 0), Blocks.DANDELION.getDefaultState());
            UUID id = reserve(f);
            var surfaces = java.util.stream.IntStream.range(0, 4)
                    .mapToObj(x -> MinecraftTypeAdapter.toColonyPos(f.base.add(x, 2, 0))).toList();
            var proposal = new MaeTerrainPreparation.Proposal(f.lot(), surfaces, surfaces.getFirst(), null, 0);
            var survey = MaeTerrainPreparation.begin(context.getWorld(), f.writer, id, List.of(proposal), f.colony.colony().center());
            fill(context, f, id, survey, publicChest, namedChest, false);
        } catch (RuntimeException | AssertionError failure) { cleanup(context, f); throw failure; }
    }

    private static void fill(TestContext context, MaeSafetyGameTest.Fixture f, UUID id,
                             MaeTerrainPreparation.Survey survey, ChestBlockEntity publicChest,
                             ChestBlockEntity namedChest, boolean approved) {
        try {
            if (!survey.complete()) survey.runSlice();
            if (survey.complete() && !approved) {
                context.assertTrue(f.writer.approveTerrain(context.getWorld(), survey.result()), "aterro não aprovado");
                reload(f);
                approved = true;
            }
            if (approved) f.writer.runSlice(context.getWorld(), id);
            var region = f.store.region(id).orElseThrow();
            var job = region.getCompound("job");
            int confirmed = 0;
            for (var element : region.getList("history", 10)) {
                if ("confirmed".equals(((NbtCompound) element).getString("materials"))) confirmed++;
            }
            context.assertTrue(Supplies.sessionCount(f.colony.colony().id(), SupplyException.FOUNDATION_GROUND) == confirmed,
                    "suprimento divergiu do confirmado na execução parcial");
            if (!"complete".equals(job.getString("state"))) {
                context.assertFalse("blocked".equals(job.getString("state")), "aterro bloqueado: " + job.getString("reason"));
                boolean next = approved;
                context.waitAndRun(1, () -> fill(context, f, id, survey, publicChest, namedChest, next));
                return;
            }
            context.assertTrue(confirmed == 8 && job.getInt("cursor") == 8, "aterro perdeu ou duplicou posição");
            context.assertTrue(namedChest.isEmpty(), "material entrou no baú nomeado");
            for (int i = 0; i < publicChest.size(); i++) context.assertTrue(publicChest.getStack(i).isOf(Items.COBBLESTONE)
                    && publicChest.getStack(i).getCount() == 64, "aterro substituiu estoque cheio");
            int flowers = drops(context, f).stream().filter(e -> e.getStack().isOf(Items.DANDELION))
                    .mapToInt(e -> e.getStack().getCount()).sum();
            context.assertTrue(flowers == 4, "loot do aterro não foi conservado: " + flowers);
            context.assertTrue(f.writer.runSlice(context.getWorld(), id) == 0, "aterro concluído repetiu efeitos");
            cleanup(context, f);
            context.complete();
        } catch (RuntimeException | AssertionError failure) { cleanup(context, f); throw failure; }
    }

    private static UUID reserve(MaeSafetyGameTest.Fixture f) {
        f.capture();
        return f.writer.reserveTerrain(f.context.getWorld(), f.colony.colony().id(),
                new WorkArea(MinecraftTypeAdapter.toColonyPos(f.base.add(-3, -6, -3)),
                        MinecraftTypeAdapter.toColonyPos(f.base.add(7, 8, 3))));
    }

    private static void ground(MaeSafetyGameTest.Fixture f) {
        for (BlockPos at : BlockPos.iterate(f.base.add(-2, -4, -2), f.base.add(5, 0, 2))) f.set(at, Blocks.DIRT.getDefaultState());
    }

    private static void reload(MaeSafetyGameTest.Fixture f) {
        var disk = new NbtCompound();
        f.store.write(disk);
        f.store = new MaeWorkStore(() -> { });
        f.store.read(disk);
        f.writer = new MaeWorldWriter(f.store);
    }

    private static List<ItemEntity> drops(TestContext context, MaeSafetyGameTest.Fixture f) {
        return context.getWorld().getEntitiesByClass(ItemEntity.class, new Box(f.base).expand(10), e -> true);
    }

    private static void cleanup(TestContext context, MaeSafetyGameTest.Fixture f) {
        for (BlockPos at : List.of(f.base.south(5), f.base.south(7))) {
            if (context.getWorld().getBlockEntity(at) instanceof ChestBlockEntity chest) chest.clear();
        }
        drops(context, f).forEach(ItemEntity::discard);
        f.cleanup();
    }

    private static void refused(TestContext context, Runnable action, String expected) {
        try { action.run(); }
        catch (IllegalStateException failure) {
            context.assertTrue(expected.equals(failure.getMessage()), "recusa incorreta: " + failure.getMessage());
            return;
        }
        context.assertTrue(false, "operação deveria recusar: " + expected);
    }
}
