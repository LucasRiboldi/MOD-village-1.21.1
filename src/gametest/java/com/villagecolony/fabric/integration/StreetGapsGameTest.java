package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

/**
 * As falhas no meio da rua viram rua — Regra 149: grama de um ou dois blocos entre duas colunas de caminho vira
 * caminho; o que não é chão natural (pedregulho) fica.
 */
public class StreetGapsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_gaps", tickLimit = 20)
    public void aOneAndATwoBlockGapBecomeStreetAndCobblestoneStays(TestContext context) {
        UUID colony = UUID.randomUUID();

        row(context, 3, Blocks.DIRT_PATH, Blocks.GRASS_BLOCK, Blocks.DIRT_PATH, Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK,
                Blocks.DIRT_PATH);
        row(context, 6, Blocks.DIRT_PATH, Blocks.COBBLESTONE, Blocks.DIRT_PATH);

        var world = context.getWorld();

        context.assertTrue(StreetGaps.fill(world, colony, context.getAbsolutePos(new BlockPos(1, 1, 3)), Direction.EAST) == 1,
                "a falha de um bloco devia virar rua");
        context.assertTrue(StreetGaps.fill(world, colony, context.getAbsolutePos(new BlockPos(3, 1, 3)), Direction.EAST) == 2,
                "a falha de dois blocos devia virar rua");

        for (int x = 1; x <= 6; x++) {
            context.assertTrue(context.getBlockState(new BlockPos(x, 1, 3)).isOf(Blocks.DIRT_PATH),
                    "a coluna " + x + " da rua devia ser caminho: " + context.getBlockState(new BlockPos(x, 1, 3)));
        }

        context.assertTrue(StreetGaps.fill(world, colony, context.getAbsolutePos(new BlockPos(1, 1, 6)), Direction.EAST) == 0,
                "o pedregulho entre duas ruas não é chão natural e devia ficar");
        context.assertTrue(context.getBlockState(new BlockPos(2, 1, 6)).isOf(Blocks.COBBLESTONE), "o pedregulho mudou");
        context.complete();
    }

    /** Uma linha de blocos a partir de x = 1, em cima de pedra. */
    private static void row(TestContext context, int z, Block... blocks) {
        for (int index = 0; index < blocks.length; index++) {
            context.setBlockState(new BlockPos(1 + index, 0, z), Blocks.STONE);
            context.setBlockState(new BlockPos(1 + index, 1, z), blocks[index]);
        }
    }
}
