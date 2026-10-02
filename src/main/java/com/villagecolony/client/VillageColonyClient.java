package com.villagecolony.client;

import com.villagecolony.fabric.overlay.OverlaySnapshotPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Entry point físico do cliente. Nenhuma classe do servidor importa MinecraftClient. */
public final class VillageColonyClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OverlaySnapshotPayload.ID,
                (payload, context) -> ClientOverlayState.replace(payload));
        ProfessionOverlayRenderer.register();
    }
}
