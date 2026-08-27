package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A soma que separa "a varredura reinicia" de "ninguém a chamou".
 *
 * <p>A sessão de 2026-08-27, 19:11, deixou a pergunta em aberto: 26
 * ciclos de colônia, dezessete passagens bastariam para varrer as 16.641
 * colunas, e a varredura não terminou nenhuma volta. Duas causas
 * possíveis, e o log não as distinguia — um ciclo em que o planejador
 * nem chega ao {@code BuildSiteScanner} não escreve linha nenhuma, e o
 * silêncio do {@code IdleLog} fica igual nos dois casos.
 *
 * <p>A soma tem os dois números que fecham a conta: quantas vezes o
 * planejador <b>perguntou</b> e quantas passagens de varredura de fato
 * <b>correram</b>. A diferença entre elas é o que nunca chegou lá.
 */
class SweepLogTest {

    private static final ColonyPos CENTER = new ColonyPos(772, 68, 898);

    private final UUID colonyId = UUID.randomUUID();

    @BeforeEach
    void forgetTheLastSession() {
        SweepLog.clearAll();
    }

    /** Colônia que nunca varreu não aparece na soma. */
    @Test
    void aColonyThatNeverSweptHasNoTally() {
        assertTrue(SweepLog.tallyOf(colonyId).isEmpty());
    }

    /** Cada passagem soma uma, e as colunas dela entram no total. */
    @Test
    void everyPassAddsItsColumns() {
        SweepLog.pass(colonyId, 1024);
        SweepLog.pass(colonyId, 1024);
        SweepLog.pass(colonyId, 617);

        SweepLog.Tally tally = SweepLog.tallyOf(colonyId).orElseThrow();

        assertEquals(3, tally.passes());
        assertEquals(2665, tally.columns());
    }

    /**
     * O reinício por deriva conta duas vezes, e guarda a maior distância.
     *
     * <p>Duas vezes de propósito: ele <b>é</b> um reinício, e a deriva é
     * a causa que se quer separar. Um número sem o outro não responde
     * nada.
     */
    @Test
    void aDriftRestartCountsAsBothAndKeepsTheFarthest() {
        SweepLog.drifted(colonyId, CENTER, new ColonyPos(772, 68, 929));
        SweepLog.restarted(colonyId);

        SweepLog.drifted(colonyId, CENTER, new ColonyPos(796, 68, 898));
        SweepLog.restarted(colonyId);

        SweepLog.Tally tally = SweepLog.tallyOf(colonyId).orElseThrow();

        assertEquals(2, tally.restarts());
        assertEquals(2, tally.drifts());

        // 31 e 24 blocos: fica o maior.
        assertEquals(31, tally.farthestDrift());
    }

    /**
     * Recomeçar do centro sem deriva conta só como reinício.
     *
     * <p>É o caso normal: a volta anterior terminou o raio, e a seguinte
     * começa do centro porque a vila muda. Confundi-lo com deriva
     * apontaria para o culpado errado.
     */
    @Test
    void aRestartWithoutDriftIsNotADrift() {
        SweepLog.restarted(colonyId);

        SweepLog.Tally tally = SweepLog.tallyOf(colonyId).orElseThrow();

        assertEquals(1, tally.restarts());
        assertEquals(0, tally.drifts());
        assertEquals(0, tally.farthestDrift());
    }

    /** A volta completa é o que separa progresso de rodar em falso. */
    @Test
    void completeRoundsAreCounted() {
        SweepLog.pass(colonyId, 1024);
        SweepLog.completed(colonyId);

        assertEquals(1, SweepLog.tallyOf(colonyId).orElseThrow().rounds());
    }

    /** A resposta que o índice deu não é passagem de varredura. */
    @Test
    void anIndexedAnswerIsNotASweepPass() {
        SweepLog.indexed(colonyId);
        SweepLog.indexed(colonyId);

        SweepLog.Tally tally = SweepLog.tallyOf(colonyId).orElseThrow();

        assertEquals(2, tally.indexed());
        assertEquals(0, tally.passes());
    }

