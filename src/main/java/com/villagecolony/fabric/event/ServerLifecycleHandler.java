package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.data.save.ColonySavedData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

/**
 * Registra os eventos de ciclo de vida do servidor.
 *
 * <p>Estes são os dois pontos onde o estado da colônia entra e sai da
 * memória. Ver ADR-002 e Save-Data-System.md.
 */
public final class ServerLifecycleHandler {

    private ServerLifecycleHandler() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleHandler::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerLifecycleHandler::onServerStopping);
    }

    /** Recoloca no registro as colônias gravadas no mundo. */
    private static void onServerStarted(MinecraftServer server) {
        VillageColonyMod.COLONIES.clear();
        VillageColonyMod.WORKERS.clear();

        for (Colony colony : ColonySavedData.get(server).colonies()) {
            VillageColonyMod.COLONIES.register(colony);
        }

        VillageColonyMod.LOGGER.info(
                "Loaded {} colonies", VillageColonyMod.COLONIES.count());
    }

    /**
     * Copia o registro para o saved data antes de o mundo fechar.
     *
     * <p>O registro é esvaziado em seguida: o processo pode abrir outro
     * save sem reiniciar, e colônias do mundo anterior não podem vazar
     * para ele.
     */
    private static void onServerStopping(MinecraftServer server) {
        ColonySavedData.get(server).sync(VillageColonyMod.COLONIES.all());

        VillageColonyMod.LOGGER.info(
                "Saved {} colonies with {} workers",
                VillageColonyMod.COLONIES.count(),
                VillageColonyMod.WORKERS.count());

        VillageColonyMod.COLONIES.clear();
        VillageColonyMod.WORKERS.clear();
    }
}
