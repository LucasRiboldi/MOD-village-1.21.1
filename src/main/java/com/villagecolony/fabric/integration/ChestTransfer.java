package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * O que a vila já tem em outro baú vai para o baú de quem precisa, antes de qualquer
 * coisa surgir — playtest 0.3.31: 64 lãs surgiram no baú do pastor com 218 nos
 * outros baús da vila. O baú da profissão continua recebendo (P0.55: o fundidor só
 * lê o dele), mas com o que existe, e não com item tirado do nada.
 */
final class ChestTransfer {

    private ChestTransfer() {
    }

    /**
     * Leva deste item, dos outros baús para {@code to}, até {@code amount}.
     *
     * @return quanto foi levado
     */
    static int bring(ServerWorld world, List<ColonyPos> chests, ColonyPos to, Item item, int amount) {
        List<ColonyPos> others = chests.stream().filter(chest -> !chest.equals(to)).toList();
        int moving = Math.min(amount, ColonyChests.countIn(world, others, item));

        if (moving <= 0) {
            return 0;
        }

        int taken = ColonyChests.withdraw(world, others, item, moving);
        int left = ChestDepositor.deposit(world, to, item, taken);

        if (left > 0) {
            // Não coube tudo: o resto volta para a vila, e nada se perde.
            ColonyChests.deposit(world, others, item, left);
        }

        if (taken - left > 0) {
            VillageColonyMod.LOGGER.info("The colony moved {} x{} into the chest at {} — the village had it elsewhere",
                    item, taken - left, to);
        }

        return taken - left;
    }
}
