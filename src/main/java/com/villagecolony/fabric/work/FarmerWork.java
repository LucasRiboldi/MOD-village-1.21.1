package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.CropPatch;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O fazendeiro: colhe a lavoura madura, replanta e guarda —
 * 2026-08-27.
 *
 * <p><b>Das sete profissões, era a única sem trabalho.</b> A colônia lhe
 * dava enxada, baú e placa com o nome, e nunca mais falava com ele:
 * lenhador, mineiro, pastor, fundidor, fabricante e construtor buscavam
 * e guardavam desde a Fase 10; ele ficava parado no meio deles. Faltava
 * a corrente inteira — recurso, produção, tarefa e meta —, e ela foi
 * ligada no mesmo ciclo que esta classe.
 *
 * <p><b>Replantar é a regra, e ela já existia.</b> É a Regra 7 do
 * lenhador aplicada onde ela nasceu: colher sem replantar transforma a
 * lavoura da vila num campo de terra arada vazia, e a colônia comeria
 * uma vez só. A semente sai da própria colheita — trigo dá semente,
 * cenoura e batata se replantam a si mesmas —, então não custa estoque.
 *
 * <p><b>Só o que está maduro.</b> Colher lavoura verde é o aldeão
 * trocando uma comida por nenhuma: o jogo devolve a semente e mais nada.
 * Quem sabe se está madura é o próprio bloco, e é a ele que se pergunta
 * — ver {@link CropPatch}.
 *
 * <p>Molde do {@code ShepherdWork}, e de propósito: procurar, andar,
 * agir com um guarda de travamento, guardar no baú. O que muda é o que
 * se procura.
 */
public final class FarmerWork {

    /** Alcance de braço. O mesmo do pastor e do lenhador. */
    private static final int REACH = 3;

    /** Até onde ele procura lavoura em volta do centro da vila. */
    private static final int SEARCH_RADIUS = 32;

    /** O raio em vigor. Encurtado nos testes, como o do mineiro. */
    private static int searchRadius = SEARCH_RADIUS;

    /**
     * Quantos tiques de expediente sem chegar antes de largar a lavoura.
     *
     * <p>Só de expediente, e é o molde do lenhador: fora da hora o aldeão
     * está proibido de andar, e punir quem não pode andar foi o que
     * queimou meia sessão do mineiro em 08-26.
     */
    private static final int STALL_LIMIT = 4 * 600;

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    private static final String SUBJECT = "farmer";

    private static final class Job {

        final Task task;

        final BlockPos center;

        BlockPos target;

        int collected;

        int stalled;

        /**
         * Se ele saiu do lugar, e há quanto tempo não sai — 2026-09-03.
         *
         * <p>O guarda acima conta tique de expediente <b>indo até o
         * alvo</b> e nunca pergunta se o aldeão andou. Ver {@link WorkStall}.
         */
        final WorkStall stall = new WorkStall();

        private Job(Task task, BlockPos center) {
            this.task = task;
            this.center = center;
        }
    }

    private FarmerWork() {
    }

    /** Encurta a busca. Só para teste de jogo, como a do mineiro. */
    public static void shortenSearchTo(int blocks) {
        searchRadius = blocks;
    }

    /** Devolve a busca ao raio de verdade. */
    public static void restoreSearch() {
        searchRadius = SEARCH_RADIUS;
    }

    /**
     * Casa fazendeiro com tarefa de comida, e diz quantos trabalham.
     *
     * @return quantos fazendeiros têm trabalho nesta passagem
     */
    public static int run(ServerWorld world, Colony colony) {
        dropClosedJobs();

        int working = 0;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() != TaskType.COLLECT_FOOD || !isOngoing(task)) {
                continue;
            }

            Optional<UUID> worker = task.executor();

            if (worker.isEmpty()) {
                continue;
            }

            JOBS.computeIfAbsent(
                    worker.get(),
                    id -> new Job(task, MinecraftTypeAdapter.toBlockPos(colony.center())));

