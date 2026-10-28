package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.Supplies;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Nenhuma profissão espera mais de 5 minutos por nada — Regra 128 (autor, 2026-10-27), em dois degraus.
 *
 * <p><b>1º degrau, 5 minutos.</b> Quem tem tarefa e soma {@link #WAIT_LIMIT} segundos esperando, bloqueado
 * ou encalhado desde o último golpe de trabalho larga a tarefa: ela volta à fila, a capacidade descansa
 * ({@code Worker.rest}) e ele entra na cadeia de ajudas de quem está sem tarefa (Regra 127, caminho,
 * recolher do chão). Caminhar não conta nem zera: a descida longa da mina não é espera.
 *
 * <p><b>2º degrau, 10 minutos.</b> A mesma tarefa largada de novo {@link #APPEAR_AFTER} tiques depois da
 * primeira vez faz o produto dela surgir no baú de quem a largou ({@link SupplyException#WAITED_TOO_LONG}).
 * A peça de obra fica de fora: a obra já faz a peça parada surgir em dois ciclos
 * ({@code ConstructionSupplyDispatches}).
 */
public final class IdleWatch {

    static {
        ServerMemory.register(IdleWatch.class, IdleWatch::clearAll);
    }

    /** Segundos esperando com tarefa até largá-la: cinco minutos (uma amostra por segundo). */
    static final int WAIT_LIMIT = 300;

    /** Dez minutos entre a primeira e a segunda vez que a mesma tarefa é largada. */
    static final long APPEAR_AFTER = 12_000;

    /** A primeira vez esquecida: largar de novo três vezes depois disso recomeça a conta. */
    static final long FORGET_AFTER = 3 * APPEAR_AFTER;

    /** No máximo isto surge de uma vez. */
    static final int MAX_APPEARING = 16;

    private static final Map<UUID, Integer> WAITING = new HashMap<>();

    private static final Map<String, Long> FIRST_LET_GO = new HashMap<>();

    private IdleWatch() {
    }

    /** Uma amostra deste trabalhador no expediente; chamada pelo {@code WorkTime}. */
    static void sample(ServerWorld world, UUID workerId, WorkTime.State state) {
        if (state == WorkTime.State.WORK || VillageColonyMod.TASKS.assignedTo(workerId).isEmpty()) {
            WAITING.remove(workerId);
            return;
        }

        if (state == WorkTime.State.WALK) {
            return;
        }

        if (WAITING.merge(workerId, 1, Integer::sum) < WAIT_LIMIT) {
            return;
        }

        WAITING.remove(workerId);
        letGo(world, workerId);
    }

    /** O 1º degrau: larga tudo, a capacidade descansa, e o 2º degrau olha cada tarefa largada. */
    static void letGo(ServerWorld world, UUID workerId) {
        List<Task> tasks = List.copyOf(VillageColonyMod.TASKS.assignedTo(workerId));

        VillageColonyMod.WORKERS.find(workerId)
                .ifPresent(worker -> tasks.forEach(task -> worker.rest(task.requiredCapability())));
        int released = WorkerJobs.dropAll(workerId);

        VillageColonyMod.LOGGER.info("Worker {} waited 5 minutes on {} without working — it lets go ({} task(s)"
                        + " requeued) and helps the village (Regra 128)", workerId.toString().substring(0, 8),
                tasks.stream().map(task -> task.type() + " " + task.targetResource()).toList(), released);

        long now = world.getTime();

        for (Task task : tasks) {
            if (task.type() != TaskType.BUILD && task.priority() != TaskPriority.CONSTRUCTION_MATERIAL
                    && isSecondTime(task, now)) {
                appear(world, workerId, task);
            }
        }
    }

    /** Se esta tarefa (colônia e produto) já foi largada há pelo menos {@link #APPEAR_AFTER}. */
    static boolean isSecondTime(Task task, long now) {
        String key = task.colonyId() + "|" + task.type() + "|" + task.targetResource();
        Long first = FIRST_LET_GO.get(key);

        if (first == null || now - first >= FORGET_AFTER) {
            FIRST_LET_GO.put(key, now);
            return false;
        }

        if (now - first < APPEAR_AFTER) {
            return false;
        }

        FIRST_LET_GO.remove(key);
        return true;
    }

    private static void appear(ServerWorld world, UUID workerId, Task task) {
        Optional<Item> item = MinecraftTypeAdapter.toItem(task.targetResource());
        Optional<com.villagecolony.core.type.ColonyPos> chest =
                VillageColonyMod.STORAGES.of(workerId).map(WorkerStorage::chestPosition);

        if (item.isEmpty() || chest.isEmpty()) {
            return;
        }

        int count = Math.max(1, Math.min(task.amount(), MAX_APPEARING));
        int placed = count - ChestDepositor.deposit(world, chest.get(), item.get(), count);

        if (placed > 0) {
            Supplies.record(SupplyException.WAITED_TOO_LONG, task.colonyId(),
                    Registries.ITEM.getId(item.get()).getPath(), placed);
            VillageColonyMod.LOGGER.info("Colony {} — {} x{} appeared in the chest of {}: the task waited"
                            + " 10 minutes (Regra 128)", task.colonyId().toString().substring(0, 8), item.get(), placed,
                    workerId.toString().substring(0, 8));
        }
    }

    /** Quantos segundos este trabalhador soma esperando com tarefa, para o painel da bancada. */
    static int waitingOf(UUID workerId) {
        return WAITING.getOrDefault(workerId, 0);
    }

    static void clearAll() {
        WAITING.clear();
        FIRST_LET_GO.clear();
    }
}
