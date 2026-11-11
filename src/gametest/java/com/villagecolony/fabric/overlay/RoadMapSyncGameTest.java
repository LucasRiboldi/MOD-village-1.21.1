package com.villagecolony.fabric.overlay;

import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

/** O sinalizador dos caminhos — Regra 176: desenha o que a vila guarda, com as cores do autor, e é só para operador. */
public class RoadMapSyncGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_map_sync", tickLimit = 20)
    public void itDrawsWhatTheVillageKeeps(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos hub = context.getAbsolutePos(new BlockPos(1, 2, 1));
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 2, 1));
        var store = ColonySavedData.get(world.getServer()).roadGraphs();

        try {
            Set<ColonyPos> line = new HashSet<>();

            for (int x = 0; x < 6; x++) {
                line.add(new ColonyPos(hub.getX() + x, hub.getY(), hub.getZ()));
            }

            ColonyPos door = new ColonyPos(hub.getX() + 5, hub.getY(), hub.getZ());
            RoadGraph graph = RoadGraph.from(line, new ColonyPos(hub.getX(), hub.getY(), hub.getZ()), Set.of(door));

            store.update(fixture.colony().id(), graph);

            RoadMapPayload map = RoadMapSync.mapAround(world, hub);

            context.assertTrue(map.lines().stream().anyMatch(drawn -> drawn.color() == RoadMapSync.LINE), "Regra 176: a linha amarela não foi desenhada");
            context.assertTrue(map.lines().stream().noneMatch(drawn -> drawn.color() == RoadMapSync.FIXED), "Regra 234: verde sem trecho travado");

            // Três colunas do meio na linha há 10 dias, as outras há 9: o passo entre duas travadas é verde, o resto amarelo (Regra 234).
            long today = 20;
            java.util.Map<Long, Long> since = new java.util.HashMap<>();

            for (int x = 0; x < 6; x++) {
                since.put(com.villagecolony.core.construction.model.ColonyRoads.column(hub.getX() + x, hub.getZ()),
                        x >= 1 && x <= 3 ? today - com.villagecolony.core.construction.model.RoadSettling.DAYS : today - 9);
            }

            store.settle(fixture.colony().id(), since);

            RoadMapPayload held = RoadMapSync.mapAround(world, hub, today);
            long green = held.lines().stream().filter(drawn -> drawn.color() == RoadMapSync.FIXED)
                    .mapToLong(drawn -> drawn.points().size()).sum();
            long yellow = held.lines().stream().filter(drawn -> drawn.color() == RoadMapSync.LINE)
                    .mapToLong(drawn -> drawn.points().size()).sum();

            context.assertTrue(green == 3, "Regra 234: o trecho travado não saiu verde coluna a coluna: " + green);
            context.assertTrue(yellow == 5, "Regra 234: o trecho de 9 dias não ficou amarelo, emendado ao verde: " + yellow);
            context.assertTrue(map.markers().stream().anyMatch(marker -> marker.color() == RoadMapSync.HUB), "Regra 176: o nó central não saiu vermelho");
            context.assertTrue(map.markers().stream().anyMatch(marker -> marker.color() == RoadMapSync.ENTRANCE), "Regra 176: a entrada não saiu branca");
            context.assertTrue(map.markers().stream().noneMatch(marker -> marker.color() == RoadMapSync.OTHER), "Regra 206: azul fora de ponta");

            ColonyPos any = new ColonyPos(0, 0, 0);

            context.assertTrue(RoadMapSync.colorOf(new RoadGraph.Node(any, Set.of(RoadGraph.Role.ENDPOINT))) == RoadMapSync.OTHER, "Regra 206: a ponta não saiu azul");
            context.assertTrue(RoadMapSync.colorOf(new RoadGraph.Node(any, Set.of(RoadGraph.Role.CYCLE_ANCHOR))) == RoadMapSync.JUNCTION, "Regra 206: nó de anel saiu azul");

            var mae = world.getServer().getCommandManager().getDispatcher().getRoot().getChild("vc").getChild("mae");
            var source = world.getServer().getCommandSource();

            context.assertFalse(mae.canUse(source.withLevel(0)), "Regra 176: jogador comum alcança o sinalizador");
            context.assertTrue(mae.canUse(source.withLevel(2)), "Regra 176: operador não alcança o sinalizador");
        } finally {
            store.update(fixture.colony().id(), RoadGraph.from(Set.of(), new ColonyPos(hub.getX(), hub.getY(), hub.getZ()), Set.of()));
            store.settle(fixture.colony().id(), java.util.Map.of());
            fixture.cleanUp();
        }

        context.complete();
    }
}
