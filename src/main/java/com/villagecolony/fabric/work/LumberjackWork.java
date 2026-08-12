package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.TreeHarvester;
import com.villagecolony.fabric.integration.TreeScanner;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

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
    private static final int REACH = 4;

    /**
     * A ferramenta que o tempo de quebra assume.
     *
     * <p>É a Regra 2 ao pé da letra: ferramenta de ferro. O aldeão não
     * carrega item nenhum — isto é o relógio da colheita, não um
     * inventário. Quando houver ferramenta de verdade, é esta linha que
     * passa a perguntar ao trabalhador o que ele tem na mão.
     */
    private static final Item TOOL = Items.IRON_AXE;

    /**
     * Quantos estágios de rachadura o cliente conhece.
     *
     * <p>Vanilla desenha de 0 a 9. Sem isso a Regra 2 seria invisível:
     * o jogador veria um aldeão parado ao lado de uma árvore que some
     * sozinha meio minuto depois.
     */
    private static final int BREAKING_STAGES = 10;

    /** De quantos em quantos ticks o braço balança. */
    private static final int SWING_INTERVAL = 5;

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
    private static final Map<UUID, Job> JOBS = new HashMap<>();

    private LumberjackWork() {
    }

    /** Uma árvore em curso, e onde ela está. */
    private static final class Job {

        private final Task task;

        /**
         * De onde parte a busca por árvore.
         *
         * <p>Guardado no despacho, quando a colônia está em mãos. O tick
         * não consulta o registro de colônias: ele roda sessenta vezes
         * por segundo, e o centro não muda entre um ciclo e outro.
         */
        private final BlockPos center;

        /** A árvore de agora. Nulo entre uma árvore e a próxima. */
        private TreeHarvester.Plan plan;

        /** Qual bloco do plano está sendo quebrado. */
        private int index;

        /** Ticks já gastos neste bloco. */
        private int progress;

        /** Ticks que este bloco pede. Zero enquanto não foi perguntado. */
        private int required;

        /** Quantos troncos esta tarefa já rendeu, para a linha de log. */
        private int collected;

        private Job(Task task, BlockPos center) {
            this.task = task;
            this.center = center;
        }

        private boolean isBetweenTrees() {
            return plan == null || index >= plan.blocks().size();
        }

        private BlockPos currentBlock() {
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
                entries.remove();

                continue;
            }

            Outcome outcome = step(world, workerId, job, searches > 0);

            if (outcome == Outcome.SEARCHED) {
                searches--;
            }

            if (outcome == Outcome.DONE) {
                entries.remove();
            }
        }
    }

    /** O que um passo consumiu, para o tick saber o que fazer com ele. */
    private enum Outcome {

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

            return Outcome.DONE;
        }

        if (!(world.getEntity(workerId) instanceof VillagerEntity villager)) {
            // Aldeão fora de chunk carregado, que é o caso comum de
            // colônia longe do jogador. A tarefa espera por ele.
            return Outcome.WORKED;
        }

        if (job.isBetweenTrees()) {
            return startNextTree(world, villager, job, storage.get(), maySearch);
        }

        if (!villager.getBlockPos().isWithinDistance(job.plan.base(), REACH)) {
            walkTo(villager, job.plan.base());

            return Outcome.WORKED;
        }

        chop(world, villager, job, storage.get());

        return Outcome.WORKED;
    }

    /**
     * Escolhe a próxima árvore, ou encerra a tarefa.
     *
     * <p>É aqui que a Regra 1 fecha o ciclo: enquanto couber madeira no
     * baú, há árvore nova; quando não couber, a tarefa termina. A
     * colônia reavalia no ciclo seguinte e só pede de novo se o espaço
     * tiver voltado — o jogador esvaziou o baú, um baú novo entrou no
     * registro.
     */
    private static Outcome startNextTree(
            ServerWorld world,
            VillagerEntity villager,
            Job job,
            WorkerStorage storage,
            boolean maySearch) {

        if (job.plan != null) {
            // A árvore anterior acabou de descer. Fechar antes de
            // procurar outra: é a ordem pedida pelo autor — derrubar,
            // recolher, e só então replantar.
            TreeHarvester.finish(world, job.plan);

            job.plan = null;
        }

        if (!maySearch) {
            return Outcome.WORKED;
        }

        Optional<BlockPos> tree = TreeScanner.findNearestLog(
                world, job.center, SEARCH_RADIUS);

        if (tree.isEmpty()) {
            // Nenhuma árvore ao alcance. Não é motivo para encerrar: a
            // floresta cresce, e a muda replantada volta a ser árvore.
            return Outcome.SEARCHED;
        }

        // Perguntar antes de derrubar. O tronco sai do mundo sem drop,
        // então madeira que não coubesse no baú seria madeira destruída:
        // a árvore sumiria e a colônia não ficaria com nada. Recolher
        // todos os recursos da árvore começa em não derrubar a árvore
        // que não se pode recolher.
        //
        // A conta é a do tronco, que é certa: um bloco, um item. O que a
        // folha dá é sorteado na hora — muda, maçã, graveto, ou nada — e
        // não dá para perguntar de antemão. São poucos itens, e o espaço
        // conferido para o tronco inteiro sobra para eles.
        int trunk = TreeHarvester.trunkSize(world, tree.get());
        int room = ChestDepositor.freeSpaceForGroup(
                world, storage.chestPosition(), ResourceGroup.WOOD);

        if (room < trunk) {
            finishTask(job, villager.getUuid(), storage, room);

            return Outcome.DONE;
        }

        TreeHarvester.Plan plan = TreeHarvester.plan(world, tree.get());

        if (plan.isEmpty()) {
            return Outcome.SEARCHED;
        }

        job.plan = plan;
        job.index = 0;
        job.progress = 0;
        job.required = 0;

        if (job.task.state() == TaskState.RESERVED) {
            job.task.start();
        }

        walkTo(villager, plan.base());

        return Outcome.SEARCHED;
    }

    /**
     * Um tick de machado no bloco da vez.
     *
     * <p>O contador sobe; quando alcança o que o bloco pede, o bloco cai
     * e o que ele deu vai direto para o baú. É o único ponto do mod que
     * escreve no mundo a cada tick, e escreve um bloco por lenhador —
     * o custo por tick tem de continuar cabendo num tick.
     */
    private static void chop(
            ServerWorld world, VillagerEntity villager, Job job, WorkerStorage storage) {

        BlockPos pos = job.currentBlock();
        BlockState state = stateAt(world, pos);

        if (state == null) {
            // Chunk descarregado no meio da colheita. Esperar é melhor
            // que pular: o bloco continua lá, e o aldeão também.
            return;
        }

        if (job.required == 0) {
            job.required = BlockBreakTime.ticksFor(world, pos, state, TOOL);
        }

        job.progress++;

        if (job.progress % SWING_INTERVAL == 1) {
            villager.swingHand(Hand.MAIN_HAND);
        }

        if (job.progress < job.required) {
            world.setBlockBreakingInfo(
                    villager.getId(), pos, job.progress * BREAKING_STAGES / job.required);

            return;
        }

        // Rachadura apagada antes de o bloco sair: um estágio deixado
        // para trás fica desenhado no ar até o cliente recarregar.
        world.setBlockBreakingInfo(villager.getId(), pos, -1);

        List<ItemStack> drops = TreeHarvester.breakOne(world, job.plan, pos);

        job.collected += countLogs(drops, job.plan);

        deposit(world, storage, drops);

        job.index++;
        job.progress = 0;
        job.required = 0;
    }

    /**
     * Encerra a tarefa por baú cheio.
     *
     * <p>É o fim previsto pela Regra 1, e não uma falha. A linha diz
     * quanto a tarefa rendeu porque é a única prova em jogo de que o
     * trabalho contínuo aconteceu — sem ela, um lenhador que trabalhou
     * dez minutos e um que nunca achou árvore produzem o mesmo silêncio.
     */
    private static void finishTask(Job job, UUID workerId, WorkerStorage storage, int room) {
        job.task.complete();

        // Tarefa cumprida, aldeão liberado. É a cessão imediata da
        // ADR-004 §5: sem destino, a task do Brain para e ele volta à
        // rotina Vanilla no mesmo tick.
        WorkTargets.clear(workerId);

        VillageColonyMod.LOGGER.info(
                "Worker {} filled the chest — {} logs collected, {} more would fit",
                storage.workerId(),
                job.collected,
                room);
    }

    /**
     * Põe no baú tudo o que o bloco deu.
     *
     * <p>Tronco, muda, maçã, graveto: o que a tabela de loot der. A
     * colônia só conta os troncos, e os outros ficam no baú sem contagem
     * — o que não é perda, é a regra de sempre: item fora da lista
     * continua no baú, apenas não é contado.
     */
    private static void deposit(
            ServerWorld world, WorkerStorage storage, List<ItemStack> drops) {

        for (ItemStack stack : drops) {
            int leftOver = ChestDepositor.deposit(
                    world, storage.chestPosition(), stack.getItem(), stack.getCount());

            if (leftOver == 0) {
                continue;
            }

            // O espaço do tronco foi conferido antes de derrubar, e o que
            // a folha dá é pouco. Chegar aqui significa baú quase cheio
            // ou alguém mexendo nele no meio da colheita — e precisa
            // aparecer, porque o item já saiu do mundo.
            VillageColonyMod.LOGGER.warn(
                    "Chest of worker {} filled up mid-harvest — {} of {} were lost",
                    storage.workerId(),
                    leftOver,
                    stack.getItem());
        }
    }

    /** Quantos troncos havia no que este bloco deu. */
    private static int countLogs(List<ItemStack> drops, TreeHarvester.Plan plan) {
        Item log = plan.species().log().asItem();
        int logs = 0;

        for (ItemStack stack : drops) {
            if (stack.isOf(log)) {
                logs += stack.getCount();
            }
        }

        return logs;
    }

    /**
     * Manda o aldeão andar até a árvore.
     *
     * <p>Escrever o destino em {@link WorkTargets} e deixar a
     * {@code GoToWorkTargetTask} conduzir. A versão anterior chamava
     * {@code getNavigation().startMovingTo} daqui e o aldeão nunca
     * chegou: o cérebro Vanilla reescrevia o destino no mesmo tick,
     * seguindo a agenda dele. Quem manda no caminho é o Brain, então o
     * pedido passou a ser feito na língua dele.
     *
     * <p>Reposto a cada tick enquanto ele estiver a caminho. Não é
     * desperdício: a leitura de 2026-08-08 mostrou o {@code WALK_TARGET}
     * já descartado pelo Vanilla no instante em que o aldeão chegava, e
     * é a reposição — não a primeira escrita — que faz o caminho
     * acontecer. Ver §17, E4.
     */
    private static void walkTo(VillagerEntity villager, BlockPos tree) {
        WorkTargets.set(villager.getUuid(), tree);
    }

    /**
     * O estado de um bloco, ou {@code null} se o chunk não está
     * carregado.
     *
     * <p>Nunca {@code world.getBlockState} direto. Ele carrega o chunk
     * que faltar, e do tick do servidor isso significa gerar terreno
     * dentro do laço. Ver §11.
     */
    private static BlockState stateAt(ServerWorld world, BlockPos pos) {
        WorldChunk chunk = world.getChunkManager()
                .getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);

        return chunk == null ? null : chunk.getBlockState(pos);
    }

    /**
     * Só tarefa de madeira.
     *
     * <p>O filtro por {@link TaskType} sozinho não bastava:
     * {@code ColonyCycle.typeFor} manda todo recurso NATURAL para
     * {@code COLLECT_WOOD}, então uma meta de pedra fazia o lenhador
     * derrubar árvore para atendê-la. O recurso é que decide.
     */
    private static boolean isWoodTask(Task task) {
        return task.type() == TaskType.COLLECT_WOOD
                && task.targetResource().group() == ResourceGroup.WOOD;
    }

    private static boolean isOngoing(Task task) {
        return task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING;
    }

    /** Esquece o trabalho de quem já não tem tarefa aberta. */
    private static void dropClosedJobs() {
        JOBS.values().removeIf(job -> !isOngoing(job.task));
    }

    /**
     * Esquece o trabalho de um trabalhador que deixou de existir.
     *
     * <p>Chamado quando o aldeão morre ou vira zumbi. Sem isto o
     * registro guardaria um plano de árvore para um UUID que ninguém
     * mais vai encontrar.
     */
    public static void forget(UUID workerId) {
        JOBS.remove(workerId);
    }

    /** Esvazia o registro, junto com o resto do estado em memória. */
    public static void clearAll() {
        JOBS.clear();
    }

    /** Quantos lenhadores estão com trabalho aberto agora. */
    public static int activeJobs() {
        return JOBS.size();
    }

    /** As posições em curso, para os testes de jogo. Somente leitura. */
    public static List<BlockPos> blocksInProgress() {
        List<BlockPos> positions = new ArrayList<>();

        for (Job job : JOBS.values()) {
            if (!job.isBetweenTrees()) {
                positions.add(job.currentBlock());
            }
        }

        return positions;
    }
}
