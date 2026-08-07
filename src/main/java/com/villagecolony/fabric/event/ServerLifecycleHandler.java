package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

/**
 * Registra os eventos de ciclo de vida do servidor.
 *
 * <p>Estes são os dois pontos onde o estado da colônia entra e sai da
 * memória: as colônias são carregadas quando o servidor sobe e devem
 * estar gravadas antes de ele parar. Ver ADR-002 e Save-Data-System.md.
 *
 * <p>Nenhum estado é criado aqui ainda — as colônias só existem a partir
 * de TASK-005.
 */
public final class ServerLifecycleHandler {

    private ServerLifecycleHandler() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleHandler::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerLifecycleHandler::onServerStopping);
    }

    private static void onServerStarted(MinecraftServer server) {
        VillageColonyMod.LOGGER.info("Server started — colony state not loaded yet (TASK-005)");
    }

    private static void onServerStopping(MinecraftServer server) {
        VillageColonyMod.LOGGER.info("Server stopping — no colony state to persist yet");
    }
}
