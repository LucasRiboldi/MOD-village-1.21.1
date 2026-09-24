package com.villagecolony.fabric.integration;

import java.util.function.Supplier;

/**
 * Um prazo de relógio para a varredura de lote — 2026-09-24.
 *
 * <p>A varredura sempre teve teto de <b>colunas</b> por passagem (1.024) e
 * cursor para retomar de onde parou. Faltava o teto de <b>tempo</b>: o custo
 * de uma coluna varia com o terreno, e no playtest de 24-09 o planejador
 * passou de 500 ms num tique. Com um prazo, a varredura para quando o tempo
 * acaba, guarda o cursor e continua no ciclo seguinte — a mesma retomada que
 * já existia, disparada também pelo relógio. Ver
 * {@code PlanningBudget.DEADLINE_MS}.
 *
 * <p><b>Sem prazo por padrão.</b> Só o ciclo de jogo arma o prazo; quem chama
 * a varredura direto — o teste de jogo, que precisa de resposta
 * determinística — continua varrendo pelo teto de colunas, como antes.
 *
 * <p>Uma thread só: o ciclo roda no tique do servidor.
 */
public final class SweepDeadline {

    /**
     * Colunas que toda passagem olha antes de consultar o relógio.
     *
     * <p>Sem este piso, um prazo já vencido na entrada faria a varredura
     * guardar o cursor sem olhar nada, para sempre.
     */
    public static final int MIN_COLUMNS = 64;

    private static long deadline = Long.MAX_VALUE;

    private SweepDeadline() {
    }

    /** Roda {@code work} com prazo de {@code millis}, e desarma no fim. */
    public static <T> T within(long millis, Supplier<T> work) {
        long previous = deadline;
        deadline = System.nanoTime() + millis * 1_000_000L;

        try {
            return work.get();
        } finally {
            deadline = previous;
        }
    }

    /** Se a passagem deve parar aqui: o piso foi cumprido e o prazo venceu. */
    public static boolean expired(int columnsLooked) {
        return columnsLooked > MIN_COLUMNS && deadline != Long.MAX_VALUE && System.nanoTime() > deadline;
    }
}
