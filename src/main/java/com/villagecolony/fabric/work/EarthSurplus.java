package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.StockCeiling;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Set;

/**
 * Regra 350: quem passa por terra solta só a recolhe enquanto o total daquele tipo nos baús acessíveis
 * estiver abaixo de {@link StockCeiling#RAW}. Ao atingir o teto, a pilha fica no chão.
 */
final class EarthSurplus {

    /** O que conta como terra: o que o nivelamento e a mina soltam e a vila não pede em obra. */
    private static final Set<Item> EARTH = Set.of(Items.DIRT, Items.GRASS_BLOCK, Items.COARSE_DIRT, Items.PODZOL,
            Items.ROOTED_DIRT, Items.MUD, Items.DIRT_PATH);

    private EarthSurplus() {
    }

    /** Se este item é terra e os baús já têm o teto dele. */
    static boolean full(ServerWorld world, List<ColonyPos> chests, Item item) {
        return EARTH.contains(item) && ColonyChests.countIn(world, chests, item) >= StockCeiling.RAW;
    }
}
