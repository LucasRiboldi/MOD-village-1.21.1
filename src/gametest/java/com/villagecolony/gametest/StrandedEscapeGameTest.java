package com.villagecolony.gametest;

import com.villagecolony.fabric.work.StrandedEscape;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * O encalhado cava a própria saída — E47, 2026-09-24.
 *
 * <p>O playtest de 24-09 deixou construtores dez blocos abaixo da vila por
 * horas. Os três casos medem a decisão da fuga direto no mundo, degrau a
 * degrau, sem depender de o aldeão andar na arena: sair de um poço de
 * pedra, não tocar em bloco construído, e não abrir água.
 */
public class StrandedEscapeGameTest implements FabricGameTest {

    /** O bloco de terreno: 9 de lado, do y=1 ao y=5. A superfície é y=6. */
    private static final int SIDE = 9;

    private static final int GROUND_TOP = 5;

    /** O poço, no meio do bloco, com o fundo em y=2. */
    private static final BlockPos SHAFT_BOTTOM = new BlockPos(4, 2, 4);

    private static void ground(TestContext context, Block material) {
        for (int x = 0; x < SIDE; x++) {
            for (int y = 1; y <= GROUND_TOP; y++) {
                for (int z = 0; z < SIDE; z++) {
                    context.setBlockState(new BlockPos(x, y, z), material.getDefaultState());
                }
            }
        }

        for (int y = SHAFT_BOTTOM.getY(); y <= GROUND_TOP; y++) {
            context.setBlockState(
                    new BlockPos(SHAFT_BOTTOM.getX(), y, SHAFT_BOTTOM.getZ()),
                    Blocks.AIR.getDefaultState());
        }
    }

    /** A vila fica para o leste, bem longe do poço. */
    private static BlockPos home(TestContext context) {
        return context.getAbsolutePos(new BlockPos(40, GROUND_TOP + 1, SHAFT_BOTTOM.getZ()));
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "stranded_escape")
    public void aStrandedWorkerDigsAStaircaseOutOfAStonePit(TestContext context) {
        ground(context, Blocks.STONE);

        ServerWorld world = context.getWorld();
        BlockPos feet = context.getAbsolutePos(SHAFT_BOTTOM);
        BlockPos sideWall = context.getAbsolutePos(SHAFT_BOTTOM.north().up(2));

        context.assertFalse(
                StrandedEscape.isOut(world, feet),
                "o fundo do poço foi lido como terreno aberto — o caso não mediria nada");

        int steps = 0;

        while (!StrandedEscape.isOut(world, feet) && steps < 8) {
            Optional<BlockPos> next = StrandedEscape.digOneStep(world, feet, home(context));

            context.assertTrue(next.isPresent(),
                    "a fuga não achou degrau em pedra natural, a " + steps + " passos do fundo");

            context.assertTrue(
                    next.get().getY() == feet.getY() + 1,
                    "o degrau não subiu um bloco: de " + feet.toShortString()
                            + " para " + next.get().toShortString());

            feet = next.get();
            steps++;
        }

        context.assertTrue(StrandedEscape.isOut(world, feet),
                "depois de " + steps + " degraus ele continua abaixo do terreno");
        context.assertTrue(steps <= 5,
                "um poço de quatro blocos pediu " + steps + " degraus");
        context.assertTrue(world.getBlockState(sideWall).isOf(Blocks.STONE),
                "a fuga quebrou a parede do lado oposto, fora da escada");

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "stranded_escape")
    public void theEscapeNeverBreaksABuiltBlock(TestContext context) {
        ground(context, Blocks.OAK_PLANKS);

        ServerWorld world = context.getWorld();
        BlockPos feet = context.getAbsolutePos(SHAFT_BOTTOM);

        Optional<BlockPos> next = StrandedEscape.digOneStep(world, feet, home(context));

        context.assertTrue(next.isEmpty(),
                "a fuga abriu degrau em tábua, que é construção e não terreno");

        for (int y = SHAFT_BOTTOM.getY(); y <= GROUND_TOP; y++) {
            BlockPos wall = context.getAbsolutePos(new BlockPos(SHAFT_BOTTOM.getX() + 1, y, 4));

            context.assertTrue(world.getBlockState(wall).isOf(Blocks.OAK_PLANKS),
                    "a parede de tábua em " + wall.toShortString() + " foi quebrada");
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "stranded_escape")
    public void theEscapeTakesAnotherWayRatherThanOpenWater(TestContext context) {
        ground(context, Blocks.STONE);

        // Água logo atrás do primeiro degrau do rumo direto, que é o leste.
        BlockPos water = SHAFT_BOTTOM.east(2).up();

        context.setBlockState(water, Blocks.WATER.getDefaultState());

        ServerWorld world = context.getWorld();
        BlockPos feet = context.getAbsolutePos(SHAFT_BOTTOM);
        BlockPos eastStep = context.getAbsolutePos(SHAFT_BOTTOM.east().up());

        Optional<BlockPos> next = StrandedEscape.digOneStep(world, feet, home(context));

        context.assertTrue(next.isPresent(),
                "com três rumos secos a fuga não achou nenhum");
        context.assertFalse(next.get().equals(eastStep),
                "a fuga abriu o degrau encostado na água");
        context.assertTrue(world.getBlockState(eastStep).isOf(Blocks.STONE),
                "o bloco que segurava a água foi quebrado");
        context.assertTrue(world.getBlockState(context.getAbsolutePos(water)).isOf(Blocks.WATER),
                "a água saiu do lugar");

        context.complete();
    }
}
