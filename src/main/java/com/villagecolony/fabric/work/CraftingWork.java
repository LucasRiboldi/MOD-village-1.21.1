package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import com.villagecolony.fabric.integration.ColonySupply;
import com.villagecolony.fabric.integration.VillageFocus;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.coordination.ColonyGoals;
import com.villagecolony.core.coordination.StockRules;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
 * <p>O material é da colônia, não do fabricante — ver {@link CraftingSteps#convertOne}
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
public final class CraftingWork {

    static {
        ServerMemory.register(CraftingWork.class, CraftingWork::clearAll);
    }

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
    static final int TICKS_PER_PIECE = 20;

    /** De quantos em quantos ticks o braço balança. */
    static final int SWING_INTERVAL = 5;

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
    static final int STALL_LIMIT = com.villagecolony.core.coordination.WorkRescue.LIMIT;

    /** O trabalho em curso de cada fabricante. */
    static final Map<UUID, Job> JOBS = new HashMap<>();

    /**
     * Como cada oficina aparece na linha de {@link IdleLog}.
     *
     * <p><b>Uma por profissão, e não uma só</b> — 2026-09-09. O
     * registrador compara colônia e assunto: com um assunto partilhado,
     * a colônia sem carpinteiro calaria a linha da colônia sem pedreiro,
     * e o segundo silêncio nunca seria dito.
     */
    static String subjectOf(TaskType type) {
        return com.villagecolony.core.coordination.CraftFamily.subjectOf(type);
    }

    private CraftingWork() {
    }

    /** Uma tarefa de fabricação em curso. */
    static final class Job {

        final Task task;

        /** Ticks já cumpridos da peça atual. */
        int progress;

        /** Quantas peças esta tarefa já rendeu. */
        int crafted;

        /** Até quanto a peça da tarefa vai, fixado na primeira passagem; -1 antes dela. */
        int goalTarget = -1;

        /**
         * Ticks de horário de trabalho andando sem chegar ao baú.
         *
         * <p>Zerado ao chegar. Ver {@link #STALL_LIMIT}.
         */
        int stalled;

        /**
         * Se ele saiu do lugar, e há quanto tempo não sai — 2026-09-03.
         *
         * <p>O guarda acima conta tique de expediente <b>indo até o
         * alvo</b> e nunca pergunta se o aldeão andou. Ver {@link WorkStall}.
         */
        final WorkStall stall = new WorkStall();

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

        for (TaskType type : com.villagecolony.core.coordination.CraftFamily.TYPES) {
            open += run(world, colony, type);
        }

        return open;
    }

    /**
     * O mesmo, para uma das duas oficinas.
     *
     * <p><b>A divisão do fabricante é de 2026-09-09</b>, a pedido do
     * autor, e ela é de <b>profissão</b>, não de implementação: a
     * máquina é a mesma — reservar, andar até o baú, contar os tiques e
     * trocar a peça —, e o que muda é qual tarefa ela atende e qual
     * família de material ela lavra. Duplicar seiscentas linhas para
     * mudar duas seria pior que o problema.
     *
     * @param type uma das oficinas de {@code CraftFamily.TYPES}
     */
    public static int run(ServerWorld world, Colony colony, TaskType type) {
        int open = 0;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() != type || !isOngoing(task)) {
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
            CraftingReport.reportIdle(colony, type);
        } else {
            IdleLog.clear(colony.id(), subjectOf(type));
        }

        CraftingReport.report(world, colony, type);

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

            if (!VillageFocus.isWorking(world, entry.getValue().task.colonyId())) {
                continue;
            }

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
            job.task.release("no chest of its own");

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

        // Fabrica onde está: ninguém anda até o baú — regra do autor, 2026-10-11.
        // O ingrediente sai de qualquer baú da colônia e a peça entra no baú dele;
        // a caminhada até a bancada soltava a tarefa "stuck on the way to its chest".
        job.stalled = 0;
        job.stall.reset();

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
    static boolean craftOne(
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

        // O pedido de areia ao pedreiro é o britador — P2c, 2026-10-11.
        if (job.task.targetResource() == ResourceType.SAND) {
            return Crusher.step(world, job, villager.getUuid(), storage.chestPosition());
        }

        // O que a obra pede vem primeiro — 2026-08-20. Enquanto o
        // fabricante só sabia fazer tábua, a casa parava em qualquer
        // peça que a colônia não tivesse; agora ele descasca tronco,
        // monta tocha e vidraça, e só volta à tábua quando não há nada
        // que a obra esteja esperando.
        if (CraftingSteps.produceForWork(world, job, villager.getUuid())) {
            return true;
        }

        // Um pouco de tudo no estoque — ItemStock, regra do autor de 2026-10-11.
        Optional<Boolean> forStock = ItemStock.step(world, job, villager.getUuid());

        if (forStock.isPresent()) {
            return forStock.get();
        }

        Optional<Boolean> forGoal = CraftingSteps.produceForGoal(world, job, villager.getUuid());

        if (forGoal.isPresent()) {
            return forGoal.get();
        }

        return CraftingSteps.convertOne(world, job, villager.getUuid());
    }

    /** Se esta peça é do pedreiro — {@code CraftFamily}, pelo material. */
    public static boolean isMasonry(ResourceId wanted) {
        return com.villagecolony.core.coordination.CraftFamily.of(wanted.path()) == TaskType.CRAFT_STONE_MATERIAL;
    }

    /** Encerra a tarefa e devolve o aldeão à rotina. */
    static void finish(Job job, UUID workerId, String why) {
        if (job.task.state() == TaskState.RESERVED) {
            job.task.start();
        }

        job.task.complete();
        WorkerStrikes.worked(workerId, job.task);
        TaskChain.nextAfter(job.task, job.crafted);

        WorkTargets.clear(workerId);

        VillageColonyMod.LOGGER.info(
                "Worker {} finished crafting — {} pieces made, stopped because {}",
                workerId,
                job.crafted,
                why);
        ActionJournal.action(workerId, "CRAFTED", job.task.targetResource().name(), job.crafted, null, why);
    }

    /** Esquece o trabalho de um trabalhador que deixou de existir. */
    public static void forget(UUID workerId) {
        Job job = JOBS.remove(workerId);

        if (job != null && job.task.state() == TaskState.EXECUTING) {
            job.task.release("the craftsman left the colony");
        }
    }

    /** Esquece tudo. Chamado ao parar o servidor. */
    public static void clearAll() {
        JOBS.clear();
    }

    static boolean isOngoing(Task task) {
        return task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING;
    }

}
