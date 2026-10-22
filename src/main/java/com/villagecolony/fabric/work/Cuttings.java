package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;

/**
 * Nada que a colônia corta do terreno some — Regra 110 (R-059 do estudo v2, autor de 2026-10-22). O que a
 * terraplanagem do lote e o corte do corredor tiram vai para os baús públicos da vila ({@link PublicChests});
 * o que não couber cai no chão ao lado da obra, inteiro, como o bloco quebrado à mão.
 */
final class Cuttings {

    private Cuttings() {
    }

    /**
     * Guarda o que se cortou.
     *
     * @param near onde o que não couber cai
     * @return quanto caiu no chão
     */
    static int keep(ServerWorld world, UUID colonyId, BlockPos near, Map<Item, Integer> taken) {
        int dropped = 0;

        for (Map.Entry<Item, Integer> entry : taken.entrySet()) {
            int left = PublicChests.store(world, colonyId, near, entry.getKey(), entry.getValue());

            dropped += left;

            while (left > 0) {
                int stack = Math.min(left, entry.getKey().getMaxCount());

                Block.dropStack(world, near.up(), new ItemStack(entry.getKey(), stack));
                left -= stack;
            }
        }

        if (dropped > 0) {
            VillageColonyMod.LOGGER.info("Colony {} had no room for {} cut blocks — left on the ground at {}",
                    colonyId.toString().substring(0, 8), dropped, near.toShortString());
        }

        return dropped;
    }
}
