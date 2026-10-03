package com.villagecolony.client.network;

import com.villagecolony.core.worker.model.ProfessionPolicySet;
import com.villagecolony.fabric.network.ProfessionPolicyRequestPayload;
import com.villagecolony.fabric.network.ProfessionPolicySyncPayload;
import com.villagecolony.fabric.network.ProfessionPolicyUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.Optional;

/** Espelho somente de apresentação da política que pertence ao servidor atual. */
public final class ClientProfessionPolicies {

    private static ProfessionPolicySet policies;

    private ClientProfessionPolicies() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ProfessionPolicySyncPayload.ID, (payload, context) ->
                context.client().execute(() -> policies = payload.policies()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> policies = null);
    }

    public static Optional<ProfessionPolicySet> current() {
        return Optional.ofNullable(policies);
    }

    public static void request() {
        ClientPlayNetworking.send(new ProfessionPolicyRequestPayload());
    }

    public static void update(ProfessionPolicySet updatedPolicies) {
        policies = updatedPolicies;
        ClientPlayNetworking.send(new ProfessionPolicyUpdatePayload(updatedPolicies));
    }
}
