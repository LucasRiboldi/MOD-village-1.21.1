package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.brain.WorkHours;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A ronda da roça — Regra 333, ADR-060. O trabalho do fazendeiro segue a roça, e não o estoque: sem tarefa de comida
 * aberta na fila, o fazendeiro livre faz a ronda da roça dele com o mesmo trabalho de sempre (colhe o maduro e guarda,
 * ara e semeia o canteiro, composta e põe pó de osso). A ronda é uma tarefa só dele, fora da fila. Acaba quando chega
 * a tarefa da fila, no fim do expediente, ou quando não há nada a fazer; aí a vila pode pedir a plantação grande
 * (Regras 241 e 267), e ele descansa {@link #REST} tiques antes da próxima ronda.
 */
final class FarmRound {

    static {
        ServerMemory.register(FarmRound.class, FarmRound::clearAll);
    }

    /** Um minuto entre o "nada a fazer" e a próxima ronda. */
    static final long REST = 1_200;

    /** Quanto uma ronda colhe antes de acabar e outra começar. */
    static final int AMOUNT = 64;

    private static final Map<UUID, Task> ROUNDS = new HashMap<>();

    /** Fazendeiro → até quando ele descansa da ronda. */
    private static final Map<UUID, Long> RESTING = new HashMap<>();

    private FarmRound() {
    }

    /** Se a tarefa é uma ronda, e não um pedido da fila. */
    static boolean isRound(Task task) {
        return ROUNDS.containsValue(task);
    }

    /** Se o fazendeiro está na ronda da roça. */
    static boolean isOnRound(UUID farmer) {
        Task round = ROUNDS.get(farmer);

        return round != null && round.isHeld();
    }

    /**
     * As rondas novas desta passagem: uma para cada fazendeiro livre, no expediente, sem trabalho e sem descanso. A
     * ronda de quem saiu do expediente acaba.
     *
     * @param busy os fazendeiros que já têm trabalho
     */
    static Map<UUID, Task> offer(ServerWorld world, Colony colony, Set<UUID> busy) {
        Map<UUID, Task> started = new HashMap<>();
        long now = world.getTime();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            UUID id = worker.villagerId();

            if (worker.profession().filter(ProfessionType.FARMER::equals).isEmpty()
                    || !(world.getEntity(id) instanceof VillagerEntity farmer)) {
                continue;
            }

            if (!WorkHours.isWorkTime(world, farmer)) {
                end(id);
                continue;
            }

            if (busy.contains(id) || isOnRound(id) || now < RESTING.getOrDefault(id, Long.MIN_VALUE)
                    || !VillageColonyMod.TASKS.assignedTo(id).isEmpty() || !IdleWorkers.isFree(world, farmer, null)) {
                continue;
            }

            Task round = Task.create(colony.id(), TaskType.COLLECT_FOOD, TaskPriority.PRODUCTION, ResourceType.WHEAT, AMOUNT);

            round.reserveFor(id);
            round.start();
            ROUNDS.put(id, round);
            started.put(id, round);
        }

        return started;
    }

    /** A ronda deste fazendeiro acaba: chegou a tarefa da fila ou o expediente terminou. */
    static void end(UUID farmer) {
        Task round = ROUNDS.remove(farmer);

        if (round != null && round.isHeld()) {
            round.cancel();
        }
    }

    /** Não há nada a fazer na roça: a ronda acaba, e ele descansa antes da próxima. */
    static void nothingLeft(ServerWorld world, UUID farmer) {
        if (ROUNDS.containsKey(farmer)) {
            end(farmer);
            RESTING.put(farmer, world.getTime() + REST);
        }
    }

    static void clearAll() {
        ROUNDS.clear();
        RESTING.clear();
    }
}
