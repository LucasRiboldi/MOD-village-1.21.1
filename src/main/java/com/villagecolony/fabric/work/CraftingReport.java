package com.villagecolony.fabric.work;

import com.villagecolony.fabric.work.CraftingWork.Job;
import com.villagecolony.VillageColonyMod;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import com.villagecolony.fabric.integration.ColonySupply;
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
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
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
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O que o ofício de bancada diz no log quando trabalha e quando fica parado —
 * separado de {@link CraftingWork} em 2026-09-24, quando ele passou de 500
 * linhas. Os comentários vieram junto sem mudança.
 */
final class CraftingReport {

    private CraftingReport() {
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
    static void reportIdle(Colony colony, TaskType type) {
        String subject = CraftingWork.subjectOf(type);

        int hands = WorkAssignment.countCapableOf(
                colony.id(), type.required(), VillageColonyMod.WORKERS);

        if (hands == 0) {
            IdleLog.record(colony.id(), subject, IdleReason.NO_WORKER);

            return;
        }

        boolean anyTask = false;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() == type && CraftingWork.isOngoing(task)) {
                anyTask = true;

                break;
            }
        }

        IdleLog.record(
                colony.id(),
                subject,
                anyTask ? IdleReason.NO_EXECUTOR : IdleReason.NO_TASK,
                hands + " able to");
    }

    /**
     * Uma linha por ciclo dizendo o que cada fabricante está fazendo.
     *
     * <p>A mesma lição do lenhador mudo: sem isto, um fabricante parado
     * por falta de baú, por horário, por chunk descarregado ou por falta
     * de tronco produzem exatamente o mesmo silêncio.
     */
    static void report(ServerWorld world, Colony colony, TaskType type) {
        StringBuilder line = new StringBuilder();
        int reported = 0;

        for (Map.Entry<UUID, Job> entry : CraftingWork.JOBS.entrySet()) {
            Job job = entry.getValue();

            if (!job.task.belongsTo(colony.id())) {
                continue;
            }

            // Só a oficina desta passagem — 2026-09-10, e é conserto de
            // um defeito que a divisão do fabricante trouxe. Sem este
            // filtro a linha saía DUAS VEZES por ciclo, idêntica: o run
            // de dois argumentos chama o parametrizado uma vez por
            // oficina, e o relatório varria todos os jobs da colônia nas
            // duas. Achado relendo o ciclo, não em jogo.
            if (job.task.type() != type) {
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

        // "manufacturers" morreu com a divisão, e o nome no log tem de
        // dizer de qual oficina se fala — senão duas linhas parecidas na
        // mesma colônia ficam indistinguíveis.
        VillageColonyMod.LOGGER.info(
                "Colony {} {}s: {}", colony.id(), CraftingWork.subjectOf(type), line);
    }

    static String describe(ServerWorld world, UUID workerId, Job job) {
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

        return (distance <= CraftingWork.REACH ? "at the chest" : "walking to the chest, " + distance
                + " blocks away")
                + ", " + clock
                + ", " + job.progress + "/" + CraftingWork.TICKS_PER_PIECE + " ticks"
                + " (" + job.crafted + " pieces so far)";
    }
}
