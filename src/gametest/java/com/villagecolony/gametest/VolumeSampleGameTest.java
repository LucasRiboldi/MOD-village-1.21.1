package com.villagecolony.gametest;

import com.villagecolony.fabric.integration.VolumeSample;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * A Regra 22 passa a dizer de que é feita — P1.6, 2026-09-19.
 *
 * <p>Duas sessões seguidas tiveram 70% das recusas em <i>"algo dentro do
 * volume"</i> sem nada que dissesse o quê. Estes cenários trancam as duas
 * metades da amostra: a <b>causa</b> separada e o <b>bloco</b> nomeado.
 */
public class VolumeSampleGameTest {

    /**
     * O bloco que barrou é nomeado, e por posição — não por visita.
     *
     * <p>A contagem por visita foi o defeito real do
     * {@code ProtectionSample} em 09-18: ele relatou <i>"1548 chest"</i>
     * numa vila de três camas, porque a varredura repassa a mesma coluna
     * a cada ciclo. O número media a frequência da varredura, não o
     * terreno — e é isso que a segunda metade deste cenário impede.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "volume_sample",
            tickLimit = 100)
    public void theBlockInTheWayIsNamedOncePerPosition(TestContext context) {
        ServerWorld world = context.getWorld();

        VolumeSample.clearAll();

        BlockPos cactus = context.getAbsolutePos(new BlockPos(1, 2, 1));

        world.setBlockState(cactus, Blocks.CACTUS.getDefaultState());

        VolumeSample.inTheColumn(world, cactus);

        if (VolumeSample.countOf("cactus") != 1) {
            throw new AssertionError(
                    "o bloco que barrou nao foi nomeado: cactus contou "
                            + VolumeSample.countOf("cactus"));
        }

        // A MESMA posição de novo, como a varredura faz a cada ciclo.
        VolumeSample.inTheColumn(world, cactus);

        VolumeSample.inTheColumn(world, cactus);

        if (VolumeSample.countOf("cactus") != 1) {
            throw new AssertionError(
                    "a mesma posicao foi contada de novo — a amostra mede a frequencia"
                            + " da varredura, e nao o terreno: cactus contou "
                            + VolumeSample.countOf("cactus"));
        }

        VolumeSample.clearAll();

        context.complete();
    }

    /**
     * As duas causas não se misturam.
     *
     * <p>É a razão de a amostra existir: <i>"a colônia já construiu ali"</i>
     * manda procurar mais longe, e <i>"tem cacto na coluna"</i> manda
     * limpar o terreno. Somadas numa linha só, escolher o conserto vira
     * chute — e foi o que custou o P1.3 antes do {@code ProtectionSample}.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "volume_sample",
            tickLimit = 100)
    public void theTwoCausesAreNeverAddedTogether(TestContext context) {
        ServerWorld world = context.getWorld();

        VolumeSample.clearAll();

        BlockPos stone = context.getAbsolutePos(new BlockPos(2, 2, 2));

        world.setBlockState(stone, Blocks.STONE.getDefaultState());

        VolumeSample.inTheColumn(world, stone);

        VolumeSample.colonyBuilt();

        VolumeSample.colonyBuilt();

        if (VolumeSample.countOf(VolumeSample.Why.IN_THE_COLUMN) != 1) {
            throw new AssertionError(
                    "a coluna barrada contou "
                            + VolumeSample.countOf(VolumeSample.Why.IN_THE_COLUMN)
                            + ", esperado 1 — as causas se misturaram");
        }

        if (VolumeSample.countOf(VolumeSample.Why.COLONY_BUILT) != 2) {
            throw new AssertionError(
                    "a obra da colonia contou "
                            + VolumeSample.countOf(VolumeSample.Why.COLONY_BUILT)
                            + ", esperado 2 — as causas se misturaram");
        }

        // E a obra da colônia NÃO entra na amostra de blocos: ela não é
        // "coisa no caminho", é a vila cheia.
        if (VolumeSample.countOf("stone") != 1) {
            throw new AssertionError(
                    "a amostra de blocos contou " + VolumeSample.countOf("stone")
                            + " pedras, esperado 1 — colonyBuilt vazou para a lista"
                            + " de blocos");
        }

        VolumeSample.clearAll();

        context.complete();
    }
}
