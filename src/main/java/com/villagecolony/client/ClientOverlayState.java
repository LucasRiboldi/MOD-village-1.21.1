package com.villagecolony.client;

import com.villagecolony.fabric.overlay.OverlaySnapshotPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Cache exclusivamente cliente; é substituído atomically quando chega um snapshot do servidor. */
public final class ClientOverlayState {

    private static volatile Map<UUID, String> professions = Map.of();
    private static volatile Map<UUID, OverlaySnapshotPayload.ConstructionEntry> constructions = Map.of();

    private ClientOverlayState() {
    }

    public static void replace(OverlaySnapshotPayload payload) {
        Map<UUID, String> next = new ConcurrentHashMap<>();
        payload.workers().forEach(worker -> next.put(worker.id(), worker.profession()));
        professions = Map.copyOf(next);
        Map<UUID, OverlaySnapshotPayload.ConstructionEntry> nextConstructions = new ConcurrentHashMap<>();
        payload.constructions().forEach(construction -> nextConstructions.put(construction.id(), construction));
        constructions = Map.copyOf(nextConstructions);
    }

    public static String professionOf(UUID id) {
        return professions.get(id);
    }

    public static Iterable<OverlaySnapshotPayload.ConstructionEntry> constructions() {
        return constructions.values();
    }

    public static void clear() {
        professions = Map.of();
        constructions = Map.of();
    }
}
