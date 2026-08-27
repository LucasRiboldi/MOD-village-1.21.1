package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.Worker;
import java.util.ArrayList;
import java.util.List;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestMarker;
import com.villagecolony.fabric.integration.TreeScanner;
import com.villagecolony.fabric.integration.RoadExtension;
import com.villagecolony.fabric.integration.VillageRoad;
import com.villagecolony.fabric.work.LumberjackWork;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.SweepLog;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.integration.ChestPlacer;
import com.villagecolony.fabric.integration.RingSweep;
import com.villagecolony.fabric.work.MinerWork;
import com.villagecolony.fabric.work.ShepherdWork;
import com.villagecolony.fabric.work.SmelterWork;
import com.villagecolony.fabric.work.TestBarrier;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.WaitingWork;
import com.villagecolony.fabric.work.ConstructionPlanner;
import com.villagecolony.fabric.work.ManufacturerWork;
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
        VillageColonyMod.CONSTRUCTIONS.clear();
        VillageColonyMod.BUILDINGS.clear();
        VillageColonyMod.MINES.clear();
        WorkTargets.clearAll();
        LumberjackWork.clearAll();
        MinerWork.clearAll();
        SmelterWork.clearAll();
        ShepherdWork.clearAll();
        RingSweep.clearAll();
        ChestPlacer.clearAll();
        ManufacturerWork.clearAll();
        BuilderWork.clearAll();
        ConstructionPlanner.clearAll();
        HousePlans.clearAll();
        WaitingWork.clearAll();
        ChestMarker.clearAll();
        TreeScanner.clearAll();
        VillageRoad.clearAll();
        RoadExtension.clearAll();
        BuildSiteScanner.clearAll();
        SweepLog.clearAll();
        TestBarrier.clearAll();
        ColonyStateLog.clearAll();
        VillageDetectionHandler.clearPending();

        ColonySavedData data = ColonySavedData.get(server);

        for (Colony colony : data.colonies()) {
            VillageColonyMod.COLONIES.register(colony);
        }

        for (Worker worker : data.workers()) {
            VillageColonyMod.WORKERS.restore(worker);
        }

        // As obras voltam pela metade de propósito: falta-lhes o projeto,
        // que só existe com um mundo carregado a quem perguntar. Elas
        // renascem no primeiro ciclo de cada colônia — ver
        // ConstructionPlanner.resume.
        for (ConstructionService.Pending project : data.projects()) {
            VillageColonyMod.CONSTRUCTIONS.registerPending(project);
        }

        for (Building building : data.buildings()) {
            VillageColonyMod.BUILDINGS.register(building);
        }

        // A mina volta inteira: a boca, o lado da galeria e a fronteira.
        // Sem ela o mineiro reprocurava uma boca que ele mesmo já tinha
        // cavado, e revarria do primeiro degrau tudo o que estava aberto.
        for (Mine mine : data.mines()) {
            VillageColonyMod.MINES.restore(mine);
        }

        // E o índice de ruas — 2026-08-27. Sem ele, a primeira busca de
        // lote de cada sessão custava dezessete ciclos de varredura antes
        // de responder qualquer coisa, e as sessões curtas acabavam
        // dentro dessa espera.
        for (ColonyRoads roads : data.roads()) {
            BuildSiteScanner.restore(roads);
        }

        VillageColonyMod.LOGGER.info(
                "Loaded {} colonies with {} workers, {} buildings, {} mines,"
                        + " {} road indexes and {} projects to resume",
                VillageColonyMod.COLONIES.count(),
                VillageColonyMod.WORKERS.count(),
                VillageColonyMod.BUILDINGS.count(),
                VillageColonyMod.MINES.count(),
                data.roads().size(),
                data.projects().size());
    }

    /**
     * Copia o registro para o saved data antes de o mundo fechar.
     *
     * <p>O registro é esvaziado em seguida: o processo pode abrir outro
     * save sem reiniciar, e colônias do mundo anterior não podem vazar
     * para ele.
     */
    /**
     * As obras em andamento, reduzidas ao que vai para o disco.
     *
     * <p>Identidade, estrutura, lugar e estado. O progresso não: quem
     * sabe o que já está de pé é o mundo, e é a ele que a sessão seguinte
     * pergunta.
     *
     * <p>As que voltaram do save e ainda não renasceram vão junto — sem
     * isso, fechar o mundo antes do primeiro ciclo apagaria a obra.
     */
    private static List<ConstructionService.Pending> openProjects() {
        List<ConstructionService.Pending> saving =
                new ArrayList<>(VillageColonyMod.CONSTRUCTIONS.allPending());

        for (ConstructionProject project : VillageColonyMod.CONSTRUCTIONS.all()) {
            if (!project.state().isOpen()) {
                continue;
            }

            saving.add(new ConstructionService.Pending(
                    project.id(),
                    project.colonyId(),
                    project.blueprint().id(),
                    project.origin(),
                    project.state()));
        }

        return saving;
    }

    private static void onServerStopping(MinecraftServer server) {
        List<ColonyRoads> roads = BuildSiteScanner.saved();

        ColonySavedData.get(server).sync(
                VillageColonyMod.COLONIES.all(),
                VillageColonyMod.WORKERS.all(),
                openProjects(),
                VillageColonyMod.BUILDINGS.all(),
                VillageColonyMod.MINES.all(),
                roads);

        VillageColonyMod.LOGGER.info(
                "Saved {} colonies with {} workers, {} buildings, {} mines,"
                        + " {} road indexes and {} open projects",
                VillageColonyMod.COLONIES.count(),
                VillageColonyMod.WORKERS.count(),
                VillageColonyMod.BUILDINGS.count(),
                VillageColonyMod.MINES.count(),
                roads.size(),
                openProjects().size());

        // A soma da barreira de teste, antes de tudo ser esquecido.
        // Silêncio aqui é a notícia boa: nenhuma casa precisou dela.
        TestBarrier.report();
        ColonyStateLog.report();

        // O que a varredura de lote fez nesta sessão, por colônia. É a
        // conta que separa "ela reinicia" de "ninguém a chamou".
        SweepLog.report();

        VillageColonyMod.COLONIES.clear();
        VillageColonyMod.WORKERS.clear();
        VillageColonyMod.STORAGES.clear();
        VillageColonyMod.TASKS.clear();
        VillageColonyMod.CONSTRUCTIONS.clear();
        VillageColonyMod.BUILDINGS.clear();
        VillageColonyMod.MINES.clear();
        WorkTargets.clearAll();
        LumberjackWork.clearAll();
        MinerWork.clearAll();
        SmelterWork.clearAll();
        ShepherdWork.clearAll();
        RingSweep.clearAll();
        ChestPlacer.clearAll();
        ManufacturerWork.clearAll();
        BuilderWork.clearAll();
        ConstructionPlanner.clearAll();
        HousePlans.clearAll();
        WaitingWork.clearAll();
        ChestMarker.clearAll();
        TreeScanner.clearAll();
        VillageRoad.clearAll();
        RoadExtension.clearAll();
        BuildSiteScanner.clearAll();
        SweepLog.clearAll();
        TestBarrier.clearAll();
        ColonyStateLog.clearAll();
        VillageDetectionHandler.clearPending();
    }
}
