package com.villagecolony.gametest;

import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.work.TestBarrier;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.List;

/**
 * A Regra 28 — a barreira de teste, e o que ela cobre.
 *
 * <p>Ela é provisória por declaração do autor, e o que estes testes
 * travam é o <b>tamanho</b> dela: sete peças, nem uma a mais. Barreira
 * que cresce sozinha é a Regra 27 morrendo em silêncio, e a diferença
 * entre "a casa subiu" e "a casa subiu com metade dos blocos riscados"
 * é exatamente esta lista.
 *
 * <p>Desde 2026-08-21 cada peça riscada nomeia a cadeia que deveria
 * tê-la produzido. Estes testes travam esse nome também: uma cadeia
 * errada no log manda o autor procurar o defeito na profissão errada.
 */
public class TestBarrierGameTest implements FabricGameTest {

    /** As cinco, e nenhuma outra. */
    private static final List<String> COVERED =
            List.of(
                    "oak_door",
                    "chest",
                    "stripped_oak_log",
                    "torch",
                    "wall_torch",
                    "glass_pane");

    /**
     * Estas a obra espera, e esperar é a Regra 27.
     *
     * <p>Cama e lampião entraram nesta lista em 2026-08-21, e não por
     * capricho: a Regra 21 morreu, então nada os repõe depois. Riscá-los
     * deixaria a casa sem eles para sempre, e a demanda de lã e de ferro
     * sumiria junto — a conta sai da obra aberta agora.
     */
    private static final List<String> NOT_COVERED =
            List.of(
                    "white_bed",
                    "red_bed",
                    "lantern",
                    "soul_lantern",
                    "cobblestone",
                    "oak_planks",
                    "oak_log",
                    "sand",
                    "glass",
                    "dirt_path");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "test_barrier")
    public void theBarrierCoversTheFivePieces(TestContext context) {
        for (String block : COVERED) {
            context.assertTrue(
                    TestBarrier.chainFor(ResourceId.vanilla(block)).isPresent(),
                    block + " saiu da barreira, e a obra vai passar a esperar por ele");
        }

        context.complete();
    }

    /**
     * Parede e chão nunca entram.
     *
     * <p>Sem pedra e sem tábua a casa tem furo de parede, e furo é a
     * Regra 22 ao contrário.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "test_barrier")
    public void theBarrierNeverCoversWallOrFloor(TestContext context) {
        for (String block : NOT_COVERED) {
            context.assertTrue(
                    TestBarrier.chainFor(ResourceId.vanilla(block)).isEmpty(),
                    block + " entrou na barreira, e a casa vai subir com furo");
        }

        context.complete();
    }

    /** Cada peça aponta para a profissão que a produz, e não outra. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "test_barrier")
    public void everyPieceNamesTheChainThatShouldHaveMadeIt(TestContext context) {
        assertChain(context, "glass_pane", "smelter");
        assertChain(context, "stripped_oak_log", "manufacturer");
        assertChain(context, "torch", "miner");
        assertChain(context, "oak_door", "manufacturer");
        assertChain(context, "chest", "manufacturer");

        context.complete();
    }

    private static void assertChain(TestContext context, String block, String profession) {
        String chain = TestBarrier.chainFor(ResourceId.vanilla(block)).orElse("");

        context.assertTrue(
                chain.contains(profession),
                block + " manda procurar em \"" + chain + "\", e quem o faz é o " + profession);
    }
}
