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

            // O construtor larga a obra (playtest 0.3.31: encalhado na mina): a vaga do
            // ajudante não é a tarefa da obra, e o planejador abre outra para o construtor.
            builds.cancel();
            ConstructionPlanner.ensureTask(colony, project);

            context.assertTrue(VillageColonyMod.TASKS.ofColony(colony.id()).stream()
                            .anyMatch(task -> task.type() == TaskType.BUILD && task.isOpen()
                                    && !BuildHelper.isHelperTask(task.id())),
                    "com só o ajudante na obra, o planejador não abriu a tarefa do construtor");

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

    /**
     * O carpinteiro sem tarefa (estoque no teto) ajuda a obra; com pedido do ofício
     * na fila, não entra — regra do autor, 2026-10-11.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_helper")
    public void anIdleCarpenterHelpsTheBuilder(TestContext context) {
        ColonyPos centre = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 2, 2)));
        Colony colony = Colony.create(UUID.randomUUID(), centre);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 2, 3));
        UUID carpenter = villager.getUuid();

        VillageColonyMod.WORKERS.register(carpenter, colony.id()).assign(ProfessionType.CARPENTER);
        owned.owning(carpenter);

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
            VillageColonyMod.TASKS.create(colony.id(), TaskType.BUILD, TaskPriority.CONSTRUCTION,
                    ResourceType.OAK_PLANKS, 10).reserveFor(UUID.randomUUID());
            Task craft = VillageColonyMod.TASKS.create(colony.id(), TaskType.CRAFT_WOOD_MATERIAL,
                    TaskPriority.PRODUCTION, ResourceType.OAK_STAIRS, 4);

            BuildHelper.lendAHand(context.getWorld(), colony);
            context.assertFalse(BuildHelper.isHelping(carpenter), "com escada pedida, o carpinteiro devia ficar no ofício");

            VillageColonyMod.TASKS.remove(craft.id());
            BuildHelper.lendAHand(context.getWorld(), colony);

            context.assertTrue(BuildHelper.isHelping(carpenter)
                            && VillageColonyMod.TASKS.assignedTo(carpenter).stream()
                                    .anyMatch(task -> task.type() == TaskType.BUILD),
                    "o carpinteiro sem tarefa não ganhou a vaga de ajudante na obra");
        } finally {
            VillageColonyMod.TASKS.ofColony(colony.id()).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            owned.cleanUp();
            BuildHelper.clearAll();
        }

        context.complete();
    }

    /**
     * O fazendeiro com a colheita na mão e a roça descansando (nada maduro) ajuda a
     * obra e devolve a colheita à fila; com a roça ativa, fica no ofício — Regra 51,
     * playtest 0.3.31 (18 minutos andando sem colher).
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_helper")
    public void aFarmerWithNothingRipeHelpsTheBuilder(TestContext context) {
        ColonyPos centre = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 2, 2)));
        Colony colony = Colony.create(UUID.randomUUID(), centre);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 2, 3));
        UUID farmer = villager.getUuid();

        VillageColonyMod.WORKERS.register(farmer, colony.id()).assign(ProfessionType.FARMER);
        owned.owning(farmer);

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
            VillageColonyMod.TASKS.create(colony.id(), TaskType.BUILD, TaskPriority.CONSTRUCTION,
                    ResourceType.OAK_PLANKS, 10).reserveFor(UUID.randomUUID());
            Task harvest = VillageColonyMod.TASKS.create(colony.id(), TaskType.COLLECT_FOOD,
                    TaskPriority.PRODUCTION, ResourceType.WHEAT, 64);

            harvest.reserveFor(farmer);

            BuildHelper.lendAHand(context.getWorld(), colony);
            context.assertFalse(BuildHelper.isHelping(farmer), "com a roça ativa, o fazendeiro devia ficar no ofício");

            FieldRest.sweptAndFoundNothing(colony.id(), context.getWorld().getTime());
            BuildHelper.lendAHand(context.getWorld(), colony);

            context.assertTrue(BuildHelper.isHelping(farmer)
                            && VillageColonyMod.TASKS.assignedTo(farmer).stream()
                                    .allMatch(task -> task.type() == TaskType.BUILD),
                    "com nada maduro, o fazendeiro não largou a colheita para ajudar a obra");
        } finally {
            FieldRest.thereIsWorkAgain(colony.id());
            VillageColonyMod.TASKS.ofColony(colony.id()).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            owned.cleanUp();
            BuildHelper.clearAll();
        }

        context.complete();
    }
}
