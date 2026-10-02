package com.villagecolony.client;

import com.villagecolony.client.network.ClientProfessionPolicies;
import net.fabricmc.api.ClientModInitializer;

/** Ponto de entrada exclusivo do cliente. */
public final class VillageColonyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientProfessionPolicies.register();
    }
}
