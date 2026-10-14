package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * A horta da casa surge sem gastar recurso — decisão do autor, 2026-10-14. {@code farmland} não é
 * fabricável ("could not make minecraft:farmland"); antes a obra travava nos últimos blocos da
 * horta e segurava a vila inteira (playtest hquYC03AAc). Agora o canteiro é moldado no local, com o
 * chão garantido embaixo, e a obra segue.
 */
public class GardenSurgesGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);
    private static final BlockPos STAND = new BlockPos(2, 2, 2);
    private static final BlockPos ORIGIN = new BlockPos(4, 2, 4);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "garden_surges", tickLimit = 100)
    public void farmlandSurgesWithoutMaterialAndDoesNotStall(TestContext context) {
        ServerWorld world = context.getWorld();
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        context.setBlockState(STAND.down(), Blocks.STONE.getDefaultState());
        world.setTimeOfDay(net.minecraft.entity.ai.brain.Schedule.WORK_TIME);

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ORIGIN));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        VillageColonyMod.COLONIES.register(colony);
        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.BUILDER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
        // Baú vazio de propósito: o canteiro não pode depender de material.

        ConstructionProject project = ConstructionProject.plan(
                colony.id(),
                Blueprint.of(
                        ResourceId.vanilla("village/plains/houses/garden"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("farmland")))),
                origin);
        VillageColonyMod.CONSTRUCTIONS.register(project);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.BUILD, TaskPriority.CONSTRUCTION, ResourceType.OAK_PLANKS, 1);
        task.reserveFor(villager.getUuid());
        BuilderWork.run(world, colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony).owning(villager.getUuid());

        context.runAtTick(80, () -> {
            try {
                BlockPos at = context.getAbsolutePos(ORIGIN);
                context.assertTrue(world.getBlockState(at).isOf(Blocks.FARMLAND),
                        "o canteiro não surgiu: " + world.getBlockState(at).getBlock());
                context.assertTrue(project.remainingCount() == 0,
                        "a horta não foi dada como construída e a obra continua travada");
                context.assertTrue(task.state() == TaskState.COMPLETED,
                        "o construtor ficou preso na peça da horta");
            } finally {
                context.setBlockState(CHEST, Blocks.AIR.getDefaultState());
                context.setBlockState(STAND.down(), Blocks.AIR.getDefaultState());
                world.setBlockState(context.getAbsolutePos(ORIGIN), Blocks.AIR.getDefaultState());
                world.setBlockState(context.getAbsolutePos(ORIGIN).down(), Blocks.AIR.getDefaultState());
                VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
                owned.cleanUp();
            }

            context.complete();
        });
    }
}
