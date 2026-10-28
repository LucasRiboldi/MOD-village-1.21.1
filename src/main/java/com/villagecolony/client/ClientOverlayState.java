package com.villagecolony.client;

import com.villagecolony.fabric.overlay.OverlaySnapshotPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Cache exclusivamente cliente; é substituído atomically quando chega um snapshot do servidor. */
public final class ClientOverlayState {

    private static volatile Map<UUID, String> professions = Map.of();
    private static volatile Map<UUID, String> badges = Map.of();
    private static volatile Map<UUID, OverlaySnapshotPayload.ConstructionEntry> constructions = Map.of();

    private ClientOverlayState() {
    }

    public static void replace(OverlaySnapshotPayload payload) {
        Map<UUID, String> next = new ConcurrentHashMap<>();
        payload.workers().forEach(worker -> next.put(worker.id(), worker.profession()));
        professions = Map.copyOf(next);
        Map<UUID, String> nextBadges = new ConcurrentHashMap<>();
        payload.workers().forEach(worker -> nextBadges.put(worker.id(), worker.badge()));
        badges = Map.copyOf(nextBadges);
        Map<UUID, OverlaySnapshotPayload.ConstructionEntry> nextConstructions = new ConcurrentHashMap<>();
        payload.constructions().forEach(construction -> nextConstructions.put(construction.id(), construction));
        constructions = Map.copyOf(nextConstructions);
    }

    public static String professionOf(UUID id) {
        return professions.get(id);
    }

    /** O estado do aldeão na última amostra do servidor ({@code WorkTime.Badge}), ou vazio. */
    public static String badgeOf(UUID id) {
        return badges.getOrDefault(id, "");
    }

    /**
     * Se o painel da profissão desenha o nome deste aldeão — e então a
     * plaquinha Vanilla sobre a cabeça dele não é desenhada. O texto do
     * painel é sempre visível desde a P0.7 (09-10), que tirou a opção de
     * escondê-lo.
     */
    public static boolean drawsNameOf(UUID id) {
        return professions.containsKey(id);
    }

    public static Iterable<OverlaySnapshotPayload.ConstructionEntry> constructions() {
        return constructions.values();
    }

    public static void clear() {
        professions = Map.of();
        badges = Map.of();
        constructions = Map.of();
    }
}
