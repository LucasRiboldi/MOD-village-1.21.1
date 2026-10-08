package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pedido do autor de 2026-10-08: o pastor sem lã a tosquiar ajuda o construtor a
 * pôr blocos; põe pelo menos dez (ou a obra acaba) e, com pedido de lã na fila,
 * volta ao rebanho.
 */
public class BuildHelperGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_helper")
    public void anIdleShepherdHelpsTheBuilderAndLeavesForTheWool(TestContext context) {
        ColonyPos centre = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 2, 2)));
        Colony colony = Colony.create(UUID.randomUUID(), centre);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 2, 3));
        UUID shepherd = villager.getUuid();

        VillageColonyMod.WORKERS.register(shepherd, colony.id()).assign(ProfessionType.SHEPHERD);
        owned.owning(shepherd);

        List<BlueprintBlock> blocks = new ArrayList<>();

        for (int x = 0; x < 10; x++) {
            blocks.add(new BlueprintBlock(new ColonyPos(x, 0, 0), ResourceId.vanilla("oak_planks")));
        }

        ConstructionProject project = ConstructionProject.plan(colony.id(),
                Blueprint.of(ResourceId.vanilla("village/plains/houses/test_helper"), blocks),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 2, 6))));

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        try {
            VillageColonyMod.CONSTRUCTIONS.register(project);

            // Sem construtor na obra, ele não ajuda.
            BuildHelper.lendAHand(context.getWorld(), colony);
            context.assertTrue(!BuildHelper.isHelping(shepherd), "ajudou sem construtor na obra");

            Task builds = VillageColonyMod.TASKS.create(colony.id(), TaskType.BUILD, TaskPriority.CONSTRUCTION,
                    ResourceType.OAK_PLANKS, 10);
            builds.reserveFor(UUID.randomUUID());

            BuildHelper.lendAHand(context.getWorld(), colony);

            context.assertTrue(BuildHelper.isHelping(shepherd)
                            && VillageColonyMod.TASKS.assignedTo(shepherd).stream()
                                    .anyMatch(task -> task.type() == TaskType.BUILD),
                    "o pastor ocioso não ganhou a vaga de ajudante na obra");

            // A obra o solta antes dos dez blocos: a vaga volta para ele.
            Task help = VillageColonyMod.TASKS.assignedTo(shepherd).getFirst();

            help.release();
            BuildHelper.lendAHand(context.getWorld(), colony);

            context.assertTrue(help.executor().map(shepherd::equals).orElse(false),
                    "solto pela obra antes dos dez blocos, a vaga não voltou para ele");

            VillageColonyMod.TASKS.create(colony.id(), TaskType.COLLECT_WOOL, TaskPriority.PRODUCTION,
                    ResourceType.WHITE_WOOL, 4);

            // Com lã na fila e nenhum bloco posto, ele fica: a ajuda conta dez.
            BuildHelper.lendAHand(context.getWorld(), colony);

            context.assertTrue(BuildHelper.isHelping(shepherd),
                    "com pedido de lã ele largou a obra antes de pôr dez blocos");

            for (int block = 0; block < BuildHelper.MIN_PLACED; block++) {
                BuildHelper.placed(shepherd);
            }

            BuildHelper.lendAHand(context.getWorld(), colony);

            context.assertTrue(!BuildHelper.isHelping(shepherd)
                            && VillageColonyMod.TASKS.assignedTo(shepherd).isEmpty(),
                    "depois de dez blocos, com pedido de lã na fila, o pastor continuou na obra");
        } finally {
            VillageColonyMod.TASKS.ofColony(colony.id()).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            owned.cleanUp();
            BuildHelper.clearAll();
        }

        context.complete();
    }
}
