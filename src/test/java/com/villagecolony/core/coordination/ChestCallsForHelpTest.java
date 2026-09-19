package com.villagecolony.core.coordination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O baú que passa da metade chama as outras profissões — 2026-09-19.
 *
 * <p><b>Regra do autor:</b> <i>"quando o baú da profissão passar da metade
 * do preenchimento, as outras profissões devem forçar a criação de itens
 * que futuramente serão utilizados para criar as estruturas do bioma"</i>.
 *
 * <p>O que estes testes trancam é a <b>borda</b>. Um gatilho que dispare
 * cedo demais põe a colônia a fabricar o tempo todo; tarde demais e ele
 * não chega antes do baú encher — que é o defeito de 12:09, em que 660
 * itens foram ao chão.
 */
class ChestCallsForHelpTest {

    /** Baú vazio não chama ninguém. */
    @Test
    void theEmptyChestAsksForNothing() {
        assertFalse(ColonyGoals.chestCallsForHelp(0));
    }

    /**
     * Exatamente na metade ainda <b>não</b> chama.
     *
     * <p>A regra diz <i>"passar da metade"</i>, e meio não passou. É a
     * borda que separa as duas leituras possíveis da frase.
     */
    @Test
    void theHalfFullChestHasNotPassedTheHalf() {
        assertFalse(
                ColonyGoals.chestCallsForHelp(ColonyGoals.CHEST_HALF_FULL),
                "na metade exata o bau ja pediu ajuda — a regra diz PASSAR da metade");
    }

    /** Um ponto acima da metade já chama. */
    @Test
    void theChestJustPastHalfCallsForHelp() {
        assertTrue(
                ColonyGoals.chestCallsForHelp(ColonyGoals.CHEST_HALF_FULL + 1),
                "passou da metade e nao pediu ajuda — o bau enche antes de alguem reagir");
    }

    /** Baú cheio chama, obviamente — é o caso que destruiu 660 itens. */
    @Test
    void theFullChestCallsForHelp() {
        assertTrue(ColonyGoals.chestCallsForHelp(100));
    }
}
