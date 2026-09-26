package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.telemetry.model.ActivityTrace;
import com.villagecolony.core.worker.model.Worker;
import java.util.ArrayList;
import java.util.List;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ColonySweepCursor;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.data.save.WorkMarksSavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.MineMarks;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.SweepLog;
import com.villagecolony.fabric.work.TestBarrier;
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
        VillageColonyMod.ACTIVITY_TRACES.clear();
        // Toda memoria de servidor se inscreve sozinha — item 3 da avaliacao,
        // 2026-09-24. Eram duas listas escritas a mao, e ja tinham divergido;
        // ver ServerMemory.
        int forgotten = ServerMemory.resetAll();
        VillageColonyMod.LOGGER.debug("Server memory reset: {} classes", forgotten);

        // As pedras que o mineiro já recusou voltam do save — ADR-025,
        // 2026-09-25. Sem isto ele voltava à mesma pedra inalcançável em
        // toda sessão. Depois da limpeza acima, que as apagaria.
        MineMarks.restore(WorkMarksSavedData.get(server).mineRefusals().stream()
                .map(refusal -> new MineMarks.Mark(
                        MinecraftTypeAdapter.toBlockPos(new ColonyPos(refusal.x(), refusal.y(), refusal.z())),
                        refusal.since(),
                        refusal.count()))
                .toList());

        BiomeConstructionSupply.restoreFailedProfessionAttempts(
                WorkMarksSavedData.get(server).supplyAttempts());

        ColonySavedData data = ColonySavedData.get(server);

        for (Colony colony : data.colonies()) {
            VillageColonyMod.COLONIES.register(colony);
        }

        for (Worker worker : data.workers()) {
            VillageColonyMod.WORKERS.restore(worker);
        }

        // O baú de cada trabalhador volta com ele — decisão do autor,
        // 2026-09-26. Ver WorkMarksSavedData.WorkerChest.
        for (WorkMarksSavedData.WorkerChest chest : WorkMarksSavedData.get(server).workerChests()) {
            if (VillageColonyMod.WORKERS.isRegistered(chest.worker())) {
                VillageColonyMod.STORAGES.register(WorkerStorage.of(
                        chest.worker(), new ColonyPos(chest.x(), chest.y(), chest.z())));
            }
        }

        // As obras voltam pela metade de propósito: falta-lhes o projeto,
        // que só existe com um mundo carregado a quem perguntar. Elas
        // renascem no primeiro ciclo de cada colônia — ver
        // ConstructionResume.resume.
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

        // E o traço de atividade de cada colônia — decisão 7B,
        // 2026-09-24. newestFirst() devolve do mais novo para o mais
        // velho; restore() espera essa mesma ordem para reconstruir o
        // buffer sem embaralhar quem é mais recente.
        for (var entry : data.activityTraces().entrySet()) {
            VillageColonyMod.ACTIVITY_TRACES.restore(
                    entry.getKey(), entry.getValue().newestFirst(ActivityTrace.CAPACITY));
        }

        // E o índice de ruas — 2026-08-27. Sem ele, a primeira busca de
        // lote de cada sessão custava dezessete ciclos de varredura antes
        // de responder qualquer coisa, e as sessões curtas acabavam
        // dentro dessa espera.
        for (ColonyRoads roads : data.roads()) {
            BuildSiteScanner.restore(roads);
        }

        // E a varredura que ficou no meio — 2026-08-27. Medido: catorze
        // passagens das dezessete necessárias iam para o lixo, porque o
        // índice só nasce de uma volta completa.
        for (ColonySweepCursor cursor : data.sweeps()) {
            BuildSiteScanner.restore(cursor);
        }

        VillageColonyMod.LOGGER.info(
                "Loaded {} colonies with {} workers, {} buildings, {} mines,"
                        + " {} road indexes, {} paused sweeps and {} projects to resume",
                VillageColonyMod.COLONIES.count(),
                VillageColonyMod.WORKERS.count(),
                VillageColonyMod.BUILDINGS.count(),
                VillageColonyMod.MINES.count(),
                data.roads().size(),
                data.sweeps().size(),
                data.projects().size());
    }

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
                    project.state(),
                    project.deferredPieces()));
        }

        return saving;
    }

    /**
     * Copia o registro para o saved data antes de o mundo fechar.
     *
     * <p>O registro é esvaziado em seguida: o processo pode abrir outro
     * save sem reiniciar, e colônias do mundo anterior não podem vazar
     * para ele.
     */
    private static void onServerStopping(MinecraftServer server) {
        List<ColonyRoads> roads = BuildSiteScanner.saved();
        List<ColonySweepCursor> sweeps = BuildSiteScanner.pausedSweeps();

        ColonySavedData.get(server).sync(
                VillageColonyMod.COLONIES.all(),
                VillageColonyMod.WORKERS.all(),
                openProjects(),
                VillageColonyMod.BUILDINGS.all(),
                VillageColonyMod.MINES.all(),
                roads,
                sweeps,
                VillageColonyMod.ACTIVITY_TRACES.all());

        // E as marcas de trabalho, num arquivo à parte — ver WorkMarksSavedData.
        WorkMarksSavedData.get(server).sync(MineMarks.marks().stream()
                .map(mark -> new WorkMarksSavedData.MineRefusal(
                        mark.stone().getX(), mark.stone().getY(), mark.stone().getZ(),
                        mark.since(), mark.count()))
                .toList());
        WorkMarksSavedData.get(server).syncSupplyAttempts(
                BiomeConstructionSupply.failedProfessionAttempts());
        WorkMarksSavedData.get(server).syncWorkerChests(VillageColonyMod.STORAGES.all().stream()
                .map(storage -> new WorkMarksSavedData.WorkerChest(
                        storage.workerId(),
                        storage.chestPosition().x(), storage.chestPosition().y(), storage.chestPosition().z()))
                .toList());

        VillageColonyMod.LOGGER.info(
                "Saved {} colonies with {} workers, {} buildings, {} mines,"
                        + " {} road indexes, {} paused sweeps and {} open projects",
                VillageColonyMod.COLONIES.count(),
                VillageColonyMod.WORKERS.count(),
                VillageColonyMod.BUILDINGS.count(),
                VillageColonyMod.MINES.count(),
                roads.size(),
                sweeps.size(),
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
        VillageColonyMod.ACTIVITY_TRACES.clear();
        // Toda memoria de servidor se inscreve sozinha — item 3 da avaliacao,
        // 2026-09-24. Eram duas listas escritas a mao, e ja tinham divergido;
        // ver ServerMemory.
        int forgotten = ServerMemory.resetAll();
        VillageColonyMod.LOGGER.debug("Server memory reset: {} classes", forgotten);
    }
}
