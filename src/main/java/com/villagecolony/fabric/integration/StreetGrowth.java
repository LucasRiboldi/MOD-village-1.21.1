package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.NeighborhoodRoads;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O braço da malha cresce pela recusa de lote — Regra 389 (ADR-065, emenda de 27-11, M1 🅲). Cada coluna recusada conta
 * para o lado da vila em que está (leste, oeste, sul, norte, pelo eixo mais perto do centro); quando a volta da busca de lote
 * fecha sem lote, o lado com mais recusas ganha a vez de crescer, e a conta recomeça. A montagem do grafo gasta a vez.
 */
public final class StreetGrowth {

    private static final Map<UUID, int[]> REFUSED = new HashMap<>();

    private static final Map<UUID, boolean[]> PENDING = new HashMap<>();

    static {
        ServerMemory.register(StreetGrowth.class, StreetGrowth::clearAll);
    }

    private StreetGrowth() {
    }

    /** Uma coluna recusada desta colônia, se ela é conhecida. */
    static void refusedAt(UUID colonyId, int x, int z) {
        VillageColonyMod.COLONIES.find(colonyId).ifPresent(colony -> count(colonyId, colony.center(), x, z));
    }

    /** Conta a recusa no lado em que está a coluna. */
    static void count(UUID colonyId, ColonyPos center, int x, int z) {
        int side = NeighborhoodRoads.sideOf(x - center.x(), z - center.z());

        if (side >= 0) {
            REFUSED.computeIfAbsent(colonyId, ignored -> new int[4])[side]++;
        }
    }

    /** A volta da busca de lote fechou sem lote: o lado com mais recusas ganha a vez, e a conta recomeça. */
    public static void noLot(UUID colonyId) {
        int[] counts = REFUSED.remove(colonyId);

        if (counts == null) {
            return;
        }

        int best = -1;

        for (int side = 0; side < counts.length; side++) {
            if (counts[side] > 0 && (best < 0 || counts[side] > counts[best])) {
                best = side;
            }
        }

        if (best >= 0) {
            PENDING.computeIfAbsent(colonyId, ignored -> new boolean[4])[best] = true;
        }
    }

    /** Os lados que ganharam a vez de crescer, e a vez é gasta. */
    public static boolean[] take(UUID colonyId) {
        boolean[] pending = PENDING.remove(colonyId);

        return pending == null ? new boolean[4] : pending;
    }

    static void clearAll() {
        REFUSED.clear();
        PENDING.clear();
    }
}
