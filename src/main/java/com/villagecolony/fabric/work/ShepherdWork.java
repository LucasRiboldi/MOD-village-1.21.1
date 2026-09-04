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
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestDepositor;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O pastor: tosquia ovelha e traz a lã — 2026-08-20.
 *
 * <p><b>Ele é quem fecha o laço da vila.</b> O ciclo desenhado desde o
 * começo é casa, cama, aldeão novo, trabalhador, casa. A Regra 21 pôs a
 * cama na lista de toda casa e registrou o que faltava: lã pede tosquia,
 * e nenhum aldeão deste mod tosquiava. O laço ficava aberto exatamente
 * no elo que faz a vila crescer sozinha.
 *
 * <p><b>A ovelha não morre, e é o ponto.</b> Tosquiar é a colheita que
 * se repete: a lã volta a crescer sozinha, e o rebanho continua lá. É a
 * mesma ideia da Regra 7 — o lenhador replanta o que corta —, e aqui ela
 * sai de graça, porque quem replanta é o próprio jogo.
 *
 * <p>Só ovelha adulta e não tosquiada. Cordeiro não dá lã no Vanilla, e
 * insistir com uma ovelha pelada seria o aldeão parado em frente a ela
 * para sempre.
 */
public final class ShepherdWork {

    /** Alcance de braço, medido no plano. O mesmo do lenhador. */
    private static final int REACH = 3;

    /** Até onde ele procura rebanho. */
    private static final int SEARCH_RADIUS = 32;

    /** O raio em vigor. Encurtado nos testes, como o do mineiro. */
    private static int searchRadius = SEARCH_RADIUS;

    /** Quantos tiques de expediente sem chegar antes de largar a ovelha. */
    private static final int STALL_LIMIT = 4 * 600;

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    private static final String SUBJECT = "shepherd";

    /** A ovelha de agora, e a lã que já veio. */
    private static final class Job {

        private final Task task;

        private final BlockPos center;

        /** A ovelha de agora. Nulo entre uma e a próxima. */
        private UUID sheep;

        private int collected;

        private int stalled;

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

    private ShepherdWork() {
    }

    /** Encurta o raio de busca. Só os testes precisam disso. */
    public static void shortenSearchTo(int blocks) {
        if (blocks <= 0) {
            throw new IllegalArgumentException("Radius must be positive: " + blocks);
        }

        searchRadius = blocks;
    }

    /** Devolve o raio ao valor de jogo. */
    public static void restoreSearch() {
        searchRadius = SEARCH_RADIUS;
    }

    /**
     * Despacho, uma vez por ciclo da colônia.
     *
     * @return quantos pastores desta colônia estão com trabalho aberto
     */
    public static int run(ServerWorld world, Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());

        int open = 0;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() != TaskType.COLLECT_WOOL || !isOngoing(task)) {
                continue;
            }

            Optional<UUID> executor = task.executor();

            if (executor.isEmpty()) {
                continue;
            }

            JOBS.computeIfAbsent(executor.get(), worker -> new Job(task, center));

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

