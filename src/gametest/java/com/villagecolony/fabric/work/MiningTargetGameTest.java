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
 *
 * <p>Um lote por caso, e o cenário fica dentro da arena.
 */
public class MiningTargetGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mining_target")
    public void aStoneWhoseOnlyFootholdIsTooHighIsRefused(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos stone = pillarScene(context, false);
        BlockPos miner = context.getAbsolutePos(new BlockPos(1, 41, 1));

        MiningTarget.Verdict verdict = MiningTarget.judge(world, stone, miner);

        context.assertTrue(verdict.rejection() == MiningTarget.Rejection.TOO_HIGH,
                "a pedra com lugar de pé só em cima dela virou alvo: " + verdict);
        context.complete();
    }

    /** Controle: com chão até o pé do pilar, a mesma pedra serve, e o lugar é o de baixo. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mining_target_floor")
    public void theSameStoneWithAFloorBesideItIsATarget(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos stone = pillarScene(context, true);
        BlockPos miner = context.getAbsolutePos(new BlockPos(1, 41, 1));

        MiningTarget.Verdict verdict = MiningTarget.judge(world, stone, miner);

        context.assertTrue(verdict.eligible(), "a pedra alcançável foi recusada: " + verdict);
        context.assertTrue(verdict.workPosition() != null
                        && verdict.workPosition().getY() - miner.getY() <= MinerWork.CLIMB,
                "a posição de trabalho ficou acima do degrau: " + verdict);
        context.complete();
    }

    /**
     * O lugar escolhido é o mais perto da pedra entre os que ele alcança, e não o
     * primeiro da busca — playtest de 08-10: o mineiro já estava num lugar de pé na
     * borda do braço (3,9 pela conta do bloco, 4,1 pela posição real), "chegou" sem
     * alcançar e ficou parado até desistir.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mining_target_nearest")
    public void theWorkPositionIsTheReachedSpotNearestTheStone(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos stone = pillarScene(context, true);
        BlockPos miner = context.getAbsolutePos(new BlockPos(3, 41, 1));

        MiningTarget.Verdict verdict = MiningTarget.judge(world, stone, miner);

        context.assertTrue(context.getAbsolutePos(new BlockPos(5, 41, 1)).equals(verdict.workPosition()),
                "a posição de trabalho não é a mais perto da pedra: " + verdict);
        context.complete();
    }

    /**
     * Trinta acima do piso, onde nenhuma arena tem bloco (um bloco do vizinho em
     * z -1 virava lugar de pé): ar dentro da arena, vidro sob o mineiro em x 0..1
     * (ou 0..5 com {@code floor}) e um pilar de pedra em x 6, y 38..42, na linha
     * z 1. Devolve o topo do pilar, que é a pedra.
     */
    private static BlockPos pillarScene(TestContext context, boolean floor) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 7; x++) {
            for (int y = 36; y <= 46; y++) {
                for (int z = 0; z <= 2; z++) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, y, z)),
                            Blocks.AIR.getDefaultState());
                }
            }
        }

        for (int x = 0; x <= (floor ? 5 : 1); x++) {
            world.setBlockState(context.getAbsolutePos(new BlockPos(x, 40, 1)),
                    Blocks.GLASS.getDefaultState());
        }

        for (int y = 38; y <= 42; y++) {
            world.setBlockState(context.getAbsolutePos(new BlockPos(6, y, 1)),
                    Blocks.STONE.getDefaultState());
        }

        return context.getAbsolutePos(new BlockPos(6, 42, 1));
    }
}
