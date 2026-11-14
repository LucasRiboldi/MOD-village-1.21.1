package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.CraftFamily;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
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
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Um pouco de tudo no estoque do carpinteiro — regra do autor, 2026-10-11 ({@link ItemStock}). */
public class ItemStockGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_stock_catalog", tickLimit = 20)
    public void allThreeWorkshopsStockBlocksBeyondTheVillagePalette(TestContext context) {
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 2, 2))));
        var catalog = ItemStock.read(context.getWorld(), colony);

        context.assertTrue(catalog.getOrDefault(TaskType.CRAFT_STONE_MATERIAL, List.of())
                        .containsAll(List.of(Items.POLISHED_TUFF, Items.DEEPSLATE_BRICK_STAIRS)),
                "o pedreiro não mantém peças de pedra fora das plantas da vila");
        context.assertTrue(catalog.getOrDefault(TaskType.CRAFT_METAL_MATERIAL, List.of())
                        .containsAll(List.of(Items.CUT_COPPER_STAIRS, Items.IRON_TRAPDOOR)),
                "o fundidor não mantém peças de metal fora das plantas da vila");
        context.assertTrue(ItemStock.EACH == 16, "a reserva deve ser de 16 por tipo");
        context.complete();
    }

    /** O trabalho livre repõe só o que falta até 16, sem acumular centenas (Regra 133). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_stock_catalog", tickLimit = 20)
    public void valueGoodsRefillsOnlyTheConsumedStoneBrick(TestContext context) {
        BlockPos chestAt = new BlockPos(2, 2, 2);
        context.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chestAt));
        ServerWorld world = context.getWorld();
        ChestDepositor.deposit(world, chest, Items.STONE, 80);
        ChestDepositor.deposit(world, chest, Items.STONE_BRICKS, 15);
        List<ColonyPos> chests = List.of(chest);

        ValueGoods.Recipe recipe = ValueGoods.recipeFor(world, chests, ProfessionType.MASON).orElseThrow();
        context.assertTrue(recipe.product() == Items.STONE_BRICKS, "o pedreiro escolheu outra peça");
        context.assertTrue(ValueGoods.batch(world, chests, recipe) == 1,
                "o lote não respeitou o déficit de uma unidade");
        context.assertTrue(ColonyChests.countIn(world, chests, Items.STONE_BRICKS) == 16,
                "o estoque passou de 16 tijolos de pedra");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_stock_the_carpenter_stocks_a_piece_of_the_village_plans", tickLimit = 400)
    public void theCarpenterStocksAPieceOfTheVillagePlans(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos chestAt = new BlockPos(2, 2, 2);
        BlockPos spareAt = new BlockPos(2, 2, 4);
        context.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        context.setBlockState(spareAt, Blocks.CHEST.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chestAt));
        ColonyPos spare = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(spareAt));

        for (Item wood : List.of(Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.ACACIA_PLANKS, Items.JUNGLE_PLANKS)) {
            ChestDepositor.deposit(world, spare, wood, 64);
        }

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 2, 2));
        villager.setBreedingAge(0);

        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(ProfessionType.CARPENTER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
        UUID keeper = UUID.randomUUID();
        VillageColonyMod.WORKERS.register(keeper, colony.id()).assign(ProfessionType.FARMER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(keeper, spare));
        ColonyFixture fixture = ColonyFixture.create().owning(colony).owning(villager.getUuid()).owning(keeper);

        ItemStock.openOrders(world, colony);

        List<Item> wood = ItemStock.itemsOf(colony.id(), TaskType.CRAFT_WOOD_MATERIAL);
        Optional<Task> order = VillageColonyMod.TASKS.ofColony(colony.id()).stream()
                .filter(task -> task.type() == TaskType.CRAFT_WOOD_MATERIAL && task.targetResource() == ItemStock.NOMINAL)
                .findFirst();

        context.assertTrue(!wood.isEmpty(), "as plantas da vila não deram item de madeira para o estoque");
        context.assertTrue(wood.stream().allMatch(item ->
                        CraftFamily.of(ItemStock.pathOf(item)) == TaskType.CRAFT_WOOD_MATERIAL),
                "item de outra oficina na lista do carpinteiro: " + wood);
        // Um pouco de todas as peças de madeira, não só as do estilo (autor, 2026-10-12).
        context.assertTrue(wood.containsAll(List.of(net.minecraft.item.Items.SPRUCE_DOOR,
                        net.minecraft.item.Items.BIRCH_TRAPDOOR, net.minecraft.item.Items.DARK_OAK_FENCE_GATE)),
                "faltam peças de madeira de outras madeiras na lista do carpinteiro: " + wood);
        context.assertTrue(order.isPresent(), "a ordem de estoque não foi aberta para o carpinteiro");

        order.get().reserveFor(villager.getUuid());
        CraftingWork.run(world, colony);

        context.runAtTick(380, () -> {
            try {
                boolean stocked = wood.stream().anyMatch(item ->
                        ColonyChests.countIn(world, List.of(chest, spare), item) >= 1
                                && item != Items.OAK_PLANKS && item != Items.SPRUCE_PLANKS
                                && item != Items.ACACIA_PLANKS && item != Items.JUNGLE_PLANKS);

                context.assertTrue(stocked, "o carpinteiro não fez nenhum item das plantas para o estoque");
            } finally {
                VillageColonyMod.TASKS.ofColony(colony.id()).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
                fixture.cleanUp();
                ItemStock.clearAll();
            }

            context.complete();
        });
    }
}
