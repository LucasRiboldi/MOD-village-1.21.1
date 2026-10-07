package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ActionTool;
import com.villagecolony.fabric.integration.ColonySupply;
import com.villagecolony.fabric.integration.DropIngredients;
import com.villagecolony.fabric.work.BlockShaping;
import com.villagecolony.fabric.work.FarmerBakery;
import com.villagecolony.fabric.work.ShepherdFlock;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;

import java.util.UUID;

/**
 * A revisão das profissões de 2026-09-30: pão do fazendeiro, rebanho do
 * pastor, ofício Vanilla bloqueado, ferramenta certa, lava que não surge e
 * ingrediente de drop que aparece. Cada caso em batch próprio: as arenas
 * vizinhas têm ovelha, aldeão e baú, e o raio de busca as alcança.
 */
public class ProfessionReviewGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 1, 2);

    private static int count(ChestBlockEntity chest, Item item) {
        int total = 0;

        for (int slot = 0; slot < chest.size(); slot++) {
            if (chest.getStack(slot).isOf(item)) {
                total += chest.getStack(slot).getCount();
            }
        }

        return total;
    }

    private static ChestBlockEntity chest(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        return (ChestBlockEntity) context.getBlockEntity(CHEST);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "review_bakery")
    public void theFarmerBakesOnlyTheWheatAboveTheReserve(TestContext context) {
        ChestBlockEntity chest = chest(context);
        chest.setStack(0, new ItemStack(Items.WHEAT, 26));
        ColonyPos at = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        int baked = FarmerBakery.bakeSurplus(context.getWorld(), at);

        context.assertTrue(baked == 6,
                "assou " + baked + " paes, esperado 6 (18 trigos acima da reserva de 8)");
        context.assertTrue(count(chest, Items.BREAD) == 6, "o pao nao entrou no bau");
        context.assertTrue(count(chest, Items.WHEAT) == FarmerBakery.WHEAT_RESERVE,
                "a reserva de trigo virou " + count(chest, Items.WHEAT));

        int again = FarmerBakery.bakeSurplus(context.getWorld(), at);

        context.assertTrue(again == 0, "a reserva foi assada: " + again);
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "review_flock")
    public void theShepherdFeedsTwoSheepToBreed(TestContext context) {
        ChestBlockEntity chest = chest(context);
        chest.setStack(0, new ItemStack(Items.WHEAT, 4));

        ColonyPos center = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        Colony colony = Colony.create(UUID.randomUUID(), center);
        VillageColonyMod.COLONIES.register(colony);

        UUID shepherdId = UUID.randomUUID();
        VillageColonyMod.WORKERS.register(shepherdId, colony.id()).assign(ProfessionType.SHEPHERD);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(shepherdId, center));
        ColonyFixture fixture = ColonyFixture.create().owning(colony).owning(shepherdId);

        SheepEntity first = context.spawnEntity(EntityType.SHEEP, new BlockPos(4, 1, 4));
        SheepEntity second = context.spawnEntity(EntityType.SHEEP, new BlockPos(5, 1, 4));
        ShepherdFlock.clearAll();

        try {
            context.assertFalse(first.isInLove() || second.isInLove(),
                    "as ovelhas nasceram no cio — o caso nao mediria nada");

            boolean paired = ShepherdFlock.tend(context.getWorld(), colony);

            context.assertTrue(paired, "o pastor nao pos o par para procriar");
            context.assertTrue(first.isInLove() && second.isInLove(),
                    "as duas ovelhas deviam estar no cio");
            context.assertTrue(count(chest, Items.WHEAT) == 2,
                    "o par devia comer 2 trigos; sobraram " + count(chest, Items.WHEAT));
            context.assertFalse(ShepherdFlock.tend(context.getWorld(), colony),
                    "um segundo par saiu antes do intervalo");
        } finally {
            fixture.cleanUp();
            ShepherdFlock.clearAll();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "review_vanilla_trade")
    public void aColonyWorkerCannotTakeAVanillaTrade(TestContext context) {
        VillagerEntity worker = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        VillagerEntity outsider = context.spawnEntity(EntityType.VILLAGER, new BlockPos(4, 1, 2));

        VillageColonyMod.WORKERS.register(worker.getUuid(), UUID.randomUUID())
                .assign(ProfessionType.LUMBERJACK);

        try {
            worker.setVillagerData(
                    worker.getVillagerData().withProfession(VillagerProfession.FARMER));
            outsider.setVillagerData(
                    outsider.getVillagerData().withProfession(VillagerProfession.FARMER));

            context.assertTrue(
                    worker.getVillagerData().getProfession() == VillagerProfession.NONE,
                    "o lenhador da colonia virou " + worker.getVillagerData().getProfession());
            context.assertTrue(
                    outsider.getVillagerData().getProfession() == VillagerProfession.FARMER,
                    "o aldeao de fora perdeu o oficio Vanilla — o guarda pegou quem nao devia");
        } finally {
            VillageColonyMod.WORKERS.remove(worker.getUuid());
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "review_tools")
    public void eachBlockIsBrokenWithTheRightIronTool(TestContext context) {
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        ItemStack diamond = new ItemStack(Items.DIAMOND_PICKAXE);

        context.assertTrue(
                ActionTool.forBlock(Blocks.DIRT.getDefaultState(), hoe).isOf(Items.IRON_SHOVEL),
                "a terra devia sair de pa");
        context.assertTrue(
                ActionTool.forBlock(Blocks.STONE.getDefaultState(), hoe).isOf(Items.IRON_PICKAXE),
                "a pedra devia sair de picareta");
        context.assertTrue(
                ActionTool.forBlock(Blocks.OAK_LOG.getDefaultState(), hoe).isOf(Items.IRON_AXE),
                "o tronco devia sair de machado");
        context.assertTrue(ActionTool.forBlock(Blocks.STONE.getDefaultState(), diamond) == diamond,
                "a picareta melhor da mao foi trocada por ferro");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "review_lava")
    public void lavaIsNeverShapedButWaterIs(TestContext context) {
        context.assertTrue(BlockShaping.isNeverPlaced(Blocks.LAVA.getDefaultState()),
                "a lava devia ser pulada");
        context.assertFalse(BlockShaping.isShapedFromTheGround(Blocks.LAVA.getDefaultState()),
                "a lava ainda entra montada");
        context.assertTrue(BlockShaping.isShapedFromTheGround(Blocks.WATER.getDefaultState()),
                "a agua deixou de ser montada");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "review_drops")
    public void theStringOfALoomAppearsAndTheLoomIsMade(TestContext context) {
        ChestBlockEntity chest = chest(context);
        chest.setStack(0, new ItemStack(Items.OAK_PLANKS, 8));

        ColonyPos center = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        Colony colony = Colony.create(UUID.randomUUID(), center);
        VillageColonyMod.COLONIES.register(colony);

        UUID carpenterId = UUID.randomUUID();
        VillageColonyMod.WORKERS.register(carpenterId, colony.id()).assign(ProfessionType.CARPENTER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(carpenterId, center));
        ColonyFixture fixture = ColonyFixture.create().owning(colony).owning(carpenterId);

        try {
            context.assertTrue(DropIngredients.isAutomatic(Items.STRING)
                            && DropIngredients.isAutomatic(Items.RED_DYE)
                            && DropIngredients.isAutomatic(Items.WHITE_DYE)
                            && DropIngredients.isAutomatic(Items.CYAN_DYE)
                            && !DropIngredients.isAutomatic(Items.OAK_PLANKS),
                    "a classificacao de drop esta errada");

            boolean made = ColonySupply.stock(context.getWorld(), colony.id(), center, Items.LOOM);

            context.assertTrue(made && count(chest, Items.LOOM) == 1,
                    "o tear nao saiu: a linha devia aparecer no bau (loom="
                            + count(chest, Items.LOOM) + ", string=" + count(chest, Items.STRING) + ")");
        } finally {
            fixture.cleanUp();
        }

        context.complete();
    }
}
