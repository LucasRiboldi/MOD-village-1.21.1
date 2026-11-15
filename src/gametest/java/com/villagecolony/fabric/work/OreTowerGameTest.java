package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * Regra 292: o pedreiro corta a coluna com minério como as vizinhas, e o ar sobre chão natural dentro de peça da vila
 * não é protegido. A geração: {@code VillageGroundGameTest}.
 */
public class OreTowerGameTest implements FabricGameTest {

    /** O pedreiro também corta a coluna com minério. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "ore_tower", tickLimit = 5)
    public void theMasonCutsAColumnWithOre(TestContext context) {
        context.setBlockState(new BlockPos(3, 0, 3), Blocks.STONE);

        for (int y = 1; y <= 4; y++) {
            context.setBlockState(new BlockPos(3, y, 3), y == 2 ? Blocks.COPPER_ORE : Blocks.DIRT);
        }

        context.setBlockState(new BlockPos(3, 5, 3), Blocks.GRASS_BLOCK);
        BlockPos top = context.getAbsolutePos(new BlockPos(3, 5, 3));

        try {
            String why = GroundSmoothing.refusal(context.getWorld(), top.getX(), top.getZ(), top.getY(), top.getY() - 4);

            context.assertTrue(why == null, "o pedreiro recusou a coluna com minério: " + why);
        } finally {
            for (BlockPos at : BlockPos.iterate(0, 0, 0, 7, 7, 7)) {
                context.setBlockState(at.toImmutable(), Blocks.AIR);
            }
        }

        context.complete();
    }

    /** O ar e a planta sobre chão natural não são peça protegida, nem dentro de peça da vila. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "ore_tower", tickLimit = 5)
    public void airInsideAVillagePieceIsNotProtected(TestContext context) {
        context.assertTrue(GroundSmoothing.protection(true, false, Blocks.AIR.getDefaultState()) == null,
                "o ar dentro da peça da vila foi tratado como protegido");
        context.assertTrue(GroundSmoothing.protection(false, false, Blocks.COBBLESTONE.getDefaultState()) != null,
                "o bloco da peça da vila deixou de ser protegido");
        context.complete();
    }
}
