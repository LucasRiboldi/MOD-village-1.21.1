package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.SweepPersistence;
import com.villagecolony.fabric.integration.VillageRoad;
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

    /**
     * Regra 307: o beiral do telhado sobre a frente da porta, com ar embaixo, não é chão do grafo — a leitura desce até o
     * chão apoiado (jogo da 0.4.90: cinco portas soltas lidas no telhado, 4 a 6 blocos acima do caminho).
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_floor", tickLimit = 5)
    public void aRoofOverTheDoorFrontIsNotItsFloor(TestContext context) {
        BlockPos ground = context.getAbsolutePos(new BlockPos(3, 1, 3));

        context.setBlockState(new BlockPos(3, 0, 3), Blocks.DIRT);
        context.setBlockState(new BlockPos(3, 1, 3), Blocks.GRASS_BLOCK);
        context.setBlockState(new BlockPos(3, 5, 3), Blocks.OAK_PLANKS);

        try {
            WorldPathTerrain terrain = new WorldPathTerrain(context.getWorld(), UUID.randomUUID());
            terrain.floorAnywhereAt(Set.of(MinecraftTypeAdapter.toColonyPos(ground)));
            OptionalInt floor = terrain.groundY(ground.getX(), ground.getZ(), ground.getY() + 1);

            context.assertTrue(floor.isPresent() && floor.getAsInt() == ground.getY(),
                    "Regra 307: o telhado sobre a frente da porta virou o chão do grafo: " + floor + " (chão em "
                            + ground.getY() + ")");
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

    /** Regra 295: a coluna que o calçador recusou é bloqueio para a busca da ligação, e a busca acha outra rota. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_floor", tickLimit = 5)
    public void aRefusedColumnBlocksTheLinkSearch(TestContext context) {
        BlockPos refused = context.getAbsolutePos(new BlockPos(2, 1, 2));
        BlockPos free = context.getAbsolutePos(new BlockPos(5, 1, 5));
        WorldPathTerrain terrain = new WorldPathTerrain(context.getWorld(), UUID.randomUUID(),
                Set.of(ColonyRoads.column(refused.getX(), refused.getZ())));

        context.assertTrue(terrain.blocked(refused.getX(), refused.getZ()), "a coluna recusada ficou livre para a busca");
        context.assertFalse(terrain.blocked(free.getX(), free.getZ()), "a coluna livre virou bloqueio");
        context.complete();
    }

    /** Regra 295: dentro da caixa de uma colônia, a pedra é chão de rua (vira cascalho), também fora de bioma de rocha. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_floor", tickLimit = 5)
    public void stoneInsideAColonyIsPavable(TestContext context) {
        BlockPos stone = context.getAbsolutePos(new BlockPos(3, 1, 3));
        BlockPos min = context.getAbsolutePos(new BlockPos(0, 0, 0));
        BlockPos max = context.getAbsolutePos(new BlockPos(7, 7, 7));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(stone));

        context.setBlockState(new BlockPos(3, 1, 3), Blocks.STONE);
        colony.restoreBounds(new VillageBounds(Math.min(min.getX(), max.getX()), min.getY(),
                Math.min(min.getZ(), max.getZ()), Math.max(min.getX(), max.getX()), max.getY(),
                Math.max(min.getZ(), max.getZ())));

        try {
            context.assertFalse(VillageRoad.isPavableGround(context.getWorld(), stone, Blocks.STONE.getDefaultState()),
                    "a pedra fora de vila e de bioma de rocha virou chão de rua");
            VillageColonyMod.COLONIES.register(colony);
            context.assertTrue(VillageRoad.isPavableGround(context.getWorld(), stone, Blocks.STONE.getDefaultState()),
                    "a pedra dentro da vila não é chão de rua");
        } finally {
            VillageColonyMod.COLONIES.remove(colony.id());
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
