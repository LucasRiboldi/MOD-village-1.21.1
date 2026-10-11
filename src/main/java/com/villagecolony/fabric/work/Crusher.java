package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.LastShortage;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O britador do pedreiro — P2c, autossuficiência, 2026-10-11 (escolha do autor).
 *
 * <p>Quando a areia falta e a busca na natureza voltou vazia, o pedreiro mói
 * pedregulho em cascalho e cascalho em areia, dois para um em cada passo (a
 * proporção do britador do MineColonies). É uma rota de produção que a vila
 * sustenta em qualquer bioma: a mina traz o pedregulho. Ele não mói o
 * pedregulho que a obra aberta ainda vai pôr ({@link BuildReserve}).
 */
public final class Crusher {

    /** Quantos de um degrau fazem um do seguinte. */
    static final int RATIO = 2;

    /** Quanta areia um pedido ao pedreiro pede. */
    static final int ORDER = 8;

    private Crusher() {
    }

    /**
     * Abre o pedido de areia ao pedreiro: a falta foi medida, a varredura da
     * natureza voltou vazia, há pedreiro e há pedra para moer.
     */
    public static void askTheMason(ServerWorld world, Colony colony) {
        UUID colonyId = colony.id();

        if (!LastShortage.stillMissing(colonyId, ResourceType.SAND)
                || !EmptySweeps.sweptEmpty(colonyId, ResourceType.SAND)
                || WorkAssignment.countCapableOf(
                        colonyId, TaskType.CRAFT_STONE_MATERIAL.required(), VillageColonyMod.WORKERS) == 0
                || !canCrush(world, colony)) {
            return;
        }

        for (Task task : VillageColonyMod.TASKS.ofColony(colonyId)) {
            if (task.type() == TaskType.CRAFT_STONE_MATERIAL && task.isOpen()
                    && task.targetResource() == ResourceType.SAND) {
                return;
            }
        }

        VillageColonyMod.TASKS.create(
                colonyId, TaskType.CRAFT_STONE_MATERIAL, TaskPriority.CONSTRUCTION_MATERIAL, ResourceType.SAND, ORDER);

        VillageColonyMod.LOGGER.info(
                "Colony {} has no sand in reach — the mason crushes {} out of stone, {} to 1 a step",
                colonyId, ORDER, RATIO);
    }

    /** Se a colônia tem cascalho ou pedregulho acima da reserva da obra para moer. */
    static boolean canCrush(ServerWorld world, Colony colony) {
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());

        return ColonyChests.countIn(world, chests, Items.GRAVEL) >= RATIO || spareCobblestone(world, colony, chests) >= RATIO;
    }

    /**
     * Um passo do britador: dois cascalhos viram uma areia; sem cascalho, dois
     * pedregulhos viram um cascalho.
     *
     * @return o que saiu, ou vazio quando não havia o que moer
     */
    static Optional<Item> crushOne(ServerWorld world, Colony colony, ColonyPos into) {
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());

        if (ColonyChests.countIn(world, chests, Items.GRAVEL) >= RATIO) {
            return turn(world, chests, Items.GRAVEL, Items.SAND, into);
        }

        if (spareCobblestone(world, colony, chests) >= RATIO) {
            return turn(world, chests, Items.COBBLESTONE, Items.GRAVEL, into);
        }

        return Optional.empty();
    }

    /**
     * Um passo do pedreiro no pedido de areia ({@link CraftingWork#craftOne}).
     *
     * @return false quando o pedido acabou
     */
    static boolean step(ServerWorld world, CraftingWork.Job job, UUID workerId, ColonyPos own) {
        Optional<Colony> colony = VillageColonyMod.COLONIES.find(job.task.colonyId());
        Optional<Item> made = colony.flatMap(found -> crushOne(world, found, own));

        if (made.isEmpty()) {
            CraftingWork.finish(job, workerId, "no stone above what the open build needs is left to crush");

            return false;
        }

        if (made.get() == Items.SAND && ++job.crafted >= job.task.amount()) {
            CraftingWork.finish(job, workerId, "crushed the sand it was asked");

            return false;
        }

        return true;
    }

    private static int spareCobblestone(ServerWorld world, Colony colony, List<ColonyPos> chests) {
        return ColonyChests.countIn(world, chests, Items.COBBLESTONE) - BuildReserve.buildNeedOf(colony, Items.COBBLESTONE);
    }

    private static Optional<Item> turn(
            ServerWorld world, List<ColonyPos> chests, Item from, Item to, ColonyPos into) {

        int taken = ColonyChests.withdraw(world, chests, from, RATIO);

        if (taken < RATIO) {
            ColonyChests.deposit(world, List.of(into), from, taken);

            return Optional.empty();
        }

        if (ColonyChests.deposit(world, List.of(into), to, 1) > 0) {
            // Sem lugar para o que saiu: a pedra volta, e nada se perde.
            ColonyChests.deposit(world, chests, from, RATIO);

            return Optional.empty();
        }

        return Optional.of(to);
    }
}
