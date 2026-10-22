package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O corredor de caminho — regra do autor de 2026-10-22: sai da porta em linha reta, vira só em ângulo
 * reto, tem um bloco de largura e para no primeiro caminho que encostar. Cenários quarenta blocos acima
 * do piso, longe das outras arenas.
 */
public class CorridorGameTest implements FabricGameTest {

    private static final int Y = 40;

    /** Porta para oeste, rua ao norte: uma reta saindo da porta e uma curva de noventa graus. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "corridor")
    public void theCorridorLeavesTheDoorStraightAndTurnsAtRightAngles(TestContext context) {
        ServerWorld world = context.getWorld();

        floor(context, 0, 8, 0, 8);

        for (int x = 0; x <= 8; x++) {
            context.setBlockState(new BlockPos(x, Y, 0), Blocks.DIRT_PATH.getDefaultState());
        }

        BlockPos door = context.getAbsolutePos(new BlockPos(7, Y, 6));
        Optional<List<BlockPos>> corridor = Corridor.plan(world, UUID.randomUUID(), door, Direction.WEST, 32, at -> false);

        context.assertTrue(corridor.isPresent(), "não houve corridor até a rua");

        List<BlockPos> cells = corridor.get();

        context.assertTrue(cells.get(0).equals(door), "o corredor não começou na porta: " + cells.get(0));
        context.assertTrue(cells.get(1).equals(door.west()), "o corredor não saiu da porta na direção dela: " + cells.get(1));
        context.assertTrue(turnsOf(cells) == 1, "devia ser uma reta e uma curva, e teve " + turnsOf(cells) + " curvas: " + cells);
        assertOrthogonal(context, cells);

        BlockPos last = cells.get(cells.size() - 1);

        context.assertTrue(last.getZ() == context.getAbsolutePos(new BlockPos(0, Y, 1)).getZ(),
                "o corredor não acabou encostado na rua: " + last);

        context.complete();
    }

    /** Uma rua paralela ao lado: o corredor para no primeiro encontro, sem correr colado nela. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "corridor")
    public void theCorridorStopsAtTheFirstPathItTouches(TestContext context) {
        ServerWorld world = context.getWorld();

        floor(context, 0, 8, 0, 8);

        // A rua corre em z=4 de x 0 a 4; a porta fica em x 7, z 5, virada para oeste.
        for (int x = 0; x <= 4; x++) {
            context.setBlockState(new BlockPos(x, Y, 4), Blocks.DIRT_PATH.getDefaultState());
        }

        BlockPos door = context.getAbsolutePos(new BlockPos(7, Y, 5));
        Optional<List<BlockPos>> corridor = Corridor.plan(world, UUID.randomUUID(), door, Direction.WEST, 32, at -> false);

        context.assertTrue(corridor.isPresent(), "não houve corredor");

        List<BlockPos> cells = corridor.get();
        BlockPos firstTouch = context.getAbsolutePos(new BlockPos(4, Y, 5));

        context.assertTrue(cells.get(cells.size() - 1).equals(firstTouch),
                "o corredor devia parar no primeiro bloco que encosta na rua (" + firstTouch + "), e foi até "
                        + cells.get(cells.size() - 1));

        context.complete();
    }

    /**
     * A busca tem teto de tempo — 0.3.90: no deserto a busca com corte chegou a 3,2 s num tique. Com o teto
     * zerado, um platô sem caminho nenhum para na primeira conferida do relógio e diz por quê. Alto e
     * pequeno, longe das arenas vizinhas.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "corridor_budget")
    public void aSearchStopsAtTheTimeCeilingAndSaysSo(TestContext context) {
        ServerWorld world = context.getWorld();
        int high = Y + 60;

        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, high, z)), Blocks.SAND.getDefaultState());
            }
        }

        BlockPos start = context.getAbsolutePos(new BlockPos(8, high, 8));
        List<String> why = new java.util.ArrayList<>();
        Optional<List<BlockPos>> corridor = Corridor.plan(world, UUID.randomUUID(), start,
                context.getRotation().rotate(Direction.NORTH), 64, Corridor.MAX_CUT, 0L, at -> false, why::add);

        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, high, z)), Blocks.AIR.getDefaultState());
            }
        }

        context.assertTrue(corridor.isEmpty(), "sem caminho nenhum, o corredor devia sair vazio");
        context.assertTrue(why.contains(Corridor.OUT_OF_TIME), "a busca não parou pelo teto de tempo: " + why.size()
                + " motivos, nenhum de tempo");
        context.complete();
    }

    private static void floor(TestContext context, int fromX, int toX, int fromZ, int toZ) {
        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                context.setBlockState(new BlockPos(x, Y, z), Blocks.DIRT.getDefaultState());
            }
        }
    }

    static int turnsOf(List<BlockPos> cells) {
        int turns = 0;

        for (int i = 2; i < cells.size(); i++) {
            BlockPos a = cells.get(i - 2);
            BlockPos b = cells.get(i - 1);
            BlockPos c = cells.get(i);

            if (b.getX() - a.getX() != c.getX() - b.getX() || b.getZ() - a.getZ() != c.getZ() - b.getZ()) {
                turns++;
            }
        }

        return turns;
    }

    static void assertOrthogonal(TestContext context, List<BlockPos> cells) {
        for (int i = 1; i < cells.size(); i++) {
            int dx = Math.abs(cells.get(i).getX() - cells.get(i - 1).getX());
            int dz = Math.abs(cells.get(i).getZ() - cells.get(i - 1).getZ());

            context.assertTrue(dx + dz == 1, "passo em diagonal de " + cells.get(i - 1) + " para " + cells.get(i));
        }
    }
}
