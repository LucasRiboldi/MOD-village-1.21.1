package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;

/**
 * Quem está à toa recolhe do chão o que a obra espera, e só isso — B-1 e Regra
 * 48, decisões do autor de 2026-10-02.
 */
public final class GroundPickupGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    private static final int TICKS = 600;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "ground_pickup", tickLimit = TICKS)
    public void workerCollectsWhileWalkingPast(TestContext context) {
        context.getWorld().setTimeOfDay(2_000);
        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 8; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
                context.setBlockState(new BlockPos(x, 2, z), Blocks.AIR.getDefaultState());
                context.setBlockState(new BlockPos(x, 3, z), Blocks.AIR.getDefaultState());
            }
        }
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST)));
        BlockPos a = context.getAbsolutePos(new BlockPos(0, 0, 0));
        BlockPos b = context.getAbsolutePos(new BlockPos(14, 6, 8));
        colony.measure(new com.villagecolony.core.colony.model.VillageBounds(
                Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
        long now = context.getWorld().getTime();
        for (long tick = now - Colony.SETTLE_TICKS; tick <= now; tick += 20) colony.attend(tick);
        VillageColonyMod.COLONIES.register(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 2, 5));
        villager.setBreedingAge(0);
        villager.setAiDisabled(true);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        Vec3d itemPos = Vec3d.ofBottomCenter(context.getAbsolutePos(new BlockPos(11, 2, 5)));
        ItemEntity item = new ItemEntity(context.getWorld(), itemPos.x, itemPos.y, itemPos.z, new ItemStack(Items.DIRT, 5));
        item.setPickupDelayInfinite();
        context.getWorld().spawnEntity(item);
        boolean[] done = {false};
        Runnable cleanup = () -> {
            VillageColonyMod.WORKERS.remove(villager.getUuid());
            VillageColonyMod.COLONIES.remove(colony.id());
            villager.discard();
            item.discard();
        };

        context.runAtTick(40, () -> {
            context.assertTrue(item.isAlive(), "item fora de alcance não deve ser recolhido");
            villager.setAiDisabled(false);
        });
        context.runAtEveryTick(() -> {
            if (done[0]) return;
            if (context.getWorld().getTime() >= now + 40 && item.isAlive()) {
                villager.getNavigation().startMovingTo(itemPos.x, itemPos.y, itemPos.z, 0.7);
            }
            if (!item.isAlive()) {
                done[0] = true;
                ChestBlockEntity box = (ChestBlockEntity) context.getBlockEntity(CHEST);
                int dirt = 0;
                for (int slot = 0; slot < box.size(); slot++) {
                    if (box.getStack(slot).isOf(Items.DIRT)) dirt += box.getStack(slot).getCount();
                }
                double moved = villager.squaredDistanceTo(Vec3d.ofBottomCenter(context.getAbsolutePos(new BlockPos(2, 2, 5))));
                cleanup.run();
                context.assertTrue(moved > 1, "aldeão deve caminhar antes da coleta");
                context.assertTrue(dirt == 5, "a terra recolhida deve chegar ao baú: " + dirt);
                context.complete();
            }
        });
        context.runAtTick(TICKS - 5, () -> {
            if (!done[0]) {
                cleanup.run();
                context.assertTrue(false, "aldeão não recolheu a terra durante o deslocamento");
            }
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "ground_pickup", tickLimit = TICKS)
    public void aPassingWorkerPicksUpAllGroundItems(TestContext context) {
        context.getWorld().setTimeOfDay(2_000);

        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 12; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
                context.setBlockState(new BlockPos(x, 2, z), Blocks.AIR.getDefaultState());
                context.setBlockState(new BlockPos(x, 3, z), Blocks.AIR.getDefaultState());
            }
        }

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        Colony colony = Colony.create(UUID.randomUUID(), chest);

        // A caixa da vila medida, como numa vila de verdade: com ela todo baú
        // dentro dela é da colônia (Regra 45), também o de fora de casa.
        BlockPos a = context.getAbsolutePos(new BlockPos(0, 0, 0));
        BlockPos b = context.getAbsolutePos(new BlockPos(12, 6, 12));
        colony.measure(new com.villagecolony.core.colony.model.VillageBounds(
                Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));

        // runGametest simula observador; marque o minuto de presença que o jogo
        // exige antes de iniciar trabalho (ADR-036 item 11).
        long now = context.getWorld().getTime();
        for (long tick = now - Colony.SETTLE_TICKS; tick <= now; tick += 20) {
            colony.attend(tick);
        }

        VillageColonyMod.COLONIES.register(colony);

        // Uma obra que ainda pede pedregulho.
        Blueprint plan = Blueprint.of(ResourceId.vanilla("test_ground_pickup"), List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("cobblestone"))));
        ConstructionProject project = ConstructionProject.plan(colony.id(), plan,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(10, 2, 10))));

        VillageColonyMod.CONSTRUCTIONS.register(project);

        // Regra 48: os dois itens devem estar ao alcance, sem depender do passeio Vanilla.
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(4, 2, 5));
        villager.setBreedingAge(0);
        villager.setAiDisabled(true);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());

        Vec3d wantedAt = Vec3d.ofBottomCenter(context.getAbsolutePos(new BlockPos(7, 2, 5)));
        Vec3d otherAt = Vec3d.ofBottomCenter(context.getAbsolutePos(new BlockPos(7, 2, 6)));
        ItemEntity wanted = new ItemEntity(context.getWorld(), wantedAt.x, wantedAt.y, wantedAt.z,
                new ItemStack(Items.COBBLESTONE, 5));
        ItemEntity other = new ItemEntity(context.getWorld(), otherAt.x, otherAt.y, otherAt.z,
                new ItemStack(Items.DIRT, 5));

        wanted.setPickupDelayInfinite();
        other.setPickupDelayInfinite();
        context.getWorld().spawnEntity(wanted);
        context.getWorld().spawnEntity(other);

        boolean[] done = {false};
        boolean[] removed = {false, false};
        recordPickupState(context, colony, villager, wanted, other, "initial");

        Runnable cleanup = () -> {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.WORKERS.remove(villager.getUuid());
            VillageColonyMod.COLONIES.remove(colony.id());
            villager.discard();
            other.discard();

            if (wanted.isAlive()) {
                wanted.discard();
            }
        };

        context.runAtEveryTick(() -> {
            if (done[0]) return;
            if ((!removed[0] && !wanted.isAlive()) || (!removed[1] && !other.isAlive())) {
                recordPickupState(context, colony, villager, wanted, other, "item_removed");
                removed[0] = !wanted.isAlive();
                removed[1] = !other.isAlive();
            }
            if (done[0] || wanted.isAlive() || other.isAlive()) {
                return;
            }

            done[0] = true;

            ChestBlockEntity box = (ChestBlockEntity) context.getBlockEntity(CHEST);
            int stored = 0;

            for (int slot = 0; slot < box.size(); slot++) {
                if (box.getStack(slot).isOf(Items.COBBLESTONE)) {
                    stored += box.getStack(slot).getCount();
                }
            }

            int dirt = 0;
            for (int slot = 0; slot < box.size(); slot++) {
                if (box.getStack(slot).isOf(Items.DIRT)) {
                    dirt += box.getStack(slot).getCount();
                }
            }

            if (stored != 5 || dirt != 5) {
                recordPickupState(context, colony, villager, wanted, other, "assertion_failed");
            }
            cleanup.run();

            context.assertTrue(stored == 5, "o pedregulho recolhido devia estar no baú: " + stored);
            context.assertTrue(dirt == 5, "a terra também deveria ser recolhida: " + dirt);
            context.complete();
        });

        context.runAtTick(TICKS - 5, () -> {
            if (done[0]) {
                return;
            }

            String where = villager.getBlockPos().toShortString();
            boolean helping = GroundPickup.isHelping(villager.getUuid());

            recordPickupState(context, colony, villager, wanted, other, "timeout");
            cleanup.run();
            context.assertTrue(false, "o pedregulho que a obra espera ficou no chão; o aldeão está em " + where
                    + ", ajudando=" + helping);
        });
    }

    /** Regra 48: o diagnóstico mostra todos os destinos públicos sem relaxar as asserções. */
    private static void recordPickupState(TestContext context, Colony colony, VillagerEntity villager,
            ItemEntity wanted, ItemEntity other, String event) {
        var chests = new java.util.ArrayList<>(ColonyChests.nearestFirst(context.getWorld(), colony.id(),
                MinecraftTypeAdapter.toColonyPos(villager.getBlockPos())));
        ColonyPos original = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        if (!chests.contains(original)) chests.add(original);
        StringBuilder stocks = new StringBuilder();
        for (ColonyPos chest : chests) {
            var entity = context.getWorld().getBlockEntity(MinecraftTypeAdapter.toBlockPos(chest));
            stocks.append(chest).append('=');
            if (entity instanceof ChestBlockEntity box) {
                for (int slot = 0; slot < box.size(); slot++) {
                    if (!box.getStack(slot).isEmpty()) stocks.append(slot).append(':').append(box.getStack(slot)).append(';');
                }
            } else {
                stocks.append("not_chest");
            }
            stocks.append('|');
        }
        VillageColonyMod.LOGGER.info(
                "PICKUP_DIAG event={} tick={} colony={} bounds={} worker={} workerPos={} wanted={} wantedPos={} wantedRemoval={} other={} otherPos={} otherRemoval={} stocks={}",
                event, context.getWorld().getTime(), colony.id(), colony.bounds(), villager.getUuid(),
                villager.getPos(), wanted.getUuid(), wanted.getPos(), wanted.getRemovalReason(),
                other.getUuid(), other.getPos(), other.getRemovalReason(), stocks);
    }
}
