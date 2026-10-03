package com.villagecolony.gametest;

import com.villagecolony.fabric.event.VillageDetectionHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Mantém o relógio dos trabalhadores nos cenários Fabric sem jogador.
 *
 * <p>Esta classe pertence somente ao mod de GameTest. Em jogo, a execução
 * continua condicionada à presença atual de um jogador na vila.
 */
public final class GameTestWorkerTickBridge implements ModInitializer {

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(VillageDetectionHandler::tickGameTestServer);
    }
}
