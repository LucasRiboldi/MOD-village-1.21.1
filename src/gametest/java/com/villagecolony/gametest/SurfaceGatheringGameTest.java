package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.FarthestVillageSector;
import com.villagecolony.fabric.integration.WorkerEquipment;
import com.villagecolony.fabric.work.SurfaceGatheringWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** Prova a coleta de grass_block fora da vila com a ferramenta do fundidor. */
public class SurfaceGatheringGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "surface_gathering", tickLimit = 100)
    public void smelterGathersGrassOutsideTheProtectedVillageRadius(TestContext context) {
        var world = context.getWorld();
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        BlockPos center = context.getAbsolutePos(CHEST);
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(center);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 2, 3));
        villager.setBreedingAge(0);
        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.SMELTER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
        owned.owning(villager.getUuid());
        WorkerEquipment.equip(world, List.of(worker));

        var sector = FarthestVillageSector.farthestLoadedSector(world, center, colony.id());
        BlockPos grass = center.offset(sector, FarthestVillageSector.PROTECTED_RADIUS + 1);
        world.setBlockState(grass.down(), Blocks.DIRT.getDefaultState());
        world.setBlockState(grass, Blocks.GRASS_BLOCK.getDefaultState());
        world.setBlockState(grass.up(), Blocks.AIR.getDefaultState());
        BlockPos stand = grass.offset(sector.rotateYClockwise(), 2);
        world.setBlockState(stand.down(), Blocks.DIRT.getDefaultState());
        villager.refreshPositionAndAngles(
                stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0f, 0.0f);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.COLLECT_SURFACE_RESOURCE,
                TaskPriority.PRODUCTION, ResourceType.GRASS_BLOCK, 1);
        task.reserveFor(villager.getUuid());
        SurfaceGatheringWork.run(world, colony);

        for (int tick = 0; tick < 40 && !world.getBlockState(grass).isAir(); tick++) {
            SurfaceGatheringWork.tick(world);
        }

        try {
            context.assertTrue(
                    world.getBlockState(grass).isAir(),
                    "o fundidor não removeu o grass_block do setor externo escolhido");
            context.assertTrue(
                    villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.IRON_SHOVEL),
                    "o fundidor não estava com a pá de ferro do mod");
            context.assertTrue(
                    ChestInventoryReader.read(world, center).amountOf(ResourceType.GRASS_BLOCK) == 1,
                    "o grass_block não chegou ao baú pessoal do fundidor");
            context.assertTrue(
                    task.state() == TaskState.COMPLETED,
                    "a tarefa não foi concluída após recolher o bloco: " + task.state());
        } finally {
            owned.cleanUp();
        }

        context.complete();
    }
}