    /**
     * O que o planejador perguntou e nunca chegou à varredura.
     *
     * <p>É a metade que faltava no log de 19:11. Vinte e seis perguntas
     * e oito passagens querem dizer que dezoito ciclos morreram antes do
     * {@code BuildSiteScanner} — e aí o culpado não é a varredura.
     */
    @Test
    void whatNeverReachedTheSweepIsCounted() {
        for (int i = 0; i < 26; i++) {
            SweepLog.asked(colonyId);
        }

        for (int i = 0; i < 8; i++) {
            SweepLog.pass(colonyId, 1024);
        }

        SweepLog.indexed(colonyId);

        SweepLog.Tally tally = SweepLog.tallyOf(colonyId).orElseThrow();

        assertEquals(26, tally.asked());
        assertEquals(17, tally.bailed());
        assertTrue(tally.gaveUpBeforeSweeping());
    }

    /**
     * Varredura movida de fora do planejador não vira dívida.
     *
     * <p>Os testes de jogo chamam o {@code BuildSiteScanner} direto, e
     * ali há passagem sem pergunta: a diferença fica negativa por
     * construção. O relatório precisa calar nesse caso em vez de dizer
     * "−1 nunca chegou à varredura", que foi o que ele disse na primeira
     * bateria depois deste instrumento entrar.
     */
    @Test
    void aSweepDrivenFromOutsideThePlannerClaimsNothing() {
        SweepLog.pass(colonyId, 49);
        SweepLog.completed(colonyId);

        SweepLog.Tally tally = SweepLog.tallyOf(colonyId).orElseThrow();

        assertEquals(0, tally.asked());
        assertFalse(tally.gaveUpBeforeSweeping());
    }

    /**
     * A pergunta da sessão de 19:11, como afirmação.
     *
     * <p>Vinte e seis passagens, nenhuma volta completa e um reinício só
     * quer dizer que a varredura <b>não</b> reinicia — o problema está em
     * outro lugar. Vinte e seis passagens com vinte e seis reinícios quer
     * dizer o contrário. A soma distingue.
     */
    @Test
    void theTallyTellsRestartingApartFromSlowGoing() {
        for (int i = 0; i < 26; i++) {
            SweepLog.asked(colonyId);
            SweepLog.pass(colonyId, 1024);
        }

        SweepLog.restarted(colonyId);

        assertFalse(SweepLog.tallyOf(colonyId).orElseThrow().restarting());

        for (int i = 0; i < 25; i++) {
            SweepLog.drifted(colonyId, CENTER, new ColonyPos(772, 68, 929));
            SweepLog.restarted(colonyId);
        }

        assertTrue(SweepLog.tallyOf(colonyId).orElseThrow().restarting());
    }

    /**
     * Uma volta completa tira a suspeita.
     *
     * <p>Reiniciar depois de terminar o raio é o que a varredura deve
     * fazer — a vila muda, e o lote de ontem pode existir amanhã.
     */
    @Test
    void aCompleteRoundClearsTheSuspicion() {
        for (int i = 0; i < 4; i++) {
            SweepLog.restarted(colonyId);
        }

        assertTrue(SweepLog.tallyOf(colonyId).orElseThrow().restarting());

        SweepLog.completed(colonyId);

        assertFalse(SweepLog.tallyOf(colonyId).orElseThrow().restarting());
    }

    /**
     * Uma colônia que mal começou não é uma colônia travada.
     *
     * <p>A soma continua contando — é a acusação que espera evidência.
     * Ver {@code RUNS_BEFORE_JUDGING}: sem o piso, a bateria de testes
     * de jogo abriu um aviso por vila de teste.
     */
    @Test
    void oneCycleIsNotEnoughToAccuseAnyone() {
        SweepLog.asked(colonyId);

        SweepLog.Tally tally = SweepLog.tallyOf(colonyId).orElseThrow();

        assertEquals(1, tally.bailed());
        assertTrue(tally.gaveUpBeforeSweeping());
        assertFalse(tally.restarting());
    }

    /** Ao parar o servidor a soma sai, para a sessão seguinte nascer limpa. */
    @Test
    void theTallyIsForgottenAtShutdown() {
        SweepLog.pass(colonyId, 1024);

        SweepLog.clearAll();

        assertTrue(SweepLog.tallyOf(colonyId).isEmpty());
    }
}
