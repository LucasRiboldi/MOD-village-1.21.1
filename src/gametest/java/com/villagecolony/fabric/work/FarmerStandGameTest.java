package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * O fazendeiro vai ao lugar de pé ao alcance da planta, e não à planta — deserto jogado, 2026-10-20:
 * a fileira da ponta da roça, fechada entre a borda e o canal de água, não tinha caminho até a planta.
 */
public class FarmerStandGameTest implements FabricGameTest {

    private static final BlockPos CROP = new BlockPos(4, 2, 4);

    /** A planta cercada: borda de pedra dos lados e canal de água na frente. */
    private static void enclosedCrop(TestContext context) {
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
            }
        }

        context.setBlockState(CROP.down(), Blocks.FARMLAND.getDefaultState());
        context.setBlockState(CROP, Blocks.WHEAT.getDefaultState());
        context.setBlockState(CROP.west(), Blocks.STONE.getDefaultState());
        context.setBlockState(CROP.west().up(), Blocks.STONE.getDefaultState());
        context.setBlockState(CROP.north(), Blocks.STONE.getDefaultState());
        context.setBlockState(CROP.north().up(), Blocks.STONE.getDefaultState());
        context.setBlockState(CROP.south().down(), Blocks.WATER.getDefaultState());
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_stand")
    public void theFarmerStandsOutsideTheEnclosedRow(TestContext context) {
        enclosedCrop(context);
        BlockPos worker = context.getAbsolutePos(new BlockPos(8, 2, 4));
        BlockPos crop = context.getAbsolutePos(CROP);

        BlockPos stand = FarmerStand.nearest(context.getWorld(), crop, worker);

        context.assertFalse(stand.equals(crop), "o lugar de pé ficou na própria planta cercada");
        context.assertTrue(stand.getSquaredDistance(crop) <= 8,
                "o lugar de pé ficou fora do alcance do braço: " + stand.subtract(crop));
        context.assertTrue(BuilderApproach.standable(context.getWorld(), stand),
                "o lugar escolhido não é de ficar de pé: " + stand.subtract(crop));
        context.assertTrue(stand.getSquaredDistance(worker) < crop.getSquaredDistance(worker),
                "o lugar de pé não ficou mais perto do fazendeiro que a planta");
        context.complete();
    }

    /** Controle: sem lugar de pé em volta, o destino continua sendo a planta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_stand")
    public void withoutAPlaceToStandTheCropIsTheTarget(TestContext context) {
        BlockPos crop = context.getAbsolutePos(CROP);

        // Ar por toda parte, sem chão: nenhum lugar de pé.
        BlockPos stand = FarmerStand.nearest(context.getWorld(), crop.up(5), context.getAbsolutePos(new BlockPos(8, 7, 4)));

        context.assertTrue(stand.equals(crop.up(5)), "sem chão em volta, apareceu um lugar de pé: " + stand);
        context.complete();
    }
}
