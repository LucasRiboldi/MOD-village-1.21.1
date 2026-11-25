package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.service.GradeCrew;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O Zelador executa o plano de alturas — Regra 357, ADR-063 §3. Lê o plano ({@link GradeSurvey}) uma vez por dia de jogo,
 * ou de novo quando as construções da colônia mudam ou a fila acaba, e leva cada coluna ao alvo na ordem da fila: frente
 * de porta, caminho, faixa das estruturas, calombo e buraco pequenos, o resto. Corta e aterra pela coluna segura de
 * sempre ({@link GroundSmoothing#column}, com as recusas das Regras 156 e 356); a coluna recusada fica de fora até a
 * próxima leitura. O orçamento do dia cresce com o atraso ({@link GradeCrew#perDay}).
 */
final class GradeWork {

    /** Quantas colunas o Zelador leva ao alvo por passada. */
    static final int PER_PASS = 16;

    /** O que a colônia está executando: a fila lida, onde parou, e o que fez hoje. */
    private static final class State {
        private final List<int[]> queue;
        private final long readAt;
        private final int buildings;
        private final int budget;
        private int next;
        private long day = -1;
        private int doneToday;

        State(List<int[]> queue, long readAt, int buildings, int budget) {
            this.queue = queue;
            this.readAt = readAt;
            this.buildings = buildings;
            this.budget = budget;
        }
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    static {
        ServerMemory.register(GradeWork.class, GradeWork::clearAll);
    }

    private GradeWork() {
    }

    /** Uma passada do Zelador: até {@value #PER_PASS} colunas, dentro do orçamento do dia. */
    static int pass(ServerWorld world, Colony colony) {
        Optional<State> state = stateOf(world, colony);

        if (state.isEmpty()) {
            return 0;
        }

        long today = Math.floorDiv(world.getTimeOfDay(), WorkClock.DAY);

        if (state.get().day != today) {
            state.get().day = today;
            state.get().doneToday = 0;
        }

        int allowed = Math.min(PER_PASS, state.get().budget - state.get().doneToday);

        if (allowed <= 0) {
            return 0;
        }

        int done = work(world, colony.id(), state.get(), allowed);

        state.get().doneToday += done;

        return done;
    }

    /** Leva até {@code columns} colunas ao alvo, sem orçamento: a passada do Zelador e a bateria. */
    static int apply(ServerWorld world, Colony colony, int columns) {
        return stateOf(world, colony).map(state -> work(world, colony.id(), state, columns)).orElse(0);
    }

    /** Quantas colunas o Zelador levou ao alvo hoje nesta colônia. */
    static int doneToday(UUID colonyId, long day) {
        State state = STATES.get(colonyId);

        return state == null || state.day != day ? 0 : state.doneToday;
    }

    static void forget(UUID colonyId) {
        STATES.remove(colonyId);
    }

    static void clearAll() {
        STATES.clear();
    }

    private static int work(ServerWorld world, UUID colonyId, State state, int columns) {
        int done = 0;

        while (done < columns && state.next < state.queue.size()) {
            int[] item = state.queue.get(state.next++);
            int x = item[0];
            int z = item[1];

            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

            if (top != item[2] && GroundSmoothing.column(world, x, z, top, item[2])) {
                done++;
            }
        }

        if (state.next >= state.queue.size()) {
            // A fila acabou: a próxima passada lê o plano de novo, para ver o que a ordem de hoje ainda deixou.
            STATES.remove(colonyId);
        }

        return done;
    }

    private static Optional<State> stateOf(ServerWorld world, Colony colony) {
        State known = STATES.get(colony.id());
        int buildings = VillageColonyMod.BUILDINGS.ofColony(colony.id()).size();

        if (known != null && world.getTime() - known.readAt < WorkClock.DAY && known.buildings == buildings) {
            return Optional.of(known);
        }

        Optional<GradeSurvey.Survey> survey = GradeSurvey.read(world, colony);

        if (survey.isEmpty()) {
            STATES.remove(colony.id());
            return Optional.empty();
        }

        State fresh = new State(survey.get().work(), world.getTime(), buildings,
                GradeCrew.perDay(survey.get().pending()));

        if (known != null) {
            fresh.day = known.day;
            fresh.doneToday = known.doneToday;
        }

        STATES.put(colony.id(), fresh);

        return Optional.of(fresh);
    }
}
