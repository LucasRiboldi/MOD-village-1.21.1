package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.ConstructionOutcome;
import com.villagecolony.core.construction.model.SkipReason;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.ColonySupply;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import net.minecraft.block.Block;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O construtor levanta a casa — TASK-034 e TASK-035.
 *
 * <p>É o primeiro trabalho do mod que <b>acrescenta</b> bloco ao mundo. O
 * lenhador tira, o fabricante transforma; este põe. E é por isso que ele
 * é o mais perigoso dos três: um bloco posto no lugar errado é dano no
 * mundo do jogador, e dano que ninguém desfaz.
 *
 * <p>Três guardas contra isso, nesta ordem:
 *
 * <ol>
 *   <li>o lote já foi escolhido livre — {@code BuildSiteScanner} recusa
 *       terreno com qualquer coisa em cima;
 *   <li>nada é posto sobre bloco que não seja substituível: grama alta
 *       e flor saem, parede de ninguém sai;
 *   <li>o material sai do baú <b>antes</b> de o bloco entrar no mundo.
 *       Se não há material, não há bloco — a colônia não cria recurso
 *       (Construction-System.md §"Regras de Arquitetura").
 * </ol>
 *
 * <p><b>O ritmo.</b> Um bloco por segundo, como a Regra 2 fez com a
 * derrubada. A casa inteira leva uns dois minutos e meio, que é tempo de
 * ver acontecendo — e o custo por tick continua sendo um contador por
 * construtor.
 *
 * <p><b>Simplificação assumida: o material não viaja.</b> O construtor
 * tira do baú da colônia sem ir até ele. Fazer o contrário exigiria
 * carregar material entre o baú e a obra, e logística e transporte estão
 * declarados fora do MVP em MVP-Tasks.md. O que ele faz a pé é ir até a
 * obra.
 */
public final class BuilderWork {

    static {
        ServerMemory.register(BuilderWork.class, BuilderWork::clearAll);
    }

    /** Um bloco por segundo. Ver a Regra 2, que fez o mesmo com a derrubada. */
    static final int TICKS_PER_BLOCK = 20;

    /**
     * Quantos ticks andando sem chegar ao bloco antes de desistir.
     *
     * <p>Quatro ciclos da colônia. O lote fica na vila e a obra é
     * escolhida em beira de rua: dois minutos de horário de trabalho sem
     * cobrir essa distância não é lentidão, é construtor preso.
     *
     * <p>Sem isto a tarefa reservada não voltava para a fila enquanto o
     * trabalhador estivesse vivo, e uma obra podia ficar parada para
     * sempre com dono. Mesma regra e mesmo motivo de
     * {@code LumberjackWork.STALL_LIMIT}.
     */
    static final int STALL_LIMIT = 4 * VillageDetector.CYCLE_TICKS;

    /** Trabalho aberto, por construtor. */
    static final Map<UUID, Job> JOBS = new HashMap<>();

    static final class Job {

        final Task task;

        final UUID projectId;

        int progress;

        int placed;

        /**
         * Ticks de horário de trabalho andando sem chegar ao bloco da
         * vez. Zerado ao chegar. Ver {@link #STALL_LIMIT}.
         */
        int stalled;

        /**
         * Se ele saiu do lugar, e há quanto tempo não sai — 2026-09-03.
         *
         * <p>O guarda acima conta tique de expediente <b>indo até o
         * alvo</b> e nunca pergunta se o aldeão andou. Ver {@link WorkStall}.
         */
        final WorkStall stall = new WorkStall();

        private Job(Task task, UUID projectId) {
            this.task = task;
            this.projectId = projectId;
        }
    }

    private BuilderWork() {
    }

    /**
     * Despacho, uma vez por ciclo da colônia.
     *
     * <p>Abre trabalho para toda tarefa de construção já reservada, e
     * fecha o de tarefa encerrada. Não põe bloco algum: quem põe é
     * {@link #tick}.
     *
     * @return quantos construtores desta colônia estão com trabalho aberto
     */
    public static int run(ServerWorld world, Colony colony) {
        Optional<ConstructionProject> project =
                VillageColonyMod.CONSTRUCTIONS.openOf(colony.id());

        if (project.isEmpty()) {
            return 0;
        }

        int open = 0;

        StringBuilder queue = new StringBuilder();

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() != TaskType.BUILD || !isOngoing(task)) {
                continue;
            }

            Optional<UUID> executor = task.executor();

