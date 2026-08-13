package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.event.VillageDetectionHandler;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.work.LumberjackWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O ciclo decidindo a partir do baú de verdade — item A do §8.
 *
 * <p>{@code ColonyCycleTest} já cobre a decisão como lógica: dado um
 * estoque e uma meta, quais tarefas nascem. O que ele não pode cobrir é
 * de onde saem os dois números, e é aí que mora a Regra 1 — a meta é o
 * que está guardado mais o que ainda cabe, e as duas metades são medidas
 * no mundo, num baú que existe.
 *
 * <p>Era também o E1 pelo lado que a Regra 1 fechou: com meta constante,
 * a colônia pedia madeira para sempre. Aqui o baú sem espaço precisa
 * calar a colônia, e o baú vazio precisa fazê-la pedir.
 */
public class ColonyCycleGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "cycle_deficit")
    public void aFullChestAsksForNothingAndAnEmptyOneAsksForWood(TestContext context) {
        clearColonyState();

        BlockPos chest = new BlockPos(2, 2, 2);
        BlockPos stand = new BlockPos(1, 2, 1);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();
        BlockPos absoluteChest = context.getAbsolutePos(chest);
        BlockPos absoluteStand = context.getAbsolutePos(stand);

        ColonyPos chestPos = MinecraftTypeAdapter.toColonyPos(absoluteChest);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, stand);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(absoluteStand));

        VillageColonyMod.COLONIES.register(colony);

        // Um lenhador, senão nenhuma tarefa de coleta nasce: o ciclo não
        // abre pedido que ninguém sabe atender.
        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.LUMBERJACK);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chestPos));

        // Baú entupido com o que não é da colônia: nenhuma madeira
        // guardada, e nenhum espaço para guardar.
        //
        // A terra é de propósito. Se o baú estivesse cheio de madeira, a
        // meta constante antiga — 64 troncos — também daria déficit zero,
        // e o teste passaria sem provar nada. Com o baú entupido de terra
        // a colônia tem zero madeira: pela regra velha faltariam 64, pela
        // Regra 1 não falta nada, porque não há onde pôr.
        int room = ChestDepositor.freeSpaceFor(world, chestPos, Items.DIRT);

        context.assertTrue(room > 0, "baú vazio devia ter espaço, tinha " + room);

        ChestDepositor.deposit(world, chestPos, Items.DIRT, room);

        VillageDetectionHandler.runCycleNow(world, absoluteStand);

        context.assertTrue(
                collectWoodTasksOf(colony) == 0,
                "sem espaço no baú a colônia pediu madeira mesmo assim: "
                        + collectWoodTasksOf(colony) + " tarefas");

        // O jogador esvazia o baú. Nada mais muda.
        emptyChest(world, absoluteChest);

        VillageDetectionHandler.runCycleNow(world, absoluteStand);

        context.assertTrue(
                collectWoodTasksOf(colony) == 1,
                "com o baú vazio a colônia devia pedir madeira, e abriu "
                        + collectWoodTasksOf(colony) + " tarefas");

        clearColonyState();

        context.complete();
    }

    /**
     * A árvore que não cabe encerra a tarefa em vez de derrubar o mundo.
     *
     * <p>É o fim normal da Regra 1 visto de perto: o baú termina quase
     * cheio, o ciclo seguinte abre um pedido do tamanho do espaço que
     * sobrou, e a primeira árvore que o lenhador olha já não cabe. A
     * tarefa é encerrada sem que ele tenha derrubado nada — e até
     * 2026-08-12 isso lançava dentro do tick do servidor, porque
     * {@code Task.complete} exige EXECUTING e a tarefa ainda estava
     * RESERVED.
     *
     * <p>Apareceu num teste que rodava com a Regra 1 desligada, e não em
     * jogo: era estado antigo de outro teste passando por este caminho.
     * O caminho, porém, é o que a Regra 1 percorre toda vez que um baú
     * enche.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "cycle_tree_too_big",
            tickLimit = 200)
    public void aTreeThatDoesNotFitEndsTheTaskInsteadOfCrashing(TestContext context) {
        clearColonyState();

        BlockPos base = new BlockPos(4, 2, 4);
        BlockPos chest = new BlockPos(2, 2, 2);
        BlockPos stand = new BlockPos(3, 2, 4);

        // Uma árvore de quatro troncos, com copa: sem copa não é árvore.
        context.setBlockState(base.down(), Blocks.DIRT.getDefaultState());

        for (int y = 0; y < 4; y++) {
            context.setBlockState(base.up(y), Blocks.OAK_LOG.getDefaultState());
        }

        context.setBlockState(base.up(3).north(), Blocks.OAK_LEAVES.getDefaultState());
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();
        ColonyPos chestPos = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest));

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, stand);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(base)));

        VillageColonyMod.COLONIES.register(colony);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.LUMBERJACK);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chestPos));

        // Espaço para dois troncos, e a árvore tem quatro.
        int room = ChestDepositor.freeSpaceFor(world, chestPos, Items.OAK_LOG);

        ChestDepositor.deposit(world, chestPos, Items.OAK_LOG, room - 2);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_WOOD,
                TaskPriority.PRODUCTION,
                ResourceType.OAK_LOG,
                2);

        // Reservada e nunca iniciada: é assim que ela chega do ciclo.
        task.reserveFor(villager.getUuid());

        LumberjackWork.run(world, colony);

        context.runAtTick(10, () -> {
            context.assertTrue(
                    task.state() == TaskState.COMPLETED,
                    "a tarefa devia ter sido encerrada, e está em " + task.state());

            context.expectBlock(Blocks.OAK_LOG, base);

            clearColonyState();
            LumberjackWork.clearAll();

            context.complete();
        });
    }

    /** Quantos pedidos de madeira esta colônia tem de pé. */
    private static int collectWoodTasksOf(Colony colony) {
        int found = 0;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() == TaskType.COLLECT_WOOD) {
                found++;
            }
        }

        return found;
    }

    /**
     * Esvazia o baú sem quebrá-lo.
     *
     * <p>Trocar o bloco despejaria o conteúdo no chão como item, e o
     * mundo do teste é partilhado pela bateria inteira.
     */
    private static void emptyChest(ServerWorld world, BlockPos chest) {
        if (world.getBlockEntity(chest) instanceof Inventory inventory) {
            inventory.clear();
        }
    }

    private static void clearColonyState() {
        VillageColonyMod.COLONIES.clear();
        VillageColonyMod.WORKERS.clear();
        VillageColonyMod.STORAGES.clear();
        VillageColonyMod.TASKS.clear();
    }
}
