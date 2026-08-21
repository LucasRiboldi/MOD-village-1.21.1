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
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.ChestDepositor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O mineiro: tira pedra do mundo e guarda no baú — 2026-08-20.
 *
 * <p><b>Por que ele existe.</b> Duas coisas travavam por falta dele. A
 * casa de planície pede 43 pedregulhos, e a Regra 24 os deixou por conta
 * do jogador: sem ele guardar pedra num baú, a obra dormia. E a vila de
 * deserto nascia, contratava, contava recurso e não construía nunca,
 * porque a única parede que a colônia sabia fazer era de madeira e ali
 * não há árvore.
 *
 * <p>Qual pedra ele tira é decisão da {@code VillagePalette}: pedregulho
 * onde há rocha, arenito no deserto — que ali é a própria parede.
 *
 * <p><b>O que ele nunca toca</b> é a Regra 3, e para pedra ela morde
 * mais que para árvore: a vila gerada e as casas do jogador são feitas
 * do mesmo material que ele cava. A conferência é feita a cada posição da
 * mina, antes de qualquer picareta.
 *
 * <p>O desenho é o do lenhador, e de propósito — despacho por ciclo,
 * trabalho por tique, um guarda de travamento para quem anda sem chegar.
 * O que muda é que não há plano de árvore: um bloco de cada vez, e a
 * busca recomeça depois de cada um.
 */
public final class MinerWork {

    /** Alcance de braço, medido no plano. O mesmo do lenhador. */
    private static final int REACH = 4;

    /**
     * A picareta de diamante, e é decisão do autor.
     *
     * <p>A Regra 2 mede a velocidade pela ferramenta de um jogador; o
     * autor pediu diamante para o mineiro, e faz sentido: são vinte
     * blocos de descida antes de a mina render alguma coisa, e com
     * picareta de madeira isso é uma sessão inteira.
     */
    private static final Item TOOL = Items.DIAMOND_PICKAXE;

    private static final int BREAKING_STAGES = 10;

    private static final int SWING_INTERVAL = 5;

    /** Uma busca por tique no servidor inteiro, como a de árvore. */
    private static final int SEARCHES_PER_TICK = 1;

    /**
     * Quantos tiques de expediente sem avanço antes de largar a pedra.
     *
     * <p>Mesma razão do guarda do lenhador: um aldeão que anda para
     * sempre sem chegar não está trabalhando, e sem isto a tarefa nunca
     * volta para a fila.
     */
    private static final int STALL_LIMIT = 4 * 600;

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    private static final String SUBJECT = "miner";

    /**
     * O assunto da busca de areia, separado do da mineração.
     *
     * <p>A chave do {@link IdleLog} inclui o assunto, e é de propósito:
     * um mineiro sem tarefa e um mineiro que não acha areia são dois
     * silêncios diferentes, e um calaria o outro se dividissem a chave.
     */
    private static final String SAND_SUBJECT = "miner sand";

    /** A pedra em curso, e o quanto dela já saiu. */
    private static final class Job {

        private final Task task;

        private final BlockPos center;

        private final ResourceId wanted;

        /** A pedra de agora. Nulo entre uma e a próxima. */
        private BlockPos target;

        private int progress;

        private int required;

        private int collected;

        private int stalled;

        private Job(Task task, BlockPos center, ResourceId wanted) {
            this.task = task;
            this.center = center;
            this.wanted = wanted;
        }
    }

    private MinerWork() {
    }

    /**
     * Despacho, uma vez por ciclo da colônia.
     *
     * @return quantos mineiros desta colônia estão com trabalho aberto
     */
    public static int run(ServerWorld world, Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());

        ResourceId wanted = HousePlans.paletteOf(world, colony.center()).stone();

