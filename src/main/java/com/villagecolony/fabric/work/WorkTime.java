package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * O tempo dos aldeões — Regra 50, pedido do autor de 2026-10-02: <i>"esse
 * registro de tempo dos aldeões deve ser uma regra de toda verificação, usar
 * como critério de melhoria e correção"</i>.
 *
 * <p><b>O que mede.</b> Uma vez por segundo, no expediente e com a vila
 * atendida, cada trabalhador cai num estado:
 *
 * <ul>
 *   <li><b>trabalhando</b> — o braço em movimento: cavar, cortar, colher,
 *       tosquiar, fabricar, fundir, assentar;
 *   <li><b>andando</b> — andou desde a amostra anterior;
 *   <li><b>esperando</b> — tem tarefa, está parado, sem destino e sem gesto:
 *       procurando alvo, esperando material, varrendo;
 *   <li><b>bloqueado</b> — tem destino e está parado há {@link #BLOCKED_AFTER}
 *       amostras: quer ir e não vai;
 *   <li><b>ocioso</b> — sem tarefa;
 *   <li><b>encalhado</b> — preso num buraco ou num curral.
 * </ul>
 *
 * <p>A cada {@link #REPORT_EVERY} tiques sai uma linha {@code VC_TIME} por
 * colônia, com a proporção de cada estado por profissão. O
 * {@code scripts/time_ledger.py} soma as linhas da sessão e aponta a
 * profissão que passa mais tempo sem trabalhar.
 */
public final class WorkTime {

    static {
        ServerMemory.register(WorkTime.class, WorkTime::clearAll);
    }

    /** O estado de um trabalhador numa amostra. */
    public enum State { WORK, WALK, WAIT, BLOCKED, IDLE, STRANDED }

    /** De quanto em quanto tempo sai a linha: cinco minutos. */
    static final long REPORT_EVERY = 6_000;

    /** Amostras paradas com destino antes de contar como bloqueado. */
    static final int BLOCKED_AFTER = 3;

    private static final class Walker {

        BlockPos last;

        int still;
    }

    /** Segundos por estado, por profissão, por colônia, na janela atual. */
    private static final Map<UUID, Map<String, EnumMap<State, Integer>>> WINDOW = new HashMap<>();

    private static final Map<UUID, Walker> WALKERS = new HashMap<>();

    private static long windowStart = Long.MIN_VALUE;

    private WorkTime() {
    }

    /** Uma amostra: chamada uma vez por segundo. */
    static void sample(ServerWorld world) {
        long now = world.getTime();

        if (windowStart == Long.MIN_VALUE) {
            windowStart = now;
        }

        for (Worker worker : List.copyOf(VillageColonyMod.WORKERS.all())) {
            UUID id = worker.villagerId();

            if (!(world.getEntity(id) instanceof VillagerEntity villager) || !villager.isAlive()
                    || !PenEscape.isWorking(world, worker.colonyId())
                    || !WorkHours.isWorkTime(world, villager)) {
                continue;
            }

            State state = stateOf(id, villager);
            String profession = worker.profession().map(Enum::name).orElse("NONE");

            WINDOW.computeIfAbsent(worker.colonyId(), ignored -> new TreeMap<>())
                    .computeIfAbsent(profession, ignored -> new EnumMap<>(State.class))
                    .merge(state, 1, Integer::sum);
        }

        if (now - windowStart >= REPORT_EVERY) {
            report(now - windowStart);
            WINDOW.clear();
            windowStart = now;
        }
    }

    /** O estado de agora, e a memória de quem está parado. */
    static State stateOf(UUID id, VillagerEntity villager) {
        Walker walker = WALKERS.computeIfAbsent(id, ignored -> new Walker());
        BlockPos here = villager.getBlockPos().toImmutable();
        boolean moved = walker.last != null && !walker.last.equals(here);

        walker.still = moved ? 0 : walker.still + 1;
        walker.last = here;

        return classify(
                StrandedWorkers.isStranded(id) || PenEscape.isEscaping(id),
                !VillageColonyMod.TASKS.assignedTo(id).isEmpty() || GroundPickup.isHelping(id),
                villager.handSwinging,
                moved,
                WorkTargets.of(id).isPresent(),
                walker.still);
    }

    /** A regra da classificação, sem o mundo — para o teste. */
    static State classify(
            boolean stranded, boolean hasTask, boolean swinging, boolean moved, boolean hasTarget, int still) {

        if (stranded) {
            return State.STRANDED;
        }

        if (!hasTask) {
            return State.IDLE;
        }

        if (swinging) {
            return State.WORK;
        }

        if (moved) {
            return State.WALK;
        }

        if (hasTarget) {
            return still >= BLOCKED_AFTER ? State.BLOCKED : State.WALK;
        }

        return State.WAIT;
    }

    private static void report(long ticks) {
        for (Map.Entry<UUID, Map<String, EnumMap<State, Integer>>> colony : WINDOW.entrySet()) {
            StringBuilder line = new StringBuilder();

            for (Map.Entry<String, EnumMap<State, Integer>> profession : colony.getValue().entrySet()) {
                line.append(' ').append(describe(profession.getKey(), profession.getValue()));
            }

            VillageColonyMod.LOGGER.info("VC_TIME version=1 colony={} window={}s{}",
                    colony.getKey().toString().substring(0, 8), ticks / 20, line);
        }
    }

    /** {@code MINER[s=600 work=40 walk=20 wait=10 blocked=5 idle=20 stranded=5]}, em %. */
    static String describe(String profession, Map<State, Integer> seconds) {
        int total = seconds.values().stream().mapToInt(Integer::intValue).sum();
        StringBuilder out = new StringBuilder(profession).append("[s=").append(total);

        for (State state : State.values()) {
            int share = total == 0 ? 0 : Math.round(100f * seconds.getOrDefault(state, 0) / total);

            out.append(' ').append(state.name().toLowerCase(Locale.ROOT)).append('=').append(share);
        }

        return out.append(']').toString();
    }

    static void clearAll() {
        WINDOW.clear();
        WALKERS.clear();
        windowStart = Long.MIN_VALUE;
    }
}
