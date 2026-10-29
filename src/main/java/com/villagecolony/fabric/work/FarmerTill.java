package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O fazendeiro lavra canteiro novo — regra do autor, 2026-10-11: com nada maduro e
 * nenhum canteiro vazio, ele ara a terra em vez de esperar o trigo crescer (no
 * playtest da 0.3.26 ele esperou 46% do expediente).
 *
 * <p>O canteiro é um retângulo 9×9 ao lado da fazenda, com a água na linha do meio
 * ({@link FarmPlots}, autor 2026-10-28: a roça crescia em manchas). Fora da rua e do que
 * a vila ou a colônia construiu, e com semente nos baús — o mesmo portão do semear: a
 * roça cresce no ritmo em que a colheita paga a semente. Até {@link #MOST_PER_FARMER}
 * casas aradas por fazendeiro; o teto do trigo ({@code StockCeiling}) faz o resto.
 */
final class FarmerTill {

    static {
        ServerMemory.register(FarmerTill.class, FarmerTill::clearAll);
    }

    /** Uma casa arada a cada cinco segundos, no máximo. */
    static final int EVERY = 100;

    /** Quantas casas um fazendeiro ara: dois canteiros de 72. */
    static final int MOST_PER_FARMER = 2 * 72;

    private static final Map<UUID, Long> LAST = new HashMap<>();

    private static final Map<UUID, Integer> TILLED = new HashMap<>();

    private FarmerTill() {
    }

    static void clearAll() {
        LAST.clear();
        TILLED.clear();
    }

    /**
     * Ara um canteiro novo na borda da roça e o deixa como alvo de semear.
     *
     * @return se ele arou
     */
    static boolean till(ServerWorld world, VillagerEntity villager, FarmerWork.Job job, WorkerStorage storage) {
        UUID id = villager.getUuid();
        long now = world.getTime();
        Long last = LAST.get(id);

        if ((last != null && now - last < EVERY) || TILLED.getOrDefault(id, 0) >= MOST_PER_FARMER
                || FarmerChores.seedChest(world, job.task.colonyId(), storage.chestPosition()).isEmpty()) {
            return false;
        }

        LAST.put(id, now);

        Optional<BlockPos> plot = FarmPlots.next(world, job.task.colonyId(), villager.getBlockPos());

        if (plot.isEmpty()) {
            return false;
        }

        world.setBlockState(plot.get(), Blocks.FARMLAND.getDefaultState());
        com.villagecolony.fabric.integration.KnownFarmland.register(job.task.colonyId(), plot.get());
        villager.swingHand(Hand.MAIN_HAND, true);
        TILLED.merge(id, 1, Integer::sum);

        job.target = plot.get();
        job.chore = FarmerWork.Chore.SOW;
        job.stalled = 0;
        com.villagecolony.fabric.brain.WorkTargets.set(id, plot.get());

        VillageColonyMod.LOGGER.info("Farmer {} tilled a new plot at {} — nothing was ripe and no plot was empty",
                id.toString().substring(0, 8), plot.get().toShortString());

        return true;
    }
}
