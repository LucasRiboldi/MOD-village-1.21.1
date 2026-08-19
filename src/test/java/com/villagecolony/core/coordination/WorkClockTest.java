package com.villagecolony.core.coordination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A janela de trabalho da colônia — a Regra 18.
 *
 * <p>Estes casos moram aqui, e não na bateria de jogo, por um motivo
 * concreto: a hora do mundo é <b>global</b> e a bateria roda testes
 * concorrentes. Um teste de jogo que virasse a noite para afirmar "à
 * noite ninguém trabalha" pararia o lenhador do teste vizinho no meio
 * do trabalho dele. Foi o que aconteceu em 2026-08-19, na primeira
 * versão desta regra.
 */
@DisplayName("WorkClock")
class WorkClockTest {

    @Test
    @DisplayName("o dia claro inteiro é expediente, e não só a fatia do Vanilla")
    void theWholeDayIsWorkTime() {
        assertTrue(WorkClock.isWorkTime(0), "o amanhecer é expediente");
        assertTrue(WorkClock.isWorkTime(1_000), "a manhã cedo é expediente");
        assertTrue(WorkClock.isWorkTime(2_000), "o WORK do Vanilla é expediente");

        // O coração da regra: às 10.000 o Vanilla já mandou o aldeão
        // para MEET, e era aqui que a colônia parava com o sol alto.
        assertTrue(WorkClock.isWorkTime(10_000), "a tarde é expediente");
    }

    @Test
    @DisplayName("a última hora de sol fica para voltar para casa")
    void theLastHourOfLightIsNotWorkTime() {
        assertTrue(WorkClock.isWorkTime(WorkClock.DUSK - 1), "um tique antes ainda é expediente");
        assertFalse(WorkClock.isWorkTime(WorkClock.DUSK), "o expediente acaba no anoitecer");
    }

    @Test
    @DisplayName("a noite não é expediente")
    void theNightIsNotWorkTime() {
        assertFalse(WorkClock.isWorkTime(12_000), "o pôr do sol não é expediente");
        assertFalse(WorkClock.isWorkTime(14_000), "a noite fechada não é expediente");
        assertFalse(WorkClock.isWorkTime(23_999), "a madrugada não é expediente");
    }

    @Test
    @DisplayName("o relógio acumulado de dias inteiros continua valendo")
    void theClockKeepsWorkingAfterManyDays() {
        long tenDays = 10L * WorkClock.DAY;

        assertTrue(WorkClock.isWorkTime(tenDays + 1_000), "décimo dia de manhã");
        assertFalse(WorkClock.isWorkTime(tenDays + 14_000), "décima noite");
    }
}
