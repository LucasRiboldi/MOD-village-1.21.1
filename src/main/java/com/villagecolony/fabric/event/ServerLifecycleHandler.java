package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestMarker;
import com.villagecolony.fabric.integration.TreeScanner;
import com.villagecolony.fabric.work.LumberjackWork;
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

    /**
     * Recoloca no registro as colônias e os trabalhadores gravados no
     * mundo.
     *
     * <p>Os trabalhadores vêm depois das colônias porque o saved data já
     * descartou os que não pertencem a nenhuma colônia carregada.
     */
    private static void onServerStarted(MinecraftServer server) {
        VillageColonyMod.COLONIES.clear();
        VillageColonyMod.WORKERS.clear();
        VillageColonyMod.STORAGES.clear();
        VillageColonyMod.TASKS.clear();
        WorkTargets.clearAll();
        LumberjackWork.clearAll();
        ChestMarker.clearAll();
        TreeScanner.clearAll();
        VillageDetectionHandler.clearPending();

        ColonySavedData data = ColonySavedData.get(server);

        for (Colony colony : data.colonies()) {
            VillageColonyMod.COLONIES.register(colony);
        }

        for (Worker worker : data.workers()) {
            VillageColonyMod.WORKERS.restore(worker);
        }

        VillageColonyMod.LOGGER.info(
                "Loaded {} colonies with {} workers",
                VillageColonyMod.COLONIES.count(),
                VillageColonyMod.WORKERS.count());
    }

    /**
     * Copia o registro para o saved data antes de o mundo fechar.
     *
     * <p>O registro é esvaziado em seguida: o processo pode abrir outro
     * save sem reiniciar, e colônias do mundo anterior não podem vazar
     * para ele.
     */
    private static void onServerStopping(MinecraftServer server) {
        ColonySavedData.get(server).sync(
                VillageColonyMod.COLONIES.all(),
                VillageColonyMod.WORKERS.all());

        VillageColonyMod.LOGGER.info(
                "Saved {} colonies with {} workers",
                VillageColonyMod.COLONIES.count(),
                VillageColonyMod.WORKERS.count());

        VillageColonyMod.COLONIES.clear();
        VillageColonyMod.WORKERS.clear();
        VillageColonyMod.STORAGES.clear();
        VillageColonyMod.TASKS.clear();
        WorkTargets.clearAll();
        LumberjackWork.clearAll();
        ChestMarker.clearAll();
        TreeScanner.clearAll();
        VillageDetectionHandler.clearPending();
    }
}
