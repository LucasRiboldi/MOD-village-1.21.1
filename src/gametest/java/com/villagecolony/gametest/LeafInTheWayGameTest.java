package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
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
import net.minecraft.block.LeavesBlock;
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
 * Folha natural no lugar da peça dá lugar a ela — bancada da taiga, 2026-10-20: 29 peças de parede
 * puladas por "spruce_leaves is in the way" deixaram a tocha sem apoio e a casa aberta. A folha posta
 * à mão (persistente) continua guardada pela Regra 3.
 */
public class LeafInTheWayGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    private static final BlockPos STAND = new BlockPos(3, 2, 2);

    private static final BlockPos SITE = new BlockPos(4, 2, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "leaf_in_the_way", tickLimit = 300)
    public void aNaturalLeafGivesWayToTheWall(TestContext context) {
        ColonyFixture owned = setUp(context, false);

        context.runAtTick(90, () -> {
            try {
                context.assertTrue(context.getBlockState(SITE).isOf(Blocks.OAK_PLANKS),
                        "a folha natural segurou o lugar da parede: " + context.getBlockState(SITE));
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }

    /** Controle: a folha persistente é de alguém e fica. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "leaf_in_the_way", tickLimit = 300)
    public void aPlacedLeafStays(TestContext context) {
        ColonyFixture owned = setUp(context, true);

        context.runAtTick(90, () -> {
            try {
                context.assertTrue(context.getBlockState(SITE.east()).isOf(Blocks.OAK_PLANKS),
                        "controle: a outra peça da parede não subiu — o cenário não mede nada");
                context.assertTrue(context.getBlockState(SITE).isOf(Blocks.SPRUCE_LEAVES),
                        "a folha posta à mão foi trocada pela parede: " + context.getBlockState(SITE));
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }

    private static ColonyFixture setUp(TestContext context, boolean persistentLeaf) {
        ServerWorld world = context.getWorld();

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);
        context.setBlockState(SITE.down(), Blocks.STONE.getDefaultState());
        context.setBlockState(SITE.down().east(), Blocks.STONE.getDefaultState());
        // O tronco ao lado segura a folha natural: sem ele ela secaria sozinha e o teste passaria à toa.
        context.setBlockState(SITE.up(), Blocks.SPRUCE_LOG.getDefaultState());
        context.setBlockState(SITE, Blocks.SPRUCE_LEAVES.getDefaultState()
                .with(LeavesBlock.PERSISTENT, persistentLeaf).with(LeavesBlock.DISTANCE, 1));

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ChestDepositor.deposit(world, chest, Items.OAK_PLANKS, 8);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.BUILDER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        ResourceId planks = MinecraftTypeAdapter.toResourceId(Blocks.OAK_PLANKS);
        ConstructionProject project = ConstructionProject.plan(colony.id(),
                Blueprint.of(ResourceId.vanilla("village/plains/houses/test_wall"), List.of(
                        new BlueprintBlock(new ColonyPos(0, 0, 0), planks),
                        new BlueprintBlock(new ColonyPos(1, 0, 0), planks))),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(SITE)));
        VillageColonyMod.CONSTRUCTIONS.register(project);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        Task task = VillageColonyMod.TASKS.create(colony.id(), TaskType.BUILD, TaskPriority.PRODUCTION,
                ResourceType.OAK_PLANKS, 2);
        task.reserveFor(villager.getUuid());

        BuilderWork.run(world, colony);

        return ColonyFixture.create().owning(colony).owning(villager.getUuid());
    }
}
