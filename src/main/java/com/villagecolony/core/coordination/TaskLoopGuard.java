package com.villagecolony.core.coordination;

import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * O pedido que fecha sem resultado entra em espera — Regra 191 (autor, 2026-11-05).
 *
 * <p>Os prazos de travamento só veem quem anda sem chegar. Tarefa que fecha "certa" com 0 peça volta no ciclo seguinte e
 * nenhum deles dispara. Aqui, {@value #STRIKES} fechamentos seguidos sem resultado seguram o pedido por
 * {@value #FIRST_HOLD} ciclos; cada nova falta depois disso dobra a espera, até {@value #LONGEST_HOLD} ciclos. Peça feita
 * zera a conta; o cru do pedido aumentar nos baús, ou uma obra nova, solta a espera. O relógio é o ciclo da colônia, porque
 * o Core não conhece o tempo do mundo (ADR-005). Nada é salvo: reiniciar recomeça a conta.
 */
public final class TaskLoopGuard {

    static final int STRIKES = 3;

    /** Um minuto: o ciclo é de 30 s. */
    static final int FIRST_HOLD = 2;

    /** Trinta minutos. */
    static final int LONGEST_HOLD = 60;

    private record Key(UUID colonyId, TaskType type, ResourceType target) {
    }

    private static final class State {
        int strikes;
        int hold;
        long until;
        int rawSeen = -1;
    }

    private static final Map<Key, State> STATES = new HashMap<>();

    private static final Map<UUID, Long> CYCLES = new HashMap<>();

    static {
        ServerMemory.register(TaskLoopGuard.class, TaskLoopGuard::clearAll);
    }

    private TaskLoopGuard() {
    }

    /** Uma passagem do ciclo desta colônia. */
    static void aCycleWentBy(UUID colonyId) {
        CYCLES.merge(colonyId, 1L, Long::sum);
    }

    /**
     * O pedido fechou sem resultado.
     *
     * @return por quantos ciclos ele entrou em espera agora; 0 se ainda não entrou
     */
    public static int fruitless(Task task) {
        State state = STATES.computeIfAbsent(keyOf(task), ignored -> new State());

        state.strikes++;

        if (state.hold == 0 && state.strikes < STRIKES) {
            return 0;
        }

        state.strikes = 0;
        state.hold = state.hold == 0 ? FIRST_HOLD : Math.min(state.hold * 2, LONGEST_HOLD);
        state.until = cycleOf(task.colonyId()) + state.hold;
        state.rawSeen = -1;

        return state.hold;
    }

    /** O pedido rendeu: a conta acaba. */
    public static void fruitful(Task task) {
        STATES.remove(keyOf(task));
    }

    /** Se este pedido está em espera agora. */
    public static boolean isHeld(Task task) {
        State state = STATES.get(keyOf(task));

        return state != null && cycleOf(task.colonyId()) < state.until;
    }

    /** O cru de um pedido em espera aumentou nos baús: ele volta. */
    static void reviewStock(UUID colonyId, ResourceTally owned) {
        long now = cycleOf(colonyId);

        STATES.entrySet().removeIf(entry -> {
            State state = entry.getValue();

            if (!entry.getKey().colonyId().equals(colonyId) || now >= state.until) {
                return false;
            }

            var raw = StockRules.rawOf(entry.getKey().target());

            if (raw.isEmpty()) {
                return false;
            }

            int amount = owned.amountOf(raw.get());
            boolean arrived = state.rawSeen >= 0 && amount > state.rawSeen;

            state.rawSeen = amount;

            return arrived;
        });
    }

    /** Obra nova muda o que se pede: as esperas desta colônia acabam. */
    public static void clear(UUID colonyId) {
        STATES.keySet().removeIf(key -> key.colonyId().equals(colonyId));
    }

    private static long cycleOf(UUID colonyId) {
        return CYCLES.getOrDefault(colonyId, 0L);
    }

    private static Key keyOf(Task task) {
        Objects.requireNonNull(task, "task");

        return new Key(task.colonyId(), task.type(), task.targetResource());
    }

    private static void clearAll() {
        STATES.clear();
        CYCLES.clear();
    }
}
