package com.villagecolony.gametest;

import com.villagecolony.fabric.integration.BlockProtection;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * O que o trabalhador nunca pode quebrar — regra do autor, 2026-08-13.
 *
 * <p>O que este teste alcança e o que não alcança precisa ficar claro. O
 * mundo do gametest não tem vila gerada: estruturas nascem da geração de
 * terreno, e a bateria roda num mundo de teste vazio. Então a metade
 * "vila original" da regra é exercitada apenas no caminho em que não há
 * estrutura nenhuma — que é o caminho que roda a cada colheita, e o que
 * precisa ser barato e não travar.
 *
 * <p>A outra metade — bloco posto pelo jogador — é testável de verdade, e
 * está aqui.
 *
 * <p>Que a caixa da vila de fato protege é verificação de jogo, e está
 * registrada como tal no §8 do Project-State.
 */
public class BlockProtectionGameTest implements FabricGameTest {

    /** Fora de vila, e sem marca de mão: pode quebrar. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "protection_wild")
    public void aWildBlockMayBeBroken(TestContext context) {
        BlockPos pos = new BlockPos(2, 2, 2);

        context.setBlockState(pos, Blocks.OAK_LEAVES.getDefaultState());

        ServerWorld world = context.getWorld();
        BlockPos absolute = context.getAbsolutePos(pos);

        context.assertTrue(
                BlockProtection.mayBreak(world, absolute, world.getBlockState(absolute)),
                "folha de floresta, fora de vila, e a proteção recusou");

        context.assertTrue(
                !BlockProtection.isVillageOriginal(world, absolute),
                "não há vila gerada no mundo do teste, e a pergunta disse que sim");

        context.complete();
    }

    /** Folha pendurada à mão é bloco do jogador, e não se toca. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "protection_player")
    public void aBlockPlacedByHandIsProtected(TestContext context) {
        BlockPos pos = new BlockPos(2, 2, 2);

        context.setBlockState(
                pos, Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));

        ServerWorld world = context.getWorld();
        BlockPos absolute = context.getAbsolutePos(pos);

        context.assertTrue(
                BlockProtection.isPlayerPlaced(world.getBlockState(absolute)),
                "a folha pendurada à mão não foi reconhecida como do jogador");

        context.assertTrue(
                !BlockProtection.mayBreak(world, absolute, world.getBlockState(absolute)),
                "a proteção deixou quebrar um bloco posto pelo jogador");

        context.complete();
    }
}
