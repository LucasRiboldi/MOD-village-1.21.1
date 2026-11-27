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
        private final GradeSurvey.Survey survey;
        private final long readAt;
        private final int buildings;
        private final @org.jspecify.annotations.Nullable Object open;
        private final int budget;
        private int next;
        private long day = -1;
        private int doneToday;

        State(List<int[]> queue, GradeSurvey.Survey survey, long readAt, int buildings,
              @org.jspecify.annotations.Nullable Object open, int budget) {
            this.queue = queue;
            this.survey = survey;
            this.readAt = readAt;
            this.buildings = buildings;
            this.open = open;
            this.budget = budget;
        }
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** Quantas colunas à frente na fila o Zelador olha para escolher a mais perto dele (Regra 391). */
    static final int LOOKAHEAD = 32;

    /** Quanto o Zelador anda até uma coluna antes de desistir dela, em tiques. */
    static final int GIVE_UP_AFTER = 1_200;

    /** Colônia → a coluna em que o Zelador trabalha agora: {x, z, alvo, tiques andados} (Regra 391). */
    private static final Map<UUID, int[]> TARGETS = new HashMap<>();

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

    /**
     * Uma passada do Zelador ao lado do bloco — Regra 391 (L3 do jogo da 0.4.150, com o pedido do K3: "o Zelador fica ao lado
     * do bloco e põe ou tira um por um"): entre as próximas {@value #LOOKAHEAD} colunas da fila, da mesma prioridade da
     * primeira, a mais perto dele; ele anda
     * até ela e leva uma coluna ao alvo por passada, dentro do orçamento do dia, só quando não está calçando. Devolve 1 com a
     * coluna feita, -1 enquanto anda e 0 sem nada a fazer.
     */
    static int walk(ServerWorld world, Colony colony, net.minecraft.entity.passive.VillagerEntity keeper) {
        Optional<State> found = PathPaving.isPaving(keeper.getUuid()) ? Optional.empty() : stateOf(world, colony);

        if (found.isEmpty()) {
            return 0;
        }

        State state = found.get();
        long today = Math.floorDiv(world.getTimeOfDay(), WorkClock.DAY);

        if (state.day != today) {
            state.day = today;
            state.doneToday = 0;
        }

        if (state.doneToday >= state.budget) {
            return 0;
        }

        int[] target = TARGETS.get(colony.id());

        if (target == null || !pending(world, target)) {
            target = nearest(world, colony.id(), state, keeper.getBlockPos());

            if (target == null) {
                TARGETS.remove(colony.id());
                return 0;
            }

            TARGETS.put(colony.id(), target);
        }

        net.minecraft.util.math.BlockPos at = new net.minecraft.util.math.BlockPos(target[0], topOf(world, target[0], target[1]),
                target[1]);

        if (!beside(keeper.getBlockPos(), at)) {
            target[3] += GroundKeeper.EVERY;

            if (target[3] > GIVE_UP_AFTER) {
                TARGETS.remove(colony.id());
                com.villagecolony.fabric.brain.WorkTargets.clear(keeper.getUuid());
                return 0;
            }

            com.villagecolony.fabric.brain.WorkTargets.set(keeper.getUuid(),
                    WalkLegs.towards(world, keeper.getBlockPos(), at.up()).orElse(at.up()), 2);
            return -1;
        }

        TARGETS.remove(colony.id());
        com.villagecolony.fabric.brain.WorkTargets.clear(keeper.getUuid());

        if (GroundSmoothing.column(world, target[0], target[1], at.getY(), target[2], "plano_alturas")) {
            state.doneToday++;
            return 1;
        }

        return 0;
    }

    /** A coluna em que o Zelador trabalha agora nesta colônia, para o teste e o log. */
    static Optional<int[]> targetOf(UUID colonyId) {
        return Optional.ofNullable(TARGETS.get(colonyId)).map(int[]::clone);
    }

    /**
     * Se o Zelador está ao lado da coluna: a até {@code REACH + 1} na horizontal, e a até o mesmo tanto acima ou abaixo, porque
     * o chão em volta da coluna a nivelar é de outra altura.
     */
    static boolean beside(net.minecraft.util.math.BlockPos keeper, net.minecraft.util.math.BlockPos column) {
        int reach = PathPaving.REACH + 1;
        long dx = (long) keeper.getX() - column.getX();
        long dz = (long) keeper.getZ() - column.getZ();

        return dx * dx + dz * dz <= (long) reach * reach && Math.abs(keeper.getY() - column.getY()) <= reach;
    }

    private static boolean pending(ServerWorld world, int[] target) {
        return world.isChunkLoaded(target[0] >> 4, target[1] >> 4) && topOf(world, target[0], target[1]) != target[2];
    }

    private static int topOf(ServerWorld world, int x, int z) {
        return world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    /** A coluna mais perto de {@code from} entre as próximas {@value #LOOKAHEAD} da fila, tirada da fila; nula no fim dela. */
    private static int @org.jspecify.annotations.Nullable [] nearest(ServerWorld world, UUID colonyId, State state,
                                                                      net.minecraft.util.math.BlockPos from) {
        while (state.next < state.queue.size() && !pending(world, state.queue.get(state.next))) {
            state.next++;
        }

        if (state.next >= state.queue.size()) {
            // A fila acabou: a próxima passada lê o plano de novo.
            STATES.remove(colonyId);
            return null;
        }

        int best = state.next;
        long bestDistance = Long.MAX_VALUE;
        // Só entre as colunas da mesma prioridade da primeira da fila: a ordem do plano fica (ADR-063 §3).
        int rank = state.queue.get(state.next)[3];

        for (int i = state.next; i < Math.min(state.queue.size(), state.next + LOOKAHEAD); i++) {
            int[] item = state.queue.get(i);
            long dx = (long) item[0] - from.getX();
            long dz = (long) item[1] - from.getZ();

            if (item[3] == rank && pending(world, item) && dx * dx + dz * dz < bestDistance) {
                best = i;
                bestDistance = dx * dx + dz * dz;
            }
        }

        java.util.Collections.swap(state.queue, best, state.next);

        int[] item = state.queue.get(state.next++);

        return new int[] {item[0], item[1], item[2], 0};
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

    /**
     * Se o plano de alturas cobre esta coluna — Regra 391 (L3 🅱️ do jogo da 0.4.150): o plano é a única régua, e o vão entre
     * as casas ({@link BuildingGaps}) não mexe na coluna dele. Sem plano lido, nenhuma.
     */
    static boolean covers(ServerWorld world, Colony colony, int x, int z) {
        return stateOf(world, colony).filter(state -> state.survey.kind(x, z) != com.villagecolony.core.construction.model.GradePlan.Kind.OUT).isPresent();
    }

    static void forget(UUID colonyId) {
        STATES.remove(colonyId);
        TARGETS.remove(colonyId);
    }

    static void clearAll() {
        STATES.clear();
        TARGETS.clear();
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

            if (top != item[2] && GroundSmoothing.column(world, x, z, top, item[2], "plano_alturas")) {
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
        // A obra que abre entra no plano já: a volta dela é do Zelador desde a abertura (Regra 364).
        Object open = VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).map(project -> (Object) project.origin()).orElse(null);

        if (known != null && world.getTime() - known.readAt < WorkClock.DAY && known.buildings == buildings
                && java.util.Objects.equals(known.open, open)) {
            return Optional.of(known);
        }

        Optional<GradeSurvey.Survey> survey = GradeSurvey.read(world, colony);

        if (survey.isEmpty()) {
            STATES.remove(colony.id());
            return Optional.empty();
        }

        State fresh = new State(survey.get().work(), survey.get(), world.getTime(), buildings, open,
                GradeCrew.perDay(survey.get().pending()));

        if (known != null) {
            fresh.day = known.day;
            fresh.doneToday = known.doneToday;
        }

        STATES.put(colony.id(), fresh);

        return Optional.of(fresh);
    }
}
