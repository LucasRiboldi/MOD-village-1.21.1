package com.villagecolony.fabric.world;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.RoadPaving;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * A rua só faz a vila crescer com 10 caminhos conectados fora dela, encostados
 * num caminho de dentro — decisão do autor, 2026-10-03.
 */
public final class RoadSpillGameTest implements FabricGameTest {

    /** A vila do teste: colunas 0..4 em X e Z; a borda leste é x = 4. */
    private static VillageBounds village(TestContext context) {
        BlockPos low = context.getAbsolutePos(new BlockPos(0, 0, 0));
        BlockPos high = context.getAbsolutePos(new BlockPos(4, 5, 4));

        return new VillageBounds(Math.min(low.getX(), high.getX()), low.getY(), Math.min(low.getZ(), high.getZ()),
                Math.max(low.getX(), high.getX()), high.getY(), Math.max(low.getZ(), high.getZ()));
    }

    /** Caminho na linha z = 2, de x = from até x = to, com chão de pedra. */
    private static void path(TestContext context, int from, int to) {
        for (int x = 0; x <= 15; x++) {
            context.setBlockState(new BlockPos(x, 0, 2), Blocks.STONE.getDefaultState());
            context.setBlockState(new BlockPos(x, 1, 2), x >= from && x <= to
                    ? Blocks.DIRT_PATH.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_spill")
    public void tenConnectedPathsOutsideGrowTheSideToTheFarthest(TestContext context) {
        path(context, 4, 14);

        List<BlockPos> chain = RoadSpill.outsideChains(context.getWorld(), village(context));

        BlockPos edge = context.getAbsolutePos(new BlockPos(4, 1, 2));
        int top = context.getWorld().getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, edge.getX(), edge.getZ());

        context.assertTrue(chain.size() == 10, "dez caminhos fora, encostados no de dentro; achou " + chain.size()
                + " — caminho em y " + edge.getY() + ", topo da coluna " + top + " ("
                + context.getWorld().getBlockState(new BlockPos(edge.getX(), top - 1, edge.getZ())) + ")");
        context.assertTrue(chain.contains(context.getAbsolutePos(new BlockPos(14, 1, 2))),
                "o mais distante, x = 14, devia estar na corrente");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_spill")
    public void nineConnectedPathsOutsideAreNotEnough(TestContext context) {
        path(context, 4, 13);

        context.assertTrue(RoadSpill.outsideChains(context.getWorld(), village(context)).isEmpty(),
                "nove caminhos fora não fazem a vila crescer");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_spill")
    public void pathsOutsideThatDoNotTouchAnInsidePathDoNotCount(TestContext context) {
        path(context, 5, 16);

        context.assertTrue(RoadSpill.outsideChains(context.getWorld(), village(context)).isEmpty(),
                "doze caminhos fora sem encostar num caminho de dentro não contam");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_spill")
    public void theColonyGrowsThatSideOnly(TestContext context) {
        path(context, 4, 14);

        VillageBounds box = village(context);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 1, 2))));

        colony.measure(box);
        VillageColonyMod.COLONIES.register(colony);

        try {
            RoadPaving.growByRoads(context.getWorld(), colony.id());

            VillageBounds grown = colony.bounds().orElseThrow();
            int farthest = context.getAbsolutePos(new BlockPos(14, 1, 2)).getX();

            context.assertTrue(grown.maxX() == farthest, "o leste devia ir até o caminho mais distante, " + farthest
                    + "; foi a " + grown.maxX());
            context.assertTrue(grown.minX() == box.minX() && grown.minZ() == box.minZ() && grown.maxZ() == box.maxZ(),
                    "só o lado leste cresce: " + box + " -> " + grown);
        } finally {
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