            queue.append(queue.isEmpty() ? "" : "; ")
                    .append(task.state())
                    .append(executor.isEmpty()
                            ? " with nobody"
                            : " by " + executor.get().toString().substring(0, 8));

            if (executor.isEmpty()) {
                continue;
            }

            Job job = JOBS.computeIfAbsent(
                    executor.get(), worker -> new Job(task, project.get().id()));

            queue.append(BuilderReport.walking(job));

            open++;
        }

        JOBS.entrySet().removeIf(entry -> {
            if (isOngoing(entry.getValue().task)) {
                return false;
            }

            // O destino morre com a tarefa — ver WorkTargets.clear.
            WorkTargets.clear(entry.getKey());

            return true;
        });

        BuilderReport.report(
                colony,
                project.get(),
                open,
                queue.isEmpty() ? "no build task" : queue.toString(),
                BuilderReport.waitingFor(project.get()));

        return open;
    }

    /** Um passo de obra, a cada tick do servidor. */
    public static void tick(ServerWorld world) {
        if (JOBS.isEmpty()) {
            return;
        }

        for (Iterator<Map.Entry<UUID, Job>> entries = JOBS.entrySet().iterator();
                entries.hasNext(); ) {

            Map.Entry<UUID, Job> entry = entries.next();

            if (!step(world, entry.getKey(), entry.getValue())) {
                entries.remove();
            }
        }
    }

    /**
     * @return false quando este trabalho acabou e pode sair do registro
     */
    static boolean step(ServerWorld world, UUID workerId, Job job) {
        if (!isOngoing(job.task)) {
            WorkTargets.clear(workerId);
            return false;
        }

        Optional<ConstructionProject> found = VillageColonyMod.CONSTRUCTIONS.find(job.projectId);

        if (found.isEmpty() || !found.get().state().isOpen()) {
            finish(job, workerId, "the project is closed");

            return false;
        }

        ConstructionProject project = found.get();

        if (!(world.getEntity(workerId) instanceof VillagerEntity villager)) {
            // Aldeão fora de chunk carregado. A obra espera por ele.
            return true;
        }

        if (!WorkHours.isWorkTime(world, villager)) {
            return true;
        }

        // A reserved task is only the hand-off from the planner to the
        // villager.  The builder must enter execution before the final
        // block can complete the task instead of releasing it back to the
        // queue.
        if (job.task.state() == TaskState.RESERVED) {
            job.task.start();
        }

        Optional<BlueprintBlock> next = project.nextBlock();

        if (next.isEmpty()) {
            if (project.isFinished()) {
                complete(project, job, workerId);
            } else {
                finish(job, workerId, "every remaining piece is waiting for physical support");
            }

            return false;
        }

        BlockPos target = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(next.get()));

        if (!BuilderApproach.isWithinReach(villager.getBlockPos(), target)) {
            // <b>De onde ele está, e não do piso</b> — 2026-09-16. Ver
            // BuilderApproach.footOf: mandar ao piso quem já subiu na obra
            // é mandá-lo para uma queda que a navegação não percorre.
            WorkTargets.set(
                    workerId,
                    BuilderApproach.footOf(world, project, target, villager.getBlockPos()));

            if (job.stall.stuck(world, villager)) {
                // Parado no mesmo bloco há quinze segundos de expediente —
                // 2026-09-03. O guarda de baixo cobra dois minutos para
                // notar o mesmo, e o construtor congelado paga os dois
                // inteiros com a obra reservada em nome dele.
                // A desistência passa a contar — 2026-09-10. Só nos dois
                // guardas de caminhada, e não no "sem material no baú"
                // logo abaixo: falta de material é problema de
                // abastecimento da colônia, e tirar o construtor do
                // ofício por causa dela trocaria a obra parada por
                // ninguém sabendo construir. Ver WorkerStrikes.
                WorkerStrikes.gaveUp(workerId, job.task);

                finish(
                        job,
                        workerId,
                        "the builder has not moved a block in " + job.stall.ticks()
                                + " ticks of work time on the way to "
                                + target.toShortString() + " — "
                                + BuilderApproach.whyNotReached(
                                        world, project, villager, target));

                return false;
            }

            if (++job.stalled > STALL_LIMIT) {
                // Andou dois minutos de horário de trabalho e não chegou
                // ao bloco. A obra continua de pé e volta para a fila; o
                // que não continua é este construtor sendo dono dela.
                WorkerStrikes.gaveUp(workerId, job.task);

                finish(
                        job,
                        workerId,
                        "the builder could not reach " + target.toShortString()
                                + " — "
                                + BuilderApproach.whyNotReached(
                                        world, project, villager, target));

                return false;
            }

            return true;
        }

        job.stalled = 0;
        job.stall.reset();

        if (++job.progress < TICKS_PER_BLOCK) {
            return true;
        }

        job.progress = 0;

        return BuilderPlacement.placeOne(world, project, job, workerId, next.get(), target);
    }

    /**
     * A casa ficou de pé.
     *
     * <p><b>Inclusive quando a obra estava marcada como esperando</b> —
     * crash de 2026-09-05, às 21:06: {@code IllegalStateException: Cannot
     * go from WAITING_RESOURCES to COMPLETED}, e o servidor caiu.
     *
     * <p>A marca de espera é posta pelo {@link BuilderMaterials#waitForResources} e
     * <b>ninguém a tira ao assentar um bloco</b>: quem a tira é o
     * {@code WaitingWork.wakeIfSupplied}, que roda no ciclo da colônia.
     * Entre a espera e o ciclo seguinte o construtor pode acabar a obra
     * — os blocos que faltavam eram do chão, ou riscados pela barreira —,
     * e aí ele chegava aqui com a marca velha na mão.
     *
     * <p><b>Não há o que esperar quando não falta bloco</b>, e é por isso
     * que a saída é encerrar a espera em vez de afrouxar a máquina de
     * estados: {@code WAITING_RESOURCES → COMPLETED} continua proibido, e
     * continua certo que esteja.
     */
    static void complete(ConstructionProject project, Job job, UUID workerId) {
        if (project.state() == ConstructionState.WAITING_RESOURCES) {
            project.moveTo(ConstructionState.BUILDING);
        }

        project.moveTo(ConstructionState.COMPLETED);

        // Terminada: é a única porta por onde uma casa de verdade entra no
        // registro — ver HousePlans.hasNoHouseYet e Building.finished.
        Building building = Building.of(project, true);

        VillageColonyMod.BUILDINGS.registerOrMerge(building);

        VillageColonyMod.LOGGER.info(
                "Colony {} finished {} at {} — {} blocks placed by {}, now colony infrastructure",
                project.colonyId(),
                project.blueprint().id(),
                project.origin(),
                job.placed,
                workerId);

        finish(job, workerId, "the house is up");
    }

    /**
     * Encerra o trabalho deste construtor.
     *
     * <p>A tarefa é liberada e não completada quando a obra não acabou:
     * {@code Task.complete} exige EXECUTING, e chamá-lo fora disso
     * derrubou o servidor uma vez (§17). Quem termina a obra é a casa
     * pronta, não o trabalhador que parou.
     *
     * <p>E quem responde se ela pode ser liberada é a própria tarefa —
     * {@link Task#isHeld()}. Perguntar {@code isOngoing} aqui derrubou o
     * servidor <b>outra vez</b>, em 2026-08-25: a tarefa de um construtor
     * morto volta para a fila como AVAILABLE, que não está encerrada e
     * também não está na mão de ninguém.
     */
    static void finish(Job job, UUID workerId, String why) {
        if (job.task.state() == TaskState.EXECUTING) {
            job.task.complete();
        } else if (job.task.isHeld()) {
            job.task.release();
        }

        WorkTargets.clear(workerId);

        VillageColonyMod.LOGGER.info("Builder {} stopped — {}", workerId, why);
    }

    /**
     * Esquece o trabalho deste construtor. Morte, zumbificação, dispensa.
     *
     * <p>Chamado de dois lugares, e nos dois a tarefa já pode ter voltado
     * para a fila antes — {@code VillagerLifecycleHandler} solta tudo o
     * que o morto tinha antes de avisar quem o empregava. Daí a pergunta
     * ser {@link Task#isHeld()}, e não "não está encerrada".
     */
    public static void forget(UUID workerId) {
        Job job = JOBS.remove(workerId);

        if (job != null && job.task.isHeld()) {
            job.task.release();
        }
    }

    /** Esquece tudo. Usado ao descarregar o mundo. */
    public static void clearAll() {
        JOBS.clear();
    }

    static boolean isOngoing(Task task) {
        return task.state() != TaskState.COMPLETED && task.state() != TaskState.CANCELLED;
    }

}
