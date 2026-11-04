package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ResourceId;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import java.util.UUID;

/** Obra pedida por comando: reserva, fluxo por tique e recusas com motivo — ADR-046. */
public final class MaeSitesGameTest implements FabricGameTest {
    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/plains_small_house_1");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_run", tickLimit = 1200)
    public void aRequestedBuildingRunsByTickUntilItConcludes(TestContext context) {
        var arena = new MaeStructureSiteGameTest.Arena(context, HOUSE, false);
        arena.guarded(() -> {
            arena.fixture.writer = new MaeWorldWriter(arena.fixture.store, arena.sources());
            var status = MaeSites.start(context.getWorld(), arena.fixture.writer, arena.fixture.colony.colony().id(),
                    HOUSE, arena.origin, 0, null);
            context.assertTrue(MaeSites.active().stream().anyMatch(active -> active.regionId().equals(status.regionId())),
                    "obra pedida não entrou nas obras em andamento");
            await(context, arena, status.regionId());
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_refuse", tickLimit = 40)
    public void requestsAreRefusedWithTheirReason(TestContext context) {
        var arena = new MaeStructureSiteGameTest.Arena(context, HOUSE, false);
        arena.guarded(() -> {
            arena.fixture.writer = new MaeWorldWriter(arena.fixture.store, arena.sources());
            UUID colony = arena.fixture.colony.colony().id();
            refused(context, () -> MaeSites.start(context.getWorld(), arena.fixture.writer, colony,
                    ResourceId.vanilla("village/plains/houses/no_such_house"), arena.origin, 0, null), "unknown_model");
            refused(context, () -> MaeSites.start(context.getWorld(), arena.fixture.writer, colony,
                    HOUSE, arena.origin, 4, null), "invalid_rotation");
            var first = MaeSites.start(context.getWorld(), arena.fixture.writer, colony, HOUSE, arena.origin, 0, null);
            refused(context, () -> MaeSites.start(context.getWorld(), arena.fixture.writer, colony,
                    HOUSE, arena.origin, 0, null), "overlapping_region");
            context.assertTrue(MaeSites.stop(first.regionId()) && !MaeSites.stop(first.regionId()),
                    "parar não tirou a obra das obras em andamento");
            arena.cleanup();
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_search", tickLimit = 40)
    public void searchFindsALevelLotNearTheRequest(TestContext context) {
        var arena = new MaeStructureSiteGameTest.Arena(context, HOUSE, false);
        arena.guarded(() -> {
            arena.fixture.writer = new MaeWorldWriter(arena.fixture.store, arena.sources());
            var status = MaeSites.startNear(context.getWorld(), arena.fixture.writer, arena.fixture.colony.colony().id(),
                    HOUSE, arena.fixture.base, 0, null);
            // O único chão nivelado no alcance é a plataforma da arena, logo abaixo da origem do lote.
            context.assertTrue(status.origin().getY() == arena.fixture.base.getY() + 1,
                    "busca escolheu lote fora da plataforma: " + status.origin().toShortString());
            MaeSites.stop(status.regionId());
            arena.cleanup();
            context.complete();
        });
    }

    private static void await(TestContext context, MaeStructureSiteGameTest.Arena arena, UUID regionId) {
        arena.guarded(() -> {
            if (MaeSites.active().stream().anyMatch(active -> active.regionId().equals(regionId))) {
                context.waitAndRun(1, () -> await(context, arena, regionId));
                return;
            }
            var job = arena.fixture.store.region(regionId).orElseThrow().getCompound("job");
            context.assertTrue("complete".equals(job.getString("state")),
                    "obra pedida terminou sem concluir: " + job.getString("state") + " " + job.getString("reason"));
            long finished = VillageColonyMod.BUILDINGS.ofColony(arena.fixture.colony.colony().id()).stream()
                    .filter(building -> building.blueprint().equals(HOUSE) && building.finished()).count();
            context.assertTrue(finished == 1, "obra pedida registrou " + finished + " edifícios");
            // Sem grafo nem índice de ruas, a entrada perto do centro vira a semente da rede MAE.
            context.assertTrue(MaeRoadExecution.TYPE.equals(job.getString("type")),
                    "obra pedida não ligou a entrada à rua: " + job.getString("type"));
            var graph = com.villagecolony.data.save.ColonySavedData.get(context.getWorld().getServer()).roadGraphs()
                    .graph(arena.fixture.colony.colony().id());
            context.assertTrue(graph.isPresent() && MaeRoadExecution.allPositions(graph.get()).contains(
                    MaeStructureSite.entry(arena.project)), "rua publicada não passa pela entrada da obra");
            arena.cleanup();
            context.complete();
        });
    }

    private static void refused(TestContext context, Runnable action, String reason) {
        try {
            action.run();
        } catch (IllegalStateException | IllegalArgumentException failure) {
            context.assertTrue(reason.equals(failure.getMessage()), "recusa incorreta: " + failure.getMessage());
            return;
        }
        context.assertTrue(false, "pedido deveria recusar: " + reason);
    }
}
