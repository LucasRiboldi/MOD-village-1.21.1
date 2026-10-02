package com.villagecolony.fabric.overlay;

import com.villagecolony.VillageColonyMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;

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
                .toList());
        server.getPlayerManager().getPlayerList().forEach(player -> {
            if (ServerPlayNetworking.canSend(player, OverlaySnapshotPayload.ID)) {
                ServerPlayNetworking.send(player, snapshot);
            }
        });
    }
}
