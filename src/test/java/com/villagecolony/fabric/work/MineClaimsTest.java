package com.villagecolony.fabric.work;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Um mineiro por mina — 2026-08-28.
 *
 * <p><b>A sessão de 2026-08-26, 23:23:08.</b> A colônia tinha dois
 * mineiros e duas tarefas de pedra — uma por recurso pedido, que é como
 * o {@code ColonyCycle} as abre —, e as duas apontavam para a mesma
 * escada. A reserva existia, mas era da <b>tarefa</b>: cada um tinha a
 * sua, e nenhuma delas falava da mina.
 *
 * <p>O cursor da galeria é do {@code Mine}, e é um só. Os dois recebiam
 * a mesma posição na mesma passagem, andavam para o mesmo lugar, e
 * davam {@code could not reach the stone} no mesmo tique — e o recuo do
 * cursor que esse aviso dispara rodava duas vezes por um bloco.
 *
 * <p>É o mesmo problema que o {@link TreeClaims} resolveu do lado do
 * lenhador, e a resposta é a mesma: a coisa disputada é que tem dono.
 * Ali é a árvore; aqui é a mina.
 */
class MineClaimsTest {

    private static final UUID COLONY = UUID.randomUUID();

    private static final UUID OTHER_COLONY = UUID.randomUUID();

    private final UUID first = UUID.randomUUID();

    private final UUID second = UUID.randomUUID();

    @BeforeEach
    void emptyTheShafts() {
        MineClaims.clearAll();
    }

    /** Mina livre é de quem chegar. */
    @Test
    void theFirstMinerToAskGetsTheShaft() {
        assertTrue(MineClaims.claim(COLONY, first));
        assertEquals(first, MineClaims.diggerIn(COLONY).orElseThrow());
    }

    /** O segundo não desce: é a escada de um só. */
    @Test
    void theSecondMinerIsTurnedAway() {
        MineClaims.claim(COLONY, first);

        assertFalse(MineClaims.claim(COLONY, second));
        assertEquals(first, MineClaims.diggerIn(COLONY).orElseThrow());
    }

    /**
     * Quem já está dentro continua dentro.
     *
     * <p>A pergunta é feita uma vez por pedra, e não uma vez por mina:
     * uma reserva que não se renovasse expulsaria o dono na segunda
     * pedra dele.
     */
    @Test
    void theHolderKeepsTheShaftOnEveryPass() {
        MineClaims.claim(COLONY, first);

        assertTrue(MineClaims.claim(COLONY, first));
        assertTrue(MineClaims.claim(COLONY, first));
    }

    /** Duas colônias, duas minas, e nenhuma sabe da outra. */
    @Test
    void eachColonyHasItsOwnShaft() {
        assertTrue(MineClaims.claim(COLONY, first));
        assertTrue(MineClaims.claim(OTHER_COLONY, second));
    }

    /** Solto o dono, a mina passa adiante. */
    @Test
    void releasingHandsTheShaftOver() {
        MineClaims.claim(COLONY, first);

        MineClaims.release(first);

        assertTrue(MineClaims.diggerIn(COLONY).isEmpty());
        assertTrue(MineClaims.claim(COLONY, second));
    }

    /**
     * Mineiro sem trabalho aberto perde a mina, e não a tranca.
     *
     * <p><b>É o que impede a reserva de vazar.</b> Morte, zumbificação,
     * tarefa devolvida pelo guarda de travamento: nem todo fim de
     * trabalho passa por um lugar só, e uma mina trancada por um aldeão
     * que não existe mais é pior que dois cavando a mesma escada.
     */
    @Test
    void theShaftPassesOnWhenTheHolderHasNoJobLeft() {
        MineClaims.claim(COLONY, first);

        MineClaims.retainOnly(Set.of(second));

        assertTrue(MineClaims.diggerIn(COLONY).isEmpty());
        assertTrue(MineClaims.claim(COLONY, second));
    }

    /** Quem ainda trabalha não é despejado pela conferência. */
    @Test
    void theWorkingHolderSurvivesTheSweep() {
        MineClaims.claim(COLONY, first);

        MineClaims.retainOnly(Set.of(first, second));

        assertEquals(first, MineClaims.diggerIn(COLONY).orElseThrow());
        assertFalse(MineClaims.claim(COLONY, second));
    }

    /** Mina de colônia que ninguém pediu não tem dono. */
    @Test
    void anUnaskedShaftHasNoDigger() {
        assertTrue(MineClaims.diggerIn(COLONY).isEmpty());
    }
}
