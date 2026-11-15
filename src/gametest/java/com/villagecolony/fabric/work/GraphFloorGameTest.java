package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.SweepPersistence;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * Regra 291: o grafo dá chão à rua de tábua (a da vila gerada ou a que a colônia assentou), à frente da porta em
 * qualquer material e à quina da plantação na altura do canteiro. O piso de tábua de casa continua não sendo rua.
 */
public class GraphFloorGameTest implements FabricGameTest {

    /** A tábua da rua é chão do grafo e célula de rua. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_floor", tickLimit = 5)
    public void aStreetOfPlanksIsGroundForTheGraph(TestContext context) {
        UUID colony = UUID.randomUUID();
        BlockPos plank = context.getAbsolutePos(new BlockPos(2, 1, 2));

        context.setBlockState(new BlockPos(2, 1, 2), Blocks.OAK_PLANKS);
        SweepPersistence.restore(new ColonyRoads(colony, MinecraftTypeAdapter.toColonyPos(plank),
                List.of(ColonyRoads.column(plank.getX(), plank.getZ()))));

        try {
            OptionalInt ground = new WorldPathTerrain(context.getWorld(), colony)
                    .groundY(plank.getX(), plank.getZ(), plank.getY());

            context.assertTrue(ground.isPresent() && ground.getAsInt() == plank.getY(),
                    "a tábua da rua não é chão do grafo: " + ground);
            context.assertTrue(RoadGraphRefresh.pathInColumn(context.getWorld(), plank.up(), null)
                    .filter(plank::equals).isPresent(), "a tábua da rua não é célula de rua do grafo");
        } finally {
            // O índice desta colônia de teste fica vazio: a coluna deixa de ser rua para os testes seguintes.
            SweepPersistence.restore(new ColonyRoads(colony, MinecraftTypeAdapter.toColonyPos(plank), List.of()));
            clearArena(context);
        }

        context.complete();
    }

    /** O piso de tábua de casa, fora de rua, não vira caminho. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_floor", tickLimit = 5)
    public void aPlankFloorIsNotAStreet(TestContext context) {
        BlockPos plank = context.getAbsolutePos(new BlockPos(5, 1, 5));

        context.setBlockState(new BlockPos(5, 1, 5), Blocks.OAK_PLANKS);

        try {
            context.assertTrue(new WorldPathTerrain(context.getWorld(), UUID.randomUUID())
                    .groundY(plank.getX(), plank.getZ(), plank.getY()).isEmpty(), "o piso de tábua virou chão do grafo");
            context.assertTrue(RoadGraphRefresh.pathInColumn(context.getWorld(), plank.up(), null).isEmpty(),
                    "o piso de tábua virou célula de rua");
        } finally {
            clearArena(context);
        }

        context.complete();
    }

    /** A frente da porta tem chão para o grafo em qualquer material: pedra, fora de bioma de rocha. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_floor", tickLimit = 5)
    public void aDoorFrontOnStoneIsGround(TestContext context) {
        BlockPos front = context.getAbsolutePos(new BlockPos(3, 1, 3));

        context.setBlockState(new BlockPos(3, 1, 3), Blocks.STONE);

        try {
            WorldPathTerrain terrain = new WorldPathTerrain(context.getWorld(), UUID.randomUUID());
            terrain.floorAnywhereAt(Set.of(MinecraftTypeAdapter.toColonyPos(front)));
            OptionalInt ground = terrain.groundY(front.getX(), front.getZ(), front.getY());

            context.assertTrue(ground.isPresent() && ground.getAsInt() == front.getY(),
                    "a frente de pedra da porta não é chão do grafo: " + ground);
        } finally {
            clearArena(context);
        }

        context.complete();
    }

    /** A quina da plantação é lida na altura do canteiro, e não no topo da coluna (um beiral em cima). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_floor", tickLimit = 5)
    public void theFarmCornerIsAtTheFarmLevel(TestContext context) {
        BlockPos corner = context.getAbsolutePos(new BlockPos(1, 1, 1));

        context.setBlockState(new BlockPos(1, 1, 1), Blocks.GRASS_BLOCK);
        context.setBlockState(new BlockPos(1, 5, 1), Blocks.STONE);

        try {
            WorldPathTerrain terrain = new WorldPathTerrain(context.getWorld(), UUID.randomUUID());
            var found = RoadShape.cornerGround(context.getWorld(), terrain, corner.getX(), corner.getZ(), corner.getY());

            context.assertTrue(found.map(ColonyPos::y).filter(y -> y == corner.getY()).isPresent(),
                    "a quina da plantação não ficou na altura do canteiro: " + found);
        } finally {
            clearArena(context);
        }

        context.complete();
    }

    /** A área é reaproveitada pelos lotes seguintes. */
    private static void clearArena(TestContext context) {
        for (BlockPos at : BlockPos.iterate(0, 1, 0, 7, 7, 7)) {
            context.setBlockState(at.toImmutable(), Blocks.AIR);
        }
    }
}
