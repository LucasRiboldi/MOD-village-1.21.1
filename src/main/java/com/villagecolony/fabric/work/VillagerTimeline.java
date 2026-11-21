package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A linha do tempo de cada aldeão — Regra 344, telemetria T2. A cada amostra do {@link WorkTime} (uma por segundo), o
 * estado dele (trabalho, caminhada, espera, bloqueio, ócio, encalhe, fora do expediente) entra aqui; quando um estado
 * novo se firma por {@value #SETTLE} amostras seguidas, o diário de ações ganha uma linha {@code STATE}: o estado novo,
 * o de antes, quanto o de antes durou, onde ele estava e o motivo (o pedido que segura parado). O pisca de um segundo
 * não vira linha. O {@code villager_timeline.py} monta o dia de cada aldeão.
 */
public final class VillagerTimeline {

    private static final int RECENT_LIMIT = 8;

    /** Quantas amostras seguidas o estado novo precisa para contar. */
    static final int SETTLE = 5;

    private static final class Track {

        String state;

        long since;
        String reason = "";
        @Nullable BlockPos destination;
        final ArrayDeque<String> recent = new ArrayDeque<>();

        String candidate = "";

        long candidateSince;

        int seen;

        Track(String state, long since) {
            this.state = state;
            this.since = since;
        }
    }

    private static final Map<UUID, Track> TRACKS = new HashMap<>();

    static {
        ServerMemory.register(VillagerTimeline.class, TRACKS::clear);
    }

    private VillagerTimeline() {
    }

    /** Uma amostra do estado deste aldeão. */
    static void sample(UUID id, String state, long now, BlockPos at, String reason) {
        sample(id, state, now, at, reason, null);
    }

    static void sample(UUID id, String state, long now, BlockPos at, String reason, @Nullable BlockPos destination) {
        Track track = TRACKS.get(id);

        if (track == null) {
            track = new Track(state, now);
            track.reason = reason;
            track.destination = destination;
            TRACKS.put(id, track);
            return;
        }

        if (track.state.equals(state)) {
            if (!reason.isEmpty()) {
                track.reason = reason;
            }
            track.destination = destination;
            track.candidate = "";
            track.seen = 0;
            return;
        }

        if (!track.candidate.equals(state)) {
            track.candidate = state;
            track.candidateSince = now;
            track.seen = 0;
        }

        if (++track.seen < SETTLE) {
            return;
        }

        long elapsed = track.candidateSince - track.since;
        ActionJournal.state(id, track.state, state, elapsed, at, describe(track.reason, track.destination));
        if (track.recent.size() == RECENT_LIMIT) {
            track.recent.removeFirst();
        }
        track.recent.addLast(track.candidateSince + ": " + track.state + " (" + elapsed / 20 + " s) "
                + describe(track.reason, track.destination) + " -> " + state + " em " + at.toShortString());
        track.reason = reason;
        track.destination = destination;
        track.state = state;
        track.since = track.candidateSince;
        track.candidate = "";
        track.seen = 0;
    }

    private static String describe(String reason, @Nullable BlockPos destination) {
        return destination == null ? reason : (reason.isEmpty() ? "" : reason + " ")
                + "destino=" + destination.toShortString();
    }

    /** Últimas mudanças firmadas e estado atual dos moradores registrados. */
    public static void append(UUID colony, long now, StringBuilder out) {
        out.append("\n== linha do tempo por aldeão (até 8 mudanças na sessão)\n");
        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony)) {
            out.append(worker.villagerId().toString(), 0, 8).append(' ')
                    .append(worker.profession().map(Enum::name).orElse("sem profissão")).append('\n');
            Track track = TRACKS.get(worker.villagerId());
            if (track == null) {
                out.append("  sem amostra\n");
                continue;
            }
            track.recent.forEach(transition -> out.append("  ").append(transition).append('\n'));
            out.append("  atual: ").append(track.state).append(" (").append((now - track.since) / 20)
                    .append(" s) ").append(describe(track.reason, track.destination)).append('\n');
        }
    }

    /** Esquece este aldeão. */
    static void forget(UUID id) {
        TRACKS.remove(id);
    }
}
