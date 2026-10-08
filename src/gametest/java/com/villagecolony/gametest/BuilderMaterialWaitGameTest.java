package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.SkipReason;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.work.BuilderWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * A peça sem material espera de lado e a obra segue pelas outras — decisão do
 * autor, 2026-10-08: o celeiro parou 817 blocos esperando um funil.
 */
public class BuilderMaterialWaitGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    private static final BlockPos STAND = new BlockPos(3, 2, 2);

    private static final BlockPos HOPPER_SITE = new BlockPos(4, 2, 2);

    private static final BlockPos STONE_SITE = new BlockPos(4, 2, 3);

    private static final ResourceId WALL = ResourceId.vanilla("village/plains/houses/test_wall");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_material_wait",
            tickLimit = 300)
    public void theBuilderGoesOnPastAPieceWithoutMaterial(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        context.setBlockState(HOPPER_SITE.down(), Blocks.STONE.getDefaultState());
        context.setBlockState(STONE_SITE.down(), Blocks.STONE.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        // Pedra para a segunda peça; nada de ferro para o funil.
        ChestDepositor.deposit(world, chest, Items.COBBLESTONE, 4);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.BUILDER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(HOPPER_SITE));
        Blueprint plan = Blueprint.of(WALL, List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), MinecraftTypeAdapter.toResourceId(Blocks.HOPPER)),
                new BlueprintBlock(new ColonyPos(0, 0, 1), MinecraftTypeAdapter.toResourceId(Blocks.COBBLESTONE))));

        ConstructionProject project = ConstructionProject.plan(colony.id(), plan, origin);
        VillageColonyMod.CONSTRUCTIONS.register(project);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.BUILD, TaskPriority.PRODUCTION, ResourceType.OAK_PLANKS, 1);
        task.reserveFor(villager.getUuid());

        BuilderWork.run(world, colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony).owning(villager.getUuid());

        context.runAtTick(200, () -> {
            try {
                context.assertTrue(context.getBlockState(STONE_SITE).isOf(Blocks.COBBLESTONE),
                        "a obra parou no funil sem material e não pôs a pedra da peça seguinte");
                context.assertTrue(project.deferredPieces().stream()
                                .anyMatch(piece -> piece.reason() == SkipReason.WAITING_MATERIAL),
                        "o funil não ficou de lado esperando material: " + project.deferredPieces());
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }
}
