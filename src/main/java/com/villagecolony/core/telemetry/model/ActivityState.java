package com.villagecolony.core.telemetry.model;

/**
 * O desfecho de um evento de traço — decisão 7B, 2026-09-24.
 *
 * <p>Espelha os quatro resultados que {@code fabric.work.ActivityLog} já
 * grava: {@code waiting}, {@code recovered}, {@code abandoned} e
 * {@code failed}. {@code IDLE} cobre a passagem de ciclo comum, sem
 * transição alguma — a Task 7 pede que <b>todo</b> tique simulado vire
 * evento, e não só os quatro que já eram logados; sem um estado para "só
 * continuou", o traço perderia justamente os ciclos em que nada mudou,
 * que é o que prova ausência de progresso num diagnóstico de endurance.
 */
public enum ActivityState {
    /** Passou o ciclo sem transição — nem entrou em espera, nem saiu dela. */
    IDLE,

    /** Entrou em espera controlada. */
    WAITING,

    /** Uma espera anteriormente observada voltou a progredir. */
    RECOVERED,

    /** O guarda de travamento abandonou a tarefa. */
    ABANDONED,

    /** Uma falha operacional levou ao abandono controlado da tarefa. */
    ERROR,

    UNKNOWN
}
