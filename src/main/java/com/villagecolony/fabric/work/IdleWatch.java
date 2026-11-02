package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nenhuma profissão espera parada mais de 5 minutos por nada — Regra 128 (autor, 2026-10-27).
 *
 * <p><b>1º degrau, 5 minutos.</b> Quem tem tarefa e soma {@link #WAIT_LIMIT} segundos esperando, bloqueado
 * ou encalhado desde o último golpe de trabalho larga a tarefa: ela volta à fila, a capacidade descansa
 * ({@code Worker.rest}) e ele entra na cadeia de ajudas de quem está sem tarefa (Regra 127, caminho,
 * recolher do chão). Caminhar sem progresso também conta: procurar o mesmo alvo sem produzir não pode
 * manter a tarefa presa indefinidamente.
 *
 * <p>O watchdog estacionário mede só quem parou. Caminhada sem resultado pertence a
 * {@link TaskProgressWatch}, que também resolve a segunda falha diária.
 */
public final class IdleWatch {

    static {
        ServerMemory.register(IdleWatch.class, IdleWatch::clearAll);
    }

    /** Segundos esperando com tarefa até largá-la: cinco minutos (uma amostra por segundo). */
    static final int WAIT_LIMIT = 300;

    private static final Map<UUID, Integer> WAITING = new HashMap<>();

    private IdleWatch() {
    }

    /** Uma amostra deste trabalhador no expediente; chamada pelo {@code WorkTime}. */
    static void sample(ServerWorld world, UUID workerId, WorkTime.State state) {
        if (state == WorkTime.State.WALK) {
            return;
        }
        if (state == WorkTime.State.WORK || VillageColonyMod.TASKS.assignedTo(workerId).isEmpty()) {
            WAITING.remove(workerId);
            return;
        }

        if (WAITING.merge(workerId, 1, Integer::sum) < WAIT_LIMIT) {
            return;
        }

        WAITING.remove(workerId);
        letGo(world, workerId);
    }

    /** Larga tudo e faz a capacidade descansar. */
    static void letGo(ServerWorld world, UUID workerId) {
        List<Task> tasks = List.copyOf(VillageColonyMod.TASKS.assignedTo(workerId));

        VillageColonyMod.WORKERS.find(workerId)
                .ifPresent(worker -> tasks.forEach(task -> worker.rest(task.requiredCapability())));
        int released = WorkerJobs.dropAll(workerId);

        VillageColonyMod.LOGGER.info("Worker {} waited 5 minutes on {} without working — it lets go ({} task(s)"
                        + " requeued) and helps the village (Regra 128)", workerId.toString().substring(0, 8),
                tasks.stream().map(task -> task.type() + " " + task.targetResource()).toList(), released);

    }

    /** Quantos segundos este trabalhador soma esperando com tarefa, para o painel da bancada. */
    static int waitingOf(UUID workerId) {
        return WAITING.getOrDefault(workerId, 0);
    }

    static void clearAll() {
        WAITING.clear();
    }
}
