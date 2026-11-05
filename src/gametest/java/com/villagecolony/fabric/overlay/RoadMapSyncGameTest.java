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
            context.assertTrue(map.markers().stream().anyMatch(marker -> marker.color() == RoadMapSync.HUB), "Regra 176: o nó central não saiu vermelho");
            context.assertTrue(map.markers().stream().anyMatch(marker -> marker.color() == RoadMapSync.ENTRANCE), "Regra 176: a entrada não saiu branca");

            var mae = world.getServer().getCommandManager().getDispatcher().getRoot().getChild("vc").getChild("mae");
            var source = world.getServer().getCommandSource();

            context.assertFalse(mae.canUse(source.withLevel(0)), "Regra 176: jogador comum alcança o sinalizador");
            context.assertTrue(mae.canUse(source.withLevel(2)), "Regra 176: operador não alcança o sinalizador");
        } finally {
            store.update(fixture.colony().id(), RoadGraph.from(Set.of(), new ColonyPos(hub.getX(), hub.getY(), hub.getZ()), Set.of()));
            fixture.cleanUp();
        }

        context.complete();
    }
}
