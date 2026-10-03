package com.villagecolony.fabric.overlay;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.SiteMarker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;

import java.util.Map;

/** Publica um snapshot leve a cada segundo; o renderer nunca consulta o servidor por frame. */
public final class OverlaySync {

    private static final int EVERY_TICKS = 20;
    private static int ticks;

    private OverlaySync() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(OverlaySync::tick);
    }

    private static void tick(MinecraftServer server) {
        if (++ticks < EVERY_TICKS) {
            return;
        }
        ticks = 0;
        OverlaySnapshotPayload snapshot = new OverlaySnapshotPayload(VillageColonyMod.WORKERS.all().stream()
                .flatMap(worker -> worker.profession().stream()
                        .map(profession -> new OverlaySnapshotPayload.WorkerEntry(
                                worker.villagerId(), profession.name())))
                .toList(), VillageColonyMod.CONSTRUCTIONS.all().stream()
                .filter(project -> project.state().isOpen())
                .map(OverlaySync::constructionEntry)
                .toList());
        server.getPlayerManager().getPlayerList().forEach(player -> {
            if (ServerPlayNetworking.canSend(player, OverlaySnapshotPayload.ID)) {
                ServerPlayNetworking.send(player, snapshot);
            }
        });
    }

    private static OverlaySnapshotPayload.ConstructionEntry constructionEntry(
            ConstructionProject project) {
        Map<ResourceId, Integer> stock = SiteMarker.rememberedStock(project.colonyId())
                .map(tally -> tally.idCounts()).orElse(Map.of());
        String missing = project.remainingMaterials().entrySet().stream()
                .filter(entry -> stock.getOrDefault(entry.getKey(), 0) < entry.getValue())
                .map(entry -> entry.getKey() + " " + Math.max(0,
                        entry.getValue() - stock.getOrDefault(entry.getKey(), 0)))
                .findFirst().orElse("");
        int total = project.blueprint().blockCount();
        return new OverlaySnapshotPayload.ConstructionEntry(project.id(),
                project.blueprint().id().toString(), project.state().name(),
                project.origin().x(), project.origin().y(), project.origin().z(),
                total - project.remainingCount(), total, missing);
    }
}
