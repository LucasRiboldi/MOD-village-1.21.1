package com.villagecolony.client;

import com.villagecolony.fabric.overlay.OverlaySnapshotPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Cache exclusivamente cliente; é substituído atomically quando chega um snapshot do servidor. */
public final class ClientOverlayState {

    private static volatile Map<UUID, String> professions = Map.of();

    private ClientOverlayState() {
    }

    public static void replace(OverlaySnapshotPayload payload) {
        Map<UUID, String> next = new ConcurrentHashMap<>();
        payload.workers().forEach(worker -> next.put(worker.id(), worker.profession()));
        professions = Map.copyOf(next);
    }

    public static String professionOf(UUID id) {
        return professions.get(id);
    }

    public static void clear() {
        professions = Map.of();
    }
}
