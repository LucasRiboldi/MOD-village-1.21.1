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
 * A duna acima da rua é cortada até a rua — autor, 2026-10-20, depois do deserto jogado: 0 lotes
 * aprovados em 2,8 dias, com o relevo de +3 a +11 acima da rua. O corte vai até
 * {@link LotTerraform#MAX_CUT}; fora da arena, porque o teto de barreira dela fica a 8 do chão.
 */
public class DuneCutGameTest implements FabricGameTest {

    private static final int SIDE = 3;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "dune_cut")
    public void aDuneUpToTheCutIsAcceptedAndLevelledToTheStreet(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = footprint(context, 0, new int[][] {{LotTerraform.MAX_CUT, 4, 3}, {4, 3, 1}, {3, 1, 0}});

        try {
            Optional<Integer> floor = LotLevel.flatGroundAt(world, UUID.randomUUID(), origin.getX(), origin.getZ(),
                    origin.getY(), origin.getY(), new ColonyPos(SIDE, 3, SIDE));

            // O piso fica logo acima da base, e a base é a rua (o mesmo do aLotTwoAboveTheRoad... da fase 1).
            context.assertTrue(floor.isPresent() && floor.get() == origin.getY() + 1,
                    "a duna até o corte devia ser aceita com a base na rua (" + origin.getY() + "): " + floor
                            + ", chão das colunas " + groundOf(world, origin));

            LotTerraform.levelFootprint(world, origin.getX(), origin.getZ(), SIDE, SIDE, origin.getY());

            for (int dx = 0; dx < SIDE; dx++) {
                for (int dz = 0; dz < SIDE; dz++) {
                    context.assertTrue(world.getBlockState(origin.add(dx, 1, dz)).isAir()
                                    && world.getBlockState(origin.add(dx, 0, dz)).isOf(Blocks.SAND),
                            "a coluna (" + dx + ", " + dz + ") não ficou com a areia na rua");
                }
            }
        } finally {
            clear(context, origin);
        }

        context.complete();
    }

    /** Controle: um acima do corte continua recusado. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "dune_cut")
    public void aDuneAboveTheCutIsRefused(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = footprint(context, 40, new int[][] {{LotTerraform.MAX_CUT + 1, 1, 0}, {1, 0, 0}, {0, 0, 0}});

        try {
            context.assertTrue(LotLevel.flatGroundAt(world, UUID.randomUUID(), origin.getX(), origin.getZ(),
                    origin.getY(), origin.getY(), new ColonyPos(SIDE, 3, SIDE)).isEmpty(),
                    "a duna acima do corte foi aceita");
        } finally {
            clear(context, origin);
        }

        context.complete();
    }

    /** Pegada de areia com o topo da coluna (dx, dz) em {@code rua + heights[dx][dz]}, longe da arena. */
    private static BlockPos footprint(TestContext context, int offset, int[][] heights) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 4, 1)).add(1_000_000, 0, 1_700_000 + offset);
        world.getChunk(origin);

        for (int dx = 0; dx < SIDE; dx++) {
            for (int dz = 0; dz < SIDE; dz++) {
                for (int dy = -3; dy <= LotTerraform.MAX_CUT + 3; dy++) {
                    Block block = dy < -1 ? Blocks.STONE : dy <= heights[dx][dz] ? Blocks.SAND : Blocks.AIR;
                    world.setBlockState(origin.add(dx, dy, dz), block.getDefaultState());
                }
            }
        }

        return origin;
    }

    private static String groundOf(ServerWorld world, BlockPos origin) {
        StringBuilder out = new StringBuilder();
        for (int dx = 0; dx < SIDE; dx++) {
            for (int dz = 0; dz < SIDE; dz++) {
                out.append(LotGround.groundInColumn(world, origin.getX() + dx, origin.getZ() + dz, origin.getY(),
                        LotTerraform.MAX_CUT).map(pos -> pos.getY() - origin.getY()).orElse(99)).append(' ');
            }
        }
        return out.toString();
    }

    private static void clear(TestContext context, BlockPos origin) {
        for (BlockPos pos : BlockPos.iterate(origin.add(0, -3, 0), origin.add(SIDE - 1, LotTerraform.MAX_CUT + 3, SIDE - 1))) {
            context.getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
        }
    }
}
