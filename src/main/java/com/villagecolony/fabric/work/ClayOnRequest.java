package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.Supplies;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.UUID;

/**
 * A argila que o fundidor pede aparece no baú dele na hora do pedido — Regra 301 (autor, 2026-11-16: "argila sempre
 * aparece automático na hora pedida"). Vale para a argila e a bola de argila, só quando nenhum baú da vila tem, e cada
 * vez fica uma linha {@code VC_SUPPLY} ({@link SupplyException#CLAY_ON_REQUEST}). Antes, ela só aparecia depois de duas
 * faltas e quatro buscas vazias, e o fundidor parava na fornalha a cada pedido.
 */
final class ClayOnRequest {

    /** Quanto aparece de cada vez. */
    static final int SURGE = 16;

    private static final List<Item> CLAY = List.of(Items.CLAY, Items.CLAY_BALL);

    private ClayOnRequest() {
    }

    /**
     * Põe a argila pedida no baú do fundidor se nenhum baú da vila tem.
     *
     * @param chests os baús da vila, para conferir que nenhum tem
     * @param own o baú do fundidor
     * @param raws o que a fornalha aceita para o pedido
     * @return se a argila apareceu
     */
    static boolean stockIfMissing(ServerWorld world, UUID colonyId, List<ColonyPos> chests, ColonyPos own,
                                  List<Item> raws) {
        for (Item raw : raws) {
            if (!CLAY.contains(raw) || ColonyChests.countIn(world, chests, raw) > 0) {
                continue;
            }

            int put = SURGE - ChestDepositor.deposit(world, own, raw, SURGE);

            if (put > 0) {
                Supplies.record(SupplyException.CLAY_ON_REQUEST, colonyId, Registries.ITEM.getId(raw).getPath(), put);

                return true;
            }
        }

        return false;
    }
}
