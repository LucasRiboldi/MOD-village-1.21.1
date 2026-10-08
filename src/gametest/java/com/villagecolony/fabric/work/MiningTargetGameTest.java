package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * E1, playtest de 2026-10-08: a pedra só vira alvo com posição de trabalho que o
 * mineiro alcance. O cenário é o da caverna embaixo da galeria — a pedra no alto de
 * um pilar, com o único lugar de pé em cima dela, dois acima do mineiro.
 */
public class MiningTargetGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mining_target")
    public void aStoneWhoseOnlyFootholdIsTooHighIsRefused(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos stone = pillarScene(context, false);
        BlockPos miner = context.getAbsolutePos(new BlockPos(1, 11, 0));

        MiningTarget.Verdict verdict = MiningTarget.judge(world, stone, miner);

        context.assertTrue(verdict.rejection() == MiningTarget.Rejection.TOO_HIGH,
                "a pedra com lugar de pé só em cima dela virou alvo: " + verdict);
        context.complete();
    }

    /** Controle: com chão até o pé do pilar, a mesma pedra serve, e o lugar é o de baixo. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mining_target")
    public void theSameStoneWithAFloorBesideItIsATarget(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos stone = pillarScene(context, true);
        BlockPos miner = context.getAbsolutePos(new BlockPos(1, 11, 0));

        MiningTarget.Verdict verdict = MiningTarget.judge(world, stone, miner);

        context.assertTrue(verdict.eligible(), "a pedra alcançável foi recusada: " + verdict);
        context.assertTrue(verdict.workPosition() != null
                        && verdict.workPosition().getY() - miner.getY() <= MinerWork.CLIMB,
                "a posição de trabalho ficou acima do degrau: " + verdict);
        context.complete();
    }

    /**
     * Ar em volta, vidro sob o mineiro em x 0..1 (ou 0..5 com {@code floor}), e um
     * pilar de pedra em x 6, y 8..12. Devolve o topo do pilar, que é a pedra.
     */
    private static BlockPos pillarScene(TestContext context, boolean floor) {
        ServerWorld world = context.getWorld();

        for (int x = -2; x <= 10; x++) {
            for (int y = 6; y <= 16; y++) {
                for (int z = -3; z <= 3; z++) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, y, z)),
                            Blocks.AIR.getDefaultState());
                }
            }
        }

        for (int x = 0; x <= (floor ? 5 : 1); x++) {
            world.setBlockState(context.getAbsolutePos(new BlockPos(x, 10, 0)),
                    Blocks.GLASS.getDefaultState());
        }

        for (int y = 8; y <= 12; y++) {
            world.setBlockState(context.getAbsolutePos(new BlockPos(6, y, 0)),
                    Blocks.STONE.getDefaultState());
        }

        return context.getAbsolutePos(new BlockPos(6, 12, 0));
    }
}
