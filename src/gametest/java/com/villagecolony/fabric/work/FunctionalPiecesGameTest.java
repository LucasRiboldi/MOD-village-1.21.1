package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceId;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * Cama, porta e baú não ficam de fora da obra por falta de apoio — C-02, autor de 2026-10-22: ganham o chão
 * do bioma embaixo, onde ali há ar ou planta; o que está embaixo e não se tira continua lá.
 */
public class FunctionalPiecesGameTest implements FabricGameTest {

    /** Alto no lote, longe dos restos perto do chão da arena. */
    private static final int Y = 40;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "functional_pieces", tickLimit = 40)
    public void aDoorOverNothingGetsTheGroundUnderIt(TestContext context) {
        BlockPos target = new BlockPos(2, Y, 2);
        BlockState door = Blocks.OAK_DOOR.getDefaultState();

        try {
            context.assertFalse(door.canPlaceAt(context.getWorld(), context.getAbsolutePos(target)),
                    "o cenário devia deixar a porta sem chão");
            context.assertTrue(FunctionalPieces.support(context.getWorld(), context.getAbsolutePos(target), door),
                    "a porta não ganhou apoio");
            context.assertFalse(context.getBlockState(target.down()).isAir(), "o chão embaixo da porta não foi posto");
        } finally {
            context.setBlockState(target.down(), Blocks.AIR.getDefaultState());
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "functional_pieces", tickLimit = 40)
    public void whatIsUnderneathAndCannotBeTakenStays(TestContext context) {
        BlockPos target = new BlockPos(2, Y, 2);
        BlockState door = Blocks.OAK_DOOR.getDefaultState();

        context.setBlockState(target.down(), Blocks.GLASS_PANE.getDefaultState());

        try {
            context.assertFalse(FunctionalPieces.support(context.getWorld(), context.getAbsolutePos(target), door),
                    "disse que apoiou a porta sobre o vidro");
            context.assertTrue(context.getBlockState(target.down()).isOf(Blocks.GLASS_PANE), "o vidro embaixo foi tirado");
        } finally {
            context.setBlockState(target.down(), Blocks.AIR.getDefaultState());
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "functional_pieces", tickLimit = 40)
    public void onlyBedDoorAndChestAreFunctional(TestContext context) {
        context.assertTrue(FunctionalPieces.isFunctional(Blocks.RED_BED.getDefaultState())
                && FunctionalPieces.isFunctional(Blocks.SPRUCE_DOOR.getDefaultState())
                && FunctionalPieces.isFunctional(Blocks.CHEST.getDefaultState()), "cama, porta ou baú não contou");
        context.assertFalse(FunctionalPieces.isFunctional(Blocks.WALL_TORCH.getDefaultState())
                || FunctionalPieces.isFunctional(Blocks.LANTERN.getDefaultState()), "tocha ou lanterna contou");
        context.assertTrue(FunctionalPieces.isFunctional(ResourceId.vanilla("white_bed"))
                && FunctionalPieces.isFunctional(ResourceId.vanilla("oak_door")), "pelo nome, cama ou porta não contou");
        context.assertFalse(FunctionalPieces.isFunctional(ResourceId.vanilla("torch")), "pelo nome, a tocha contou");
        context.complete();
    }
}
