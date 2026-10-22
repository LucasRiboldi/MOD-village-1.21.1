package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nada que a colônia corta some — Regra 110. Sem baú público com lugar, o que se cortou cai no chão ao lado da
 * obra, inteiro, em pilhas do tamanho do jogo.
 */
public class CuttingsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "cuttings", tickLimit = 40)
    public void withNoRoomInTheChestsTheCutBlocksAreLeftOnTheGround(TestContext context) {
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
            }
        }

        BlockPos near = context.getAbsolutePos(new BlockPos(2, 1, 2));
        Map<Item, Integer> taken = new LinkedHashMap<>();
        taken.put(Items.SAND, 70);
        taken.put(Items.SANDSTONE, 3);

        // Colônia que não existe: nenhum baú público, tudo cai.
        int dropped = Cuttings.keep(context.getWorld(), UUID.randomUUID(), near, taken);

        List<ItemEntity> items = context.getWorld().getEntitiesByClass(ItemEntity.class, new Box(near).expand(3),
                item -> item.isAlive());
        int sand = items.stream().filter(item -> item.getStack().isOf(Items.SAND)).mapToInt(item -> item.getStack().getCount()).sum();
        int sandstone = items.stream().filter(item -> item.getStack().isOf(Items.SANDSTONE))
                .mapToInt(item -> item.getStack().getCount()).sum();

        items.forEach(ItemEntity::discard);

        context.assertTrue(dropped == 73, "disse que largou " + dropped + ", e não 73");
        context.assertTrue(sand == 70 && sandstone == 3, "no chão ficaram " + sand + " de areia e " + sandstone
                + " de arenito, e não 70 e 3");
        context.complete();
    }
}
