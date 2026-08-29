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
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.OreVein;
import com.villagecolony.fabric.integration.MineMouth;
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

    /**
     * Alcance de braço, medido no espaço. O mesmo número do lenhador.
     *
     * <p><b>Era medido no plano, e isso era o E30.</b> Sem o {@code dy}
     * na conta, o mineiro cavava a mina inteira de pé na superfície,
     * furando o chão para baixo, e <b>nunca entrava nela</b>: a sessão de
     * 2026-08-26 o pegou quebrando pedra a nove blocos de altura,
     * {@code digging Pedra at 721, 54, 897, 9 blocks away, 1/6 ticks}.
     * Funcionava enquanto a escada descia debaixo dele, e morria quando
     * a galeria corria na horizontal — aí ele precisaria ter descido, e
     * ficava parado até o guarda devolver a tarefa.
     *
     * <p>Com o {@code dy}, a Regra 29 volta a valer como está escrita:
     * <i>"o mineiro anda até o fim da vila e desce cavando em escada,
     * para poder voltar a subir"</i>. Os degraus de dois blocos de altura
     * existem justamente para ele caber de pé lá dentro.
     */
    static final int REACH = MinerReach.REACH;

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
    static final int STALL_LIMIT = 4 * 600;

    static final Map<UUID, Job> JOBS = new HashMap<>();

    static final String SUBJECT = "miner";

    /**
     * O assunto da busca de areia, separado do da mineração.
     *
     * <p>A chave do {@link IdleLog} inclui o assunto, e é de propósito:
     * um mineiro sem tarefa e um mineiro que não acha areia são dois
     * silêncios diferentes, e um calaria o outro se dividissem a chave.
     */
    private static final String SAND_SUBJECT = "miner sand";

    /** A pedra em curso, e o quanto dela já saiu. */
    static final class Job {

        final Task task;

        final BlockPos center;

        final ResourceId wanted;

        /** A pedra de agora. Nulo entre uma e a próxima. */
        BlockPos target;

        /**
         * Onde ficar de pé para bater nela — calculado uma vez.
         *
         * <p>A busca do {@link #approachTo} custa umas seiscentas
         * leituras de bloco, e o destino é reposto a cada tique enquanto
         * ele caminha. Guardar é a diferença entre uma vez por pedra e
         * seiscentas leituras por tique por mineiro.
         */
        BlockPos approach;

        int progress;

        int required;

        int collected;

        int stalled;

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

        // A linha do ciclo — 2026-08-22. Ela não existia, e a sessão
        // daquele dia pagou por isso: dois mineiros com tarefa aberta
        // passaram treze minutos sem produzir <b>uma linha sequer</b>.
        // Ver MinerReport.
        MinerReport.report(world, colony);

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
            // Só conta tique de expediente — 2026-08-27, e é o molde do
            // lenhador, que esta classe segue de propósito. O guarda pune
            // quem anda sem chegar; fora da hora o aldeão está PROIBIDO
            // de andar, porque a GoToWorkTargetTask nem começa. A sessão
            // de 08-26 queimou metade do orçamento com ele dormindo: o
            // contador foi de 886 a 2086 com o relatório dizendo
            // "off hours".
            if (WorkHours.isWorkTime(world, villager)) {
                job.stalled++;
            }

            if (job.stalled >= STALL_LIMIT) {
                giveUp(world, workerId, job);
            } else {
                // O mesmo destino da primeira vez, e pelo mesmo motivo:
                // repor a pedra aqui era repor a rocha maciça, e a
                // navegação não tem como cumprir isso — ver approachTo.
                //
                // Guardado, e não recalculado: a busca custa umas
                // seiscentas leituras de bloco, e isto roda todo tique
                // enquanto ele caminha.
                //
                // E por pernas — 2026-08-28. Ver MinerReach.legTowards:
                // a navegação não traça um caminho de vinte blocos por
                // dentro da rocha, e ele ficava parado na superfície
                // acima da galeria.
                WorkTargets.set(
                        workerId,
                        MinerReach.legTowards(
                                villager.getBlockPos(), job.approach, mouthOf(job)));
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
        job.approach = approachTo(world, job.target);
        job.progress = 0;
        job.required = 0;
        job.stalled = 0;

        WorkTargets.set(workerId, job.approach);

        return true;
    }

    /**
     * Onde ficar de pé para bater nesta pedra — 2026-08-27.
     *
     * <p><b>Mandar o aldeão até a pedra era mandá-lo para dentro da
     * rocha.</b> Bloco sólido nunca é alcançável: a navegação devolve
     * caminho parcial, e ele estaciona onde parou.
     *
     * <p><b>Olhar só os vizinhos era pouco, e a Regra 29 é a prova.</b>
     * Um degrau da escada anda um para a frente e um para baixo:
     *
     * <pre>
     * degrau 1   (1, 64, 0)   onde ele está de pé
     * degrau 2   (2, 63, 0)   o alvo — DIAGONAL, não encosta em face nenhuma
     * </pre>
     *
     * <p>As seis faces não alcançam a diagonal, e o método caía no "fica
     * a própria pedra" já no segundo degrau. <b>E o aldeão alcançava o
     * tempo todo</b>: de pé no degrau 1 ele está a 1,1 bloco do centro do
     * degrau 2, e o braço dele é quatro. O lugar existia; a busca é que
     * não sabia procurá-lo.
     *
     * <p>Explica por que algumas sessões cavaram e outras não: a galeria
     * é reta, e blocos consecutivos dela <b>encostam</b>. Os onze blocos
     * da sessão das 22:23 foram todos de galeria; a escada e a frente do
     * túnel nunca saíram.
     *
     * <p><b>A busca é por distância, e não por ordem de face.</b> O
     * lugar mais perto do alvo é o que dá menos chance de o caminho ser
     * interrompido no meio. O cubo de raio quatro são umas seiscentas
     * leituras — caro para um tique, barato uma vez por pedra, e é uma
     * vez por pedra que ela roda: quem chama guarda o resultado.
     *
     * <p>Sem lugar nenhum ao alcance fica a própria pedra, que é o que
     * se fazia antes — pior destino, mas nunca pior que nenhum. Quem
     * trata esse caso é o guarda de travamento e o recuo da galeria.
     *
     * <p><b>"Cabe um aldeão" é uma pergunta só</b>, e quem responde é o
     * {@link BuilderApproach#standable}. Esta classe tinha a sua, mais
     * frouxa — pedia <i>qualquer coisa que não fosse ar</i> embaixo, e
     * água serve —, e a sessão da meia-noite as pegou discordando na
     * mesma linha de log: <i>"it was walking to 732,46,878, which is not
     * standable"</i>. Escolhedor e relator não podem responder diferente
     * à mesma pergunta; é a falha que a distância já tinha tido.
     */
    public static BlockPos approachTo(ServerWorld world, BlockPos target) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int dx = -MinerReach.REACH; dx <= MinerReach.REACH; dx++) {
            for (int dy = -MinerReach.REACH; dy <= MinerReach.REACH; dy++) {
                for (int dz = -MinerReach.REACH; dz <= MinerReach.REACH; dz++) {

                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    BlockPos at = target.add(dx, dy, dz);

                    double distance = MinerReach.distanceTo(
                            at.getX() + 0.5, at.getY(), at.getZ() + 0.5, target);

                    if (distance > MinerReach.REACH || distance >= nearestDistance) {
                        continue;
                    }

                    if (!BuilderApproach.standable(world, at)) {
                        continue;
                    }

                    nearest = at;
                    nearestDistance = distance;
                }
            }
        }

        return nearest == null ? target : nearest;
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

        // Regra 30: o minério que não é carvão vai para o baú da boca
        // da mina, e só transborda para o do mineiro quando aquele
        // lotar. Decidido aqui, com o bloco em mãos: no baú só
        // chegam itens, e minério cru não diz de que pedra veio.
        int took = MinerHaul.deposit(
                world, storage, drops, MinerHaul.treasureChestFor(world, job, state));

        job.collected += took;

        // A linha que faltava. Trabalho mudo não se diagnostica — é o
        // §11, e foi ele que custou quatro sessões à Fase 10.
        VillageColonyMod.LOGGER.info(
                "Miner {} took {} from {} — {} this task",
                villager.getUuid(),
                took,
                job.target.toShortString(),
                job.collected);

        release(villager.getUuid(), job);
    }

    /** Larga a pedra de agora e volta a procurar. */
    private static void release(UUID workerId, Job job) {
        job.target = null;
        job.approach = null;
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
    private static void giveUp(ServerWorld world, UUID workerId, Job job) {
        VillageColonyMod.LOGGER.info(
                "Miner {} could not reach the stone at {} — task back to the queue. {}",
                workerId,
                job.target.toShortString(),
                world.getEntity(workerId) instanceof VillagerEntity villager
                        ? MinerReport.whyNotReached(world, villager, job.target)
                        : "the miner left the world");

        job.task.release();

        // A posição volta para o cursor da galeria — 2026-08-27. Sem
        // isto o mod marchava pela ordem de cavar com o mundo intacto.
        MineDigging.couldNotReach(job.task.colonyId(), job.target);

        release(workerId, job);

        // O cursor da varredura de areia sai junto: sem isso a passagem
        // seguinte reencontraria exatamente a mesma areia inalcançável,
        // que é a roda que a Regra 9 fechou do lado do lenhador.
        SandGathering.forget(workerId);
    }

    static boolean isWithinReach(VillagerEntity villager, BlockPos target) {
        return MinerReach.isWithinReach(
                villager.getX(), villager.getY(), villager.getZ(), target);
    }

    /** A distância deste aldeão até aquela pedra. */
    static double distanceTo(VillagerEntity villager, BlockPos target) {
        return MinerReach.distanceTo(
                villager.getX(), villager.getY(), villager.getZ(), target);
    }

    /**
     * A boca da mina desta colônia, se ela tem uma.
     *
     * <p>Vazia para o mineiro de superfície e para o de areia: nenhum
     * dos dois tem descida a fazer, e mandá-los à boca seria um desvio.
     */
    private static Optional<BlockPos> mouthOf(Job job) {
        return VillageColonyMod.MINES.of(job.task.colonyId())
                .map(mine -> MinecraftTypeAdapter.toBlockPos(mine.shaft().entry()));
    }

    private static boolean isOngoing(Task task) {
        return task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING;
    }

    private static void dropClosedJobs() {
        JOBS.entrySet().removeIf(entry -> !isOngoing(entry.getValue().task));

        // A reserva da mina segue os trabalhos abertos, e é o que a
        // impede de vazar: nem todo fim de trabalho passa por um lugar
        // só, e mina trancada por um aldeão que já não existe é pior
        // que dois cavando a mesma escada.
        MineClaims.retainOnly(JOBS.keySet());
    }

    /** Esquece o trabalho deste aldeão. Morte, zumbificação, dispensa. */
    public static void forget(UUID workerId) {
        JOBS.remove(workerId);

        WorkTargets.clear(workerId);
        SandGathering.forget(workerId);

        // Na hora, e não no ciclo seguinte: morte e zumbificação passam
        // por aqui, e a mina não fica meio minuto fechada por causa de
        // um aldeão que já morreu.
        MineClaims.release(workerId);
    }

    /** Esvazia o registro. Chamado ao parar o servidor. */
    public static void clearAll() {
        JOBS.clear();
    }

    /** Quantos mineiros estão com trabalho aberto agora. */
    public static int activeJobs() {
        return JOBS.size();
    }

    /**
     * Quantos tiques este mineiro já andou sem chegar na pedra.
     *
     * <p>Não é estado novo — é o contador do guarda de travamento, lido
     * de fora, como o {@code BuildSiteScanner.sweepPausedAt}. Existe
     * porque a pergunta que ele responde não tem outro observável: o
     * guarda só fala quando estoura, e o defeito era ele <b>contar</b>
     * quando não devia.
     */
    public static int stallOf(UUID workerId) {
        Job job = JOBS.get(workerId);

        return job == null ? 0 : job.stalled;
    }

    /** Quanta pedra este mineiro já trouxe nesta tarefa. */
    public static int collectedBy(UUID workerId) {
        Job job = JOBS.get(workerId);

        return job == null ? 0 : job.collected;
    }
}
