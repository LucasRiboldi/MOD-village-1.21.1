package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * Regra 377 (notas do jogo da 0.4.145, J4 🅱️: "depois de um dia os blocos postos com o objetivo desta escadaria devem
 * sumir, eles são usados como utilitário, mas não pode estragar o cenário"): o pedregulho que o aldeão põe para sair de
 * onde ficou preso some depois de um dia de jogo; o que o jogador trocou fica.
 */
public class TemporaryStepsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "temporary_steps", tickLimit = 20)
    public void anEscapeStepIsGoneAfterADay(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos step = context.getAbsolutePos(new BlockPos(2, 2, 2));
        BlockPos fresh = context.getAbsolutePos(new BlockPos(4, 2, 2));
        BlockPos changed = context.getAbsolutePos(new BlockPos(6, 2, 2));
        long now = 1_000_000;

        try {
            for (BlockPos at : new BlockPos[] {step, fresh, changed}) {
                world.setBlockState(at, Blocks.COBBLESTONE.getDefaultState());
            }
            TemporarySteps.placed(step, now);
            TemporarySteps.placed(changed, now);
            TemporarySteps.placed(fresh, now + TemporarySteps.LIFETIME);
            // O jogador trocou este pelo dele: não é mais o degrau de fuga.
            world.setBlockState(changed, Blocks.STONE_BRICKS.getDefaultState());

            TemporarySteps.sweep(world, now + TemporarySteps.LIFETIME + 1);

            context.assertTrue(world.getBlockState(step).isAir(), "Regra 377: o degrau de fuga ficou depois de um dia");
            context.assertTrue(world.getBlockState(fresh).isOf(Blocks.COBBLESTONE), "o degrau novo sumiu antes do dia");
            context.assertTrue(world.getBlockState(changed).isOf(Blocks.STONE_BRICKS), "o bloco do jogador sumiu");
        } finally {
            for (BlockPos at : new BlockPos[] {step, fresh, changed}) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
                TemporarySteps.forget(at);
            }
        }

        context.complete();
    }
}
