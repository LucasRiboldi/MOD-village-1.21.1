package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.gametest.ColonyFixture;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** O animal solto também come para procriar; a coleta o traz depois (ADR-039 E2). */
public class ShepherdLooseFlockGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_hours", tickLimit = 20)
    public void twoLooseSheepAreFedToBreed(TestContext context) {
        BlockPos chestAt = new BlockPos(1, 1, 1);
        context.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        ((ChestBlockEntity) context.getBlockEntity(chestAt)).setStack(0, new ItemStack(Items.WHEAT, 4));

        ColonyPos center = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chestAt));
        Colony colony = Colony.create(UUID.randomUUID(), center);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture fixture = ColonyFixture.create().owning(colony);
        UUID shepherd = UUID.randomUUID();

        VillageColonyMod.WORKERS.register(shepherd, colony.id()).assign(ProfessionType.SHEPHERD);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(shepherd, center));
        fixture.owning(shepherd);
        ShepherdFlock.clearAll();

        SheepEntity first = context.spawnEntity(EntityType.SHEEP, new BlockPos(4, 1, 4));
        SheepEntity second = context.spawnEntity(EntityType.SHEEP, new BlockPos(5, 1, 4));

        try {
            context.assertTrue(!ShepherdFlock.isKept(context.getWorld(), first)
                            && !ShepherdFlock.isKept(context.getWorld(), second),
                    "o cenário guardou as ovelhas: o teste não mede o animal solto");
            context.assertTrue(ShepherdFlock.tend(context.getWorld(), colony), "o pastor não alimentou as soltas");
            context.assertTrue(first.isInLove() && second.isInLove(), "uma ovelha solta não entrou no cio");
            context.assertTrue(ColonyChests.countIn(context.getWorld(), List.of(center), Items.WHEAT) == 2,
                    "o par devia comer 2 trigos do baú");
        } finally {
            fixture.cleanUp();
            ShepherdFlock.clearAll();
        }

        context.complete();
    }
}
