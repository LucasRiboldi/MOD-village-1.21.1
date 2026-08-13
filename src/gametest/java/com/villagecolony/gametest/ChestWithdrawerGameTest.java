package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Tirar do baú — a primeira coisa que o mod faz que diminui o que o
 * jogador tem.
 *
 * <p>Até a Fase 9 a colônia só somava: contava o baú, punha madeira
 * dentro, e nada saía. É por isso que estes testes olham tanto para o que
 * **fica** quanto para o que sai.
 */
public class ChestWithdrawerGameTest implements FabricGameTest {

    private static ColonyPos chestAt(TestContext context, BlockPos chest) {
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        return MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest));
    }

    /** O que se pede sai, e o resto fica. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "withdraw_exact")
    public void whatIsAskedForComesOut(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyPos chest = chestAt(context, new BlockPos(2, 2, 2));

        ChestDepositor.deposit(world, chest, Items.OAK_LOG, 40);

        int taken = ChestWithdrawer.withdraw(world, chest, Items.OAK_LOG, 12);

        context.assertTrue(taken == 12, "esperava 12 troncos, saíram " + taken);

        int left = ChestInventoryReader
                .read(world, MinecraftTypeAdapter.toBlockPos(chest))
                .amountOf(ResourceType.OAK_LOG);

        context.assertTrue(left == 28, "esperava 28 no baú, ficaram " + left);

        context.complete();
    }

    /**
     * Pedir mais do que existe tira o que existe.
     *
     * <p>Não é erro: o baú pode ter menos, e quem chama decide o que
     * fazer com a diferença.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "withdraw_partial")
    public void askingForMoreThanThereIsTakesWhatThereIs(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyPos chest = chestAt(context, new BlockPos(2, 2, 2));

        ChestDepositor.deposit(world, chest, Items.OAK_LOG, 5);

        int taken = ChestWithdrawer.withdraw(world, chest, Items.OAK_LOG, 64);

        context.assertTrue(taken == 5, "esperava 5, saíram " + taken);

        int left = ChestInventoryReader
                .read(world, MinecraftTypeAdapter.toBlockPos(chest))
                .amountOf(ResourceType.OAK_LOG);

        context.assertTrue(left == 0, "o baú devia ter ficado sem tronco, tem " + left);

        context.complete();
    }

    /**
     * O item do jogador não é tocado.
     *
     * <p>O baú da colônia costuma ser um baú que o jogador também usa. A
     * colônia tira o que ela conta, e mais nada — é a mesma regra que
     * faz o diamante do jogador não contar como espaço livre.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "withdraw_foreign")
    public void anotherPlayersItemIsNeverTaken(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyPos chest = chestAt(context, new BlockPos(2, 2, 2));

        ChestDepositor.deposit(world, chest, Items.DIAMOND, 9);

        List<ItemStack> taken = ChestWithdrawer.withdrawGroup(
                world, chest, ResourceGroup.WOOD, 64);

        context.assertTrue(taken.isEmpty(), "tirou o que não era da colônia");

        int diamonds = 0;

        BlockPos absolute = MinecraftTypeAdapter.toBlockPos(chest);

        for (int slot = 0; slot < 27; slot++) {
            ItemStack stack = context.getWorld().getBlockEntity(absolute) instanceof
                    net.minecraft.inventory.Inventory inventory
                    ? inventory.getStack(slot)
                    : ItemStack.EMPTY;

            if (stack.isOf(Items.DIAMOND)) {
                diamonds += stack.getCount();
            }
        }

        context.assertTrue(diamonds == 9, "o diamante do jogador mudou: " + diamonds);

        context.complete();
    }

    /**
     * O grupo devolve a espécie, e não só a conta.
     *
     * <p>O fabricante pega madeira, não carvalho — e qual espécie ele
     * pegou decide qual receita se aplica. Devolver um número deixaria
     * quem chama sem saber o que tem na mão.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "withdraw_group")
    public void theGroupSaysWhichSpeciesCameOut(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyPos chest = chestAt(context, new BlockPos(2, 2, 2));

        ChestDepositor.deposit(world, chest, Items.BIRCH_LOG, 3);
        ChestDepositor.deposit(world, chest, Items.JUNGLE_LOG, 4);

        List<ItemStack> taken = ChestWithdrawer.withdrawGroup(
                world, chest, ResourceGroup.WOOD, 5);

        int total = 0;

        for (ItemStack stack : taken) {
            total += stack.getCount();

            context.assertTrue(
                    stack.isOf(Items.BIRCH_LOG) || stack.isOf(Items.JUNGLE_LOG),
                    "saiu o que não era madeira: " + stack);
        }

        context.assertTrue(total == 5, "esperava 5 troncos, saíram " + total);

        // E o que sobrou continua no baú: sete tinham entrado.
        int left = ChestInventoryReader
                .read(world, MinecraftTypeAdapter.toBlockPos(chest))
                .amountOfGroup(ResourceGroup.WOOD);

        context.assertTrue(left == 2, "esperava 2 no baú, ficaram " + left);

        context.complete();
    }

    /** Baú que não existe não é erro: não sai nada. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "withdraw_no_chest")
    public void thereIsNothingToTakeFromAMissingChest(TestContext context) {
        ServerWorld world = context.getWorld();

        ColonyPos nowhere = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(2, 2, 2)));

        context.assertTrue(
                ChestWithdrawer.withdraw(world, nowhere, Items.OAK_LOG, 5) == 0,
                "tirou tronco de um baú que não existe");

        context.assertTrue(
                ChestWithdrawer.withdrawGroup(world, nowhere, ResourceGroup.WOOD, 5).isEmpty(),
                "tirou madeira de um baú que não existe");

        context.complete();
    }
}
