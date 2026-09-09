package com.villagecolony.fabric.work;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A roça que não coube cede a vez às casas — 2026-09-09.
 *
 * <p><b>Uma roça sem lote parava a vila inteira.</b> A sessão de 09-09
 * mediu a colônia {@code 634bf5cc} presa por uma hora:
 *
 * <pre>
 * no building work: the only free lot is outside the farmer's reach
 * sweep: 108 planner runs, 0 passes over 0 columns, 108 answered by the index
 * </pre>
 *
 * <p>Cento e oito passagens do planejador, nenhuma obra aberta. O lote
 * que o índice devolvia era sempre o mesmo, sempre longe demais, e a
 * recusa dele encerrava a passagem — o planejador nunca chegava a tentar
 * uma casa.
 *
 * <p>E o preço não era ficar sem roça. Sem obra não há pedido de tábua
 * nem de pedra, então o construtor e o mineiro ficavam sem tarefa:
 * <b>78 dos 108 ciclos saíram com "assigned 0 tasks (0 open)"</b>, com
 * 86 pedregulhos e 83 tábuas paradas no baú. A queixa do autor foi
 * exatamente essa — <i>"não vi os trabalhadores trabalhando"</i>.
 *
 * <p>O que estes testes travam é o <b>recuo com prazo</b>: a roça sai da
 * frente, e volta. Cancelá-la de vez trocaria um defeito por outro — a
 * vila cresceria para sempre sem lavoura nenhuma.
 */
class FarmPostponementTest {

    /** Vinte ciclos de colônia, o mesmo fôlego do PatienceClock. */
    private static final long POSTPONEMENT = 20 * 600;

    private final UUID colony = UUID.randomUUID();

    @BeforeEach
    void forgetWhatOtherTestsPostponed() {
        FarmPlans.clearAll();
    }

    /** Antes de tentar, nada está adiado: a roça tem a vez. */
    @Test
    void aColonyThatNeverTriedIsNotPostponed() {
        assertFalse(FarmPlans.postponed(colony, 0));
    }

    /** Recusado o lote, ela cede — e é isto que deixa a casa passar. */
    @Test
    void theFarmStepsAsideOnceTheLotWasOutOfReach() {
        FarmPlans.postpone(colony, 0);

        assertTrue(
                FarmPlans.postponed(colony, 0),
                "a roça não cedeu a vez, e o planejador volta a recusar o mesmo lote");
    }

    /** E segue cedendo enquanto o prazo corre. */
    @Test
    void itKeepsStandingAsideWhileTheClockRuns() {
        FarmPlans.postpone(colony, 0);

        assertTrue(FarmPlans.postponed(colony, POSTPONEMENT - 1));
    }

    /**
     * Mas ela volta, e este é o teste que impede a correção virar outro
     * defeito.
     *
     * <p>Recuo sem prazo é a roça cancelada para sempre: a vila cresceria
     * até não caber mais ninguém e nunca plantaria. A cota de um por
     * quinze aldeões continua de pé — o que mudou é só quando ela é
     * cobrada.
     */
    @Test
    void theFarmComesBackAfterTheDeadline() {
        FarmPlans.postpone(colony, 0);

        assertFalse(
                FarmPlans.postponed(colony, POSTPONEMENT),
                "a roça cedeu a vez para sempre, e a vila nunca mais planta");
    }

    /** O prazo é de cada colônia, e não do mod. */
    @Test
    void eachColonyStandsAsideOnItsOwn() {
        FarmPlans.postpone(colony, 0);

        assertFalse(
                FarmPlans.postponed(UUID.randomUUID(), 0),
                "a recusa de uma vila adiou a roça de outra");
    }

    /**
     * E tentar de novo reinicia o prazo.
     *
     * <p>A vila que continua sem lote ao alcance não deve pagar uma
     * passagem recusada a cada vinte ciclos para sempre: cada recusa nova
     * empurra a próxima tentativa para longe outra vez.
     */
    @Test
    void aSecondRefusalPushesTheRetryFurtherOut() {
        FarmPlans.postpone(colony, 0);
        FarmPlans.postpone(colony, POSTPONEMENT);

        assertTrue(FarmPlans.postponed(colony, POSTPONEMENT + 1));
    }
}
