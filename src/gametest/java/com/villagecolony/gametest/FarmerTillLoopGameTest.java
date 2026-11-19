package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.FarmOwners;
import com.villagecolony.fabric.work.FarmerWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Regra 315: a terra comum entre os canteiros da roça (Regra 289) é alvo de semear até o fim — a conferência do alvo
 * aceitava só canteiro arado, e o fazendeiro largava e pegava o mesmo bloco sem parar (simulação da taiga da 0.4.92, 70%
 * do expediente travado).
 */
public class FarmerTillLoopGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    private static final BlockPos GREEN = new BlockPos(4, 1, 4);

    private static final BlockPos DIRT = new BlockPos(5, 1, 4);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_till_loop", tickLimit = 300)
    public void theCommonGroundBetweenPlotsIsTilledAndSown(TestContext context) {
        for (int x = 3; x <= 6; x++) {
            for (int z = 3; z <= 5; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT.getDefaultState());
            }
        }

        context.setBlockState(GREEN, Blocks.FARMLAND.getDefaultState());
        context.setBlockState(GREEN.up(), Blocks.WHEAT.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, net.minecraft.util.math.Direction.SOUTH));

        if (context.getWorld().getBlockEntity(context.getAbsolutePos(CHEST)) instanceof ChestBlockEntity chest) {
            chest.setStack(0, new ItemStack(Items.WHEAT_SEEDS, 8));
            chest.setStack(1, new ItemStack(Items.BONE_MEAL, 64));
        }

        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST).add(200, 0, 200)));
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), colony.id(),
                ResourceId.vanilla("village/plains/houses/plains_small_farm_1"),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(GREEN)),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(DIRT.up()))));

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, 2, 3));
        villager.setBreedingAge(0);
        owned.owning(villager.getUuid());
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(ProfessionType.FARMER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST))));
        Task task = VillageColonyMod.TASKS.create(colony.id(), TaskType.COLLECT_FOOD, TaskPriority.PRODUCTION,
                ResourceType.WHEAT, 8);
        task.reserveFor(villager.getUuid());

        FarmerWork.shortenSearchTo(4);
        FarmerWork.run(context.getWorld(), colony);

        context.runAtTick(240, () -> {
            try {
                context.assertTrue(context.getBlockState(DIRT).isOf(Blocks.FARMLAND)
                                && context.getBlockState(DIRT.up()).isOf(Blocks.WHEAT),
                        "Regra 315: a terra comum da roça não foi arada e semeada: " + context.getBlockState(DIRT)
                                + " / " + context.getBlockState(DIRT.up()));
            } finally {
                VillageColonyMod.TASKS.remove(task.id());
                VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
                FarmOwners.forget(villager.getUuid());
                FarmerWork.forgetColony(colony.id());
                FarmerWork.clearAll();
                owned.cleanUp();
                villager.discard();
            }

            context.complete();
        });
    }
}
