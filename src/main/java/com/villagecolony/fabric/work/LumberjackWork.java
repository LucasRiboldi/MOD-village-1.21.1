package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.TreeHarvester;
import com.villagecolony.fabric.integration.TreeScanner;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O lenhador trabalhando — TASK-024, TASK-025 e as duas regras de
 * 2026-08-08.
 *
 * <p>Regra 1: ele colhe até os baús da colônia encherem. A tarefa não
 * termina numa árvore; termina quando não cabe mais madeira. Enquanto
 * couber, ele derruba uma árvore atrás da outra dentro da mesma tarefa.
 *
 * <p>Regra 2: ele quebra um bloco de cada vez, no tempo que um jogador
 * com machado de ferro levaria. A árvore que caía dentro de um tick agora
 * leva o que tem de levar — dez ticks por tronco, e o que a fórmula do
 * jogo disser por folha. Ver {@link BlockBreakTime}.
 *
 * <p>São as duas faces da mesma mudança: quanto colher, e em quanto
 * tempo. Juntas elas tiram o lenhador do regime de "uma árvore inteira
 * por ciclo, para sempre" e o põem em trabalho contínuo com um teto real.
 *
 * <p><b>Dois relógios.</b> {@link #run} roda no ciclo longo da colônia e
 * só faz o despacho: abrir trabalho para tarefa nova, fechar o de tarefa
 * encerrada. {@link #tick} roda a cada tick do servidor e é onde o
 * trabalho acontece. Separar os dois é o que mantém o custo por tick
 * pequeno — o tick avança um contador e, quando ele estoura, quebra um
 * bloco; a varredura por árvore, que é a parte cara, tem orçamento
 * próprio e nunca acontece duas vezes no mesmo tick.
 *
 * <p>As demais regras decididas em 2026-08-08 continuam valendo:
 * qualquer árvore da tabela de {@code TreeSpecies}, tronco e copa, muda
 * da própria espécie na base, nada cai no chão, e árvore que não caiba
 * no baú fica de pé.
 *
 * <p><b>O que saiu daqui em 2026-08-20</b>, quando este arquivo tinha
 * mil e duzentas linhas contra o teto de quinhentas. Eram cinco
 * perguntas independentes morando juntas:
 *
 * <pre>
 * {@link TreeChoice}         qual árvore agora, e quando desistir dela
 * {@link TreeFelling}        derrubar o bloco e guardar o que ele deu
 * {@link TreeMarks}          o que sai da escolha, e por quanto tempo
 * {@link TreeClaims}         as árvores que já têm dono
 * {@link LumberjackReport}   a linha que o lenhador deixa no log
 * </pre>
 *
 * <p>O que ficou é o laço: despachar por ciclo, andar por tique, decidir
 * a cada passo. {@code Job} fica aqui porque é o estado que o laço
 * carrega — os outros cinco o leem, e por isso ele é do pacote.
 */
public final class LumberjackWork {

    /** Até onde o lenhador procura árvore, a partir do centro. */
    public static final int SEARCH_RADIUS = 64;

    /**
     * De que distância ele consegue derrubar.
     *
     * <p>Alcance de braço, não de tiro: o trabalhador precisa chegar
     * perto. É o que faz a tarefa levar tempo em vez de acontecer no
     * instante em que é criada.
     *
     * <p>Medido contra a base da árvore, e não contra cada bloco. Quem
     * está ao pé do tronco alcança a copa dele; exigir aproximação por
     * folha poria o aldeão a dar voltas em torno da própria árvore.
     */
    static final int REACH = 4;

    /**
     * Quantas buscas por árvore cabem num tick, no servidor inteiro.
     *
     * <p>{@code TreeScanner} olha até mil colunas, e é de longe a coisa
     * mais cara deste arquivo. Uma por tick já é generosa: um lenhador
     * que acabou de derrubar uma árvore espera no máximo alguns ticks
     * pela próxima, e vinte lenhadores nunca pagam vinte varreduras no
     * mesmo tick. Sem este teto, a Regra 1 — que faz o lenhador querer
     * árvore nova assim que termina uma — transformaria trabalho
     * contínuo em varredura contínua.
     */
    private static final int SEARCHES_PER_TICK = 1;

    /**
     * O trabalho em curso de cada lenhador.
     *
     * <p>Em memória e não persistido, como a própria tarefa: ao
     * reiniciar o servidor a colônia repensa e reabre o trabalho. Um
     * contador por trabalhador é barato; é a varredura por trabalhador
     * por tick que não seria.
     */
    static final Map<UUID, Job> JOBS = new HashMap<>();

    /** Como esta profissão aparece na linha de {@link IdleLog}. */
    static final String SUBJECT = "lumberjack";

    private LumberjackWork() {
    }

    /** Uma árvore em curso, e onde ela está. */
    static final class Job {

        final Task task;

        /**
         * De onde parte a busca por árvore.
         *
         * <p>Guardado no despacho, quando a colônia está em mãos. O tick
         * não consulta o registro de colônias: ele roda sessenta vezes
         * por segundo, e o centro não muda entre um ciclo e outro.
         */
        final BlockPos center;

        /** A árvore de agora. Nulo entre uma árvore e a próxima. */
        TreeHarvester.Plan plan;

        /** Qual bloco do plano está sendo quebrado. */
        int index;

        /** Ticks já gastos neste bloco. */
        int progress;

        /** Ticks que este bloco pede. Zero enquanto não foi perguntado. */
        int required;

        /** Quantos troncos esta tarefa já rendeu, para a linha de log. */
        int collected;

        /**
         * Ticks de horário de trabalho desde o último avanço de verdade.
         *
         * <p>Zerado quando um bloco cai e quando uma árvore nova começa —
         * os dois únicos sinais de que o trabalho anda. Andar não conta:
         * é exatamente o aldeão que anda para sempre sem chegar que este
         * contador existe para pegar. Ver {@link #TreeChoice.STALL_LIMIT}.
         */
        int stalled;

        /**
         * Se ele saiu do lugar, e há quanto tempo não sai — 2026-09-03.
         *
         * <p>O guarda acima conta tique de expediente <b>indo até o
         * alvo</b> e nunca pergunta se o aldeão andou. Ver {@link WorkStall}.
         */
        final WorkStall stall = new WorkStall();

        Job(Task task, BlockPos center) {
            this.task = task;
            this.center = center;
        }

        boolean isBetweenTrees() {
            return plan == null || index >= plan.blocks().size();
        }

        BlockPos currentBlock() {
            return plan.blocks().get(index);
        }
    }

    /**
     * Despacho, uma vez por ciclo da colônia.
     *
     * <p>Abre trabalho para toda tarefa de madeira já reservada que
     * ainda não tenha, e fecha o de tarefa encerrada. Não derruba nada:
     * quem derruba é {@link #tick}.
     *
     * @return quantos lenhadores desta colônia estão com trabalho aberto
     */
    public static int run(ServerWorld world, Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        int open = 0;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (!isWoodTask(task) || !isOngoing(task)) {
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
            LumberjackReport.reportIdle(colony);
        } else {
            IdleLog.clear(colony.id(), SUBJECT);
        }

        LumberjackReport.report(world, colony);

        return open;
    }

    /**
     * Um passo de trabalho, a cada tick do servidor.
     *
     * <p>Cada lenhador avança o contador do bloco que está quebrando, e
     * quebra quando o contador estoura. Quem ainda está longe da árvore
     * apenas anda — o destino já foi escrito, e a task do Brain conduz.
     */
    public static void tick(ServerWorld world) {
        if (JOBS.isEmpty()) {
            return;
        }

        int searches = SEARCHES_PER_TICK;

        for (Iterator<Map.Entry<UUID, Job>> entries = JOBS.entrySet().iterator();
                entries.hasNext(); ) {

            Map.Entry<UUID, Job> entry = entries.next();
            UUID workerId = entry.getKey();
            Job job = entry.getValue();

            if (!isOngoing(job.task)) {
                closePlan(world, job);
                entries.remove();

                // O destino morre com a tarefa — ver WorkTargets.clear.
                WorkTargets.clear(workerId);

                continue;
            }

            Outcome outcome = step(world, workerId, job, searches > 0);

            if (outcome == Outcome.SEARCHED) {
                searches--;
            }

            if (outcome == Outcome.DONE) {
                closePlan(world, job);
                entries.remove();
            }
        }
    }

    /** O que um passo consumiu, para o tick saber o que fazer com ele. */
    enum Outcome {

        /** Nada de especial: andou, quebrou, ou esperou. */
        WORKED,

        /** Gastou a busca por árvore deste tick. */
        SEARCHED,

        /** O trabalho acabou e o registro pode esquecê-lo. */
        DONE
    }

    /**
     * Leva um lenhador um tick adiante.
     *
     * <p>Cada saída sem trabalho tem motivo próprio e nenhuma é erro:
     * trabalhador que morreu, aldeão fora de chunk carregado, baú
     * perdido, nenhuma árvore ao alcance. O trabalho fica onde está e o
     * tick seguinte tenta de novo.
     */
    private static Outcome step(ServerWorld world, UUID workerId, Job job, boolean maySearch) {
        Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(workerId);

        if (storage.isEmpty()) {
            // Sem baú não há onde guardar. Soltar a tarefa devolve o
            // trabalho a quem tenha baú, em vez de travar a fila.
            job.task.release();

            // E devolve o aldeão à agenda Vanilla: sem tarefa não há
            // por que ele continuar andando até a árvore.
            WorkTargets.clear(workerId);

            // Precisa aparecer. Este caminho solta a tarefa em silêncio
            // e ela volta para a fila, é reservada de novo no ciclo
            // seguinte e solta outra vez — um ciclo perpétuo que, do
            // lado de fora, parecia trabalho acontecendo. Ver a entrada
            // de 2026-08-12.
            VillageColonyMod.LOGGER.info(
                    "Worker {} has no chest — wood task returned to the queue",
                    workerId);

            return Outcome.DONE;
        }

        if (!(world.getEntity(workerId) instanceof VillagerEntity villager)) {
            // Aldeão fora de chunk carregado, que é o caso comum de
            // colônia longe do jogador. A tarefa espera por ele, e o
            // relógio de travamento não corre: colônia dormindo não é
            // trabalhador preso.
            return Outcome.WORKED;
        }

        // Parado no mesmo bloco há quinze segundos de expediente —
        // 2026-09-03. O guarda de baixo cobra dois minutos para notar o
        // mesmo. Ver WorkStall.
        if (job.stall.stuck(world, villager)) {
            return TreeChoice.giveUp(world, job, workerId);
        }

        if (WorkHours.isWorkTime(world, villager) && ++job.stalled > TreeChoice.stallLimit) {
            return TreeChoice.giveUp(world, job, workerId);
        }

        if (job.isBetweenTrees()) {
            return TreeChoice.startNextTree(world, villager, job, storage.get(), maySearch);
        }

        if (!villager.getBlockPos().isWithinDistance(job.plan.base(), REACH)) {
            TreeChoice.walkTo(villager, job.plan.base());

            return Outcome.WORKED;
        }

        TreeFelling.chop(world, villager, job, storage.get());

        return Outcome.WORKED;
    }

    /**
     * Só tarefa de madeira.
     *
     * <p>O filtro por {@link TaskType} sozinho não bastava:
     * {@code ColonyCycle.typeFor} manda todo recurso NATURAL para
     * {@code COLLECT_WOOD}, então uma meta de pedra fazia o lenhador
     * derrubar árvore para atendê-la. O recurso é que decide.
     */
    static boolean isWoodTask(Task task) {
        return task.type() == TaskType.COLLECT_WOOD
                && task.targetResource().group() == ResourceGroup.WOOD;
    }

    static boolean isOngoing(Task task) {
        return task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING;
    }

    /** Esquece o trabalho de quem já não tem tarefa aberta. */
    private static void dropClosedJobs() {
        JOBS.entrySet().removeIf(entry -> {
            if (isOngoing(entry.getValue().task)) {
                return false;
            }

            TreeClaims.unclaim(entry.getValue().plan);

                // O destino morre com a tarefa — ver WorkTargets.clear.
                WorkTargets.clear(entry.getKey());

            return true;
        });
    }

    /**
     * Devolve os troncos que este plano tinha reservado.
     *
     * <p>Chamado em toda saída — árvore terminada, tarefa encerrada,
     * trabalhador morto. Uma reserva esquecida é uma árvore que ninguém
     * mais pode cortar até o servidor reiniciar.
     */
    /**
     * Encerra o plano de um trabalho que acaba agora.
     *
     * <p>A regra do autor, de 2026-08-15: <b>o lenhador sempre planta no
     * lugar onde cortou.</b> Até aqui o replantio morava só em
     * {@link #startNextTree}, e acontecia quando o lenhador ia procurar a
     * árvore seguinte. Quem derrubasse uma árvore e perdesse o trabalho
     * antes disso deixava o toco sem muda, para sempre — e há três formas
     * de perdê-lo no mesmo tick: a tarefa cancelada, o baú que sumiu do
     * registro e o guarda de travamento.
     *
     * <p>Nenhuma delas é rara o bastante para deixar buraco na floresta,
     * e nenhuma delas aparecia: {@code unclaim} soltava o tronco
     * reservado e ia embora calado.
     *
     * <p>A conta é a dos <b>troncos</b>, e não a do plano inteiro.
     * {@code plan.blocks()} traz os troncos primeiro e a copa depois, e
     * quem manda na muda é o tronco: derrubado o último, o lugar onde a
     * árvore estava é chão livre, e a regra do autor diz para plantar.
     * Esperar a copa acabar deixaria sem muda justamente o trabalho
     * interrompido entre o último tronco e a última folha — que é a
     * janela onde este método existe para agir.
     *
     * <p>Com tronco de pé não se planta: a árvore ainda está ali, a muda
     * ficaria debaixo dela, e é a mesma recusa que
     * {@code TreeHarvester.finish} faz pelo outro caminho. Ela desce na
     * passagem seguinte e a muda entra com ela.
     */
    private static void closePlan(ServerWorld world, Job job) {
        if (job.plan != null && job.index >= job.plan.logs()) {
            TreeHarvester.finish(world, job.plan);
        }

        TreeClaims.unclaim(job.plan);
    }

    /**
     * Esquece o trabalho de um trabalhador que deixou de existir.
     *
     * <p>Chamado quando o aldeão morre ou vira zumbi. Sem isto o
     * registro guardaria um plano de árvore para um UUID que ninguém
     * mais vai encontrar.
     */
    public static void forget(UUID workerId) {
        Job job = JOBS.remove(workerId);

        if (job != null) {
            TreeClaims.unclaim(job.plan);
        }
    }

    /** Esvazia o registro, junto com o resto do estado em memória. */
    public static void clearAll() {
        JOBS.clear();
        TreeClaims.clearAll();

        TreeMarks.clearAll();
    }

    /** Quantos lenhadores estão com trabalho aberto agora. */
    public static int activeJobs() {
        return JOBS.size();
    }

    /**
     * As posições em curso de uma colônia, para os testes de jogo.
     *
     * <p>Somente leitura, e <b>por colônia</b>. A versão global durou
     * até 2026-08-19, quando três testes de lenhador passaram a falhar
     * um a cada três execuções: {@code JOBS} é estático e a bateria roda
     * testes concorrentes — um teste que atravessa noventa tiques
     * continua vivo enquanto o batch seguinte começa. Quem perguntava
     * "quantos lenhadores estão com árvore" recebia os do servidor
     * inteiro e afirmava sobre o vizinho. É a mesma regra que
     * {@code ColonyFixture} já escrevia do outro lado: nenhum teste
     * afirma sobre o que não criou.
     */
    public static List<BlockPos> blocksInProgress(UUID colonyId) {
        List<BlockPos> positions = new ArrayList<>();

        for (Job job : JOBS.values()) {
            if (!job.isBetweenTrees() && job.task.belongsTo(colonyId)) {
                positions.add(job.currentBlock());
            }
        }

        return positions;
    }
}
