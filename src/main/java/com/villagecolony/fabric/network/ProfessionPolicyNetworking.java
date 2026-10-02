package com.villagecolony.fabric.network;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.data.save.ProfessionPolicySavedData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/** Registro e autorização da sincronização servidor-cliente das políticas. */
public final class ProfessionPolicyNetworking {

    private static final int POLICY_PERMISSION_LEVEL = 2;

    private ProfessionPolicyNetworking() {
    }

    /** Registra tipos de pacote e receptores somente uma vez, durante a inicialização comum. */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(ProfessionPolicySyncPayload.ID, ProfessionPolicySyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ProfessionPolicyRequestPayload.ID, ProfessionPolicyRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ProfessionPolicyUpdatePayload.ID, ProfessionPolicyUpdatePayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sendSnapshot(handler.player));
        ServerPlayNetworking.registerGlobalReceiver(ProfessionPolicyRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> sendSnapshot(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(ProfessionPolicyUpdatePayload.ID, (payload, context) ->
                context.server().execute(() -> applyUpdate(context.player(), payload)));
    }

    private static void applyUpdate(ServerPlayerEntity player, ProfessionPolicyUpdatePayload payload) {
        if (!player.hasPermissionLevel(POLICY_PERMISSION_LEVEL)) {
            VillageColonyMod.LOGGER.warn("[Village Colony] Ignored profession policy update from {} without permission",
                    player.getGameProfile().getName());
            sendSnapshot(player);
            return;
        }
        ProfessionPolicySavedData savedData = ProfessionPolicySavedData.get(player.getServer());
        savedData.replace(payload.policies());
        for (ServerPlayerEntity target : player.getServer().getPlayerManager().getPlayerList()) {
            sendSnapshot(target);
        }
    }

    private static void sendSnapshot(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new ProfessionPolicySyncPayload(
                ProfessionPolicySavedData.get(player.getServer()).policies()));
    }
}
