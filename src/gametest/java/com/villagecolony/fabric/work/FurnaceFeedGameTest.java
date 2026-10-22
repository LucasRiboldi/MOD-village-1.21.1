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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O insumo da fornalha feito pelo pedreiro — B1, 2026-10-21: no deserto o fundidor parou 41 vezes
 * sem arenito, com areia nos baús, e o arenito liso da obra só apareceu porque o vigia G2 o fez surgir.
 */
public class FurnaceFeedGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    /** Sem arenito e com areia, o fundidor pede o arenito ao pedreiro e solta a fornalha. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "furnace_feed", tickLimit = 20)
    public void aSmelterWithoutSandstoneAsksTheMasonForIt(TestContext context) {
        UUID mason = UUID.randomUUID();
        UUID smelter = UUID.randomUUID();
        ColonyPos chest = chestWithSand(context, 8);
        Colony colony = colonyWith(chest, mason);
        Task smooth = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.SMELT_MATERIAL, TaskPriority.CONSTRUCTION_MATERIAL, ResourceType.SMOOTH_SANDSTONE, 6);

        try {
            VillageColonyMod.WORKERS.find(mason).orElseThrow().assign(ProfessionType.MASON);
            smooth.reserveFor(smelter);

            context.assertTrue(SmelterGathers.handOff(context.getWorld(), smelter, smooth, List.of(Items.SANDSTONE)),
                    "sem arenito e com areia, o fundidor devia soltar a fornalha parada");
            context.assertTrue(smooth.state() == TaskState.AVAILABLE, "o arenito liso não voltou à fila: " + smooth.state());

            long orders = VillageColonyMod.TASKS.ofColony(colony.id()).stream()
                    .filter(task -> task.isOpen() && FurnaceFeed.isOrder(task)
                            && task.targetResource() == ResourceType.SANDSTONE)
                    .count();

            context.assertTrue(orders == 1, "devia haver um pedido de arenito ao pedreiro, e há " + orders);

            smooth.reserveFor(smelter);
            context.assertTrue(SmelterGathers.handOff(context.getWorld(), smelter, smooth, List.of(Items.SANDSTONE)),
                    "com o pedido aberto, o fundidor segue fora da fornalha");
            context.assertTrue(VillageColonyMod.TASKS.ofColony(colony.id()).stream()
                            .filter(task -> task.isOpen() && FurnaceFeed.isOrder(task)).count() == 1,
                    "o segundo passe abriu outro pedido em vez de esperar o primeiro");
        } finally {
            VillageColonyMod.TASKS.ofColony(colony.id()).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
            forget(colony, mason);
        }

        context.complete();
    }

    /** O pedreiro faz o arenito de quatro areias, e guarda no baú dele. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "furnace_feed", tickLimit = 20)
    public void theMasonMakesSandstoneOutOfFourSand(TestContext context) {
        UUID mason = UUID.randomUUID();
        ColonyPos chest = chestWithSand(context, 4);
        Colony colony = colonyWith(chest, mason);

        try {
            context.assertTrue(FurnaceFeed.makeOne(context.getWorld(), colony, Items.SANDSTONE, 1, chest),
                    "quatro areias deviam render um arenito");

            List<ColonyPos> chests = List.of(chest);

            context.assertTrue(ColonyChests.countIn(context.getWorld(), chests, Items.SANDSTONE) == 1
                            && ColonyChests.countIn(context.getWorld(), chests, Items.SAND) == 0,
                    "o baú devia ter um arenito e nenhuma areia");
        } finally {
            forget(colony, mason);
        }

        context.complete();
    }

    /** A areia que a obra aberta vai pôr fica: nem pedido, nem receita. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "furnace_feed", tickLimit = 20)
    public void theSandTheOpenBuildNeedsIsNotTurnedIntoSandstone(TestContext context) {
        UUID mason = UUID.randomUUID();
        ColonyPos chest = chestWithSand(context, 4);
        Colony colony = colonyWith(chest, mason);
        List<BlueprintBlock> floor = new ArrayList<>();

        for (int x = 0; x < 2; x++) {
            floor.add(new BlueprintBlock(new ColonyPos(x, 0, 0), ResourceId.vanilla("sand")));
        }

        ConstructionProject project = ConstructionProject.plan(colony.id(),
                Blueprint.of(ResourceId.vanilla("test_floor"), floor),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(4, 2, 4))));
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        try {
            VillageColonyMod.CONSTRUCTIONS.register(project);
            VillageColonyMod.WORKERS.find(mason).orElseThrow().assign(ProfessionType.MASON);

            context.assertFalse(FurnaceFeed.askTheMason(context.getWorld(), colony, List.of(Items.SANDSTONE)),
                    "a obra pede duas das quatro areias: sobram duas, e o arenito pede quatro");
            context.assertFalse(FurnaceFeed.makeOne(context.getWorld(), colony, Items.SANDSTONE, 1, chest),
                    "o pedreiro gastou a areia da obra");
            context.assertTrue(ColonyChests.countIn(context.getWorld(), List.of(chest), Items.SAND) == 4,
                    "a areia da obra saiu do baú");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.TASKS.ofColony(colony.id()).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
            forget(colony, mason);
        }

        context.complete();
    }

    /** O pedreiro de verdade, pelo CraftingWork, atende o pedido de arenito com a areia da vila. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "furnace_feed_work", tickLimit = 300)
    public void theMasonAtWorkFillsTheSandstoneOrder(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyPos chest = chestWithSand(context, 8);
        world.setTimeOfDay(Schedule.WORK_TIME);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 2, 2));
        villager.setBreedingAge(0);

        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(ProfessionType.MASON);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
        ColonyFixture fixture = ColonyFixture.create().owning(colony).owning(villager.getUuid());

        Task order = VillageColonyMod.TASKS.create(colony.id(), TaskType.CRAFT_STONE_MATERIAL,
                TaskPriority.CONSTRUCTION_MATERIAL, ResourceType.SANDSTONE, 1);
        order.reserveFor(villager.getUuid());
        CraftingWork.run(world, colony);

        context.runAtTick(280, () -> {
            try {
                int sandstone = ColonyChests.countIn(world, List.of(chest), Items.SANDSTONE);

                context.assertTrue(sandstone >= 1, "o pedreiro não fez o arenito do pedido: " + sandstone);
            } finally {
                VillageColonyMod.TASKS.ofColony(colony.id()).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
                fixture.cleanUp();
            }

            context.complete();
        });
    }

    /** O pedido nominal de peça de obra (alvo pedregulho) não é pedido de insumo da fornalha. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "furnace_feed", tickLimit = 20)
    public void theWorkPieceRequestIsNotAFurnaceOrder(TestContext context) {
        UUID colony = UUID.randomUUID();
        Task nominal = VillageColonyMod.TASKS.create(colony, TaskType.CRAFT_STONE_MATERIAL,
                TaskPriority.CONSTRUCTION_MATERIAL, CraftsmanRequest.NOMINAL, 1);
        Task feed = VillageColonyMod.TASKS.create(colony, TaskType.CRAFT_STONE_MATERIAL,
                TaskPriority.CONSTRUCTION_MATERIAL, ResourceType.SANDSTONE, 1);
        Task cut = VillageColonyMod.TASKS.create(colony, TaskType.CRAFT_STONE_MATERIAL,
                TaskPriority.CONSTRUCTION_MATERIAL, ResourceType.CUT_SANDSTONE, 1);

        try {
            context.assertFalse(FurnaceFeed.isOrder(nominal), "o pedido de peça de obra virou pedido de fornalha");
            context.assertTrue(FurnaceFeed.isOrder(feed), "o pedido de arenito devia ser de fornalha");
            context.assertFalse(FurnaceFeed.isOrder(cut), "arenito cortado é peça fabricada, não cru da fornalha");
        } finally {
            VillageColonyMod.TASKS.ofColony(colony).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
        }

        context.complete();
    }

    private static ColonyPos chestWithSand(TestContext context, int sand) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ChestDepositor.deposit(context.getWorld(), chest, Items.SAND, sand);

        return chest;
    }

    private static Colony colonyWith(ColonyPos chest, UUID worker) {
        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(worker, colony.id());
        VillageColonyMod.STORAGES.register(WorkerStorage.of(worker, chest));

        return colony;
    }

    private static void forget(Colony colony, UUID worker) {
        VillageColonyMod.STORAGES.remove(worker);
        VillageColonyMod.WORKERS.remove(worker);
        VillageColonyMod.COLONIES.remove(colony.id());
    }
}
