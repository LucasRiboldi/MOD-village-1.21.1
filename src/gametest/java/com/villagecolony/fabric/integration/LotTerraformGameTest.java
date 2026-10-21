package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * A terraplanagem do lote, fase 1 — autor, 2026-10-20: cada coluna a até dois da rua é aceita, e
 * aplainada até a base na rua; a casa pronta segue a Regra 19.
 */
public class LotTerraformGameTest implements FabricGameTest {

    private static final int SIDE = 3;

    /** Pegada de 3 × 3 com o chão da coluna (dx, dz) em {@code roadY + heights[dx][dz]}, sobre pedra. */
    private static BlockPos footprint(TestContext context, int[][] heights, Block ground) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(2, 4, 2));

        for (int dx = 0; dx < SIDE; dx++) {
            for (int dz = 0; dz < SIDE; dz++) {
                BlockPos column = origin.add(dx, 0, dz);

                for (int dy = -3; dy <= 3; dy++) {
                    int top = heights[dx][dz];
                    Block block = dy < top ? Blocks.STONE : dy == top ? ground : Blocks.AIR;
                    world.setBlockState(column.up(dy), block.getDefaultState());
                }
            }
        }

        return origin;
    }

    private static void clear(TestContext context, BlockPos origin) {
        for (BlockPos pos : BlockPos.iterate(origin.add(0, -3, 0), origin.add(SIDE - 1, 3, SIDE - 1))) {
            context.getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lot_terraform")
    public void aLotTwoAboveTheRoadIsAcceptedWithTheBaseOnTheStreet(TestContext context) {
        BlockPos origin = footprint(context, new int[][] {{2, 2, 2}, {2, 2, 2}, {2, 2, 2}}, Blocks.GRASS_BLOCK);

        try {
            Optional<Integer> floor = LotLevel.flatGroundAt(context.getWorld(), UUID.randomUUID(),
                    origin.getX(), origin.getZ(), origin.getY(), origin.getY(), new ColonyPos(SIDE, 3, SIDE));

            context.assertTrue(floor.isPresent() && floor.get() == origin.getY() + 1,
                    "o lote dois acima da rua devia ser aceito com o piso logo acima da rua: " + floor);
        } finally {
            clear(context, origin);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lot_terraform")
    public void anUnevenFootprintIsLevelledToTheStreet(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = footprint(context, new int[][] {{2, 0, -2}, {1, 0, -1}, {2, -2, 0}}, Blocks.GRASS_BLOCK);

        try {
            int moved = LotTerraform.levelFootprint(world, origin.getX(), origin.getZ(), SIDE, SIDE, origin.getY());

            for (int dx = 0; dx < SIDE; dx++) {
                for (int dz = 0; dz < SIDE; dz++) {
                    int top = LotGround.groundInColumn(world, origin.getX() + dx, origin.getZ() + dz, origin.getY())
                            .map(BlockPos::getY).orElse(Integer.MIN_VALUE);
                    context.assertTrue(top == origin.getY(),
                            "a coluna (" + dx + ", " + dz + ") ficou com o chão em " + (top - origin.getY()));
                }
            }

            context.assertTrue(moved == 2 + 2 + 1 + 1 + 2 + 2, "mexeu " + moved + " blocos, e não 10");
        } finally {
            clear(context, origin);
        }

        context.complete();
    }

    /** Cortar terra encostada na água abriria o lote para ela: a coluna da beira não se corta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lot_terraform")
    public void groundBesideWaterIsNeverCut(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = footprint(context, new int[][] {{2, 2, 2}, {2, 2, 2}, {2, 2, 2}}, Blocks.GRASS_BLOCK);
        BlockPos shore = origin.add(-1, 1, 1);

        world.setBlockState(shore, Blocks.WATER.getDefaultState());

        try {
            context.assertFalse(LotTerraform.columnLevels(world, origin.getX(), origin.getZ() + 1,
                    origin.getY() + 2, origin.getY()), "a coluna encostada na água foi dada como cortável");
            context.assertTrue(LotTerraform.columnLevels(world, origin.getX() + 2, origin.getZ() + 1,
                    origin.getY() + 2, origin.getY()), "controle: a coluna longe da água deixou de ser cortável");
        } finally {
            world.setBlockState(shore, Blocks.AIR.getDefaultState());
            clear(context, origin);
        }

        context.complete();
    }

    /** Controle: o que não é chão do bioma não se corta, e o lote não é aceito. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lot_terraform")
    public void aBumpThatIsNotGroundIsNeverCut(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = footprint(context, new int[][] {{0, 0, 0}, {0, 2, 0}, {0, 0, 0}}, Blocks.GRASS_BLOCK);
        world.setBlockState(origin.add(1, 1, 1), Blocks.COBBLESTONE.getDefaultState());
        world.setBlockState(origin.add(1, 2, 1), Blocks.COBBLESTONE.getDefaultState());

        try {
            context.assertFalse(LotTerraform.columnLevels(world, origin.getX() + 1, origin.getZ() + 1,
                    origin.getY() + 2, origin.getY()), "o degrau de pedregulho foi dado como cortável");
            context.assertTrue(LotLevel.flatGroundAt(world, UUID.randomUUID(), origin.getX(), origin.getZ(),
                    origin.getY(), origin.getY(), new ColonyPos(SIDE, 3, SIDE)).isEmpty(),
                    "o lote com um degrau de pedregulho foi aceito");
        } finally {
            clear(context, origin);
        }

        context.complete();
    }
}
