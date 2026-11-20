package com.villagecolony.fabric.work;

import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.CropPatch;
import net.minecraft.block.ComposterBlock;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O compostor do fazendeiro — P8b (autor, 2026-10-11) e Regra 237 (autor, 2026-11-11, opção F2).
 *
 * <p>O local de trabalho do fazendeiro no Vanilla é o compostor. Sem colheita, canteiro
 * vazio nem lavoura verde ({@link BoneMealRound}, Regra 310), ele composta a semente que
 * sobra acima da reserva, com a chance de cada item do próprio jogo ({@code ComposterBlock},
 * sete níveis por farinha de osso). A reserva é uma semente por canteiro conhecido da roça,
 * e nunca menos de {@value #MIN_SEED_RESERVE}.
 */
public final class FarmerCompost {

    static {
        ServerMemory.register(FarmerCompost.class, FarmerCompost::clearAll);
    }

    /** O mínimo de sementes que fica para replantar, mesmo sem roça conhecida (Regra 237). */
    static final int MIN_SEED_RESERVE = 8;

    /** Níveis do compostor por farinha de osso, como no jogo. */
    static final int LEVELS_PER_BONE_MEAL = 7;

    /** Um gesto a cada segundo. */
    static final int EVERY = 20;

    private static final List<Item> COMPOSTED = List.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS);

    private static final Map<UUID, Double> LEVELS = new HashMap<>();

    private static final Map<UUID, Long> LAST = new HashMap<>();

    private FarmerCompost() {
    }

    static void clearAll() {
        LEVELS.clear();
        LAST.clear();
    }

    /**
     * Um gesto do fazendeiro sem colheita, canteiro nem lavoura verde: semente sobrando no compostor.
     *
     * @return se ele trabalhou
     */
    static boolean tend(ServerWorld world, VillagerEntity villager, UUID colonyId, WorkerStorage storage) {
        long now = world.getTime();
        Long last = LAST.get(villager.getUuid());

        if (last != null && now - last < EVERY) {
            return false;
        }

        LAST.put(villager.getUuid(), now);

        List<ColonyPos> chests = ColonyChests.ownFirst(world, colonyId, storage.chestPosition());

        if (compostASeed(world, colonyId, chests, storage.chestPosition())) {
            villager.swingHand(Hand.MAIN_HAND, true);

            return true;
        }

        return false;
    }

    /** As sementes que ficam: uma por canteiro da roça, e nunca menos de {@value #MIN_SEED_RESERVE} (Regra 237). */
    static int seedReserve(int plots) {
        return Math.max(MIN_SEED_RESERVE, plots);
    }

    /** Uma semente acima da reserva vai ao compostor; os níveis viram farinha de osso. */
    static boolean compostASeed(ServerWorld world, UUID colonyId, List<ColonyPos> chests, ColonyPos into) {
        for (Item seed : COMPOSTED) {
            if (KeepLedger.atMost("seed", ColonyChests.countIn(world, chests, seed), seedReserve(CropPatch.knownOf(colonyId).size()))
                    || ColonyChests.withdraw(world, chests, seed, 1) == 0) {
                continue;
            }

            double levels = LEVELS.merge(colonyId,
                    (double) ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(seed), Double::sum);

            if (levels >= LEVELS_PER_BONE_MEAL
                    && ColonyChests.deposit(world, List.of(into), Items.BONE_MEAL, 1) == 0) {
                LEVELS.put(colonyId, levels - LEVELS_PER_BONE_MEAL);
            }

            return true;
        }

        return false;
    }
}
