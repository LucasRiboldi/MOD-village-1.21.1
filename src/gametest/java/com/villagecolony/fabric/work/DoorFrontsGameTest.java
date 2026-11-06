package com.villagecolony.fabric.work;

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

/** A entrada de cada porta térrea, pelo lado de fora — Regra 207. Em coordenada do mundo: a arena pode vir girada. */
public class DoorFrontsGameTest implements FabricGameTest {

    private static void door(ServerWorld world, BlockPos at, Direction inward) {
        var lower = Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, inward);

        world.setBlockState(at, lower.with(DoorBlock.HALF, DoubleBlockHalf.LOWER), 18);
        world.setBlockState(at.up(), lower.with(DoorBlock.HALF, DoubleBlockHalf.UPPER), 18);
    }

    private static void ground(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin.add(-3, -1, -3), origin.add(9, 5, 9))) {
            world.setBlockState(at, at.getY() == origin.getY() ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState(), 18);
        }
    }

    private static void clean(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin.add(-3, -1, -3), origin.add(9, 5, 9))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }
    }

    /** Sem parede no fundo os dois lados empatam, e o lado de dentro ganhava. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_fronts_tie", tickLimit = 20)
    public void withBothSidesFreeItLeavesByTheOutside(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 60, 1));
        BlockPos min = origin.up();
        BlockPos max = origin.add(4, 4, 4);

        ground(world, origin);

        try {
            door(world, origin.add(4, 1, 2), Direction.WEST);

            List<BlockPos> fronts = DoorFronts.of(world, min, max);

            context.assertTrue(fronts.equals(List.of(origin.add(5, 0, 2))), "Regra 207: não saiu pelo lado de fora: " + fronts);
            context.assertTrue(PathLinks.entranceOf(world, min, max).orElseThrow().front().getX() == origin.getX() + 5,
                    "Regra 207: a entrada antiga ficou do lado de dentro");
        } finally {
            clean(world, origin);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_fronts_many", tickLimit = 20)
    public void everyGroundDoorCountsOnceAndInnerDoorsDoNot(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 68, 1));
        BlockPos min = origin.up();
        BlockPos max = origin.add(4, 4, 4);

        ground(world, origin);

        try {
            // Duas portas em lados diferentes.
            door(world, origin.add(4, 1, 0), Direction.WEST);
            door(world, origin.add(0, 1, 4), Direction.EAST);
            // Porta dupla: conta uma vez.
            door(world, origin.add(4, 1, 3), Direction.WEST);
            door(world, origin.add(4, 1, 4), Direction.WEST);
            // Porta de dentro, entre duas paredes.
            door(world, origin.add(2, 1, 2), Direction.WEST);
            world.setBlockState(origin.add(1, 1, 2), Blocks.STONE.getDefaultState(), 18);
            world.setBlockState(origin.add(3, 1, 2), Blocks.STONE.getDefaultState(), 18);

            Set<BlockPos> fronts = Set.copyOf(DoorFronts.of(world, min, max));

            context.assertTrue(fronts.size() == 3, "Regra 207: contou errado as portas: " + fronts);
            context.assertTrue(fronts.containsAll(List.of(origin.add(5, 0, 0), origin.add(-1, 0, 4))), "Regra 207: faltou porta: " + fronts);
            context.assertTrue(fronts.contains(origin.add(5, 0, 3)) != fronts.contains(origin.add(5, 0, 4)), "Regra 207: a porta dupla não contou uma vez: " + fronts);
        } finally {
            clean(world, origin);
        }

        context.complete();
    }

    /** A porta alta: degrau de escada na frente e o chão firme um bloco abaixo dele. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_fronts_stairs", tickLimit = 20)
    public void itWalksDownTheStepsToTheFirstGround(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 76, 1));
        BlockPos min = origin.up();
        BlockPos max = origin.add(4, 4, 4);

        ground(world, origin);

        try {
            door(world, origin.add(4, 2, 2), Direction.WEST);
            world.setBlockState(origin.add(5, 1, 2), Blocks.OAK_STAIRS.getDefaultState(), 18);

            List<BlockPos> fronts = DoorFronts.of(world, min, max);

            context.assertTrue(fronts.equals(List.of(origin.add(6, 0, 2))), "Regra 207: não desceu o degrau até o chão: " + fronts);
        } finally {
            clean(world, origin);
        }

        context.complete();
    }

    /** Uma parede logo do lado de fora: a porta sai pelo outro lado, em vez de sumir. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_fronts_blocked", tickLimit = 20)
    public void aBlockedOutsideFallsBackToTheOtherSide(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 84, 1));
        BlockPos min = origin.up();
        BlockPos max = origin.add(4, 4, 4);

        ground(world, origin);

        try {
            door(world, origin.add(4, 1, 2), Direction.WEST);
            world.setBlockState(origin.add(5, 1, 2), Blocks.STONE.getDefaultState(), 18);

            List<BlockPos> fronts = DoorFronts.of(world, min, max);

            context.assertTrue(fronts.equals(List.of(origin.add(-1, 0, 2))), "Regra 207: com o lado de fora fechado, a porta sumiu: " + fronts);
        } finally {
            clean(world, origin);
        }

        context.complete();
    }

    /** A única porta dá para um quintal fechado, dentro da caixa da peça: a entrada é o primeiro chão depois dela. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_fronts_yard", tickLimit = 20)
    public void theOnlyDoorOfAClosedYardCountsAtItsFirstGround(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 92, 1));
        BlockPos min = origin.up();
        BlockPos max = origin.add(8, 4, 4);

        ground(world, origin);

        try {
            door(world, origin.add(2, 1, 2), Direction.WEST);
            world.setBlockState(origin.add(1, 1, 2), Blocks.STONE.getDefaultState(), 18);
            world.setBlockState(origin.add(5, 1, 2), Blocks.GRINDSTONE.getDefaultState(), 18);

            List<BlockPos> fronts = DoorFronts.of(world, min, max);

            context.assertTrue(fronts.equals(List.of(origin.add(3, 0, 2))), "Regra 209: a única porta ficou sem entrada: " + fronts);
        } finally {
            clean(world, origin);
        }

        context.complete();
    }

    /** O açougue e o pastor da vila gerada: uma porta livre e outra para o quintal cercado. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_fronts_yard_other", tickLimit = 20)
    public void aClosedYardDoesNotCountWhenAnotherDoorIsFree(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 100, 1));
        BlockPos min = origin.up();
        BlockPos max = origin.add(8, 4, 4);

        ground(world, origin);

        try {
            door(world, origin.add(2, 1, 2), Direction.WEST);
            world.setBlockState(origin.add(1, 1, 2), Blocks.STONE.getDefaultState(), 18);
            world.setBlockState(origin.add(5, 1, 2), Blocks.OAK_FENCE.getDefaultState(), 18);
            door(world, origin.add(1, 1, 4), Direction.NORTH);

            List<BlockPos> fronts = DoorFronts.of(world, min, max);

            context.assertTrue(fronts.equals(List.of(origin.add(1, 0, 5))), "Regra 209: o quintal fechado contou como entrada: " + fronts);
        } finally {
            clean(world, origin);
        }

        context.complete();
    }

    /** Assoalho não é quintal: a porta de dentro, sobre tábua, não vira entrada nem sendo a única. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_fronts_floor", tickLimit = 20)
    public void aWoodFloorIsNotAYard(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 108, 1));
        BlockPos min = origin.up();
        BlockPos max = origin.add(8, 4, 4);

        ground(world, origin);

        try {
            door(world, origin.add(2, 1, 2), Direction.WEST);

            for (BlockPos at : BlockPos.iterate(origin.add(1, 0, 2), origin.add(4, 0, 2))) {
                world.setBlockState(at, Blocks.OAK_PLANKS.getDefaultState(), 18);
            }

            world.setBlockState(origin.add(5, 1, 2), Blocks.STONE.getDefaultState(), 18);
            world.setBlockState(origin.add(1, 1, 2), Blocks.STONE.getDefaultState(), 18);

            List<BlockPos> fronts = DoorFronts.of(world, min, max);

            context.assertTrue(fronts.isEmpty(), "Regra 209: o assoalho virou entrada: " + fronts);
        } finally {
            clean(world, origin);
        }

        context.complete();
    }
}
