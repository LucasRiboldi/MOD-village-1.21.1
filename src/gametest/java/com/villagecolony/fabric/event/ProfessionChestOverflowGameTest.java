package com.villagecolony.fabric.event;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ProfessionChestOverflow;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Set;

public final class ProfessionChestOverflowGameTest implements FabricGameTest {
    private static final BlockPos SOURCE = new BlockPos(2, 2, 2);
    private static final BlockPos COMMUNITY = new BlockPos(4, 2, 2);
    private static final BlockPos OTHER_PROFESSION = new BlockPos(6, 2, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "profession_chest_overflow")
    public void aFullProfessionChestMovesItsLastTenSlotsToACommunityChest(TestContext context) {
        ChestBlockEntity source = chest(context, SOURCE);
        ChestBlockEntity community = chest(context, COMMUNITY);
        fill(source, Items.COBBLESTONE);

        int moved = ProfessionChestOverflow.relieve(context.getWorld(),
                List.of(position(context, SOURCE), position(context, COMMUNITY)),
                Set.of(position(context, SOURCE)));

        context.assertTrue(moved == 10, "deveria mover os dez slots reservados");
        for (int slot = 0; slot < source.size(); slot++) {
            boolean expectedEmpty = slot >= source.size() - 10;
            context.assertTrue(source.getStack(slot).isEmpty() == expectedEmpty,
                    "slot inesperado depois do alívio: " + slot);
        }
        context.assertTrue(count(community, Items.COBBLESTONE) == 10,
                "os itens não chegaram ao baú comunitário");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "profession_chest_overflow")
    public void anotherProfessionChestIsNeverUsedAsOverflow(TestContext context) {
        ChestBlockEntity source = chest(context, SOURCE);
        ChestBlockEntity community = chest(context, COMMUNITY);
        ChestBlockEntity otherProfession = chest(context, OTHER_PROFESSION);
        fill(source, Items.DIRT);

        ProfessionChestOverflow.relieve(context.getWorld(),
                List.of(position(context, SOURCE), position(context, OTHER_PROFESSION),
                        position(context, COMMUNITY)),
                Set.of(position(context, SOURCE), position(context, OTHER_PROFESSION)));

        context.assertTrue(count(otherProfession, Items.DIRT) == 0,
                "outro baú profissional recebeu o excedente");
        context.assertTrue(count(community, Items.DIRT) == 10,
                "o baú comunitário não recebeu os dez itens");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "profession_chest_overflow")
    public void itemsStayInPlaceWhenCommunityChestsHaveNoRoom(TestContext context) {
        ChestBlockEntity source = chest(context, SOURCE);
        ChestBlockEntity community = chest(context, COMMUNITY);
        fill(source, Items.OAK_LOG);
        fill(community, Items.STONE);

        int moved = ProfessionChestOverflow.relieve(context.getWorld(),
                List.of(position(context, SOURCE), position(context, COMMUNITY)),
                Set.of(position(context, SOURCE)));

        context.assertTrue(moved == 0, "itens foram removidos sem destino");
        context.assertTrue(count(source, Items.OAK_LOG) == source.size(),
                "o baú profissional perdeu itens sem espaço comunitário");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "profession_chest_overflow")
    public void sourceOutsideThisVillageIsNotMoved(TestContext context) {
        ChestBlockEntity source = chest(context, SOURCE);
        chest(context, COMMUNITY);
        fill(source, Items.IRON_INGOT);

        int moved = ProfessionChestOverflow.relieve(context.getWorld(),
                List.of(position(context, COMMUNITY)),
                Set.of(position(context, SOURCE)));

        context.assertTrue(moved == 0, "um baú de outra vila foi alterado");
        context.assertTrue(count(source, Items.IRON_INGOT) == source.size(),
                "itens cruzaram o limite entre vilas");
        context.complete();
    }

    private static ChestBlockEntity chest(TestContext context, BlockPos relative) {
        context.setBlockState(relative, Blocks.CHEST.getDefaultState());
        return (ChestBlockEntity) context.getBlockEntity(relative);
    }

    private static ColonyPos position(TestContext context, BlockPos relative) {
        return MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(relative));
    }

    private static void fill(ChestBlockEntity chest, Item item) {
        for (int slot = 0; slot < chest.size(); slot++) {
            chest.setStack(slot, new ItemStack(item));
        }
    }

    private static int count(ChestBlockEntity chest, Item item) {
        int amount = 0;
        for (int slot = 0; slot < chest.size(); slot++) {
            ItemStack stack = chest.getStack(slot);
            if (stack.isOf(item)) {
                amount += stack.getCount();
            }
        }
        return amount;
    }
}
