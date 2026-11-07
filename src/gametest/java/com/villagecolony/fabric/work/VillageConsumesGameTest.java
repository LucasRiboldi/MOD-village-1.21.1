package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ToolWear;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** Ferramenta e manutenção gastam estoque, sem nunca parar a vila — Regra 221. */
public class VillageConsumesGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(5, 1, 7);

    private static ChestBlockEntity chest(TestContext context, Worker worker) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        VillageColonyMod.STORAGES.register(WorkerStorage.of(
                worker.villagerId(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST))));

        return (ChestBlockEntity) context.getBlockEntity(CHEST);
    }

    private static void standing(ColonyFixture fixture, VillagerEntity worker) {
        BlockPos at = worker.getBlockPos();

        VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), fixture.colony().id(),
                ResourceId.vanilla("village/plains/houses/plains_temple_4"),
                new ColonyPos(at.getX() - 2, at.getY(), at.getZ() - 2),
                new ColonyPos(at.getX() + 2, at.getY() + 4, at.getZ() + 2), true));
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_consumes_visit", tickLimit = 20)
    public void aCarpenterSpendsPlanksKeepingABuildingOncePerDay(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        Colony colony = fixture.colony();
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);

            Worker worker = fixture.equippedWorker(context, ProfessionType.CARPENTER, new BlockPos(3, 1, 3));
            VillagerEntity carpenter = (VillagerEntity) world.getEntity(worker.villagerId());
            ChestBlockEntity chest = chest(context, worker);

            chest.setStack(0, new ItemStack(Items.OAK_PLANKS, BuildingUpkeep.RESERVE + BuildingUpkeep.ITEMS));
            standing(fixture, carpenter);

            context.assertTrue(BuildingUpkeep.start(world, colony, carpenter, ResourceGroup.PLANKS) == 1, "a visita não começou");
            context.assertTrue(BuildingUpkeep.isVisiting(worker.villagerId()), "ele devia estar em visita");
            context.assertFalse(IdleWorkers.isFree(world, carpenter, null), "em visita, ele não está livre para outra ajuda");

            BuildingUpkeep.tick(world);

            context.assertTrue(chest.count(Items.OAK_PLANKS) == BuildingUpkeep.RESERVE,
                    "a visita devia gastar " + BuildingUpkeep.ITEMS + " tábuas, ficaram " + chest.count(Items.OAK_PLANKS));
            context.assertTrue(BuildingUpkeep.start(world, colony, carpenter, ResourceGroup.PLANKS) == 0,
                    "a mesma construção recebeu duas visitas no dia");
        } finally {
            world.setTimeOfDay(before);
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
            fixture.cleanUp();
        }

        context.complete();
    }

    /** Abaixo da reserva a madeira fica para a obra: é o que evita faltar material no meio de uma. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_consumes_reserve", tickLimit = 20)
    public void belowTheReserveNobodySpends(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        Colony colony = fixture.colony();
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);

            Worker worker = fixture.equippedWorker(context, ProfessionType.CARPENTER, new BlockPos(3, 1, 3));
            VillagerEntity carpenter = (VillagerEntity) world.getEntity(worker.villagerId());
            ChestBlockEntity chest = chest(context, worker);

            chest.setStack(0, new ItemStack(Items.OAK_PLANKS, BuildingUpkeep.RESERVE + BuildingUpkeep.ITEMS - 1));
            standing(fixture, carpenter);

            context.assertTrue(BuildingUpkeep.start(world, colony, carpenter, ResourceGroup.PLANKS) == 0,
                    "abaixo da reserva, a manutenção saiu mesmo assim");
        } finally {
            world.setTimeOfDay(before);
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
            fixture.cleanUp();
        }

        context.complete();
    }

    /** Com obra aberta, o material é dela. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_consumes_open_work", tickLimit = 20)
    public void anOpenWorkHoldsTheUpkeep(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        Colony colony = fixture.colony();
        long before = world.getTimeOfDay();
        ConstructionProject project = ConstructionProject.plan(colony.id(),
                Blueprint.of(ResourceId.vanilla("oak_planks"), List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks")))),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(8, 1, 8))));

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        try {
            world.setTimeOfDay(2_000);
            VillageColonyMod.CONSTRUCTIONS.register(project);

            Worker worker = fixture.equippedWorker(context, ProfessionType.CARPENTER, new BlockPos(3, 1, 3));
            VillagerEntity carpenter = (VillagerEntity) world.getEntity(worker.villagerId());
            ChestBlockEntity chest = chest(context, worker);

            chest.setStack(0, new ItemStack(Items.OAK_PLANKS, 64));
            standing(fixture, carpenter);

            context.assertTrue(BuildingUpkeep.start(world, colony, carpenter, ResourceGroup.PLANKS) == 0,
                    "com obra aberta, a manutenção gastou o material dela");
        } finally {
            world.setTimeOfDay(before);
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
            fixture.cleanUp();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_consumes_tool", tickLimit = 20)
    public void aToolLastsThreeDaysAndTheNewOneComesOutOfTheChests(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));

        try {
            Worker worker = fixture.equippedWorker(context, ProfessionType.MINER, new BlockPos(3, 1, 3));
            VillagerEntity miner = (VillagerEntity) world.getEntity(worker.villagerId());
            ChestBlockEntity chest = chest(context, worker);
            ItemStack tool = miner.getEquippedStack(EquipmentSlot.MAINHAND);

            context.assertTrue(tool.isDamageable(), "o mineiro devia ter ferramenta, tem " + tool);

            chest.setStack(0, new ItemStack(Items.OAK_PLANKS, 4));
            chest.setStack(1, new ItemStack(Items.COBBLESTONE, 6));

            ToolWear.pass(world, fixture.colony());
            context.assertTrue(tool.isDamaged(), "um dia de trabalho não gastou a ferramenta");

            ToolWear.pass(world, fixture.colony());
            int renewed = ToolWear.pass(world, fixture.colony());

            context.assertTrue(renewed == 1 && !tool.isDamaged(), "no terceiro dia ela devia ser renovada: " + renewed);
            context.assertTrue(chest.count(Items.OAK_PLANKS) == 2 && chest.count(Items.COBBLESTONE) == 3,
                    "a renovada custa 2 tábuas e 3 pedras");
        } finally {
            fixture.cleanUp();
        }

        context.complete();
    }

    /** Sem material a ferramenta fica no último ponto: a falta de estoque não tira a ferramenta de ninguém. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_consumes_no_stock", tickLimit = 20)
    public void withoutStockTheToolStaysInTheHand(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));

        try {
            Worker worker = fixture.equippedWorker(context, ProfessionType.MINER, new BlockPos(3, 1, 3));
            VillagerEntity miner = (VillagerEntity) world.getEntity(worker.villagerId());
            ItemStack tool = miner.getEquippedStack(EquipmentSlot.MAINHAND);

            chest(context, worker);

            for (int day = 0; day < 10; day++) {
                ToolWear.pass(world, fixture.colony());
            }

            context.assertTrue(miner.getEquippedStack(EquipmentSlot.MAINHAND) == tool && tool.getDamage() == tool.getMaxDamage() - 1,
                    "sem estoque ela devia ficar no último ponto, está em " + tool.getDamage());
        } finally {
            fixture.cleanUp();
        }

        context.complete();
    }
}
