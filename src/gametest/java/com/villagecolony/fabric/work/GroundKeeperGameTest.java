package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.OptionalInt;

/** O fardo de feno no caminho do grafo: o grafo o atravessa e o Zelador o tira ao calçar (Regra 331). */
public class GroundKeeperGameTest implements FabricGameTest {

    private static final int SIZE = 12;

    private static void platform(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 0, SIZE))) {
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
            world.setBlockState(at.down(), Blocks.STONE.getDefaultState(), 18);
        }
    }

    private static void clean(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin.down(), origin.add(SIZE, 5, SIZE))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }
    }

    /** O grafo lê o chão debaixo do fardo de feno, e não o topo do fardo. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "groundskeeper_hay_graph", tickLimit = 20)
    public void theGraphCrossesAHayBale(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 100, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(6, 1, 6));
        BlockPos floor = origin.add(4, 0, 4);

        try {
            world.setBlockState(floor.up(), Blocks.HAY_BLOCK.getDefaultState(), 18);
            world.setBlockState(floor.up(2), Blocks.HAY_BLOCK.getDefaultState(), 18);

            WorldPathTerrain terrain = new WorldPathTerrain(world, fixture.colony().id());
            OptionalInt ground = terrain.groundY(floor.getX(), floor.getZ(), floor.getY());

            context.assertTrue(ground.isPresent() && ground.getAsInt() == floor.getY(),
                    "Regra 331: o grafo não atravessou o feno; chão lido " + ground);
            context.assertTrue(terrain.canCut(floor.getX(), floor.getZ(), floor.getY()),
                    "Regra 331: o feno trancou a coluna do caminho");
        } finally {
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** O Zelador calça a coluna do grafo que tem feno em cima: o fardo sai e o caminho entra. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "groundskeeper_hay_paving", tickLimit = 20)
    public void theGroundskeeperTakesTheHayOffThePath(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 120, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(6, 1, 6));
        Colony colony = fixture.colony();
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);

            var keeper = fixture.equippedWorker(context, ProfessionType.GROUNDSKEEPER, BlockPos.ORIGIN.up());
            BlockPos floor = origin.add(6, 0, 5);

            world.setBlockState(floor.up(), Blocks.HAY_BLOCK.getDefaultState(), 18);
            world.getEntity(keeper.villagerId()).refreshPositionAndAngles(floor.getX() - 0.5, floor.getY() + 1,
                    floor.getZ() + 0.5, 0, 0);

            PathLinks.offerRepair(colony.id(), List.of(new PathStep(floor.getX(), floor.getY(), floor.getZ(), Side.EAST, 0, 0)));
            PathPaving.pass(world, colony);

            context.assertFalse(world.getBlockState(floor.up()).isOf(Blocks.HAY_BLOCK), "Regra 331: o feno ficou no caminho");
            context.assertTrue(world.getBlockState(floor).isOf(Blocks.DIRT_PATH), "Regra 331: a coluna com feno não foi calçada");
        } finally {
            world.setTimeOfDay(before);
            PathLinks.drop(colony.id());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }
}
