package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Set;

/** Quem não sai do cercado por nenhuma saída, três rodadas seguidas, vai para fora da cerca — Regra 114. */
public class PenLiftGameTest implements FabricGameTest {

    private static final BlockPos INSIDE = new BlockPos(3, 2, 3);

    private static final BlockPos BARRIER = new BlockPos(4, 2, 3);

    private static final BlockPos OUTSIDE = new BlockPos(5, 2, 3);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_lift", tickLimit = 60)
    public void threeFailedRoundsLiftTheWorkerOverTheFence(TestContext context) {
        VillagerEntity villager = penned(context);
        FencedIn.Exit exit = exit(context);
        Set<BlockPos> reach = Set.of(context.getAbsolutePos(INSIDE));

        context.waitAndRun(10, () -> {
            for (int round = 1; round < PenLift.ROUNDS; round++) {
                context.assertFalse(PenLift.failedRound(context.getWorld(), villager, List.of(exit), reach),
                        "na rodada " + round + " ele ainda tenta sair andando");
            }

            context.assertTrue(PenLift.failedRound(context.getWorld(), villager, List.of(exit), reach),
                    "na terceira rodada ele devia ir para fora da cerca");
            context.assertTrue(villager.getBlockPos().equals(context.getAbsolutePos(OUTSIDE)),
                    "ele foi para " + villager.getBlockPos().toShortString() + ", não para trás da cerca");
            context.assertTrue(PenLift.rounds(villager.getUuid()) == 0, "a conta não recomeçou depois de sair");
            context.complete();
        });
    }

    /** Controle: sem chão livre do lado de fora, ele não é posto em lugar nenhum. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_lift", tickLimit = 60)
    public void withNoRoomOutsideNobodyIsLifted(TestContext context) {
        VillagerEntity villager = penned(context);
        FencedIn.Exit exit = exit(context);
        Set<BlockPos> reach = Set.of(context.getAbsolutePos(INSIDE));

        context.setBlockState(OUTSIDE, Blocks.STONE.getDefaultState());
        context.setBlockState(OUTSIDE.up(), Blocks.STONE.getDefaultState());
        context.setBlockState(OUTSIDE.up(2), Blocks.STONE.getDefaultState());

        context.waitAndRun(10, () -> {
            for (int round = 0; round < PenLift.ROUNDS + 1; round++) {
                PenLift.failedRound(context.getWorld(), villager, List.of(exit), reach);
            }

            context.assertTrue(villager.getBlockPos().equals(context.getAbsolutePos(INSIDE)),
                    "sem lugar do lado de fora ele saiu do cercado para " + villager.getBlockPos().toShortString());
            PenLift.forget(villager.getUuid());
            context.complete();
        });
    }

    /** Um bloco de chão cercado de cerca nos quatro lados, sem portão. */
    private static VillagerEntity penned(TestContext context) {
        for (int x = 1; x <= 6; x++) {
            for (int z = 1; z <= 6; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
            }
        }

        for (BlockPos fence : List.of(INSIDE.north(), INSIDE.south(), INSIDE.west(), BARRIER)) {
            context.setBlockState(fence, Blocks.OAK_FENCE.getDefaultState());
        }

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, INSIDE);
        villager.setAiDisabled(true);

        return villager;
    }

    private static FencedIn.Exit exit(TestContext context) {
        return new FencedIn.Exit(false, context.getAbsolutePos(INSIDE), context.getAbsolutePos(BARRIER),
                context.getAbsolutePos(OUTSIDE));
    }
}