        int open = 0;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() != TaskType.COLLECT_STONE || !isOngoing(task)) {
                continue;
            }

            Optional<UUID> executor = task.executor();

            if (executor.isEmpty()) {
                continue;
            }

            JOBS.computeIfAbsent(executor.get(), worker -> new Job(task, center, wanted));

            open++;
        }

        dropClosedJobs();

        if (open == 0) {
            reportIdle(colony);
        } else {
            IdleLog.clear(colony.id(), SUBJECT);
        }

        return open;
    }

    /** Por que não houve trabalho de mineração, uma vez por motivo. */
    private static void reportIdle(Colony colony) {
        int able = WorkAssignment.countCapableOf(
                colony.id(), TaskType.COLLECT_STONE.required(), VillageColonyMod.WORKERS);

        if (able == 0) {
            IdleLog.record(colony.id(), SUBJECT, IdleReason.NO_WORKER);

            return;
        }

        IdleLog.record(colony.id(), SUBJECT, IdleReason.NO_TASK, able + " able to mine");
    }

    /** Um tique de trabalho para cada mineiro com pedra em mãos. */
    public static void tick(ServerWorld world) {
        if (JOBS.isEmpty()) {
            return;
        }

        int searches = SEARCHES_PER_TICK;

        for (Iterator<Map.Entry<UUID, Job>> entries = JOBS.entrySet().iterator();
                entries.hasNext(); ) {

            Map.Entry<UUID, Job> entry = entries.next();

            if (!isOngoing(entry.getValue().task)) {
                entries.remove();

                continue;
            }

            if (step(world, entry.getKey(), entry.getValue(), searches > 0)) {
                searches--;
            }
        }
    }

    /**
     * Um passo.
     *
     * @return se esta passagem gastou uma busca do orçamento do tique
     */
    private static boolean step(
            ServerWorld world, UUID workerId, Job job, boolean maySearch) {

        Entity entity = world.getEntity(workerId);

        if (!(entity instanceof VillagerEntity villager)) {
            return false;
        }

        Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(workerId);

        if (storage.isEmpty()) {
            return false;
        }

        if (job.target == null) {
            if (!maySearch) {
                return false;
            }

            return startNextStone(world, workerId, job, villager);
        }

        BlockState state = world.getBlockState(job.target);

        if (state.isAir()) {
            // Alguém tirou a pedra entre planejar e chegar. Procura outra.
            release(workerId, job);

            return false;
        }

        if (!isWithinReach(villager, job.target)) {
            job.stalled++;

            if (job.stalled >= STALL_LIMIT) {
                giveUp(workerId, job);
            } else {
                WorkTargets.set(workerId, job.target);
            }

            return false;
        }

        mine(world, villager, job, storage.get());

        return false;
    }

    /**
     * Acha o próximo bloco, reserva-o e manda o aldeão andar até lá.
     *
     * <p><b>Dois caminhos, e quem decide é o recurso da tarefa.</b> Pedra
     * está em toda parte abaixo do chão e se busca descendo a escada da
     * Regra 29; areia mora na praia e na duna, e a vinte blocos não há
     * nenhuma fora do deserto. A mesma profissão, duas geografias.
     *
     * <p>A geometria de cada um saiu daqui em 2026-08-21 — ver
     * {@link MineDigging} e {@link SandGathering}. O que ficou é o que os
     * dois compartilham, que é o trabalho em si: a picareta, o baú, o
     * guarda de travamento e a tarefa.
     *
     * @return se esta passagem gastou uma busca do orçamento do tique
     */
    private static boolean startNextStone(
            ServerWorld world, UUID workerId, Job job, VillagerEntity villager) {

        UUID colonyId = job.task.colonyId();

        Optional<BlockPos> found =
                job.task.targetResource().group() == ResourceGroup.SAND
                        ? SandGathering.nextTarget(world, workerId, colonyId, job.center)
                        : MineDigging.nextTarget(world, workerId, colonyId, job.center);

        if (found.isEmpty()) {
            return true;
        }

        job.target = found.get();
        job.progress = 0;
        job.required = 0;
        job.stalled = 0;

        WorkTargets.set(workerId, job.target);

        return true;
    }

    /** Quebra a pedra em curso, no tempo que ela pede. */
    private static void mine(
            ServerWorld world, VillagerEntity villager, Job job, WorkerStorage storage) {

        BlockState state = world.getBlockState(job.target);

        if (job.required == 0) {
            job.required = BlockBreakTime.ticksFor(world, job.target, state, TOOL);
        }

        job.progress++;

        if (job.progress % SWING_INTERVAL == 1) {
            villager.swingHand(Hand.MAIN_HAND);
        }

        if (job.progress < job.required) {
            world.setBlockBreakingInfo(
                    villager.getId(), job.target, job.progress * BREAKING_STAGES / job.required);

            return;
        }

        world.setBlockBreakingInfo(villager.getId(), job.target, -1);

        List<ItemStack> drops = new ArrayList<>(
                Block.getDroppedStacks(state, world, job.target, null, null, ItemStack.EMPTY));

        world.removeBlock(job.target, false);

        int took = deposit(world, storage, drops);

        job.collected += took;

        // A linha que faltava. Trabalho mudo não se diagnostica — é o
        // §11, e foi ele que custou quatro sessões à Fase 10.
        VillageColonyMod.LOGGER.info(
                "Miner {} took {} from {} — {} this task",
                villager.getUuid(),
                took,
                job.target,
                job.collected);

        release(villager.getUuid(), job);
    }

    /**
     * Guarda o que caiu no baú do mineiro.
     *
     * <p>O que não couber é perdido, e é o mesmo E3 do lenhador: o bloco
     * já saiu do mundo. Fica em WARN para não sumir em silêncio.
     *
     * @return quantas peças entraram
     */
    private static int deposit(
            ServerWorld world, WorkerStorage storage, List<ItemStack> drops) {

        ColonyPos chest = storage.chestPosition();
        int stored = 0;

        for (ItemStack drop : drops) {
            // Devolve quantos **não** couberam, e não quantos entraram.
            // Ler ao contrário foi o defeito que este mineiro cometeu no
            // primeiro teste dele: todo pedregulho guardado virava uma
            // linha de "filled up" com o baú vazio ao lado.
            int leftOver = ChestDepositor.deposit(
                    world, chest, drop.getItem(), drop.getCount());

            stored += drop.getCount() - leftOver;

            if (leftOver > 0) {
                VillageColonyMod.LOGGER.warn(
                        "Chest of miner at {} filled up — {} of {} lost",
                        chest,
                        leftOver,
                        drop.getCount());
            }
        }

        return stored;
    }

    /** Larga a pedra de agora e volta a procurar. */
    private static void release(UUID workerId, Job job) {
        job.target = null;
        job.progress = 0;
        job.required = 0;
        job.stalled = 0;

        WorkTargets.clear(workerId);
    }

    /**
     * Devolve a tarefa quando o mineiro não chega à pedra.
     *
     * <p>O cursor da busca é esquecido junto: sem isso a passagem
     * seguinte reencontraria exatamente a mesma pedra inalcançável, que é
     * a roda que a Regra 9 fechou do lado do lenhador.
     */
    private static void giveUp(UUID workerId, Job job) {
        VillageColonyMod.LOGGER.info(
                "Miner {} could not reach the stone at {} — task back to the queue",
                workerId,
                job.target);

        job.task.release();

        release(workerId, job);

        // O cursor da varredura de areia sai junto: sem isso a passagem
        // seguinte reencontraria exatamente a mesma areia inalcançável,
        // que é a roda que a Regra 9 fechou do lado do lenhador.
        SandGathering.forget(workerId);
    }

    private static boolean isWithinReach(VillagerEntity villager, BlockPos target) {
        double dx = villager.getX() - (target.getX() + 0.5);
        double dz = villager.getZ() - (target.getZ() + 0.5);

        return dx * dx + dz * dz <= REACH * REACH;
    }

    private static boolean isOngoing(Task task) {
        return task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING;
    }

    private static void dropClosedJobs() {
        JOBS.entrySet().removeIf(entry -> !isOngoing(entry.getValue().task));
    }

    /** Esquece o trabalho deste aldeão. Morte, zumbificação, dispensa. */
    public static void forget(UUID workerId) {
        JOBS.remove(workerId);

        WorkTargets.clear(workerId);
        SandGathering.forget(workerId);
    }

    /** Esvazia o registro. Chamado ao parar o servidor. */
    public static void clearAll() {
        JOBS.clear();
    }

    /** Quantos mineiros estão com trabalho aberto agora. */
    public static int activeJobs() {
        return JOBS.size();
    }

    /** Quanta pedra este mineiro já trouxe nesta tarefa. */
    public static int collectedBy(UUID workerId) {
        Job job = JOBS.get(workerId);

        return job == null ? 0 : job.collected;
    }
}
