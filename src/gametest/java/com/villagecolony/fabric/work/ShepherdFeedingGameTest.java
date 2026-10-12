package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O pastor alimenta os animais com qualquer comida que haja nos baús da vila — regra do
 * autor, 2026-10-12. Sementes de abóbora não estavam na lista fixa de antes.
 */
public class ShepherdFeedingGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_feeding", tickLimit = 20)
    public void theShepherdFeedsTheChickensWithAnySeedFromTheChests(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos chestAt = context.getAbsolutePos(new BlockPos(1, 2, 1));
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(chestAt);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID shepherd = UUID.randomUUID();
        ChickenEntity[] hens = new ChickenEntity[2];

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(shepherd, colony.id()).assign(ProfessionType.SHEPHERD);
        owned.owning(shepherd);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(shepherd, chest));
        world.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        ((Inventory) world.getBlockEntity(chestAt)).setStack(0, new ItemStack(Items.PUMPKIN_SEEDS, 8));

        try {
            for (int i = 0; i < hens.length; i++) {
                BlockPos at = context.getAbsolutePos(new BlockPos(3 + i, 2, 3));
                hens[i] = EntityType.CHICKEN.create(world);
                hens[i].refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
                world.spawnEntity(hens[i]);
            }

            ShepherdFlock.clearAll();
            ShepherdFlock.tend(world, colony);

            context.assertTrue(hens[0].isInLove() && hens[1].isInLove(),
                    "as galinhas não comeram as sementes de abóbora do baú");
        } finally {
            for (ChickenEntity hen : hens) {
                if (hen != null) {
                    hen.discard();
                }
            }

            world.setBlockState(chestAt, Blocks.AIR.getDefaultState());
            ShepherdFlock.clearAll();
            owned.cleanUp();
        }

        context.complete();
    }
}