            working++;
        }

        if (working == 0) {
            reportIdle(colony);
        } else {
            IdleLog.clear(colony.id(), SUBJECT);
        }

        return working;
    }

    private static void reportIdle(Colony colony) {
        int able = WorkAssignment.countCapableOf(
                colony.id(), TaskType.COLLECT_FOOD.required(), VillageColonyMod.WORKERS);

        IdleLog.record(
                colony.id(),
                SUBJECT,
                able == 0 ? IdleReason.NO_WORKER : IdleReason.NO_TASK,
                able + " able to farm");
    }

    /** Um passo de cada fazendeiro, a cada tique do servidor. */
    public static void tick(ServerWorld world) {
        for (Map.Entry<UUID, Job> entry : new ArrayList<>(JOBS.entrySet())) {
            step(world, entry.getKey(), entry.getValue());
        }
    }

    private static void step(ServerWorld world, UUID workerId, Job job) {
        Entity entity = world.getEntity(workerId);

        if (!(entity instanceof VillagerEntity villager)) {
            return;
        }

        Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(workerId);

        if (storage.isEmpty()) {
            return;
        }

        if (job.target == null) {
            findCrop(world, workerId, job);

            return;
        }

        if (!CropPatch.isRipe(world.getBlockState(job.target))) {
            // Alguém colheu entre planejar e chegar, ou o bloco mudou.
            release(workerId, job);

            return;
        }

        if (!isWithinReach(villager, job.target)) {
            if (WorkHours.isWorkTime(world, villager)) {
                job.stalled++;
            }

            // Parado no mesmo bloco há quinze segundos de expediente —
            // 2026-09-03. Oito vezes mais rápido que o guarda de baixo, e
            // é o mesmo defeito. Ver WorkStall.
            if (job.stall.stuck(world, villager) || job.stalled >= STALL_LIMIT) {
                giveUp(workerId, job);
            }

            return;
        }

        // Chegou e vai colher — E36, 2026-09-04. Trabalhar é a prova de
        // que ele não está congelado; pegar alvo novo não é. Ver findCrop.
        job.stall.reset();

        harvest(world, villager, job, storage.get());
    }

    /**
     * A lavoura madura mais perto do centro da vila.
     *
     * <p>Do centro para fora, e não do aldeão: a lavoura da vila é da
     * vila, e dois fazendeiros que buscassem cada um a partir de si
     * acabariam em cantos opostos do mesmo campo.
     */
    private static void findCrop(ServerWorld world, UUID workerId, Job job) {
        Optional<BlockPos> found = CropPatch.ripeNear(world, job.center, searchRadius);

        if (found.isEmpty()) {
            IdleLog.record(
                    job.task.colonyId(),
                    SUBJECT,
                    IdleReason.NO_TARGET,
                    "no ripe crop within " + searchRadius + " blocks of the village");

            return;
        }

        IdleLog.clear(job.task.colonyId(), SUBJECT);

        job.target = found.get();
        job.stalled = 0;

        // <b>E o guarda de imobilidade NÃO é zerado aqui</b> — E36,
        // 2026-09-04. A pergunta que ele faz é <i>o aldeão saiu do
        // bloco?</i>, e ela não tem nada a ver com qual é o alvo: quem
        // estava congelado continua congelado depois de a pedra à frente
        // dele sumir. Zerar por alvo novo deixava <b>imune</b> quem troca
        // de alvo com frequência, e foi o que os mineiros travados da
        // sessão de 09-04 exibiram por vinte e cinco minutos com
        // {@code stall 0/2400, still 0/300} e nenhum passo dado.
        //
        // Quem zera é o movimento — o WorkStall vê sozinho — e o ramo em
        // que ele trabalha, que é o que o construtor e o fabricante
        // sempre fizeram. O de 2.400 continua por alvo, porque é isso que
        // ele mede: andei demais até ESTE alvo.

        WorkTargets.set(workerId, job.target);
    }

    /**
     * Colhe, replanta e guarda.
     *
     * <p>A ordem importa: a semente sai da própria colheita, então é
     * preciso ter o que caiu em mãos antes de replantar. O que sobra vai
     * para o baú.
     */
    private static void harvest(
            ServerWorld world, VillagerEntity villager, Job job, WorkerStorage storage) {

        BlockState state = world.getBlockState(job.target);

        villager.swingHand(Hand.MAIN_HAND);

        List<ItemStack> drops = new ArrayList<>(
                Block.getDroppedStacks(state, world, job.target, null, null, ItemStack.EMPTY));

        // Replantar antes de guardar — a Regra 7, onde ela nasceu. O
        // jogo devolve a semente junto com a comida, e ela sai do que
        // caiu em vez de sair do baú.
        boolean replanted = CropPatch.replant(world, job.target, state, drops);

        int took = store(world, storage, drops);

        job.collected += took;

        VillageColonyMod.LOGGER.info(
                "Farmer {} harvested {} at {} — {} this task, {}",
                villager.getUuid(),
                took,
                job.target.toShortString(),
                job.collected,
                replanted ? "replanted" : "nothing left to replant");

        release(villager.getUuid(), job);
    }

    /**
     * Guarda no baú do fazendeiro o que sobrou depois de replantar.
     *
     * <p>Devolve quantos <b>entraram</b>. O {@code ChestDepositor}
     * devolve quantos não couberam, e ler ao contrário foi o defeito que
     * o mineiro cometeu no primeiro teste dele — todo item guardado
     * virava uma linha de "baú cheio" com o baú vazio ao lado.
     */
    private static int store(
            ServerWorld world, WorkerStorage storage, List<ItemStack> drops) {

        int stored = 0;

        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }

            int leftOver = ChestDepositor.deposit(
                    world, storage.chestPosition(), drop.getItem(), drop.getCount());

            stored += drop.getCount() - leftOver;

            if (leftOver > 0) {
                VillageColonyMod.LOGGER.warn(
                        "Chest of farmer at {} filled up — {} of {} lost",
                        storage.chestPosition(),
                        leftOver,
                        drop.getCount());
            }
        }

        return stored;
    }

    private static boolean isWithinReach(VillagerEntity villager, BlockPos target) {
        return villager.getBlockPos().isWithinDistance(target, REACH);
    }

    /** Larga a lavoura de agora e volta a procurar. */
    private static void release(UUID workerId, Job job) {
        job.target = null;
        job.stalled = 0;

        // O guarda de imobilidade sobrevive a largar a lavoura — E36.
        // Ver findCrop. Quem zera é o ramo de trabalho, antes do harvest.

        WorkTargets.clear(workerId);
    }

    /** Devolve a tarefa quando o fazendeiro não chega à lavoura. */
    private static void giveUp(UUID workerId, Job job) {
        VillageColonyMod.LOGGER.info(
                "Farmer {} could not reach the crop at {} — task back to the queue",
                workerId,
                job.target.toShortString());

        job.task.release();

        release(workerId, job);
    }

    private static boolean isOngoing(Task task) {
        return task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING;
    }

    private static void dropClosedJobs() {
        JOBS.entrySet().removeIf(entry -> {
            if (isOngoing(entry.getValue().task)) {
                return false;
            }

            // O destino morre com a tarefa — ver WorkTargets.clear.
            WorkTargets.clear(entry.getKey());

            return true;
        });
    }

    /** Esquece o trabalho deste aldeão. Morte, zumbificação, dispensa. */
    public static void forget(UUID workerId) {
        JOBS.remove(workerId);

        WorkTargets.clear(workerId);
    }

    /** Esquece tudo. Chamado ao descarregar o mundo. */
    public static void clearAll() {
        JOBS.clear();

        restoreSearch();
    }

    /** Quanto este fazendeiro já colheu nesta tarefa. */
    public static int collectedBy(UUID workerId) {
        Job job = JOBS.get(workerId);

        return job == null ? 0 : job.collected;
    }
}
