package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O pastor ajuda na obra no tempo livre — pedido do autor, 2026-10-08: <i>"o pastor
 * não deve ficar ocioso; no tempo livre, ajudar o construtor para as obras nascerem
 * mais rápido, com mais um adicionando blocos"</i>. No playtest daquele dia ele
 * ficou 57% ocioso, com o rebanho cheio e a lã crescendo.
 *
 * <p>A obra é uma tarefa só ({@code ConstructionPlanner.ensureTask}); o ajudante
 * ganha uma segunda tarefa de obra, reservada direto para ele, e o
 * {@link BuilderWork} abre trabalho para toda tarefa de obra com executor — os dois
 * põem peças da mesma obra, cada um a próxima da fila. Só ajuda com o construtor
 * trabalhando e obra que ainda tenha {@link #MIN_REMAINING} peças; um ajudante por
 * colônia. <b>O ofício vem primeiro:</b> pedido de lã na fila e ele larga a obra.
 */
public final class BuildHelper {

    static {
        ServerMemory.register(BuildHelper.class, BuildHelper::clearAll);
    }

    /** Peças que a obra ainda precisa ter para valer a ajuda. */
    static final int MIN_REMAINING = 8;

    /** Tarefa de ajudante → o ajudante. */
    private static final Map<UUID, UUID> HELPERS = new HashMap<>();

    private BuildHelper() {
    }

    /** Uma vez por ciclo da colônia, depois do pastor e antes do construtor. */
    static void lendAHand(ServerWorld world, Colony colony) {
        UUID colonyId = colony.id();
        boolean woolWaiting = VillageColonyMod.TASKS.availableFor(colonyId).stream()
                .anyMatch(task -> task.type() == TaskType.COLLECT_WOOL);

        for (Iterator<Map.Entry<UUID, UUID>> it = HELPERS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, UUID> entry = it.next();
            Optional<Task> task = VillageColonyMod.TASKS.find(entry.getKey());

            if (task.isEmpty() || !task.get().isOpen()) {
                it.remove();
                continue;
            }

            if (!task.get().colonyId().equals(colonyId)) {
                continue;
            }

            // Solta pela obra (passo travado, peça sem apoio) ou chamado pela lã: a
            // tarefa de ajudante não fica na fila para outro pegar.
            if (task.get().state() == TaskState.AVAILABLE || woolWaiting) {
                task.get().cancel();
                it.remove();

                VillageColonyMod.LOGGER.info("Shepherd {} leaves the build — {}",
                        entry.getValue().toString().substring(0, 8),
                        woolWaiting ? "the flock needs shearing" : "the helping hand was released");
            }
        }

        if (woolWaiting || isHelpingIn(colonyId)) {
            return;
        }

        Optional<ConstructionProject> project = VillageColonyMod.CONSTRUCTIONS.openOf(colonyId);

        if (project.isEmpty()
                || project.get().state() != ConstructionState.BUILDING
                || project.get().remainingCount() < MIN_REMAINING
                || project.get().nextBlock().isEmpty()
                || !builderAtWork(colonyId)) {
            return;
        }

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colonyId)) {
            UUID id = worker.villagerId();

            if (worker.profession().filter(ProfessionType.SHEPHERD::equals).isEmpty()
                    || !VillageColonyMod.TASKS.assignedTo(id).isEmpty()
                    || !(world.getEntity(id) instanceof VillagerEntity villager) || !villager.isAlive()
                    || StrandedWorkers.isStranded(id)
                    || ShepherdHerding.isHerding(id)) {
                continue;
            }

            Task help = VillageColonyMod.TASKS.create(colonyId, TaskType.BUILD, TaskPriority.CONSTRUCTION,
                    ResourceType.OAK_PLANKS, project.get().remainingCount());

            help.reserveFor(id);
            HELPERS.put(help.id(), id);

            VillageColonyMod.LOGGER.info(
                    "Shepherd {} lends a hand at the build of {} — {} pieces left",
                    id.toString().substring(0, 8), project.get().blueprint().id(),
                    project.get().remainingCount());

            return;
        }
    }

    /** Se este aldeão está ajudando numa obra. */
    public static boolean isHelping(UUID workerId) {
        return HELPERS.containsValue(workerId);
    }

    private static boolean isHelpingIn(UUID colonyId) {
        return HELPERS.keySet().stream()
                .map(VillageColonyMod.TASKS::find)
                .flatMap(Optional::stream)
                .anyMatch(task -> task.colonyId().equals(colonyId));
    }

    /** O construtor está na obra: uma tarefa de obra em curso, com executor, que não é a do ajudante. */
    private static boolean builderAtWork(UUID colonyId) {
        return VillageColonyMod.TASKS.ofColony(colonyId).stream()
                .anyMatch(task -> task.type() == TaskType.BUILD
                        && (task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING)
                        && task.executor().isPresent()
                        && !HELPERS.containsKey(task.id()));
    }

    public static void clearAll() {
        HELPERS.clear();
    }
}
