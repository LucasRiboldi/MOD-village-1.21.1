package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

/**
 * Enche baú de cenário direto no inventário.
 *
 * <p>O {@code ChestDepositor} obedece ao teto de três compartimentos por item
 * (ADR-036 9) e não serve mais para entupir um baú com um item só.
 */
final class TestChests {

    private TestChests() {
    }

    /** Põe uma pilha cheia em cada compartimento vazio. */
    static void fillEmptySlots(ServerWorld world, ColonyPos chest, Item item) {
        if (!(world.getBlockEntity(MinecraftTypeAdapter.toBlockPos(chest)) instanceof ChestBlockEntity block)) {
            throw new AssertionError("o baú do cenário não existe em " + chest);
        }

        for (int slot = 0; slot < block.size(); slot++) {
            if (block.getStack(slot).isEmpty()) {
                block.setStack(slot, new ItemStack(item, item.getDefaultStack().getMaxCount()));
            }
        }

        block.markDirty();
    }
}
