package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.util.math.BlockPos;

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
final class VillagerTimeline {

    /** Quantas amostras seguidas o estado novo precisa para contar. */
    static final int SETTLE = 5;

    private static final class Track {

        String state;

        long since;

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
        Track track = TRACKS.get(id);

        if (track == null) {
            TRACKS.put(id, new Track(state, now));
            return;
        }

        if (track.state.equals(state)) {
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

        ActionJournal.state(id, track.state, state, track.candidateSince - track.since, at, reason);
        track.state = state;
        track.since = track.candidateSince;
        track.candidate = "";
        track.seen = 0;
    }

    /** Esquece este aldeão. */
    static void forget(UUID id) {
        TRACKS.remove(id);
    }
}
