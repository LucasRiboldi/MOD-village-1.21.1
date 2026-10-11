package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.gametest.ColonyFixture;
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
import java.util.Optional;
import java.util.UUID;

/**
 * A vidraça que a obra pede vai para a oficina do fundidor, e ele a faz — divisão por
 * material, regra do autor de 2026-10-11 ({@code CraftFamily}).
 */
public class CraftFamilyGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_stock", tickLimit = 300)
    public void theSmelterMakesThePaneTheBuildAsksFor(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos chestAt = new BlockPos(2, 2, 2);
        context.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chestAt));
        ChestDepositor.deposit(world, chest, Items.GLASS, 6);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 2, 2));
        villager.setBreedingAge(0);

        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(ProfessionType.SMELTER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
        ColonyFixture fixture = ColonyFixture.create().owning(colony).owning(villager.getUuid());

        ConstructionProject project = ConstructionProject.plan(colony.id(),
                Blueprint.of(ResourceId.vanilla("test_window"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("glass_pane")))),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(6, 2, 6))));
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        VillageColonyMod.CONSTRUCTIONS.register(project);

        CraftsmanRequest.askBeforeTheWorkWaits(world, project);

        Optional<Task> asked = VillageColonyMod.TASKS.ofColony(colony.id()).stream()
                .filter(task -> task.type() == TaskType.CRAFT_METAL_MATERIAL)
                .findFirst();

        context.assertTrue(asked.isPresent(), "a vidraça devia ser pedida à oficina do fundidor: "
                + VillageColonyMod.TASKS.ofColony(colony.id()).stream().map(Task::type).toList());

        asked.get().reserveFor(villager.getUuid());
        CraftingWork.run(world, colony);

        context.runAtTick(280, () -> {
            try {
                int panes = ColonyChests.countIn(world, List.of(chest), Items.GLASS_PANE);

                context.assertTrue(panes >= 1, "o fundidor não fez a vidraça: " + panes);
            } finally {
                VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
                VillageColonyMod.TASKS.ofColony(colony.id()).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
                fixture.cleanUp();
            }

            context.complete();
        });
    }
}
