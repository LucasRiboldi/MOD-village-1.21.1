package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A linha de desistência do lenhador dizia o limite, e não o contador —
 * 2026-09-05, corrigido em 2026-09-09.
 *
 * <p><b>A medida.</b> Ela imprimia {@code TreeChoice.stallLimit}, que é a
 * constante 2.400, e saía igual para os dois guardas. Na sessão de 09-05
 * o log dizia <i>"made no progress for 2400 work ticks"</i> em
 * desistências separadas por <b>600 tiques</b> — e algumas delas eram do
 * guarda de imobilidade, que fala aos 300. Oito vezes de diferença com o
 * mesmo texto.
 *
 * <p>O preço não foi o log feio: foi a leitura. A frase mandava procurar
 * o defeito no guarda — que estava certo — em vez de no castigo da
 * árvore, que era onde ele estava. Está registrado como pendência 🟠 de
 * 09-05.
 *
 * <p><b>Por que isto é unitário e não gametest.</b> O que se afirma aqui
 * é a frase, e frase é função pura. Um gametest provaria que a tarefa
 * volta para a fila — o que
 * {@code LumberjackGameTest.theStallGuardReturnsTheTaskAndForgetsTheTree}
 * já prova desde 08-15 — e não que o número impresso é o certo, que é
 * exatamente o que faltava.
 */
class LumberjackGiveUpReasonTest {

    /** O contador de imobilidade, e não o limite do outro guarda. */
    @Test
    void theMotionlessReasonCarriesTheCounterItWasGiven() {
        assertEquals("has not moved a block in 300 work ticks",
                TreeChoice.motionless(300));
    }

    @Test
    void theNoProgressReasonCarriesTheCounterItWasGiven() {
        assertEquals("made no progress for 2401 work ticks",
                TreeChoice.noProgress(2401));
    }

    /**
     * <b>O defeito, escrito como teste.</b> Uma desistência aos 600
     * tiques não pode sair falando em 2.400.
     */
    @Test
    void aGiveUpAtSixHundredDoesNotClaimTwoThousandFourHundred() {
        String said = TreeChoice.noProgress(600);

        assertTrue(said.contains("600"), said);
        assertFalse(said.contains(String.valueOf(TreeChoice.STALL_LIMIT)), said);
    }

    /**
     * <b>A escolha entre os dois guardas, que é onde o defeito morava.</b>
     *
     * <p>Achado do `gauntlet-verifier` na iteração 1: a primeira versão
     * deste arquivo afirmava só os construtores da frase, e o defeito de
     * 09-05 não estava neles — estava no <b>argumento</b>. O mutante
     * `noProgress(stallLimit)` nos dois pontos de desistência recriava a
     * pendência inteira, compilava, e passava nos 681 unitários e nos 274
     * gametests. Um teste que não cai sobre o defeito que diz cobrir não
     * cobre nada.
     *
     * <p>Aqui os dois ramos são a função, e o contador é parâmetro: a
     * asserção recebe os dois números e exige que saia o do guarda que
     * falou.
     */
    @Test
    void theMotionlessGuardIsTheOneThatSpeaksWhenItFired() {
        assertEquals("has not moved a block in 300 work ticks",
                TreeChoice.reasonFor(true, 300, 2401));
    }

    @Test
    void theStallGuardIsTheOneThatSpeaksWhenItFired() {
        assertEquals("made no progress for 2401 work ticks",
                TreeChoice.reasonFor(false, 300, 2401));
    }

    /**
     * E nenhum dos dois pode devolver a constante.
     *
     * <p>É a pendência de 09-05 escrita como asserção: seja qual for o
     * guarda, o número que sai é um dos dois contadores que entraram.
     */
    @Test
    void neitherGuardEverPrintsTheLimitItself() {
        for (boolean motionless : new boolean[] {true, false}) {
            String said = TreeChoice.reasonFor(motionless, 300, 600);

            assertFalse(said.contains(String.valueOf(TreeChoice.STALL_LIMIT)),
                    "a frase voltou a imprimir a constante: " + said);

            assertTrue(said.contains("300") || said.contains("600"), said);
        }
    }

    /**
     * E os dois guardas deixam de dizer a mesma coisa.
     *
     * <p>É a metade que o número sozinho não resolve: com o mesmo texto,
     * quem lê o log não sabe se o aldeão ficou parado ou se andou sem
     * chegar — e as duas coisas levam a correções diferentes. O mineiro
     * separa as duas frases desde 2026-09-03, e o lenhador não separava.
     */
    @Test
    void theTwoGuardsDoNotSayTheSameThing() {
        assertNotEquals(TreeChoice.motionless(300), TreeChoice.noProgress(300));
    }
}
