package com.villagecolony.core.coordination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Metade do cru fica para o pedreiro — 2026-09-19.
 *
 * <p><b>O que isto tranca, medido na sessão de 17:15.</b> A obra parou
 * <b>39 vezes</b> esperando {@code cut_sandstone}, e o baú da colônia
 * tinha <b>139 arenitos LISOS e zero arenito cru</b>. O fundidor assou o
 * estoque inteiro, e o arenito cortado sai do <b>cru</b>.
 *
 * <p>É o mesmo defeito que a reserva de tronco de 09-05 corrigiu do lado
 * da madeira, com outro material — e por isso a conta tem a mesma forma.
 */
class RawStoneReserveTest {

    /** Sem nada processado, metade do cru pode ir à fornalha. */
    @Test
    void halfOfTheRawMayBeSmelted() {
        assertEquals(50, ColonyGoals.rawToKeep(100, 0), "a reserva nao e metade");

        assertEquals(50, ColonyGoals.rawThatMayBeSmelted(100, 0));
    }

    /**
     * O caso da sessão: tudo processado e nada cru — não se assa mais.
     *
     * <p>É a afirmação que impede o defeito de voltar: com 139 lisos e
     * zero crus, a resposta tem de ser <b>zero</b>.
     */
    @Test
    void theExhaustedRawIsNeverSmeltedAgain() {
        assertEquals(
                0,
                ColonyGoals.rawThatMayBeSmelted(0, 139),
                "o fundidor assaria o que nao existe, e o pedreiro fica sem materia");
    }

    /**
     * Empatado, para de assar.
     *
     * <p>O ponto de equilíbrio é o que o autor chamou de metade em 09-05:
     * a proporção não se move.
     */
    @Test
    void theBalancedStockStopsTheFurnace() {
        assertEquals(0, ColonyGoals.rawThatMayBeSmelted(40, 40));

        assertTrue(
                ColonyGoals.rawThatMayBeSmelted(40, 10) > 0,
                "com muito cru e pouco processado a fornalha tem de seguir");
    }

    /**
     * Com muito processado, o pouco cru que resta fica inteiro.
     *
     * <p>Esta afirmação saiu errada na primeira versão — eu exigia
     * {@code rawToKeep(5, 500) == 0}, que é o contrário do que a regra
     * quer: com quinhentos lisos e cinco crus, os cinco são <b>tudo</b>
     * o que o pedreiro tem, e assá-los é exatamente o defeito de 17:15.
     * Guardar os cinco é a resposta certa, e o que não se pode é assar
     * nenhum.
     */
    @Test
    void theLastRawOnesAreKeptWhole() {
        assertEquals(5, ColonyGoals.rawToKeep(5, 500), "os ultimos crus foram para a fornalha");

        assertEquals(
                0,
                ColonyGoals.rawThatMayBeSmelted(5, 500),
                "o fundidor levaria o ultimo arenito cru da colonia");

        assertEquals(0, ColonyGoals.rawThatMayBeSmelted(0, 0));
    }
}
