package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.ShepherdFlock;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** O rebanho cresce além de 8 só com comida sobrando — Regra 238. Cada caso em lote próprio: o relógio do pastor é global. */
public class FlockSurplusGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 1, 2);

    private static final BlockPos FENCE = new BlockPos(4, 1, 6);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_surplus_fed")
    public void withFoodToSpareTheFlockGrowsPastEight(TestContext context) {
        context.assertTrue(breedsAtEight(context, ShepherdFlock.SURPLUS_FOOD), "Regra 238: com 32 trigos o rebanho de 8 não procriou");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_surplus_short")
    public void withoutFoodToSpareTheFlockStopsAtEight(TestContext context) {
        context.assertFalse(breedsAtEight(context, ShepherdFlock.SURPLUS_FOOD - 1), "Regra 238: com 31 trigos o rebanho de 8 procriou");
        context.complete();
    }

    /** Oito ovelhas amarradas perto de um pastor, e {@code wheat} trigos no baú: o pastor põe um par no cio? */
    private static boolean breedsAtEight(TestContext context, int wheat) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        context.setBlockState(FENCE, Blocks.OAK_FENCE.getDefaultState());
        ((ChestBlockEntity) context.getBlockEntity(CHEST)).setStack(0, new ItemStack(Items.WHEAT, wheat));

        ColonyPos center = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        Colony colony = Colony.create(UUID.randomUUID(), center);
        VillageColonyMod.COLONIES.register(colony);

        VillagerEntity shepherd = context.spawnEntity(EntityType.VILLAGER, new BlockPos(4, 1, 4));
        VillageColonyMod.WORKERS.register(shepherd.getUuid(), colony.id()).assign(ProfessionType.SHEPHERD);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(shepherd.getUuid(), center));
        ColonyFixture fixture = ColonyFixture.create().owning(colony).owning(shepherd.getUuid());
        List<SheepEntity> flock = new ArrayList<>();

        for (int i = 0; i < ShepherdFlock.FLOCK_TARGET; i++) {
            SheepEntity sheep = context.spawnEntity(EntityType.SHEEP, new BlockPos(3 + i % 4, 1, 3 + i / 4));
            sheep.attachLeash(LeashKnotEntity.getOrCreate(context.getWorld(), context.getAbsolutePos(FENCE)), true);
            flock.add(sheep);
        }

        ShepherdFlock.clearAll();

        try {
            ShepherdFlock.tend(context.getWorld(), colony);

            return flock.stream().anyMatch(SheepEntity::isInLove);
        } finally {
            flock.forEach(SheepEntity::discard);
            shepherd.discard();
            fixture.cleanUp();
            ShepherdFlock.clearAll();
        }
    }
}
