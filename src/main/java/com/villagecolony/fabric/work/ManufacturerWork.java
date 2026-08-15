package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
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

    /** O trabalho em curso de cada fabricante. */
    private static final Map<UUID, Job> JOBS = new HashMap<>();

    private ManufacturerWork() {
    }

    /** Uma tarefa de fabricação em curso. */
    private static final class Job {

        private final Task task;

        /** Ticks já cumpridos da peça atual. */
        private int progress;

        /** Quantas peças esta tarefa já rendeu. */
        private int crafted;

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

        JOBS.values().removeIf(job -> !isOngoing(job.task));

        report(world, colony);

        return open;
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

            return true;
        }

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

        return convertOne(world, job, villager.getUuid());
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
