package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import com.villagecolony.fabric.integration.ColonySupply;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O fabricante trabalhando — Fase 9, TASK-027 a TASK-029.
 *
 * <p>Ele tira tronco de um baú da colônia, transforma em tábua pela
 * receita do próprio jogo, e devolve a tábua ao mesmo baú de onde o tronco
 * saiu. É a primeira profissão que <b>consome</b>: até aqui a colônia só
 * somava.
 *
 * <p>O material é da colônia, não do fabricante — ver {@link #convertOne}
 * para o que a sessão de 2026-08-14 mostrou sobre isso. O que continua
 * sendo dele é o <b>lugar</b>: ele anda até o próprio baú e trabalha ali,
 * e sem baú próprio não trabalha.
 *
 * <p>Tem a forma do lenhador — despacho no ciclo longo, trabalho um passo
 * por tick — e por bons motivos: o custo por tick tem de continuar
 * cabendo num tick, e o trabalho tem de ser visível para quem está
 * jogando. Ele anda até o próprio baú e trabalha ali.
 *
 * <p><b>A regra que o lenhador não tem, e esta profissão precisa:</b>
 * nada sai do baú antes da peça ficar pronta. O tronco é retirado,
 * transformado e devolvido como tábua no <b>mesmo tick</b>. Durante a
 * espera, o que existe é um contador — e não um tronco na mão de um
 * aldeão que pode morrer, ser zumbificado, ou estar num servidor que vai
 * ser desligado. Ver o E3 do §17 para o que acontece quando alguma coisa
 * sai do mundo antes de ter para onde ir.
 */
public final class ManufacturerWork {

    /**
     * Quantos ticks uma peça leva.
     *
     * <p>Um segundo. É um número inventado, e vale dizer: a Regra 2 tem
     * a fórmula do jogo para tempo de quebra, e fabricar não tem
     * equivalente — o jogador faz tábua num clique.
     *
     * <p>O que ele precisa cumprir: ser visível, para o trabalho não
     * acontecer num piscar; e ser curto o bastante para uma pilha de
     * troncos não levar a tarde inteira. Se o autor quiser outro ritmo, é
     * esta linha.
     */
    private static final int TICKS_PER_PIECE = 20;

    /** De que distância ele alcança o próprio baú. */
    private static final int REACH = 3;

    /** De quantos em quantos ticks o braço balança. */
    private static final int SWING_INTERVAL = 5;

    /**
     * Quantos ticks andando sem chegar ao baú antes de desistir.
     *
     * <p>Quatro ciclos da colônia. O baú do fabricante é o dele, a
     * poucos blocos da cama: dois minutos de horário de trabalho sem
     * cobrir essa distância não é lentidão, é aldeão preso.
     *
     * <p>Sem isto a tarefa reservada não voltava para a fila enquanto o
     * trabalhador estivesse vivo — a vaga ficava com quem nunca
     * chegaria. Mesma regra e mesmo motivo de
     * {@code LumberjackWork.STALL_LIMIT}.
     */
    private static final int STALL_LIMIT = 4 * VillageDetector.CYCLE_TICKS;

    /** O trabalho em curso de cada fabricante. */
    private static final Map<UUID, Job> JOBS = new HashMap<>();

    /** Como esta profissão aparece na linha de {@link IdleLog}. */
    private static final String SUBJECT = "manufacturer";

    private ManufacturerWork() {
    }

    /** Uma tarefa de fabricação em curso. */
    private static final class Job {

        private final Task task;

        /** Ticks já cumpridos da peça atual. */
        private int progress;

        /** Quantas peças esta tarefa já rendeu. */
        private int crafted;

        /**
         * Ticks de horário de trabalho andando sem chegar ao baú.
         *
         * <p>Zerado ao chegar. Ver {@link #STALL_LIMIT}.
         */
        private int stalled;

        private Job(Task task) {
            this.task = task;
        }
    }

    /**
     * Despacho, uma vez por ciclo da colônia.
     *
     * <p>Abre trabalho para toda tarefa de fabricação já reservada, e
     * fecha o de tarefa encerrada. Não fabrica nada: quem fabrica é
     * {@link #tick}.
     *
     * @return quantos fabricantes desta colônia estão com trabalho aberto
     */
    public static int run(ServerWorld world, Colony colony) {
        int open = 0;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() != TaskType.CRAFT_MATERIAL || !isOngoing(task)) {
                continue;
            }

            Optional<UUID> executor = task.executor();

            if (executor.isEmpty()) {
                continue;
            }

            JOBS.computeIfAbsent(executor.get(), worker -> new Job(task));

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

        if (open == 0) {
            reportIdle(colony);
        } else {
            IdleLog.clear(colony.id(), SUBJECT);
        }

        report(world, colony);

        return open;
    }

    /**
     * Diz por que nenhum fabricante desta colônia está trabalhando.
     *
     * <p>Mesma razão e mesma forma de {@code LumberjackWork.reportIdle}:
     * {@link #report} só fala de fabricante <b>com</b> trabalho aberto, e
     * o silêncio de uma colônia sem tarefa era indistinguível do silêncio
     * de uma colônia sem fabricante.
     *
     * <p>Vale mais aqui do que no lenhador, e o E10 é a prova: a Fase 9
     * rodou uma sessão inteira encerrando tarefa por falta de tronco, com
     * 134 troncos guardados. O que faltava não era a tarefa — era saber
     * de qual dos lados vinha o silêncio.
     */
    private static void reportIdle(Colony colony) {
        int hands = WorkAssignment.countCapableOf(
                colony.id(), TaskType.CRAFT_MATERIAL.required(), VillageColonyMod.WORKERS);

        if (hands == 0) {
            IdleLog.record(colony.id(), SUBJECT, IdleReason.NO_WORKER);

            return;
        }

        boolean anyTask = false;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() == TaskType.CRAFT_MATERIAL && isOngoing(task)) {
                anyTask = true;

                break;
            }
        }

        IdleLog.record(
                colony.id(),
                SUBJECT,
                anyTask ? IdleReason.NO_EXECUTOR : IdleReason.NO_TASK,
                hands + " able to");
    }

    /**
     * Um passo de trabalho, a cada tick do servidor.
     *
     * <p>O custo por tick é um contador por fabricante. A parte que mexe
     * no mundo — tirar do baú, fabricar, devolver — acontece uma vez por
     * peça, e não a cada tick.
     */
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

        Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(workerId);

        if (storage.isEmpty()) {
            job.task.release();

            WorkTargets.clear(workerId);

            VillageColonyMod.LOGGER.info(
                    "Worker {} has no chest — crafting task returned to the queue", workerId);

            return false;
        }

        if (!(world.getEntity(workerId) instanceof VillagerEntity villager)) {
            // Aldeão fora de chunk carregado. A tarefa espera por ele.
            return true;
        }

        if (!WorkHours.isWorkTime(world, villager)) {
            return true;
        }

        BlockPos chest = MinecraftTypeAdapter.toBlockPos(storage.get().chestPosition());

        if (!villager.getBlockPos().isWithinDistance(chest, REACH)) {
            WorkTargets.set(workerId, chest);

            if (++job.stalled > STALL_LIMIT) {
                // Andou dois minutos de horário de trabalho e não chegou
                // ao próprio baú. Devolver a tarefa à fila é melhor que
                // guardá-la para quem não a fará — e em silêncio isto
                // seria indistinguível de trabalho acontecendo.
                job.task.release();

                WorkTargets.clear(workerId);

                VillageColonyMod.LOGGER.info(
                        "Worker {} could not reach its chest at {} in {} work ticks"
                                + " — crafting task returned to the queue",
                        workerId,
                        chest.toShortString(),
                        STALL_LIMIT);

                return false;
            }

            return true;
        }

        job.stalled = 0;

        return craftOne(world, villager, job, storage.get());
    }

    /**
     * Um tick de trabalho na peça da vez.
     *
     * <p>O contador sobe; quando ele estoura, o tronco sai do baú e a
     * tábua entra — as duas coisas no mesmo tick.
     *
     * @return false quando não há mais o que fabricar e a tarefa acabou
     */
    private static boolean craftOne(
            ServerWorld world, VillagerEntity villager, Job job, WorkerStorage storage) {

        if (job.task.state() == TaskState.RESERVED) {
            job.task.start();
        }

        if (job.progress % SWING_INTERVAL == 0) {
            villager.swingHand(Hand.MAIN_HAND, true);
        }

        if (++job.progress < TICKS_PER_PIECE) {
            return true;
        }

        job.progress = 0;

        // O que a obra pede vem primeiro — 2026-08-20. Enquanto o
        // fabricante só sabia fazer tábua, a casa parava em qualquer
        // peça que a colônia não tivesse; agora ele descasca tronco,
        // monta tocha e vidraça, e só volta à tábua quando não há nada
        // que a obra esteja esperando.
        if (produceForWork(world, job, villager.getUuid())) {
            return true;
        }

        return convertOne(world, job, villager.getUuid());
    }

    /**
     * Faz a primeira peça que a obra pede e a colônia não tem.
     *
     * <p>Pedido do autor em 2026-08-20: <i>"o fabricante deve descascar
     * stripped_oak, criar tocha e a vidraça, se os materiais necessários
     * existirem nos baús da vila"</i>.
     *
     * <p>São dois caminhos, e a diferença entre eles é do jogo e não do
     * mod:
     *
     * <pre>
     * descascar   não é receita de bancada — é machado no tronco. Sai
     *             de uma conversão nominal: oak_log vira
     *             stripped_oak_log, e o nome basta porque é convenção
     *             do próprio jogo
     *
     * montar      tocha e vidraça são receitas de verdade, e quem as
     *             conhece é o livro do jogo. CraftingLookup.billFor
     *             procura pelo RESULTADO, que é a pergunta certa aqui
     * </pre>
     *
     * @return se fez alguma coisa nesta passagem
     */
    private static boolean produceForWork(ServerWorld world, Job job, UUID workerId) {
        Optional<Colony> colony = VillageColonyMod.COLONIES.find(job.task.colonyId());

        if (colony.isEmpty()) {
            return false;
        }

        Optional<ConstructionProject> open =
                VillageColonyMod.CONSTRUCTIONS.openOf(colony.get().id());

        if (open.isEmpty()) {
            return false;
        }

        for (ResourceId wanted : open.get().remainingMaterials().keySet()) {
            Optional<Item> item = MinecraftTypeAdapter.toBlock(wanted).map(Block::asItem);

            if (item.isEmpty()) {
                continue;
            }

            List<ColonyPos> chests =
                    ColonyChests.nearestFirst(colony.get().id(), colony.get().center());

            if (ColonyChests.countIn(world, chests, item.get()) > 0) {
                // A colônia já tem. Não é o fabricante quem falta.
                continue;
            }

            if (strip(world, colony.get(), wanted, workerId)
                    || ColonySupply.take(
                            world, colony.get().id(), colony.get().center(), item.get())) {

                return true;
            }
        }

        return false;
    }

    /**
     * Descasca um tronco, se a colônia tiver um e a obra quiser o pelado.
     *
     * <p>Não passa pelo livro de receitas porque não é receita: no jogo,
     * descascar é passar o machado. O que se aproveita é o nome —
     * {@code stripped_oak_log} sai de {@code oak_log} —, e essa
     * convenção vale para as nove madeiras.
     */
    private static boolean strip(
            ServerWorld world, Colony colony, ResourceId wanted, UUID workerId) {

        if (!wanted.path().startsWith("stripped_")) {
            return false;
        }

        ResourceId bark = new ResourceId(
                wanted.namespace(), wanted.path().substring("stripped_".length()));

        Optional<Item> log = MinecraftTypeAdapter.toBlock(bark).map(Block::asItem);
        Optional<Item> naked = MinecraftTypeAdapter.toBlock(wanted).map(Block::asItem);

        if (log.isEmpty() || naked.isEmpty()) {
            return false;
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(colony.id(), colony.center());

        if (ColonyChests.withdraw(world, chests, log.get(), 1) < 1) {
            return false;
        }

        Optional<ColonyPos> room =
                ColonyChests.firstWithRoomFor(world, chests, naked.get(), 1);

        if (room.isEmpty()) {
            // Não cabe em baú nenhum. Devolve o tronco: descascar sem
            // onde guardar destruiria material do jogador.
            ColonyChests.firstWithRoomFor(world, chests, log.get(), 1)
                    .ifPresent(back -> ChestDepositor.deposit(world, back, log.get(), 1));

            return false;
        }

        ChestDepositor.deposit(world, room.get(), naked.get(), 1);

        VillageColonyMod.LOGGER.info(
                "Manufacturer {} stripped a {} into {}", workerId, bark.path(), wanted.path());

        return true;
    }

    /**
     * Tira um tronco dos baús da colônia, faz a tábua, devolve ao baú de
     * onde o tronco saiu.
     *
     * <p>Nesta ordem e no mesmo tick. A conferência de espaço vem antes
     * da retirada: tirar o tronco e descobrir depois que a tábua não cabe
     * seria destruir o tronco do jogador, que é exatamente o defeito que
     * o E3 registra do outro lado.
     *
     * <p><b>De qualquer baú da colônia, e não só do próprio.</b> Até
     * 2026-08-14 era só do próprio, por uma intenção que o mundo não
     * sustenta: quem colhe deposita no baú <em>dele</em>, e nada nunca põe
     * tronco no baú de um fabricante. A sessão daquele dia mostrou o
     * resultado — dezessete tarefas encerradas com "no logs left in the
     * chest", zero tábuas, e 134 troncos guardados na colônia. A meta da
     * Regra 5 se mede na colônia inteira ({@code ColonyGoals} soma o
     * {@code ResourceTally} dela); o executor media um baú só, e a
     * discordância entre os dois é que abria tarefa por ciclo para
     * encerrá-la no tick seguinte.
     *
     * <p>A tábua volta para o mesmo baú de onde o tronco veio, e não para
     * o do fabricante: é o que preserva a regra do mesmo baú no mesmo
     * tick, e o que garante que o lugar aberto pela retirada é o lugar
     * onde a peça cabe.
     */
    private static boolean convertOne(ServerWorld world, Job job, UUID workerId) {
        ColonyPos chest = null;
        List<ItemStack> logs = List.of();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(job.task.colonyId())) {
            Optional<WorkerStorage> owned = VillageColonyMod.STORAGES.of(worker.villagerId());

            if (owned.isEmpty()) {
                continue;
            }

            ColonyPos candidate = owned.get().chestPosition();

            logs = ChestWithdrawer.withdrawGroup(world, candidate, ResourceGroup.WOOD, 1);

            if (!logs.isEmpty()) {
                chest = candidate;

                break;
            }
        }

        if (logs.isEmpty()) {
            finish(job, workerId, "no logs left in the colony chests");

            return false;
        }

        ItemStack log = logs.get(0);

        Optional<ItemStack> planks = CraftingLookup.resultOfOne(world, log);

        if (planks.isEmpty()) {
            // O jogo não conhece receita para este tronco sozinho. Devolve
            // o que tirou: o item é do jogador, e some se ninguém o puser
            // de volta.
            ChestDepositor.deposit(world, chest, log.getItem(), log.getCount());

            finish(job, workerId, "nothing to make out of " + log.getItem());

            return false;
        }

        ItemStack result = planks.get();

        int leftOver = ChestDepositor.deposit(
                world, chest, result.getItem(), result.getCount());

        if (leftOver > 0) {
            // O baú encheu no meio. O que não coube volta como tronco não
            // dá: o tronco já virou tábua. Devolver o que sobrou ao baú é
            // impossível por definição — ele está cheio —, então o que se
            // pode fazer é parar antes de acontecer de novo.
            VillageColonyMod.LOGGER.warn(
                    "Worker {} crafted {} planks that did not fit — chest is full",
                    workerId,
                    leftOver);
        }

        job.crafted++;

        return true;
    }

    /** Encerra a tarefa e devolve o aldeão à rotina. */
    private static void finish(Job job, UUID workerId, String why) {
        if (job.task.state() == TaskState.RESERVED) {
            job.task.start();
        }

        job.task.complete();

        WorkTargets.clear(workerId);

        VillageColonyMod.LOGGER.info(
                "Worker {} finished crafting — {} pieces made, stopped because {}",
                workerId,
                job.crafted,
                why);
    }

    /** Esquece o trabalho de um trabalhador que deixou de existir. */
    public static void forget(UUID workerId) {
        Job job = JOBS.remove(workerId);

        if (job != null && job.task.state() == TaskState.EXECUTING) {
            job.task.release();
        }
    }

    /** Esquece tudo. Chamado ao parar o servidor. */
    public static void clearAll() {
        JOBS.clear();
    }

    /** Quantas peças este trabalhador já fez na tarefa atual. */
    public static int craftedBy(UUID workerId) {
        Job job = JOBS.get(workerId);

        return job == null ? 0 : job.crafted;
    }

    private static boolean isOngoing(Task task) {
        return task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING;
    }

    /**
     * Uma linha por ciclo dizendo o que cada fabricante está fazendo.
     *
     * <p>A mesma lição do lenhador mudo: sem isto, um fabricante parado
     * por falta de baú, por horário, por chunk descarregado ou por falta
     * de tronco produzem exatamente o mesmo silêncio.
     */
    private static void report(ServerWorld world, Colony colony) {
        StringBuilder line = new StringBuilder();
        int reported = 0;

        for (Map.Entry<UUID, Job> entry : JOBS.entrySet()) {
            Job job = entry.getValue();

            if (!job.task.belongsTo(colony.id())) {
                continue;
            }

            if (reported++ > 0) {
                line.append("; ");
            }

            line.append(entry.getKey().toString(), 0, 8)
                    .append(" ")
                    .append(describe(world, entry.getKey(), job));
        }

        if (reported == 0) {
            return;
        }

        VillageColonyMod.LOGGER.info("Colony {} manufacturers: {}", colony.id(), line);
    }

    private static String describe(ServerWorld world, UUID workerId, Job job) {
        if (!(world.getEntity(workerId) instanceof VillagerEntity villager)) {
            return "not loaded (" + job.crafted + " pieces so far)";
        }

        String clock = WorkHours.isWorkTime(world, villager) ? "work time" : "off hours";

        Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(workerId);

        if (storage.isEmpty()) {
            return "no chest, " + clock + " (" + job.crafted + " pieces so far)";
        }

        BlockPos chest = MinecraftTypeAdapter.toBlockPos(storage.get().chestPosition());

        int distance = (int) Math.sqrt(villager.getBlockPos().getSquaredDistance(chest));

        return (distance <= REACH ? "at the chest" : "walking to the chest, " + distance
                + " blocks away")
                + ", " + clock
                + ", " + job.progress + "/" + TICKS_PER_PIECE + " ticks"
                + " (" + job.crafted + " pieces so far)";
    }
}