        return open;
    }

    /** Por que não houve tosquia, uma vez por motivo. */
    private static void reportIdle(Colony colony) {
        int able = WorkAssignment.countCapableOf(
                colony.id(), TaskType.COLLECT_WOOL.required(), VillageColonyMod.WORKERS);

        if (able == 0) {
            IdleLog.record(colony.id(), SUBJECT, IdleReason.NO_WORKER);

            return;
        }

        IdleLog.record(colony.id(), SUBJECT, IdleReason.NO_TASK, able + " able to shear");
    }

    /** Um tique de trabalho para cada pastor com ovelha em vista. */
    public static void tick(ServerWorld world) {
        if (JOBS.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, Job> entry : Map.copyOf(JOBS).entrySet()) {
            if (!isOngoing(entry.getValue().task)) {
                JOBS.remove(entry.getKey());

                // O destino morre com a tarefa — ver WorkTargets.clear.
                WorkTargets.clear(entry.getKey());

                continue;
            }

            step(world, entry.getKey(), entry.getValue());
        }
    }

    private static void step(ServerWorld world, UUID workerId, Job job) {
        if (!(world.getEntity(workerId) instanceof VillagerEntity villager)) {
            return;
        }

        Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(workerId);

        if (storage.isEmpty()) {
            return;
        }

        if (job.sheep == null) {
            findSheep(world, workerId, job);

            return;
        }

        if (!(world.getEntity(job.sheep) instanceof SheepEntity sheep) || !isWoolly(sheep)) {
            // Morreu, foi tosquiada por outro, ou saiu do carregado.
            release(workerId, job);

            return;
        }

        if (!isWithinReach(villager, sheep)) {
            // <b>Só tique de expediente</b> — 2026-09-03, e o pastor era a
            // única profissão que andava sem esta conferência. Fora da hora
            // a GoToWorkTargetTask nem começa, então ele está PROIBIDO de
            // andar até a ovelha, e o guarda existe para punir quem anda
            // sem chegar. Sem isto ele queimava o orçamento dormindo — é o
            // defeito que o mineiro teve em 2026-08-26, quando o contador
            // foi de 886 a 2086 com o relatório dizendo "off hours".
            if (WorkHours.isWorkTime(world, villager)) {
                job.stalled++;
            }

            // E parado no mesmo bloco há quinze segundos: oito vezes mais
            // rápido que os dois minutos de baixo. Ver WorkStall, que faz a
            // pergunta do expediente por conta própria.
            if (job.stall.stuck(world, villager) || job.stalled >= STALL_LIMIT) {
                giveUp(workerId, job);
            } else {
                WorkTargets.set(workerId, sheep.getBlockPos());
            }

            return;
        }

        // Chegou e vai tosquiar — E36, 2026-09-04. Trabalhar é a prova
        // de que ele não está congelado; pegar ovelha nova não é. Ver
        // findSheep.
        job.stall.reset();

        shear(world, villager, sheep, job, storage.get());
    }

    /** A ovelha mais próxima que ainda tem lã. */
    private static void findSheep(ServerWorld world, UUID workerId, Job job) {
        Box around = new Box(job.center).expand(searchRadius);

        List<SheepEntity> flock = world.getEntitiesByClass(
                SheepEntity.class, around, ShepherdWork::isWoolly);

        if (flock.isEmpty()) {
            return;
        }

        SheepEntity nearest = flock.get(0);

        for (SheepEntity sheep : flock) {
            if (sheep.getBlockPos().getSquaredDistance(job.center)
                    < nearest.getBlockPos().getSquaredDistance(job.center)) {

                nearest = sheep;
            }
        }

        job.sheep = nearest.getUuid();
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

        WorkTargets.set(workerId, nearest.getBlockPos());
    }

    /**
     * Tosquia, e guarda a lã no baú do pastor.
     *
     * <p>A lã sai da própria ovelha — a cor dela, e não branco sempre.
     * Uma colônia que só contasse lã branca ignoraria o rebanho preto do
     * jogador e mandaria tosquiar para sempre.
     */
    private static void shear(
            ServerWorld world,
            VillagerEntity villager,
            SheepEntity sheep,
            Job job,
            WorkerStorage storage) {

        villager.swingHand(Hand.MAIN_HAND);

        Item wool = woolOf(sheep);

        sheep.setSheared(true);

        int dropped = 1 + world.random.nextInt(3);

        ColonyPos chest = storage.chestPosition();

        int leftOver = ChestDepositor.deposit(world, chest, wool, dropped);

        job.collected += dropped - leftOver;

        if (leftOver > 0) {
            VillageColonyMod.LOGGER.warn(
                    "Chest of shepherd at {} filled up — {} of {} lost",
                    chest,
                    leftOver,
                    dropped);
        }

        VillageColonyMod.LOGGER.info(
                "Shepherd {} sheared {} of {} — {} this task",
                villager.getUuid(),
                dropped - leftOver,
                wool,
                job.collected);

        release(villager.getUuid(), job);
    }

    /**
     * A lã da cor desta ovelha.
     *
     * <p>A tabela é do jogo, e não uma escrita aqui: uma colônia que
     * contasse só lã branca ignoraria o rebanho preto do jogador e
     * mandaria tosquiar para sempre, porque a meta nunca fecharia.
     */
    private static Item woolOf(SheepEntity sheep) {
        return switch (sheep.getColor()) {
            case WHITE -> Blocks.WHITE_WOOL.asItem();
            case ORANGE -> Blocks.ORANGE_WOOL.asItem();
            case MAGENTA -> Blocks.MAGENTA_WOOL.asItem();
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL.asItem();
            case YELLOW -> Blocks.YELLOW_WOOL.asItem();
            case LIME -> Blocks.LIME_WOOL.asItem();
            case PINK -> Blocks.PINK_WOOL.asItem();
            case GRAY -> Blocks.GRAY_WOOL.asItem();
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL.asItem();
            case CYAN -> Blocks.CYAN_WOOL.asItem();
            case PURPLE -> Blocks.PURPLE_WOOL.asItem();
            case BLUE -> Blocks.BLUE_WOOL.asItem();
            case BROWN -> Blocks.BROWN_WOOL.asItem();
            case GREEN -> Blocks.GREEN_WOOL.asItem();
            case RED -> Blocks.RED_WOOL.asItem();
            case BLACK -> Blocks.BLACK_WOOL.asItem();
        };
    }

    private static boolean isWoolly(SheepEntity sheep) {
        return sheep.isAlive() && !sheep.isSheared() && !sheep.isBaby();
    }

    private static boolean isWithinReach(VillagerEntity villager, SheepEntity sheep) {
        double dx = villager.getX() - sheep.getX();
        double dz = villager.getZ() - sheep.getZ();

        return dx * dx + dz * dz <= REACH * REACH;
    }

    private static void release(UUID workerId, Job job) {
        job.sheep = null;
        job.stalled = 0;

        // O guarda de imobilidade sobrevive a largar a ovelha — E36. Ver
        // findSheep. Quem zera é o ramo de trabalho, antes do shear.

        WorkTargets.clear(workerId);
    }

    private static void giveUp(UUID workerId, Job job) {
        VillageColonyMod.LOGGER.info(
                "Shepherd {} could not reach the sheep — task back to the queue", workerId);

        job.task.release();

        release(workerId, job);
    }

    private static boolean isOngoing(Task task) {
        return task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING;
    }

    /** Esquece o trabalho deste aldeão. */
    public static void forget(UUID workerId) {
        JOBS.remove(workerId);

        WorkTargets.clear(workerId);
    }

    /** Esvazia o registro. Chamado ao parar o servidor. */
    public static void clearAll() {
        JOBS.clear();
    }

    /** Quanta lã este pastor já trouxe nesta tarefa. */
    /**
     * Quantos tiques de expediente este pastor já andou sem chegar.
     *
     * <p>Não é estado novo — é o contador do guarda de travamento, lido de
     * fora, como o do mineiro. Existe porque a pergunta que ele responde
     * não tem outro observável: o guarda só fala quando estoura, e o
     * defeito era ele <b>contar</b> quando não devia.
     */
    public static int stallOf(UUID workerId) {
        Job job = JOBS.get(workerId);

        return job == null ? 0 : job.stalled;
    }

    public static int collectedBy(UUID workerId) {
        Job job = JOBS.get(workerId);

        return job == null ? 0 : job.collected;
    }
}
