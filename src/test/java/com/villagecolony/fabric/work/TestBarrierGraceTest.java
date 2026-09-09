package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A barreira espera antes de riscar — 2026-09-09.
 *
 * <p><b>O que estes testes travam é um grito falso.</b> A sessão de
 * 09-06 riscou 24 {@code stripped_oak_log} dizendo <i>"that chain did
 * not deliver"</i>, e a colônia que os riscou tinha <b>cinquenta toras
 * de carvalho no baú</b> no mesmo instante:
 *
 * <pre>
 * 19:24:59  Colony 1760a4b3 stores {OAK_LOG=50, OAK_PLANKS=174, ...}
 * 19:25:02  TEST BARRIER skipped stripped_oak_log — that chain did not deliver
 * </pre>
 *
 * <p>A cadeia entrega: {@code ManufacturerWork.strip} roda antes da
 * guarda de conversão e escolhe a espécie que a colônia tem, desde
 * 09-05. Ela só nunca teve o ciclo — o construtor via a falta e riscava
 * no mesmo tique. Na sessão inteira o fabricante descascou <b>um</b>
 * tronco.
 *
 * <p>Acusar a cadeia errada é pior que o buraco na parede: manda o autor
 * procurar defeito numa profissão que está certa.
 */
class TestBarrierGraceTest {

    private static final ResourceId STRIPPED = ResourceId.vanilla("stripped_oak_log");

    private final UUID project = UUID.randomUUID();

    @BeforeEach
    void forgetWhatOtherTestsMissed() {
        TestBarrier.clearAll();
    }

    /** A primeira falta não risca: é ela que abre a espera. */
    @Test
    void theFirstMissDoesNotStrike() {
        assertFalse(
                TestBarrier.graceExpired(0, project, STRIPPED),
                "riscou na primeira falta, e o fabricante não teve um tique para descascar");
    }

    /** Nem a falta seguinte, enquanto a carência corre. */
    @Test
    void theBarrierKeepsWaitingWhileTheGraceRuns() {
        TestBarrier.graceExpired(0, project, STRIPPED);

        assertFalse(TestBarrier.graceExpired(2999, project, STRIPPED));
    }

    /**
     * E depois do prazo ela risca, que é a Regra 28 como sempre foi.
     *
     * <p>A carência adia o risco; não o revoga. Cadeia que de fato não
     * entrega continua deixando a casa subir sem a peça, e continua
     * gritando — só que agora o grito é verdadeiro.
     */
    @Test
    void afterTheGraceItStrikesLikeItAlwaysDid() {
        TestBarrier.graceExpired(0, project, STRIPPED);

        assertTrue(
                TestBarrier.graceExpired(3000, project, STRIPPED),
                "a carência não venceu, e a obra ficaria esperando para sempre");
    }

    /**
     * O relógio é da obra <b>e</b> da peça.
     *
     * <p>A mesma peça faltando em duas casas são duas esperas: a carência
     * gasta numa não pode riscar a peça da outra, que acabou de começar.
     */
    @Test
    void eachProjectWaitsOnItsOwn() {
        TestBarrier.graceExpired(0, project, STRIPPED);

        assertFalse(
                TestBarrier.graceExpired(3000, UUID.randomUUID(), STRIPPED),
                "a espera de uma obra venceu a peça de outra");
    }

    /**
     * E o despertador da obra não começa a contar.
     *
     * <p>{@code hasMaterialForNextBlock} pergunta por {@link
     * TestBarrier#willStrike}, e ele lê sem mexer no relógio. Se
     * perguntar contasse, a carência venceria antes de o construtor ter
     * tentado uma vez — e a espera que ela existe para dar nunca teria
     * acontecido.
     */
    @Test
    void askingDoesNotStartTheClock() {
        assertFalse(TestBarrier.willStrike(0, project, STRIPPED));
        assertFalse(
                TestBarrier.willStrike(3000, project, STRIPPED),
                "a pergunta começou a contar, e a carência venceu sozinha");
    }

    /**
     * As duas perguntas casam, e é o que impede o laço.
     *
     * <p>Enquanto a barreira espera, a peça segura a obra como qualquer
     * outra — o despertador cai no teste de material de verdade. Depois
     * que ela desiste, a peça deixa de segurar, porque o construtor vai
     * passar por cima. Se as duas discordassem, a obra acordaria,
     * tentaria, falharia e dormiria de novo, todo ciclo.
     */
    @Test
    void theAlarmAgreesWithWhatTheBuilderWillDo() {
        TestBarrier.graceExpired(0, project, STRIPPED);

        assertFalse(
                TestBarrier.willStrike(2999, project, STRIPPED),
                "o despertador liberou a obra enquanto o construtor ainda esperava");

        assertTrue(
                TestBarrier.willStrike(3000, project, STRIPPED),
                "o construtor vai riscar e o despertador ainda segura a obra");
    }

    /** Peça fora da barreira nunca entra nesta conta. */
    @Test
    void aPieceOutsideTheBarrierIsNotCovered() {
        assertTrue(TestBarrier.chainFor(ResourceId.vanilla("cobblestone")).isEmpty());
    }
}
