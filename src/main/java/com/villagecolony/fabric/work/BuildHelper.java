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
 * colônia.
 *
 * <p><b>Ajuda que conta:</b> depois de entrar, ele põe pelo menos {@link #MIN_PLACED}
 * blocos — ou a obra acaba — antes de voltar ao rebanho, mesmo com pedido de lã na
 * fila (pedido do autor, 2026-10-08: no playtest ele saía depois de 8 e 21 segundos).
 * Se a obra o soltar antes disso, a vaga volta para ele, até {@link #MAX_RETURNS} vezes.
 */
public final class BuildHelper {

    static {
        ServerMemory.register(BuildHelper.class, BuildHelper::clearAll);
    }

    /** Peças que a obra ainda precisa ter para valer a ajuda. */
    static final int MIN_REMAINING = 8;

    /** Tarefa de ajudante → o ajudante. */
    private static final Map<UUID, UUID> HELPERS = new HashMap<>();

    /** Blocos que ele põe antes de poder voltar ao rebanho. */
    static final int MIN_PLACED = 10;

    /** Quantas vezes a vaga volta para ele quando a obra o solta antes disso. */
    static final int MAX_RETURNS = 3;

    /** Ajudante → blocos postos nesta ajuda. */
    private static final Map<UUID, Integer> PLACED = new HashMap<>();

    /** Ajudante → quantas vezes a vaga voltou para ele. */
    private static final Map<UUID, Integer> RETURNS = new HashMap<>();

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
                // A obra acabou (ou a vaga fechou): a ajuda termina aqui.
                forgetHelper(entry.getValue());
                it.remove();
                continue;
            }

            if (!task.get().colonyId().equals(colonyId)) {
                continue;
            }

            UUID helper = entry.getValue();
            int placed = PLACED.getOrDefault(helper, 0);
            boolean didHisPart = placed >= MIN_PLACED;

            // Solta pela obra antes dos dez: a vaga volta para ele (até MAX_RETURNS).
            if (task.get().state() == TaskState.AVAILABLE && !didHisPart
                    && RETURNS.getOrDefault(helper, 0) < MAX_RETURNS) {
                RETURNS.merge(helper, 1, Integer::sum);
                task.get().reserveFor(helper);
                continue;
            }

            // Solta pela obra de vez, ou chamado pela lã depois de fazer a parte dele:
            // a tarefa de ajudante não fica na fila para outro pegar.
            boolean released = task.get().state() == TaskState.AVAILABLE;

            if (released || (woolWaiting && didHisPart)) {
                task.get().cancel();
                it.remove();
                forgetHelper(helper);

                VillageColonyMod.LOGGER.info("Shepherd {} leaves the build after {} blocks — {}",
                        helper.toString().substring(0, 8), placed,
                        released ? "the helping hand was released" : "the flock needs shearing");
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
            PLACED.put(id, 0);
            RETURNS.put(id, 0);

            VillageColonyMod.LOGGER.info(
                    "Shepherd {} lends a hand at the build of {} — {} pieces left",
                    id.toString().substring(0, 8), project.get().blueprint().id(),
                    project.get().remainingCount());

            return;
        }
    }

    /** Um bloco posto na obra por este aldeão; conta se ele é ajudante. */
    static void placed(UUID workerId) {
        PLACED.computeIfPresent(workerId, (helper, count) -> count + 1);
    }

    /** Quantos blocos o ajudante já pôs nesta ajuda. */
    static int placedBy(UUID helper) {
        return PLACED.getOrDefault(helper, 0);
    }

    private static void forgetHelper(UUID helper) {
        PLACED.remove(helper);
        RETURNS.remove(helper);
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
        PLACED.clear();
        RETURNS.clear();
    }
}
