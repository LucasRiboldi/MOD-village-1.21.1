package com.villagecolony.core.construction.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O plano de alturas da vila — ADR-063, fase 1: o cálculo, sem Minecraft. */
class GradePlanTest {

    /** O paredão de 4 entre o piso de duas estruturas vira degraus de 1 (o curral grande do jogo da 0.4.136). */
    @Test
    void aCliffBetweenTwoAnchorsBecomesStepsOfOne() {
        GradePlan plan = new GradePlan(10, 1);

        for (int x = 0; x < 10; x++) {
            plan.ground(x, 0, x < 5 ? 0 : 4);
        }
        plan.anchor(0, 0, 0).anchor(9, 0, 4);

        int[][] target = plan.plan().target();

        assertEquals(0, target[0][0], "a âncora de baixo saiu da altura dela");
        assertEquals(4, target[9][0], "a âncora de cima saiu da altura dela");
        for (int x = 1; x < 10; x++) {
            assertTrue(Math.abs(target[x][0] - target[x - 1][0]) <= 1, "degrau maior que 1 entre " + (x - 1) + " e " + x
                    + ": " + java.util.Arrays.deepToString(target));
        }
    }

    /** O chão que já anda em degraus de 1 fica onde está. */
    @Test
    void groundThatAlreadyStepsByOneDoesNotMove() {
        GradePlan plan = new GradePlan(6, 1);

        for (int x = 0; x < 6; x++) {
            plan.ground(x, 0, x);
        }
        plan.anchor(0, 0, 0);

        GradePlan.Result result = plan.plan();

        assertEquals(0, result.pending(), "coluna mexida sem precisar: " + java.util.Arrays.deepToString(result.target()));
    }

    /** A distância contorna o que fica fora do plano: a parede entre as âncoras não é atalho. */
    @Test
    void theDistanceGoesAroundWhatIsOutOfThePlan() {
        GradePlan walled = new GradePlan(7, 9);
        GradePlan open = new GradePlan(7, 9);

        for (int z = 0; z < 8; z++) {
            walled.out(3, z);
        }
        for (GradePlan plan : new GradePlan[] {walled, open}) {
            plan.anchor(2, 0, 0).anchor(4, 0, 6);
        }

        assertTrue(walled.plan().conflicts().isEmpty(), "com a volta de 18 passos, 6 de altura cabem em degraus de 1");
        assertFalse(open.plan().conflicts().isEmpty(), "sem a parede, 6 de altura em 2 passos não cabem e não foi dito");
    }

    /** O caminho fica plano onde cabe: o chão que sobe e desce de 1 em 1 vira uma linha só. */
    @Test
    void thePathStaysFlatWhereItFits() {
        GradePlan plan = new GradePlan(9, 1);

        for (int x = 0; x < 9; x++) {
            plan.ground(x, 0, x % 2).path(x, 0);
        }
        plan.anchor(0, 0, 0).anchor(8, 0, 0);

        int[][] target = plan.plan().target();

        for (int x = 0; x < 9; x++) {
            assertEquals(0, target[x][0], "o caminho subiu e desceu: " + java.util.Arrays.deepToString(target));
        }
    }

    /** O que fica fora do plano (pegada de casa, boca da mina, chão do jogador) não muda. */
    @Test
    void whatIsOutOfThePlanKeepsItsHeight() {
        GradePlan plan = new GradePlan(5, 1);

        plan.ground(0, 0, 0).ground(1, 0, 0).ground(2, 0, 9).ground(3, 0, 0).ground(4, 0, 0);
        plan.out(2, 0).anchor(0, 0, 0);

        assertEquals(9, plan.plan().target()[2][0]);
    }

    /** Duas portas a 6 de altura e 3 passos: o plano diz onde, para a rampa ou a escada da fase 4. */
    @Test
    void anchorsThatDoNotFitAreReported() {
        GradePlan plan = new GradePlan(4, 1);

        plan.anchor(0, 0, 70).anchor(3, 0, 64);

        GradePlan.Result result = plan.plan();

        assertFalse(result.conflicts().isEmpty(), "o conflito entre as portas não foi dito");
        assertEquals(70, result.target()[0][0]);
        assertEquals(64, result.target()[3][0]);
    }
}
