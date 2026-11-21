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

    /**
     * Regra 287: o canteiro vazio é visto pelo lugar da planta, um bloco acima da terra. Mirando o meio da terra
     * arada, a linha batia na terra vizinha e o fazendeiro não semeava a três blocos com o caminho livre.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes", tickLimit = 40)
    public void theFarmerSeesAnEmptyPlotThreeBlocksAway(TestContext context) {
        for (int x = 0; x <= 3; x++) {
            context.setBlockState(new BlockPos(x, 1, 4), Blocks.DIRT);
        }

        context.setBlockState(new BlockPos(4, 1, 4), Blocks.FARMLAND);
        VillagerEntity farmer = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 4));
        BlockPos plot = context.getAbsolutePos(new BlockPos(4, 1, 4));

        context.runAtTick(5, () -> {
            context.assertFalse(FarmerWork.reaches(farmer.getBlockPos(), plot), "o braço de 3 não chegava ao canteiro");
            context.assertTrue(FarmerWork.isWithinReach(context.getWorld(), farmer, plot),
                    "a linha livre até o lugar da planta, e o fazendeiro não via o canteiro vazio");
            context.killAllEntities();
            context.complete();
        });
    }

    /** Baús da vila a 80% pedem o armazém; abaixo de 70% o pedido sai. A sobra não vira construtor (Regra 298). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes", tickLimit = 20)
    public void fullChestsAskForAStorageHouse(TestContext context) {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));
        SimpleInventory full = new SimpleInventory(10);
        SimpleInventory empty = new SimpleInventory(10);

        for (int slot = 0; slot < 8; slot++) {
            full.setStack(slot, new ItemStack(Items.COBBLESTONE));
        }

        try {
            ProfessionChestOverflow.measureFullness(colony, List.of(full));
            context.assertTrue(ProfessionChestOverflow.needsStorage(colony.id()), "baús a 80% deviam pedir armazém");

            ProfessionChestOverflow.measureFullness(colony, List.of(full, empty));
            context.assertFalse(ProfessionChestOverflow.needsStorage(colony.id()), "a 40% o pedido devia sair");
        } finally {
            ProfessionChestOverflow.measureFullness(colony, List.of(empty));
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

        // Regra 340: o tijolo de pedra é especial (reserva 1); abaixo dela sai uma receita, e não o lote inteiro.
        int made = ValueGoods.batch(context.getWorld(), chests, recipe);

        context.assertTrue(made > 0 && made <= recipe.out(), "o lote não saiu como uma receita: " + made);
        context.assertTrue(com.villagecolony.fabric.integration.ColonyChests.countIn(context.getWorld(), chests,
                Items.STONE_BRICKS) == made, "o tijolo de pedra feito não ficou no baú");
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
        context.assertTrue(ValueGoods.batch(world, chests, ore) == StockReserve.of(Items.IRON_INGOT),
                "os ferros crus deviam virar lingotes até a reserva do lingote (Regra 340)");

        var stairs = ValueGoods.RECIPES.get(ProfessionType.MASON).stream()
                .filter(recipe -> recipe.product() == Items.COBBLESTONE_STAIRS).findFirst().orElseThrow();
        ChestDepositor.deposit(world, chest(context), Items.COBBLESTONE, 23);

        context.assertTrue(ValueGoods.batch(world, chests, stairs) == 4,
                "Regra 349: acima da reserva de 16, 7 pedregulhos permitem fazer 4 degraus");
        context.assertTrue(com.villagecolony.fabric.integration.ColonyChests.countIn(world, chests, Items.COBBLESTONE)
                == 17, "a reserva e a sobra que não fez receita deviam ficar");
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

    /** O4 (autor, 2026-10-28): o carpinteiro sem tarefa adianta a madeira — tábua da tora que a vila tem mais. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes_wood", tickLimit = 20)
    public void theIdleCarpenterMakesPlanksAhead(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST);
        List<ColonyPos> chests = List.of(chest(context));
        var world = context.getWorld();

        ChestDepositor.deposit(world, chest(context), Items.ACACIA_LOG, ValueGoods.KEEP_RAW + 2);
        var recipe = ValueGoods.recipeFor(world, chests, ProfessionType.CARPENTER).orElseThrow();

        context.assertTrue(recipe.product() == Items.ACACIA_PLANKS, "devia fazer tábua de acácia: " + recipe);
        context.assertTrue(ValueGoods.batch(world, chests, recipe) == 8, "2 toras deviam dar 8 tábuas");
        context.complete();
    }

    /** O1 (autor, 2026-10-28): o pedreiro faz antes a peça de pedra que a obra aberta pede. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes_wanted", tickLimit = 20)
    public void theMasonMakesWhatTheOpenBuildAsksFirst(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST);
        List<ColonyPos> chests = List.of(chest(context));
        var world = context.getWorld();

        ChestDepositor.deposit(world, chest(context), Items.STONE, ValueGoods.KEEP_RAW + 8);
        ChestDepositor.deposit(world, chest(context), Items.COBBLESTONE, ValueGoods.KEEP_RAW + 8);

        var plain = ValueGoods.recipeFor(world, chests, ProfessionType.MASON, item -> false).orElseThrow();
        var wanted = ValueGoods.recipeFor(world, chests, ProfessionType.MASON,
                item -> item == Items.COBBLESTONE_WALL).orElseThrow();

        context.assertTrue(plain.product() == Items.STONE_BRICKS, "sem obra pedindo, a ordem de sempre: " + plain);
        context.assertTrue(wanted.product() == Items.COBBLESTONE_WALL, "a obra pede muro, e o muro vem antes: " + wanted);
        context.complete();
    }

    /** O7 (autor, 2026-10-28): o fazendeiro faz composto — a semente que sobra vira farinha de osso. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes_compost", tickLimit = 20)
    public void theIdleFarmerCompostsSpareSeeds(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST);
        List<ColonyPos> chests = List.of(chest(context));
        var world = context.getWorld();

        ChestDepositor.deposit(world, chest(context), Items.WHEAT_SEEDS, ValueGoods.KEEP_RAW + 24);
        var recipe = ValueGoods.recipeFor(world, chests, ProfessionType.FARMER).orElseThrow();

        context.assertTrue(recipe.product() == Items.BONE_MEAL, "a semente de sobra devia virar farinha de osso: " + recipe);
        context.assertTrue(ValueGoods.batch(world, chests, recipe) >= 1, "o composto não saiu");
        context.assertTrue(com.villagecolony.fabric.integration.ColonyChests.countIn(world, chests, Items.WHEAT_SEEDS)
                >= ValueGoods.KEEP_RAW, "a semente de plantar devia ficar");
        context.complete();
    }

    /**
     * Sem chegar ao nível da mina, a escada recomeça na mesma boca da superfície, cursor no primeiro degrau, virando
     * para o rumo seguinte — boca nova nunca (autor, 2026-10-29).
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes", tickLimit = 20)
    public void theStairStartsOverFromTheSameMouthTurned(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        BlockPos column = context.getAbsolutePos(new BlockPos(2, 1, 2));
        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getWorld()
                .getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column).down());
        ColonyPos deep = new ColonyPos(mouth.x() + 30, mouth.y() - 40, mouth.z() + 30);

        VillageColonyMod.MINES.restore(Mine.restore(colonyId, MineShaft.from(deep, Side.EAST), 500));
        MineEntrance.opened(colonyId, mouth);

        try {
            context.assertTrue(MineRestart.restart(context.getWorld(), colonyId,
                    VillageColonyMod.MINES.of(colonyId).orElseThrow()), "a escada devia recomeçar");

            Mine fresh = VillageColonyMod.MINES.of(colonyId).orElseThrow();

            context.assertTrue(fresh.shaft().entry().equals(mouth), "a mina devia voltar à mesma boca: " + fresh.shaft().entry());
            context.assertTrue(fresh.shaft().descent() == Side.EAST.clockwise(), "a escada devia virar de rumo");
            context.assertTrue(fresh.arms().getFirst().cut() == 0, "o cursor devia voltar ao primeiro degrau");
        } finally {
            VillageColonyMod.MINES.removeOfColony(colonyId);
        }

        context.complete();
    }

    /** Sem a boca lembrada na superfície, a mina fica como está: nunca abre outra boca. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_fixes", tickLimit = 20)
    public void withoutTheFirstMouthTheMineKeepsItsStair(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        ColonyPos buried = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 1, 2)));
        ColonyPos deep = new ColonyPos(buried.x() + 30, buried.y() - 40, buried.z() + 30);
        Mine before = Mine.restore(colonyId, MineShaft.from(deep, Side.EAST), 500);

        VillageColonyMod.MINES.restore(before);
        MineEntrance.opened(colonyId, buried);

        try {
            context.assertFalse(MineRestart.restart(context.getWorld(), colonyId, before), "abriu uma boca nova");
            context.assertTrue(VillageColonyMod.MINES.of(colonyId).orElseThrow().shaft().entry().equals(deep),
                    "a mina devia ficar como estava");
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
