package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * Tarefa para quem ficou livre entre um ciclo e outro — F10, 2026-09-30.
 *
 * <p>A tarefa só era distribuída dentro do ciclo de colônia, a cada 30 s:
 * quem terminava ou desistia no meio ficava parado até lá, e na sessão das
 * 02:45 as rodadas terminaram com 518 tarefas abertas somadas. Uma vez por
 * segundo, a colônia que tem trabalhador livre e tarefa aberta distribui e
 * despacha na hora.
 *
 * <p>Não decide nada que o ciclo decide: não abre nem cancela tarefa, e não
 * avança o relógio dos descansos — {@link WorkAssignment#assignWithoutAClock}.
 * Só no caminho de jogo, como a fusão: as arenas da bateria ficam lado a lado.
 *
 * <p><b>E não devolve a tarefa a quem acabou de soltá-la</b> — E49,
 * 2026-09-30. Ver {@link IdleHandouts}.
 */
final class IdleHands {

    /** De quanto em quanto tempo olhar, em tiques. */
    static final int EVERY_TICKS = 20;

    private static final IdleHandouts HANDOUTS = new IdleHandouts();

    static {
        ServerMemory.register(IdleHands.class, IdleHands::clearAll);
    }

    private IdleHands() {
    }

    /** Todas as colônias ativas perto de quem joga. */
    static int assignNow(ServerWorld world) {
        HANDOUTS.forgetExpired(world.getTime());

        int handed = 0;

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (colony.isActive() && VillageFocus.isWorking(world, colony.id())) {
                handed += assignNow(world, colony);
            }
        }

        return handed;
    }

    static int assignNow(ServerWorld world, Colony colony) {
        if (VillageColonyMod.TASKS.availableFor(colony.id()).isEmpty()) {
            return 0;
        }

        List<Worker> idle = WorkAssignment.idleWorkers(
                colony.id(), VillageColonyMod.WORKERS, VillageColonyMod.TASKS);

        if (idle.isEmpty()) {
            return 0;
        }

        long now = world.getTime();

        int handed = WorkAssignment.assignWithoutAClock(
                colony.id(),
                VillageColonyMod.WORKERS,
                VillageColonyMod.TASKS,
                VillageColonyMod.STORAGES::hasStorage,
                (worker, task) -> HANDOUTS.mayHand(worker.villagerId(), task.id(), now)
                        && ColonyCycleRunner.canReserveTask(world, colony.id(), task));

        if (handed > 0) {
            for (Worker worker : idle) {
                for (Task task : VillageColonyMod.TASKS.assignedTo(worker.villagerId())) {
                    HANDOUTS.handed(worker.villagerId(), task.id(), now);
                }
            }

            // Quem recebeu tarefa começa a andar agora, e não no ciclo.
            ColonyCycleRunner.runOngoingWork(world, colony);

            VillageColonyMod.LOGGER.info(
                    "Colony {} handed {} tasks between cycles to workers who had just come free",
                    colony.id(), handed);
        }

        return handed;
    }

    /** Esquece as entregas — para o mundo que foi descarregado. */
    static void clearAll() {
        HANDOUTS.clear();
    }
}
