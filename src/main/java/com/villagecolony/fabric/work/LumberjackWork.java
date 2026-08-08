package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.TreeHarvester;
import com.villagecolony.fabric.integration.TreeScanner;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * O lenhador trabalhando — TASK-024 e TASK-025.
 *
 * <p>Roda no ciclo da colônia, depois de {@code ColonyCycle} ter
 * distribuído as tarefas. Para cada tarefa de madeira já reservada:
 * achar árvore, andar até ela, derrubar, guardar no baú.
 *
 * <p>Regras decididas pelo autor em 2026-08-08:
 *
 * <ul>
 *   <li>só tronco de carvalho, e replanta muda na base;
 *   <li>a madeira vai direto para o baú do trabalhador;
 *   <li>procura até {@link #SEARCH_RADIUS} blocos do centro da colônia.
 * </ul>
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
     */
    private static final int REACH = 3;

    private LumberjackWork() {
    }

    /**
     * Um passo de trabalho para cada lenhador ocupado da colônia.
     *
     * <p>Um passo, e não a tarefa inteira: o ciclo roda a cada 600
     * ticks, e o trabalhador que ainda está longe da árvore anda neste
     * ciclo e derruba no seguinte.
     *
     * @return quantos troncos foram derrubados agora
     */
    public static int run(ServerWorld world, Colony colony) {
        int felled = 0;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() != TaskType.COLLECT_WOOD || !isOngoing(task)) {
                continue;
            }

            felled += advance(world, colony, task);
        }

        return felled;
    }

    private static boolean isOngoing(Task task) {
        return task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING;
    }

    /**
     * Leva uma tarefa um passo adiante.
     *
     * <p>Cada saída sem trabalho tem motivo próprio e nenhuma é erro:
     * trabalhador que morreu, aldeão fora de chunk carregado, baú
     * perdido, nenhuma árvore ao alcance. A tarefa fica onde está e o
     * ciclo seguinte tenta de novo.
     */
    private static int advance(ServerWorld world, Colony colony, Task task) {
        Optional<UUID> executor = task.executor();

        if (executor.isEmpty()) {
            return 0;
        }

        Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(executor.get());

        if (storage.isEmpty()) {
            // Sem baú não há onde guardar. Soltar a tarefa devolve o
            // trabalho a quem tenha baú, em vez de travar a fila.
            task.release();

            return 0;
        }

        Optional<VillagerEntity> villager = findVillager(world, colony, executor.get());

        if (villager.isEmpty()) {
            return 0;
        }

        Optional<BlockPos> tree = TreeScanner.findNearestLog(
                world, MinecraftTypeAdapter.toBlockPos(colony.center()), SEARCH_RADIUS);

        if (tree.isEmpty()) {
            return 0;
        }

        if (task.state() == TaskState.RESERVED) {
            task.start();
        }

        return work(world, villager.get(), tree.get(), storage.get(), task);
    }

    /** Andar, ou derrubar se já chegou. */
    private static int work(
            ServerWorld world,
            VillagerEntity villager,
            BlockPos tree,
            WorkerStorage storage,
            Task task) {

        if (!villager.getBlockPos().isWithinDistance(tree, REACH)) {
            walkTo(villager, tree);

            return 0;
        }

        int felled = TreeHarvester.fell(world, tree);

        if (felled == 0) {
            return 0;
        }

        int leftOver = ChestDepositor.deposit(
                world, storage.chestPosition(), Items.OAK_LOG, felled);

        if (leftOver > 0) {
            VillageColonyMod.LOGGER.info(
                    "Chest of worker {} is full — {} logs had nowhere to go",
                    storage.workerId(),
                    leftOver);
        }

        task.complete();

        VillageColonyMod.LOGGER.info(
                "Worker {} felled {} logs at {}",
                storage.workerId(),
                felled,
                tree.toShortString());

        return felled;
    }

    /**
     * Manda o aldeão andar até a árvore.
     *
     * <p>Limite conhecido: o cérebro Vanilla do aldeão tem agenda
     * própria — dormir, trabalhar, socializar — e pode sobrescrever este
     * destino. O caminho correto seria uma task no {@code Brain}, que é
     * mudança maior. No MVP o pedido é repetido a cada ciclo, e o log de
     * derrubada dirá se ele chega. Ver §9.
     */
    private static void walkTo(VillagerEntity villager, BlockPos tree) {
        villager.getNavigation().startMovingTo(
                tree.getX() + 0.5, tree.getY(), tree.getZ() + 0.5, 0.5);
    }

    /**
     * O aldeão deste trabalhador, se estiver carregado.
     *
     * <p>Busca por UUID, que é consulta direta num índice do servidor. A
     * primeira versão varria uma caixa de 128 blocos de lado por tarefa
     * por ciclo — muito mais caro, e sem ganho: o servidor já sabe onde
     * cada entidade está.
     *
     * <p>Devolve vazio quando o aldeão não está carregado, o que é o
     * caso comum de colônia longe do jogador.
     */
    private static Optional<VillagerEntity> findVillager(
            ServerWorld world, Colony colony, UUID villagerId) {

        return world.getEntity(villagerId) instanceof VillagerEntity villager
                ? Optional.of(villager)
                : Optional.empty();
    }
}
