package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.service.ProfessionDemand;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ProfessionChestOverflow;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** As correções do jogo da 0.3.99 para os ociosos e travados — escolhas do autor de 2026-10-28. */
public class IdleFixesGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    /** O pastor só pega tarefa de lã com ovelha lanuda no alcance e menos de 64 da lã no baú. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes", tickLimit = 40)
    public void woolIsTakenOnlyWithAWoollySheepAndRoomForIt(TestContext context) {
        Colony colony = colonyWithChest(context);
        UUID keeper = UUID.randomUUID();
        VillageColonyMod.WORKERS.register(keeper, colony.id());
        VillageColonyMod.STORAGES.register(
                com.villagecolony.core.storage.model.WorkerStorage.of(keeper, chest(context)));

        try {
            SheepEntity sheep = context.spawnEntity(EntityType.SHEEP, new BlockPos(5, 2, 5));
            sheep.setSheared(true);
            context.assertFalse(WoolGate.open(context.getWorld(), colony.id(), ResourceType.WHITE_WOOL),
                    "sem ovelha com lã a tarefa não devia abrir");

            sheep.setSheared(false);
            context.assertTrue(WoolGate.open(context.getWorld(), colony.id(), ResourceType.WHITE_WOOL),
                    "com ovelha lanuda e baú vazio a tarefa devia abrir");

            ChestDepositor.deposit(context.getWorld(), chest(context), Items.WHITE_WOOL, WoolGate.CAP);
            context.assertFalse(WoolGate.open(context.getWorld(), colony.id(), ResourceType.WHITE_WOOL),
                    "com 64 lãs no baú não se tosquia mais");
        } finally {
            VillageColonyMod.STORAGES.remove(keeper);
            VillageColonyMod.WORKERS.remove(keeper);
            VillageColonyMod.COLONIES.remove(colony.id());
            context.killAllEntities();
        }

        context.complete();
    }

    /** O carpinteiro não recebe a ordem de estoque de peça de tábua quando a madeira tem de ficar em tora. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes", tickLimit = 20)
    public void aWoodOrderIsNotOpenedWhenTheLogsMustStay(TestContext context) {
        Colony colony = colonyWithChest(context);
        List<ColonyPos> chests = List.of(chest(context));

        try {
            ChestDepositor.deposit(context.getWorld(), chest(context), Items.ACACIA_LOG, 4);
            context.assertTrue(ItemStock.woodHeldInLogs(context.getWorld(), colony, chests, Items.ACACIA_FENCE),
                    "sem tábua e com a tora reservada a ordem de cerca não devia abrir");

            ChestDepositor.deposit(context.getWorld(), chest(context), Items.ACACIA_PLANKS, 16);
            context.assertFalse(ItemStock.woodHeldInLogs(context.getWorld(), colony, chests, Items.ACACIA_FENCE),
                    "com tábua no baú a ordem pode abrir");
        } finally {
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }

    /** O fazendeiro alcança a planta dois blocos acima a três de distância, como o jogador. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes", tickLimit = 40)
    public void theFarmerReachesACropOnATerraceLikeAPlayer(TestContext context) {
        context.setBlockState(new BlockPos(4, 3, 4), Blocks.FARMLAND);
        context.setBlockState(new BlockPos(4, 4, 4), Blocks.WHEAT);
        VillagerEntity farmer = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        BlockPos crop = context.getAbsolutePos(new BlockPos(4, 4, 4));

        context.runAtTick(5, () -> {
            context.assertFalse(FarmerWork.reaches(farmer.getBlockPos(), crop), "o alcance antigo de 3 não chegava");
            context.assertTrue(FarmerWork.isWithinReach(context.getWorld(), farmer, crop),
                    "a 4,5 do olho com a linha livre ele devia alcançar a planta");
            context.killAllEntities();
            context.complete();
        });
    }

    /** Baús da vila a 80% pedem o armazém e um construtor a mais; abaixo de 70% o pedido sai. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes", tickLimit = 20)
    public void fullChestsAskForAStorageHouseAndASecondBuilder(TestContext context) {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));
        SimpleInventory full = new SimpleInventory(10);
        SimpleInventory empty = new SimpleInventory(10);

        for (int slot = 0; slot < 8; slot++) {
            full.setStack(slot, new ItemStack(Items.COBBLESTONE));
        }

        try {
            ProfessionChestOverflow.measureFullness(colony, List.of(full));
            context.assertTrue(ProfessionChestOverflow.needsStorage(colony.id()), "baús a 80% deviam pedir armazém");
            context.assertTrue(ProfessionDemand.spareOf(colony.id()).contains(ProfessionType.BUILDER),
                    "baús cheios deviam pedir um construtor a mais");

            ProfessionChestOverflow.measureFullness(colony, List.of(full, empty));
            context.assertFalse(ProfessionChestOverflow.needsStorage(colony.id()), "a 40% o pedido devia sair");
            context.assertTrue(ProfessionDemand.spareOf(colony.id()).isEmpty(), "e o construtor a mais também");
        } finally {
            ProfessionDemand.setSpare(colony.id(), List.of());
        }

        context.complete();
    }

    /** Com o cru acima da reserva, o ofício livre faz o lote de valor; sem cru, não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes", tickLimit = 20)
    public void spareStoneBecomesStoneBricks(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST);
        List<ColonyPos> chests = List.of(chest(context));

        context.assertTrue(ValueGoods.recipeFor(context.getWorld(), chests, ProfessionType.MASON).isEmpty(),
                "sem pedra sobrando não há lote");

        ChestDepositor.deposit(context.getWorld(), chest(context), Items.STONE, ValueGoods.KEEP_RAW + ValueGoods.BATCH);
        var recipe = ValueGoods.recipeFor(context.getWorld(), chests, ProfessionType.MASON).orElseThrow();

        context.assertTrue(ValueGoods.batch(context.getWorld(), chests, recipe) == ValueGoods.BATCH, "o lote não saiu");
        context.assertTrue(com.villagecolony.fabric.integration.ColonyChests.countIn(context.getWorld(), chests,
                Items.STONE_BRICKS) == ValueGoods.BATCH, "o lote devia virar 8 tijolos de pedra");
        context.complete();
    }

    /**
     * Regra 146: o fundidor funde todo o minério cru (sem reserva), e a receita que não é 1 por 1 sai na
     * proporção dela — 6 pedregulhos dão 4 degraus.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes", tickLimit = 20)
    public void rawOreIsSmeltedWholeAndStairsComeInTheirRatio(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST);
        List<ColonyPos> chests = List.of(chest(context));
        var world = context.getWorld();

        ChestDepositor.deposit(world, chest(context), Items.RAW_IRON, 5);
        var ore = ValueGoods.recipeFor(world, chests, ProfessionType.SMELTER).orElseThrow();

        context.assertTrue(ore.product() == Items.IRON_INGOT, "o fundidor devia fundir o ferro primeiro: " + ore);
        context.assertTrue(ValueGoods.batch(world, chests, ore) == 5, "os 5 ferros crus deviam virar 5 lingotes");

        var stairs = ValueGoods.RECIPES.get(ProfessionType.MASON).stream()
                .filter(recipe -> recipe.product() == Items.COBBLESTONE_STAIRS).findFirst().orElseThrow();
        ChestDepositor.deposit(world, chest(context), Items.COBBLESTONE, ValueGoods.KEEP_RAW + 7);

        context.assertTrue(ValueGoods.batch(world, chests, stairs) == 4, "7 de sobra fazem uma receita: 4 degraus");
        context.assertTrue(com.villagecolony.fabric.integration.ColonyChests.countIn(world, chests, Items.COBBLESTONE)
                == ValueGoods.KEEP_RAW + 1, "a reserva e a sobra que não fez receita deviam ficar");
        context.complete();
    }

    /** T5 (autor, 2026-10-29): a vidraça sai do fundidor — areia, vidro, vidraça; 6 vidros dão 16 vidraças. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes_panes", tickLimit = 20)
    public void theSmelterTurnsSpareGlassIntoPanes(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST);
        List<ColonyPos> chests = List.of(chest(context));
        var world = context.getWorld();

        ChestDepositor.deposit(world, chest(context), Items.GLASS, ValueGoods.KEEP_RAW + 6);
        var recipe = ValueGoods.recipeFor(world, chests, ProfessionType.SMELTER).orElseThrow();

        context.assertTrue(recipe.product() == Items.GLASS_PANE, "o fundidor devia fazer vidraça do vidro: " + recipe);
        context.assertTrue(ValueGoods.batch(world, chests, recipe) == 16, "6 vidros deviam virar 16 vidraças");
        context.assertTrue(com.villagecolony.fabric.integration.ColonyChests.countIn(world, chests, Items.GLASS)
                == ValueGoods.KEEP_RAW, "a reserva de vidro das obras devia ficar");
        context.complete();
    }

    /** Sem chegar ao nível da mina, a escada recomeça na boca da superfície, cursor no primeiro degrau. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes", tickLimit = 20)
    public void theStairStartsOverFromTheSurfaceMouth(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 1, 2)));
        ColonyPos deep = new ColonyPos(mouth.x() + 30, mouth.y() - 40, mouth.z() + 30);

        VillageColonyMod.MINES.restore(Mine.restore(colonyId, MineShaft.from(deep, Side.EAST), 500));
        MineEntrance.opened(colonyId, mouth);

        try {
            context.assertTrue(MineRestart.restart(context.getWorld(), colonyId,
                    VillageColonyMod.MINES.of(colonyId).orElseThrow()), "a escada devia recomeçar");

            Mine fresh = VillageColonyMod.MINES.of(colonyId).orElseThrow();

            // O arco e a lanterna da boca nova sobem o topo da coluna: confere a altura contra o nível fundo.
            context.assertTrue(fresh.shaft().entry().y() >= mouth.y() - 3,
                    "a mina nova devia começar no chão da superfície: " + fresh.shaft().entry());
            context.assertTrue(fresh.arms().getFirst().cut() == 0, "o cursor devia voltar ao primeiro degrau");
        } finally {
            VillageColonyMod.MINES.removeOfColony(colonyId);
        }

        context.complete();
    }

    private static Colony colonyWithChest(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST);
        Colony colony = Colony.create(UUID.randomUUID(), chest(context));
        VillageColonyMod.COLONIES.register(colony);

        return colony;
    }

    private static ColonyPos chest(TestContext context) {
        return MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
    }
}
