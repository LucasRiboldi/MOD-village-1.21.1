package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Em vila nova, só o grafo traça o caminho (Regra 214), e o pedreiro ocioso ajuda a calçar (Regra 215). */
public class GraphOnlyPathsGameTest implements FabricGameTest {

    private static final int SIZE = 40;

    private static void platform(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 0, SIZE))) {
            world.getChunk(at);
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
            world.setBlockState(at.down(), Blocks.STONE.getDefaultState(), 18);
        }
    }

    private static void clean(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin.down(), origin.add(SIZE, 5, SIZE))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }
    }

    private static ColonyPos cell(BlockPos at) {
        return MinecraftTypeAdapter.toColonyPos(at);
    }

    /** O construtor está ausente: o pedreiro sem tarefa pega a calçada. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_only_mason", tickLimit = 20)
    public void anIdleMasonPavesThePath(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 60, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 20));
        Colony colony = fixture.colony();
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);

            var mason = fixture.equippedWorker(context, ProfessionType.MASON, BlockPos.ORIGIN.up());
            BlockPos floor = origin.add(6, 0, 5);

            // A arena pode vir girada: a posição vem do mundo.
            world.getEntity(mason.villagerId()).refreshPositionAndAngles(floor.getX() - 0.5, floor.getY() + 1, floor.getZ() + 0.5, 0, 0);

            PathLinks.offerRepair(colony.id(), List.of(new PathStep(floor.getX(), floor.getY(), floor.getZ(), Side.EAST, 0, 0)));
            PathPaving.pass(world, colony);

            context.assertTrue(world.getBlockState(floor).isOf(Blocks.DIRT_PATH), "Regra 215: o pedreiro ocioso não calçou; vez=" + PathPaving.isPaving(mason.villagerId()) + " plano=" + PathLinks.planOf(colony.id()).isPresent() + " livre=" + world.getEntity(mason.villagerId()));
            context.assertTrue(mason.profession().filter(ProfessionType.MASON::equals).isPresent(), "o trabalhador não é pedreiro");
        } finally {
            world.setTimeOfDay(before);
            PathLinks.drop(colony.id());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** O carpinteiro continua de fora: a ajuda é só do pedreiro. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_only_carpenter", tickLimit = 20)
    public void aCarpenterDoesNotPave(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 72, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 20));
        Colony colony = fixture.colony();
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);
            var carpenter = fixture.equippedWorker(context, ProfessionType.CARPENTER, BlockPos.ORIGIN.up());
            BlockPos floor = origin.add(6, 0, 5);

            world.getEntity(carpenter.villagerId()).refreshPositionAndAngles(floor.getX() - 0.5, floor.getY() + 1, floor.getZ() + 0.5, 0, 0);

            PathLinks.offerRepair(colony.id(), List.of(new PathStep(floor.getX(), floor.getY(), floor.getZ(), Side.EAST, 0, 0)));
            PathPaving.pass(world, colony);

            context.assertFalse(world.getBlockState(floor).isOf(Blocks.DIRT_PATH), "Regra 215: o carpinteiro calçou");
        } finally {
            world.setTimeOfDay(before);
            PathLinks.drop(colony.id());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** A porta sem ligação: em vila nova o sistema antigo não traça, porque o grafo já cuida dela. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_only_tend", tickLimit = 20)
    public void onlyTheGraphTracesInANewVillage(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 84, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 2));
        Colony colony = fixture.colony();
        BlockPos min = origin.add(15, 1, 15);
        BlockPos max = origin.add(19, 3, 19);
        Building house = new Building(UUID.randomUUID(), colony.id(), ResourceId.vanilla("village/plains/houses/test_house"), cell(min), cell(max), true);

        try {
            VillageColonyMod.BUILDINGS.register(house);

            var door = Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.SOUTH);

            world.setBlockState(origin.add(17, 1, 15), door.with(DoorBlock.HALF, DoubleBlockHalf.LOWER), 18);
            world.setBlockState(origin.add(17, 2, 15), door.with(DoorBlock.HALF, DoubleBlockHalf.UPPER), 18);

            for (int x = 2; x <= 38; x++) {
                BlockPos at = origin.add(x, 0, 2);

                world.setBlockState(at, Blocks.DIRT_PATH.getDefaultState(), 18);
                RoadIndex.remember(colony.id(), at);
            }

            PathLinks.tend(world, colony, true);

            context.assertTrue(PathLinks.planOf(colony.id()).isEmpty(), "Regra 214: o traçado antigo correu em vila nova");

            PathLinks.tend(world, colony, false);

            context.assertTrue(PathLinks.planOf(colony.id()).isPresent(), "a vila antiga, sem grafo, continua traçando pela porta");
        } finally {
            VillageColonyMod.BUILDINGS.remove(house.id());
            PathLinks.drop(colony.id());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** Sem portão de pé (Regra 3), a mina ainda é entrada: a borda da folga da escada, do lado do centro da vila. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_only_mouth", tickLimit = 20)
    public void aMineWithoutGateIsStillAnEntrance(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 96, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 2));
        Colony colony = fixture.colony();

        try {
            BlockPos mouth = origin.add(20, 1, 25);

            VillageColonyMod.MINES.open(colony.id(), MineShaft.from(cell(mouth), Side.SOUTH));

            WorldPathTerrain terrain = new WorldPathTerrain(world, colony.id());
            Set<ColonyPos> fronts = MineGates.fronts(world, colony, terrain);

            context.assertTrue(fronts.size() == 1, "Regra 214: a mina sem portão ficou sem entrada: " + fronts);

            ColonyPos front = fronts.iterator().next();

            context.assertFalse(terrain.blocked(front.x(), front.z()), "a entrada caiu na folga da escada: " + front);
            context.assertTrue(front.z() < mouth.getZ(), "a entrada não ficou do lado do centro da vila: " + front);
        } finally {
            VillageColonyMod.MINES.removeOfColony(colony.id());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }
}
