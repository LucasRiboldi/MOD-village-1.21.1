package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * O E31 — o relatório da barreira afirmava o que não tinha medido.
 *
 * <p><b>A sessão das 23:06, em 2026-08-26.</b> Zero obras, zero
 * projetos, nenhum bloco assentado — a vila passou os dois minutos
 * inteiros varrendo — e mesmo assim o servidor parou dizendo:
 *
 * <pre>
 * TEST BARRIER covered for nothing this session — every piece came from
 * the colony's own chests. Rule 28 can go.
 * </pre>
 *
 * <p>A frase é uma <b>conclusão sobre a Regra 28</b>: ela diz que a
 * barreira pode sair porque a colônia produziu tudo sozinha. Numa
 * sessão que não construiu nada, a barreira não foi exercitada uma vez
 * — o silêncio dela não é notícia boa, é ausência de notícia. E estava
 * marcada como a notícia boa no {@code TODO}, que é onde a mentira
 * custava.
 *
 * <p><b>O que estes testes travam:</b> a frase só vale quando alguma
 * peça foi assentada. Sem obra, o veredito é {@code NOTHING_BUILT}, e
 * ele não absolve ninguém.
 */
class TestBarrierReportTest {

    private static final UUID PROJECT = UUID.randomUUID();

    @BeforeEach
    void forgetTheLastSession() {
        TestBarrier.clearAll();
    }

    /**
     * O E31 em uma linha: sessão sem obra não absolve a Regra 28.
     *
     * <p>Nada riscado e nada assentado é exatamente o estado da sessão
     * das 23:06 — e era dela que saía a frase.
     */
    @Test
    void aSessionThatBuiltNothingProvesNothing() {
        assertEquals(TestBarrier.Verdict.NOTHING_BUILT, TestBarrier.verdict());
    }

    /** Assentou e não riscou nada: aí sim a colônia deu conta sozinha. */
    @Test
    void aSessionThatBuiltWithoutSkippingClearsRule28() {
        TestBarrier.laidOne();

        assertEquals(TestBarrier.Verdict.COVERED_FOR_NOTHING, TestBarrier.verdict());
    }

    /**
     * Peça riscada é medida, e não depende de a obra ter terminado.
     *
     * <p>Riscar já é a barreira trabalhando: a casa ficou sem aquele
     * bloco porque a cadeia não entregou.
     */
    @Test
    void aSkippedPieceIsAMeasurementOnItsOwn() {
        TestBarrier.skip(PROJECT, ResourceId.vanilla("glass_pane"), "the smelter's glass");

        assertEquals(TestBarrier.Verdict.COVERED, TestBarrier.verdict());
    }

    /**
     * A sessão de 08-26: 127 peças assentadas, 19 delas da barreira.
     *
     * <p>Ter construído não apaga o que foi riscado — a casa subiu, e
     * subiu com material que não era da colônia.
     */
    @Test
    void buildingDoesNotExcuseWhatWasSkipped() {
        for (int piece = 0; piece < 127; piece++) {
            TestBarrier.laidOne();
        }

        TestBarrier.skip(PROJECT, ResourceId.vanilla("stripped_oak_log"), "the stripping");

        assertEquals(TestBarrier.Verdict.COVERED, TestBarrier.verdict());
    }

    /** A soma esquece as duas metades, e não só uma. */
    @Test
    void forgettingTheSessionForgetsWhatWasBuiltToo() {
        TestBarrier.laidOne();
        TestBarrier.skip(PROJECT, ResourceId.vanilla("torch"), "the miner's coal");

        TestBarrier.clearAll();

        assertEquals(TestBarrier.Verdict.NOTHING_BUILT, TestBarrier.verdict());
    }
}
