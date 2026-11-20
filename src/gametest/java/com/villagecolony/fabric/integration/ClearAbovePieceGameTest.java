package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Regra 337: na geração, a coluna da peça fixa da vila fica livre acima do piso. A casa que desce ao solo suavizado na
 * montanha não deixa pedra, terra, caminho, árvore nem nada solto em cima dela.
 */
public class ClearAbovePieceGameTest implements FabricGameTest {

    /** O morro que ficava acima do piso da casa, com bloco natural, caminho, pedregulho, tronco e um pedaço solto no alto. */
    private static final List<BlockState> HILL = List.of(Blocks.DIRT.getDefaultState(), Blocks.STONE.getDefaultState(),
            Blocks.DIRT_PATH.getDefaultState(), Blocks.COBBLESTONE.getDefaultState(), Blocks.CALCITE.getDefaultState(),
            Blocks.GRAVEL.getDefaultState(), Blocks.GRASS_BLOCK.getDefaultState(), Blocks.OAK_LOG.getDefaultState());

    private static final int FLOATING = 14;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "clear_above_piece", tickLimit = 5)
    public void nothingStaysAboveTheFloorOfAPiece(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos at = context.getAbsolutePos(BlockPos.ORIGIN).add(2_700_000, 20, 2_700_000);
        int floor = at.getY();
        var chunk = world.getChunk(at);

        try {
            for (int y = floor - 4; y <= floor + FLOATING + 2; y++) {
                world.setBlockState(at.withY(y), Blocks.AIR.getDefaultState());
            }

            for (int y = floor - 4; y <= floor; y++) {
                world.setBlockState(at.withY(y), Blocks.DIRT.getDefaultState());
            }

            for (int i = 0; i < HILL.size(); i++) {
                world.setBlockState(at.withY(floor + 1 + i), HILL.get(i));
            }

            // Um vão de ar e, acima dele, o pedaço que ficava solto.
            world.setBlockState(at.withY(floor + FLOATING), Blocks.STONE.getDefaultState());

            int top = VillageGround.topOf(chunk, at.getX(), at.getZ(), floor + FLOATING + 2);

            PieceGround.found(world, chunk, at.getX(), at.getZ(), top, floor, VillageSoil.of(chunk));

            for (int y = floor + 1; y <= floor + FLOATING + 2; y++) {
                context.assertTrue(world.getBlockState(at.withY(y)).isAir(), "Regra 337: ficou "
                        + world.getBlockState(at.withY(y)).getBlock() + " " + (y - floor) + " acima do piso da peça");
            }

            context.assertFalse(world.getBlockState(at.withY(floor)).isAir(), "Regra 337: o piso da peça sumiu");
        } finally {
            for (int y = floor - 4; y <= floor + FLOATING + 2; y++) {
                world.setBlockState(at.withY(y), Blocks.AIR.getDefaultState());
            }
        }

        context.complete();
    }
}
