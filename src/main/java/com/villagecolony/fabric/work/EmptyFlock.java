package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

/**
 * Nenhuma ovelha com lã no raio — playtest de 2026-10-03.
 *
 * <p>O pastor segurou a tarefa de lã por meia hora — 0% de trabalho, 58% de
 * espera — sem tosquiar e sem uma linha no log: {@code ShepherdWork.findSheep}
 * voltava calado e a tarefa nunca era solta. Agora um ciclo inteiro sem
 * ovelha é uma busca vazia: a tarefa volta à fila com o castigo do
 * {@link EmptySweeps}, e na terceira a lã aparece no baú
 * ({@link LocateFallback}, pedido do autor).
 */
final class EmptyFlock {

    /** Quanto tempo sem ovelha com lã conta como uma busca vazia: um ciclo da colônia. */
    static final int SEARCH = 600;

    private EmptyFlock() {
    }

    /** Fecha a busca vazia: conta, talvez estoca, e solta ou cancela a tarefa. */
    static void endSearch(ServerWorld world, Task task, UUID workerId, int radius) {
        // Antes de esperar, o rebanho em déficit cresce — E7.
        ShepherdFlock.breedForWool(world, task.colonyId());

        int empty = EmptySweeps.foundNothing(task.colonyId(), task.targetResource(), world.getTime());
        boolean stocked = LocateFallback.afterEmptySearch(world, task, empty);

        VillageColonyMod.LOGGER.info("Shepherd {} found no sheep with wool within {} blocks for {} ticks — {}",
                workerId, radius, SEARCH, stocked ? "the wool was stocked instead" : "task back to the queue");

        if (stocked) {
            task.cancel();
        } else {
            task.release();
        }
    }
}
