package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O fundidor: areia vira vidro — 2026-08-20.
 *
 * <p><b>A exceção honesta, enfim resolvida.</b> A Regra 10 registrou em
 * 2026-08-18 que a vidraça era o único material da casa que a colônia
 * não conseguiria fazer: ela pede vidro, vidro pede fundir, e ninguém
 * ali fundia. Ficou escrito que "enquanto não houver forno, o vidro é
 * material que o jogador guarda no baú". Agora há quem funda.
 *
 * <p>Quem sabe o que a fornalha faz é o próprio jogo — {@code
 * CraftingLookup.smelted} pergunta ao livro de receitas dele. Escrever a
 * tabela aqui a faria envelhecer no primeiro datapack.
 *
 * <p><b>Sem forno, e é decisão.</b> O aldeão não acende fogo nem carrega
 * combustível: ele transforma o que está no baú, do mesmo jeito que o
 * fabricante transforma tronco em tábua sem bancada. Pôr forno de
 * verdade no mundo é escrever bloco fora da planta, e a Regra 3 tem
 * opinião sobre isso.
 *
 * <p><b>O que falta desta cadeia</b>, e está dito para não passar por
 * pronto: ninguém <b>colhe areia</b> ainda. O fundidor funde a areia que
 * houver nos baús da colônia — do jogador, ou do mineiro no dia em que
 * ele receber meta de areia. Sem areia, ele diz que não tem e para.
 */
public final class SmelterWork {

    /** Quantos tiques uma peça leva para fundir. */
    private static final int TICKS_PER_PIECE = 20;

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    private static final String SUBJECT = "smelter";

    /** O que este fundidor está fundindo, e há quanto tempo. */
    private static final class Job {

        private final Task task;

        private int progress;

        private int smelted;

        private Job(Task task) {
            this.task = task;
        }
    }

    private SmelterWork() {
    }

    /**
     * Despacho, uma vez por ciclo da colônia.
     *
     * @return quantos fundidores desta colônia estão com trabalho aberto
     */
    public static int run(ServerWorld world, Colony colony) {
        int open = 0;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() != TaskType.SMELT_MATERIAL || !isOngoing(task)) {
                continue;
            }

            Optional<UUID> executor = task.executor();

            if (executor.isEmpty()) {
                continue;
            }

            JOBS.computeIfAbsent(executor.get(), worker -> new Job(task));

            open++;
        }

        JOBS.entrySet().removeIf(entry -> !isOngoing(entry.getValue().task));

        if (open == 0) {
            reportIdle(colony);
        } else {
            IdleLog.clear(colony.id(), SUBJECT);
        }

        return open;
    }

    /** Por que não houve fundição, uma vez por motivo. */
    private static void reportIdle(Colony colony) {
        int able = WorkAssignment.countCapableOf(
                colony.id(), TaskType.SMELT_MATERIAL.required(), VillageColonyMod.WORKERS);

        if (able == 0) {
            IdleLog.record(colony.id(), SUBJECT, IdleReason.NO_WORKER);

            return;
        }

        IdleLog.record(colony.id(), SUBJECT, IdleReason.NO_TASK, able + " able to smelt");
    }

    /** Um tique de fundição para cada fundidor com trabalho aberto. */
    public static void tick(ServerWorld world) {
        if (JOBS.isEmpty()) {
            return;
        }

        JOBS.entrySet().removeIf(entry -> {
            Job job = entry.getValue();

            if (!isOngoing(job.task)) {
                return true;
            }

            if (++job.progress < TICKS_PER_PIECE) {
                return false;
            }

            job.progress = 0;

            return !smeltOne(world, entry.getKey(), job);
        });
    }

    /**
     * Funde uma peça do que houver nos baús da colônia.
     *
     * <p>Os baús são percorridos na ordem de registro dos trabalhadores,
     * como o fabricante fazia antes de {@code ColonyChests} existir. É
     * dívida conhecida e está no backlog: o certo é por distância, e
     * somando entre baús.
     *
     * @return se ainda há o que fundir
     */
    private static boolean smeltOne(ServerWorld world, UUID workerId, Job job) {
        for (Worker worker : VillageColonyMod.WORKERS.ofColony(job.task.colonyId())) {
            Optional<WorkerStorage> owned = VillageColonyMod.STORAGES.of(worker.villagerId());

            if (owned.isEmpty()) {
                continue;
            }

            ColonyPos chest = owned.get().chestPosition();

            List<ItemStack> raw = ChestWithdrawer.withdrawGroup(
                    world, chest, ResourceGroup.SAND, 1);

            if (raw.isEmpty()) {
                continue;
            }

            return convert(world, chest, raw.get(0), job, workerId);
        }

        finish(job, workerId, "nothing in the colony chests to smelt");

        return false;
    }

    /** Põe a peça fundida de volta no baú de onde a crua saiu. */
    private static boolean convert(
            ServerWorld world, ColonyPos chest, ItemStack raw, Job job, UUID workerId) {

        Optional<ItemStack> result = CraftingLookup.smelted(world, raw);

        if (result.isEmpty()) {
            // O forno não faz nada com isto. Devolve intacto: tirar do
            // baú e não devolver seria a colônia destruindo material.
            ChestDepositor.deposit(world, chest, raw.getItem(), raw.getCount());

            finish(job, workerId, "the furnace makes nothing out of " + raw.getItem());

            return false;
        }

        ItemStack made = result.get();

        int leftOver = ChestDepositor.deposit(world, chest, made.getItem(), made.getCount());

        if (leftOver == made.getCount()) {
            // Não coube nada. Devolve a matéria-prima e para: fundir sem
            // onde guardar gasta o ingrediente à toa.
            ChestDepositor.deposit(world, chest, raw.getItem(), raw.getCount());

            finish(job, workerId, "no room in the chest for " + made.getItem());

            return false;
        }

        job.smelted += made.getCount() - leftOver;

        VillageColonyMod.LOGGER.info(
                "Smelter {} made {} out of {} — {} this task",
                workerId,
                made.getItem(),
                raw.getItem(),
                job.smelted);

        return true;
    }

    private static void finish(Job job, UUID workerId, String why) {
        VillageColonyMod.LOGGER.info("Smelter {} stopped — {}", workerId, why);

        job.task.release();
    }

    private static boolean isOngoing(Task task) {
        return task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING;
    }

    /** Esquece o trabalho deste aldeão. */
    public static void forget(UUID workerId) {
        JOBS.remove(workerId);
    }

    /** Esvazia o registro. Chamado ao parar o servidor. */
    public static void clearAll() {
        JOBS.clear();
    }

    /** Quantas peças este fundidor já fez nesta tarefa. */
    public static int smeltedBy(UUID workerId) {
        Job job = JOBS.get(workerId);

        return job == null ? 0 : job.smelted;
    }
}
