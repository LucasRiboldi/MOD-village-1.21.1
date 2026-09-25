package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyEdits;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.MineFlooding;
import com.villagecolony.fabric.integration.OreVein;
import com.villagecolony.fabric.integration.MineMouth;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    static {
        ServerMemory.register(MinerWork.class, MinerWork::clearAll);
    }

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
     * Quantos blocos um aldeão sobe de um passo — E40, 2026-09-09.
     *
     * <p>Um. É o degrau do jogo, e não uma folga escolhida aqui: a
     * navegação sobe um bloco e não sobe dois. O relatório já dizia o
     * número desde 2026-08-29 — <i>"N blocks below it and unable to
     * climb"</i>, que só sai a partir de dois —, e faltava alguém
     * perguntar antes de mandar.
     *
     * <p>Público desde 2026-09-19: a bateria afirma a propriedade do
     * degrau, e escrever o número lá seria deixá-lo discordar daqui.
     */
    public static final int CLIMB = 1;

    static final int BREAKING_STAGES = 10;

    static final int SWING_INTERVAL = 5;

    /** Uma busca por tique no servidor inteiro, como a de árvore. */
    static final int SEARCHES_PER_TICK = 1;

    /**
     * Quantos tiques de expediente sem avanço antes de largar a pedra.
     *
     * <p>Mesma razão do guarda do lenhador: um aldeão que anda para
     * sempre sem chegar não está trabalhando, e sem isto a tarefa nunca
     * volta para a fila.
     */
    public static final int STALL_LIMIT = 4 * 600;

    /**
     * O mesmo número, no lugar onde ele passou a morar — 2026-09-03.
     *
     * <p>O detector de imobilidade nasceu aqui e foi para o
     * {@link WorkStall}, porque o buraco que ele tapa não era do mineiro:
     * era do <b>desenho</b>, e as cinco profissões que andam o têm igual.
     *
     * <p>Fica como apelido para não obrigar quem lê o relatório do mineiro
     * a saber onde a conta mora.
     */
    public static final int STILL_LIMIT = WorkStall.LIMIT;

    /**
     * Os trabalhos abertos, <b>na ordem em que foram despachados</b>.
     *
     * <p>Era {@code HashMap} até 2026-09-04. A ordem estável tornou a
     * contenção reproduzível, mas não justa: o primeiro trabalho sem alvo
     * podia consumir a única busca global por tique para sempre, mesmo
     * sem encontrar pedra. O cursor {@link #lastSearchWorker} agora gira
     * somente entre quem precisa buscar, sem mudar o limite global nem a
     * ordem dos mineiros que já têm alvo.
     *
     * <p>A ordem continua estável para reproduzir sessões; a concessão da
     * busca, porém, avança em rodízio para nenhum UUID ficar favorecido.
     */
    static final Map<UUID, Job> JOBS = new LinkedHashMap<>();

    /** Último mineiro que realmente recebeu o orçamento de busca. */
    static UUID lastSearchWorker;

    static final String SUBJECT = "miner";

    /**
     * O assunto da busca de areia, separado do da mineração.
     *
     * <p>A chave do {@link IdleLog} inclui o assunto, e é de propósito:
     * um mineiro sem tarefa e um mineiro que não acha areia são dois
     * silêncios diferentes, e um calaria o outro se dividissem a chave.
     */
    static final String SAND_SUBJECT = "miner sand";

    /** A pedra em curso, e o quanto dela já saiu. */
    static final class Job {

        final Task task;

        final BlockPos center;

        final ResourceType wanted;

        /** A pedra de agora. Nulo entre uma e a próxima. */
        BlockPos target;

        /**
         * Onde ficar de pé para bater nela — calculado uma vez.
         *
         * <p>A busca do {@link MinerApproach#approachTo} custa umas seiscentas
         * leituras de bloco, e o destino é reposto a cada tique enquanto
         * ele caminha. Guardar é a diferença entre uma vez por pedra e
         * seiscentas leituras por tique por mineiro.
         */
        BlockPos approach;

        int progress;

        int required;

        int collected;

        /**
         * Tiques esperando a areia assentar — 2026-09-19.
         *
         * <p>Mora no {@code Job} porque é ele que sobrevive entre tiques.
         * Zera quando o alvo muda: a paciência é <b>desta</b> queda, e
         * não do turno inteiro. Ver {@link MineSettling}.
         */
        int settling;

        /**
         * Quanto do recurso pedido já entrou no baú nesta tarefa.
         *
         * <p>Separado de {@link #collected} desde 2026-09-09, e é o E3:
         * {@code collected} conta <b>tudo</b> o que o mineiro guardou —
         * terra, carvão, minério —, e comparar isso com a meta de um
         * recurso só é comparar coisas diferentes. É este número que a
         * meta enfrenta, e é ele que encerra a tarefa.
         */
        int toward;

        int stalled;

        /** Se ele saiu do lugar, e há quanto tempo não sai. */
        final WorkStall stall = new WorkStall();

        /** Se ele está encurtando a distância até a pedra — E44. Ver MineLease. */
        final MineLease lease = new MineLease();

        Job(Task task, BlockPos center) {
            this.task = task;
            this.center = center;
            this.wanted = task.targetResource();
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

        int open = 0;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() != TaskType.COLLECT_STONE || !isOngoing(task)) {
                continue;
            }

            Optional<UUID> executor = task.executor();

            if (executor.isEmpty()) {
                continue;
            }

            JOBS.computeIfAbsent(executor.get(), worker -> new Job(task, center));

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
    static void reportIdle(Colony colony) {
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

        List<UUID> searchCandidates = new ArrayList<>();

        for (Iterator<Map.Entry<UUID, Job>> entries = JOBS.entrySet().iterator();
                entries.hasNext(); ) {
            Map.Entry<UUID, Job> entry = entries.next();
            UUID workerId = entry.getKey();
            Job job = entry.getValue();

            if (!isOngoing(job.task)) {
                entries.remove();

                // O destino morre com a tarefa — ver WorkTargets.clear.
                WorkTargets.clear(workerId);

                // A reserva também morre com a tarefa: esperar o ciclo
                // seguinte manteria o ramal fechado sem um job que o use.
                MineClaims.release(workerId);

                continue;
            }

            if (job.target == null) {
                searchCandidates.add(workerId);
            } else {
                MinerSteps.step(world, workerId, job, false);
            }
        }

        if (searchCandidates.isEmpty()) {
            return;
        }

        int start = searchStartIndex(searchCandidates, lastSearchWorker);

        for (int offset = 0; offset < searchCandidates.size(); offset++) {
            UUID workerId = searchCandidates.get((start + offset) % searchCandidates.size());
            Job job = JOBS.get(workerId);

            if (job != null && MinerSteps.step(world, workerId, job, SEARCHES_PER_TICK > 0)) {
                lastSearchWorker = workerId;

                return;
            }
        }
    }

    /** Índice do próximo candidato depois do mineiro que consumiu a busca. */
    static int searchStartIndex(List<UUID> candidates, UUID previousWorker) {
        if (candidates.isEmpty() || previousWorker == null) {
            return 0;
        }

        int previousIndex = candidates.indexOf(previousWorker);

        return previousIndex < 0 ? 0 : (previousIndex + 1) % candidates.size();
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
    static Optional<BlockPos> mouthOf(Job job) {
        return mineOf(job).map(mine -> MinecraftTypeAdapter.toBlockPos(mine.shaft().entry()));
    }

    /**
     * A mina desta colonia, se ela tem uma.
     *
     * <p>A mina inteira e nao so a boca, porque o passo de caminhada e
     * dado pela <b>ordem de cavar</b> desde 2026-08-29: e ela que sabe
     * onde o corredor passa. Ver {@link MinerLeg#legTowards}.
     */
    static Optional<Mine> mineOf(Job job) {
        return VillageColonyMod.MINES.of(job.task.colonyId());
    }

    /**
     * A boca da mina em que este mineiro está trabalhando.
     *
     * <p>Existe para o relatório — 2026-08-29. A linha precisa poder
     * dizer <i>"foi mandado à boca da mina"</i> por extenso, e não só em
     * coordenadas: a pergunta que a sessão de 08-28 deixou é <b>se a
     * perna do {@code legTowards} disparou</b>, e comparar números com a
     * linha de abertura da mina, dez minutos de log acima, é trabalho que
     * o instrumento deveria poupar.
     *
     * @return vazio para quem não tem trabalho aberto, e para o mineiro
     *     de superfície e o de areia — nenhum dos dois tem mina
     */
    static Optional<BlockPos> mouthFor(UUID workerId) {
        Job job = JOBS.get(workerId);

        return job == null ? Optional.empty() : mouthOf(job);
    }

    static boolean isOngoing(Task task) {
        return task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING;
    }

    static void dropClosedJobs() {
        JOBS.entrySet().removeIf(entry -> {
            if (isOngoing(entry.getValue().task)) {
                return false;
            }

            // O destino morre com a tarefa — ver WorkTargets.clear.
            WorkTargets.clear(entry.getKey());

            return true;
        });

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
        lastSearchWorker = null;

        // E as pedras de castigo — E44, 2026-09-10. Pelo mesmo motivo
        // que o LumberjackWork.clearAll leva o TreeMarks junto: o mapa é
        // estático e vive enquanto o servidor viver, o que em jogo é o
        // certo e numa bateria é o contrário — as áreas de teste são
        // reaproveitadas, e uma posição marcada por um teste reaparece
        // como rocha boa no seguinte.
        MineMarks.clearAll();
    }

}
