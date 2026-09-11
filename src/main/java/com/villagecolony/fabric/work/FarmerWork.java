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
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.CropPatch;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

    /**
     * O que o fazendeiro foi fazer neste alvo — 2026-09-05.
     *
     * <p>Ele só colhia, e por isso ficava parado: a lavoura da vila é
     * pequena, e depois de colhida não há nada maduro por muito tempo —
     * 86 ciclos ociosos de 81 na sessão de 2026-09-04.
     *
     * <p><b>Arar não está aqui, e saiu por queixa do autor</b> — <i>"não
     * podem arar qualquer lugar"</i>. Quem abre roça é a obra, com a
     * planta do próprio jogo e num lote livre dentro da vila; o
     * fazendeiro cuida da lavoura que existe.
     */
    private enum Chore {

        /** Lavoura madura: colher e replantar do que caiu. */
        HARVEST,

        /** Canteiro arado e vazio: plantar semente do baú. */
        SOW
    }

    private static final class Job {

        final Task task;

        final BlockPos center;

        BlockPos target;

        Chore chore = Chore.HARVEST;

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

    /**
     * Até onde o fazendeiro enxerga lavoura, a partir do centro da vila.
     *
     * <p><b>É a mesma medida que limita onde a roça pode nascer</b> —
     * 2026-09-05, e ela é publicada para que as duas concordem por
     * construção. A primeira roça da colônia nasceu a 105 blocos do
     * centro, na ponta de uma estrada nova, e o fazendeiro nunca a viu:
     * duas constantes separadas discordando é exatamente o defeito que
     * o {@code isOpenSpace} da mina já tinha tido.
     */
    public static int reach() {
        return searchRadius;
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

    /**
     * Um passo de cada fazendeiro, a cada tique do servidor.
     *
     * <p><b>Tarefa que já voltou para a fila não se trabalha</b> —
     * 2026-09-05, e a falta desta linha derrubou o servidor:
     *
     * <pre>
     * java.lang.IllegalStateException: Cannot release a task that is AVAILABLE
     *   at Task.release(Task.java:201)
     *   at FarmerWork.giveUp(FarmerWork.java:360)
     * </pre>
     *
     * <p>O laço é o do {@code giveUp}: o guarda de imobilidade dispara,
     * a tarefa volta para a fila, o {@link #release} zera o alvo — e
     * <b>não</b> o contador de imobilidade, que é o E36 de 2026-09-04 e
     * está certo. Na passagem seguinte o fazendeiro escolhe outra
     * lavoura, continua sem andar, e o guarda dispara de novo: segundo
     * {@code release} numa tarefa que já está AVAILABLE.
     *
     * <p>O {@code dropClosedJobs} tiraria o trabalho da lista, só que ele
     * roda uma vez por ciclo da colônia — seiscentos tiques — e o guarda
     * dispara em trezentos. Cabem dois na janela.
     *
     * <p><b>As outras seis profissões já tinham esta linha</b>, e é a
     * única razão de ter sido o fazendeiro a cair. Ver
     * {@code MinerWork.tick}, de onde a forma foi copiada.
     */
    public static void tick(ServerWorld world) {
        for (Iterator<Map.Entry<UUID, Job>> entries = JOBS.entrySet().iterator();
                entries.hasNext(); ) {

            Map.Entry<UUID, Job> entry = entries.next();

            if (!isOngoing(entry.getValue().task)) {
                entries.remove();

                // O destino morre com a tarefa — ver WorkTargets.clear.
                WorkTargets.clear(entry.getKey());

                continue;
            }

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
            findWork(world, workerId, job, storage.get());

            return;
        }

        if (!stillWorth(world, job, storage.get())) {
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
        // que ele não está congelado; pegar alvo novo não é. Ver findWork.
        job.stall.reset();

        switch (job.chore) {
            case HARVEST -> harvest(world, villager, job, storage.get());
            case SOW -> sow(world, villager, job, storage.get());
        }
    }

    /**
     * Se o alvo ainda vale a caminhada.
     *
     * <p>A pergunta é a do próprio trabalho, e é por isso que ela mora
     * aqui e não no {@code step}: colher pede lavoura madura, semear pede
     * canteiro vazio <b>e</b> semente no baú, arar pede a terra ainda de
     * pé. Perguntar sempre "está maduro?" mandaria o semeador embora na
     * primeira passagem.
     */
    private static boolean stillWorth(ServerWorld world, Job job, WorkerStorage storage) {
        return switch (job.chore) {
            case HARVEST -> CropPatch.isRipe(world.getBlockState(job.target));
            case SOW -> CropPatch.isEmptyPlot(world, job.target)
                    && ChestWithdrawer.seedIn(world, storage.chestPosition()).isPresent();
        };
    }

    /**
     * A lavoura madura mais perto do centro da vila.
     *
     * <p>Do centro para fora, e não do aldeão: a lavoura da vila é da
     * vila, e dois fazendeiros que buscassem cada um a partir de si
     * acabariam em cantos opostos do mesmo campo.
     */
    private static void findWork(
            ServerWorld world, UUID workerId, Job job, WorkerStorage storage) {

        UUID colonyId = job.task.colonyId();

        // <b>Volta inteira sem nada compra silêncio</b> — P1.5, 2026-09-11.
        // O motivo já foi dito quando a volta fechou; aqui não se fala de
        // novo, senão o descanso vira a enxurrada que ele evita. Ver
        // FieldRest para por que ele existe e por que é curto.
        if (FieldRest.isResting(colonyId, world.getTime())) {
            return;
        }

        CropPatch.Field field = CropPatch.survey(world, colonyId, job.center, searchRadius);

        Optional<BlockPos> found = field.ripe();
        Chore chore = Chore.HARVEST;

        if (found.isEmpty()) {
            // <b>Sem semente não há o que semear nem por que arar</b> —
            // 2026-09-05, e é este gate que dá teto ao campo sem uma
            // constante inventada. Os dois trabalhos gastam semente, e a
            // semente só sobra quando a colheita sobra: a roça cresce no
            // ritmo em que a lavoura paga por ela, e para de crescer
            // quando o baú seca.
            if (ChestWithdrawer.seedIn(world, storage.chestPosition()).isPresent()) {
                found = field.emptyPlot();
                chore = Chore.SOW;
            }
        }

        if (found.isEmpty()) {
            // <b>Pelo recordAt, e não pelo record</b> — o molde do P0.6.
            // Este método roda por tique e os dois motivos alternam por
            // construção: toda volta termina em NO_TARGET e a seguinte
            // recomeça em SWEEP_INCOMPLETE. A regra de transição sozinha
            // deixou 4.389 linhas num log de 6.117 na areia.
            //
            // E são dois motivos, não um: até 2026-09-11 esta linha saía
            // sempre como NO_TARGET, cujo texto é <i>"nothing to work on
            // in the whole radius"</i> — e o raio inteiro nunca tinha
            // sido olhado. O enum afirmava a cobertura que a varredura
            // truncada não entregava. Ver CropPatch#survey.
            IdleLog.recordAt(
                    colonyId,
                    SUBJECT,
                    field.incomplete() ? IdleReason.SWEEP_INCOMPLETE : IdleReason.NO_TARGET,
                    "nothing ripe and no empty plot within "
                            + searchRadius + " blocks of the village",
                    world.getTime());

            if (!field.incomplete()) {
                FieldRest.sweptAndFoundNothing(colonyId, world.getTime());
            }

            return;
        }

        // Achou: o campo voltou a render, e o descanso não vale mais.
        FieldRest.thereIsWorkAgain(colonyId);

        IdleLog.clear(colonyId, SUBJECT);

        job.target = found.get();
        job.chore = chore;
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
     * Planta a semente do baú no canteiro — 2026-09-05.
     *
     * <p><b>A semente sai antes de a muda entrar, e volta se não
     * entrar.</b> É a mesma conta do fundidor: tirar do baú e não
     * entregar nada seria a colônia destruindo material do jogador.
     */
    private static void sow(
            ServerWorld world, VillagerEntity villager, Job job, WorkerStorage storage) {

        Optional<Item> seed = ChestWithdrawer.seedIn(world, storage.chestPosition());

        if (seed.isEmpty()
                || !ChestWithdrawer.takeSeed(world, storage.chestPosition(), seed.get())) {

            release(villager.getUuid(), job);

            return;
        }

        villager.swingHand(Hand.MAIN_HAND);

        if (CropPatch.sow(world, job.target, seed.get())) {
            VillageColonyMod.LOGGER.info(
                    "Farmer {} sowed {} at {}",
                    villager.getUuid(),
                    seed.get(),
                    job.target.toShortString());
        } else {
            ChestDepositor.deposit(world, storage.chestPosition(), seed.get(), 1);
        }

        release(villager.getUuid(), job);
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
        // Ver findWork. Quem zera é o ramo de trabalho, antes do harvest.

        WorkTargets.clear(workerId);
    }

    /**
     * Devolve a tarefa quando o fazendeiro não chega à lavoura.
     *
     * <p><b>Quem responde se ela pode ser devolvida é a própria tarefa</b>
     * — {@link Task#isHeld()}. É a mesma lição que o {@code BuilderWork}
     * registrou depois de derrubar o servidor duas vezes, e o fazendeiro
     * a repetiu em 2026-09-05: uma tarefa que já voltou para a fila é
     * AVAILABLE, que não está encerrada e também não está na mão de
     * ninguém.
     *
     * <p>O laço que levava até aqui foi fechado no {@link #tick}, e este
     * guarda é a segunda linha: nem todo caminho que solta uma tarefa
     * passa pelo laço do tique — morte, zumbificação e dispensa soltam
     * pelo {@code VillagerLifecycleHandler}, e podem cair no meio de uma
     * passagem.
     */
    private static void giveUp(UUID workerId, Job job) {
        VillageColonyMod.LOGGER.info(
                "Farmer {} could not reach the crop at {} — task back to the queue",
                workerId,
                job.target.toShortString());

        if (job.task.isHeld()) {
            job.task.release();
        }

        // A desistência passa a contar — 2026-09-10. Ver WorkerStrikes:
        // até aqui o fazendeiro devolvia a tarefa sem deixar rastro no
        // trabalhador, e repetia o mesmo lote fora de alcance a cada
        // ciclo.
        WorkerStrikes.gaveUp(workerId, job.task);

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

        FieldRest.clearAll();

        restoreSearch();
    }

    /**
     * Esquece a varredura de uma colônia — o cursor e o descanso.
     *
     * <p>Existe para o teste de jogo, e é necessário: os dois estados são
     * por colônia, e o mundo do gametest é um só. Um descanso deixado
     * para trás faria o fazendeiro do teste seguinte não varrer o campo
     * que o teste acabou de plantar — e a falha apareceria no teste
     * errado.
     */
    public static void forgetColony(UUID colonyId) {
        FieldRest.forget(colonyId);
    }

    /** Quanto este fazendeiro já colheu nesta tarefa. */
    public static int collectedBy(UUID workerId) {
        Job job = JOBS.get(workerId);

        return job == null ? 0 : job.collected;
    }
}
