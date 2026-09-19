package com.villagecolony.gametest;

import com.villagecolony.fabric.work.MineSettling;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * O mineiro espera a areia assentar — 2026-09-19, pedido do autor.
 *
 * <p>Estes cenários usam <b>areia de verdade caindo</b>, e não um dublê:
 * o {@code FallingBlockEntity} é o que existe entre o bloco sair e o bloco
 * pousar, e é exatamente ele que a espera consulta. Um teste com entidade
 * falsa provaria que a classe lê uma lista, não que o mineiro espera a
 * duna.
 */
public class MineSettlingGameTest {

    /** Chão sólido para a areia ter onde pousar. */
    private static void ground(TestContext context) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    /**
     * Enquanto a areia cai, o mineiro não cava.
     *
     * <p>É a metade que o autor pediu: <i>esperar todos os blocos caírem
     * antes de continuar minerando</i>.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_settling",
            tickLimit = 200)
    public void theFallingSandHoldsThePickaxe(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        BlockPos target = context.getAbsolutePos(new BlockPos(0, 1, 0));

        // <b>A areia é posta no mundo e deixada cair</b>, em vez de a
        // entidade ser criada à mão. `spawnFromBlock` lê o estado da
        // posição, e num espaço vazio ela nasce e some no mesmo tique —
        // foi o que fez a primeira versão deste teste falhar. Assim a
        // queda é a do jogo: bloco sem apoio, gravidade, entidade.
        // Baixa o bastante para a queda passar pelo raio do alvo (3) já
        // nos primeiros tiques, e alta o bastante para ainda estar no ar.
        BlockPos high = context.getAbsolutePos(new BlockPos(0, 4, 0));

        world.setBlockState(high, Blocks.SAND.getDefaultState());

        world.scheduleBlockTick(high, Blocks.SAND, 1);

        context.runAtTick(3, () -> {
            if (!MineSettling.stillFalling(world, target)) {
                throw new AssertionError(
                        "a areia estava caindo sobre o alvo e o mineiro nao esperou");
            }

            if (!MineSettling.waits(world, target, 0)) {
                throw new AssertionError("waits() liberou a picareta no meio da queda");
            }

            context.complete();
        });
    }

    /**
     * Assentada a areia, a picareta é liberada.
     *
     * <p>Sem esta metade a correção seria um mineiro que nunca mais cava:
     * o teste de cima passaria com {@code stillFalling} devolvendo
     * sempre {@code true}.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_settling",
            tickLimit = 200)
    public void theSettledSandReleasesThePickaxe(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        BlockPos target = context.getAbsolutePos(new BlockPos(0, 1, 0));

        BlockPos high = context.getAbsolutePos(new BlockPos(0, 4, 0));

        world.setBlockState(high, Blocks.SAND.getDefaultState());

        world.scheduleBlockTick(high, Blocks.SAND, 1);

        // Tempo de sobra para a queda inteira assentar.
        context.runAtTick(60, () -> {
            if (MineSettling.stillFalling(world, target)) {
                throw new AssertionError(
                        "a areia ja assentou e a espera continuou segurando o mineiro");
            }

            context.complete();
        });
    }

    /**
     * A paciência tem fim, e a queda eterna não aposenta o mineiro.
     *
     * <p>Areia caindo sobre água não assenta no lugar, e um gerador de
     * areia de jogador cairia para sempre. Esgotada a paciência, ele cava
     * assim mesmo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_settling",
            tickLimit = 200)
    public void thePatienceRunsOutAndHeDigsAnyway(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        BlockPos target = context.getAbsolutePos(new BlockPos(0, 1, 0));

        BlockPos high = context.getAbsolutePos(new BlockPos(0, 4, 0));

        world.setBlockState(high, Blocks.SAND.getDefaultState());

        world.scheduleBlockTick(high, Blocks.SAND, 1);

        context.runAtTick(3, () -> {
            // Mesma queda do primeiro cenário: o que muda é só a conta de
            // espera já gasta.
            if (!MineSettling.waits(world, target, 0)) {
                throw new AssertionError("a queda nao segurou nem no primeiro tique");
            }

            if (MineSettling.waits(world, target, MineSettling.PATIENCE)) {
                throw new AssertionError(
                        "a paciencia esgotou e a espera continuou — o mineiro fica preso"
                                + " numa queda que nao termina");
            }

            context.complete();
        });
    }
}
