package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import net.minecraft.block.Block;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
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

    /** Um bloco por segundo. Ver a Regra 2, que fez o mesmo com a derrubada. */
    private static final int TICKS_PER_BLOCK = 20;

    /** De quão perto o construtor precisa estar do bloco que põe. */
    private static final int REACH = 5;

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
    private static final int STALL_LIMIT = 4 * VillageDetector.CYCLE_TICKS;

    /** Trabalho aberto, por construtor. */
    private static final Map<UUID, Job> JOBS = new HashMap<>();

    private static final class Job {

        private final Task task;

        private final UUID projectId;

        private int progress;

        private int placed;

        /**
         * Ticks de horário de trabalho andando sem chegar ao bloco da
         * vez. Zerado ao chegar. Ver {@link #STALL_LIMIT}.
         */
        private int stalled;

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

            JOBS.computeIfAbsent(executor.get(), worker -> new Job(task, project.get().id()));

            open++;
        }

        JOBS.values().removeIf(job -> !isOngoing(job.task));

        report(
                colony,
                project.get(),
                open,
                queue.isEmpty() ? "no build task" : queue.toString(),
                waitingFor(project.get()));

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
    private static boolean step(ServerWorld world, UUID workerId, Job job) {
        if (!isOngoing(job.task)) {
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

        Optional<BlueprintBlock> next = project.nextBlock();

        if (next.isEmpty()) {
            complete(project, job, workerId);

            return false;
        }

        BlockPos target = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(next.get()));

        if (!villager.getBlockPos().isWithinDistance(target, REACH)) {
            WorkTargets.set(workerId, target);

            if (++job.stalled > STALL_LIMIT) {
                // Andou dois minutos de horário de trabalho e não chegou
                // ao bloco. A obra continua de pé e volta para a fila; o
                // que não continua é este construtor sendo dono dela.
                finish(job, workerId, "the builder could not reach " + target.toShortString());

                return false;
            }

            return true;
        }

        job.stalled = 0;

        if (++job.progress < TICKS_PER_BLOCK) {
            return true;
        }

        job.progress = 0;

        return placeOne(world, project, job, workerId, next.get(), target);
    }

    /**
     * Põe um bloco, se houver material e lugar.
     *
     * @return false quando a obra não pode continuar agora
     */
    private static boolean placeOne(
            ServerWorld world,
            ConstructionProject project,
            Job job,
            UUID workerId,
            BlueprintBlock block,
            BlockPos target) {

        Optional<Block> material = MinecraftTypeAdapter.toBlock(block.block());

        if (material.isEmpty()) {
            // O jogo não conhece este bloco — datapack que saiu, versão
            // que mudou. Riscar é melhor que travar a obra para sempre.
            VillageColonyMod.LOGGER.warn(
                    "Project {} asks for {}, which this game does not have — skipped",
                    project.id(),
                    block.block());

            project.markPlaced(block);

            return true;
        }

        BlockState state = material.get().getDefaultState();

        if (!world.getBlockState(target).isReplaceable()) {
            // Já tem coisa ali, e não é grama alta: pode ser peça de
            // vila, pode ser construção do jogador. A Regra 3 manda não
            // mexer, e a obra segue sem este bloco.
            VillageColonyMod.LOGGER.info(
                    "Project {} skips {} — {} is in the way",
                    project.id(),
                    target,
                    world.getBlockState(target).getBlock());

            project.markPlaced(block);

            return true;
        }

        if (!state.canPlaceAt(world, target)) {
            // Tocha sem parede, porta sem chão. Riscar em vez de tentar
            // de novo: a ordem de baixo para cima já deu a esta posição a
            // melhor chance que ela teria, e insistir é obra que não
            // termina nunca.
            VillageColonyMod.LOGGER.info(
                    "Project {} skips {} at {} — nothing holds it",
                    project.id(),
                    block.block(),
                    target);

            project.markPlaced(block);

            return true;
        }

        if (!takeMaterial(world, project, material.get().asItem())) {
            waitForResources(project, job, workerId, block);

            return false;
        }

        world.setBlockState(target, state, Block.NOTIFY_ALL);

        placeSecondHalf(world, target, state);

        project.markPlaced(block);

        job.placed++;

        return true;
    }

    /**
     * Completa um bloco que ocupa dois lugares.
     *
     * <p>É o E8 do §17, e a outra ponta de
     * {@code StructureBlueprintReader.isSecondHalf}: o projeto guarda uma
     * porta só, e é aqui que ela vira duas metades <b>ligadas</b> em vez
     * de dois blocos independentes no estado padrão.
     *
     * <p>Escrever a segunda metade em vez de deixar o jogo fazê-lo é
     * deliberado. {@code Block.onPlaced} faria isso, e faria também tudo
     * o mais que a colocação por jogador dispara — som, evento, lógica de
     * item. O construtor não é um jogador com uma porta na mão; ele está
     * montando uma casa a partir de um arquivo, e o que ele precisa é da
     * propriedade que liga as duas metades.
     *
     * <p>Só escreve onde há lugar. A metade de cima cai sobre o que o
     * projeto já pôs no andar de cima em nenhum caso — a leitura descarta
     * aquela posição —, mas o mundo é do jogador e pode ter qualquer
     * coisa ali. A Regra 3 vale aqui como vale no resto da obra: nada
     * substitui o que não é substituível.
     *
     * <p>Bloco de uma parte só passa direto: {@code contains} responde
     * não, e nada acontece.
     */
    private static void placeSecondHalf(ServerWorld world, BlockPos pos, BlockState state) {
        if (state.contains(Properties.DOUBLE_BLOCK_HALF)) {
            put(world, pos.up(), state.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));

            return;
        }

        if (state.contains(Properties.BED_PART) && state.contains(Properties.HORIZONTAL_FACING)) {
            // A cabeceira vai para onde o estado padrão aponta, e não
            // para onde o arquivo dizia: a orientação é a metade do E8
            // que continua aberta (TASK-046). Uma cama virada para o
            // norte numa casa que a queria virada para o leste continua
            // sendo uma cama — dois pés lado a lado não eram.
            put(
                    world,
                    pos.offset(state.get(Properties.HORIZONTAL_FACING)),
                    state.with(Properties.BED_PART, BedPart.HEAD));
        }
    }

    /** Escreve, se o lugar aceitar. */
    private static void put(ServerWorld world, BlockPos pos, BlockState state) {
        if (!world.getBlockState(pos).isReplaceable()) {
            VillageColonyMod.LOGGER.info(
                    "Could not finish the two-part block at {} — {} is in the way",
                    pos,
                    world.getBlockState(pos).getBlock());

            return;
        }

        world.setBlockState(pos, state, Block.NOTIFY_ALL);
    }

    /**
     * Tira uma peça de material dos baús da colônia.
     *
     * <p>De qualquer baú dela, e não só do baú do construtor — a obra é
     * da colônia, não dele.
     *
     * <p>Era o contrário do fabricante, que tirava só do próprio baú. Em
     * 2026-08-14 deixou de ser: aquela intenção não sobrevivia ao mundo,
     * onde quem colhe deposita no baú dele e nada nunca enche o baú de um
     * fabricante. As duas profissões que consomem material da colônia
     * passaram a lê-lo do mesmo lugar.
     *
     * @return false quando não há esse material em baú nenhum
     */
    private static boolean takeMaterial(
            ServerWorld world, ConstructionProject project, Item material) {

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(project.colonyId())) {
            Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(worker.villagerId());

            if (storage.isEmpty()) {
                continue;
            }

            if (ChestWithdrawer.withdraw(world, storage.get().chestPosition(), material, 1) > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * O material do próximo bloco já está em algum baú da colônia?
     *
     * <p>Pergunta sem tirar nada, e existe para {@code ConstructionPlanner}
     * poder tirar a obra de {@code WAITING_RESOURCES}. A varredura é a
     * mesma de {@link #takeMaterial} — todos os baús de todos os
     * trabalhadores da colônia — porque as duas precisam concordar: uma
     * que dissesse "tem" e outra que não achasse poria a obra a acordar e
     * voltar a dormir todo ciclo.
     */
    public static boolean hasMaterialForNextBlock(
            ServerWorld world, ConstructionProject project) {

        Optional<BlueprintBlock> next = project.nextBlock();

        if (next.isEmpty()) {
            // Nada a pôr: a obra acabou e quem a fecha é o construtor.
            return true;
        }

        Optional<Block> material = MinecraftTypeAdapter.toBlock(next.get().block());

        if (material.isEmpty()) {
            // Bloco que este jogo não conhece. placeOne o risca e segue,
            // então acordar a obra é o certo — ela não vai travar nele.
            return true;
        }

        Item item = material.get().asItem();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(project.colonyId())) {
            Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(worker.villagerId());

            if (storage.isEmpty()) {
                continue;
            }

            if (ChestWithdrawer.countIn(world, storage.get().chestPosition(), item) > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Falta material: a obra espera, e a tarefa volta para a fila.
     *
     * <p>WAITING_RESOURCES é estado previsto (Construction-System.md), e
     * não defeito. Quem destrava é o ciclo da colônia, que vê a falta e
     * pede o que falta — e o jogador, que estoca o que a colônia não
     * produz.
     *
     * <p>Até 2026-08-15 essa frase descrevia uma intenção que nenhum
     * código cumpria: a única transição para {@code BUILDING} estava na
     * criação do projeto, e {@code ensureTask} não abre tarefa fora de
     * {@code BUILDING}. Na prática isto era estado terminal — a obra da
     * sessão das 19:44 estava parada em 149 blocos com 52 tábuas no baú.
     * Quem destrava de verdade é {@code ConstructionPlanner.plan}, com
     * {@link #hasMaterialForNextBlock}.
     */
    private static void waitForResources(
            ConstructionProject project, Job job, UUID workerId, BlueprintBlock block) {

        if (project.state() == ConstructionState.BUILDING) {
            project.moveTo(ConstructionState.WAITING_RESOURCES);
        }

        finish(job, workerId, "no " + block.block() + " in the colony chests");
    }

    /** A casa ficou de pé. */
    private static void complete(ConstructionProject project, Job job, UUID workerId) {
        project.moveTo(ConstructionState.COMPLETED);

        Building building = Building.of(project);

        VillageColonyMod.BUILDINGS.register(building);

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
     */
    private static void finish(Job job, UUID workerId, String why) {
        if (job.task.state() == TaskState.EXECUTING) {
            job.task.complete();
        } else if (isOngoing(job.task)) {
            job.task.release();
        }

        WorkTargets.clear(workerId);

        VillageColonyMod.LOGGER.info("Builder {} stopped — {}", workerId, why);
    }

    /** Esquece o trabalho deste construtor. Morte, zumbificação, dispensa. */
    public static void forget(UUID workerId) {
        Job job = JOBS.remove(workerId);

        if (job != null && isOngoing(job.task)) {
            job.task.release();
        }
    }

    /** Esquece tudo. Usado ao descarregar o mundo. */
    public static void clearAll() {
        JOBS.clear();
    }

    private static boolean isOngoing(Task task) {
        return task.state() != TaskState.COMPLETED && task.state() != TaskState.CANCELLED;
    }

    /**
     * Uma linha por ciclo, quando há obra.
     *
     * <p>Existe pelo mesmo motivo da linha do lenhador: sem ela, "a obra
     * não anda" e "não há obra" são indistinguíveis no log, e foi essa
     * cegueira que custou as sessões do §11.
     */
    private static void report(
            Colony colony,
            ConstructionProject project,
            int builders,
            String queue,
            String waiting) {

        VillageColonyMod.LOGGER.info(
                "Colony {} builders: {} working, {} at {}, {} blocks left — {}{}",
                colony.id(),
                builders,
                project.state(),
                project.origin(),
                project.remainingCount(),
                queue,
                waiting);
    }

    /**
     * O que a obra dormindo está esperando.
     *
     * <p>Vazio quando ela não está dormindo — a linha já é longa.
     *
     * <p>Existe por causa da sessão das 21:29 de 2026-08-15, a primeira
     * a rodar {@code wakeIfSupplied}. Ele se comportou como devia e não
     * acordou nada, porque o material do próximo bloco não estava em baú
     * algum. Só que o log dizia apenas {@code WAITING_RESOURCES ... no
     * build task}, e daí não sai a pergunta seguinte: <b>esperando o
     * quê?</b>
     *
     * <p>A resposta muda tudo. Se falta tábua, a colônia fabrica e a
     * casa anda sozinha. Se falta pedregulho ou vidro, ninguém nesta
     * vila produz aquilo — e a obra não está lenta, está impossível.
     * Dois estados idênticos no log, e correções que não se parecem.
     */
    private static String waitingFor(ConstructionProject project) {
        if (project.state() != ConstructionState.WAITING_RESOURCES) {
            return "";
        }

        return project.nextBlock()
                .map(block -> ", waiting for " + block.block())
                .orElse(", waiting with nothing left to place");
    }
}
