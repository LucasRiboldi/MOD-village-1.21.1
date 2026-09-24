package com.villagecolony.fabric.work;

import java.util.Objects;

/**
 * O resumo de latência de uma corrida de endurance — decisão 9B,
 * 2026-09-24.
 *
 * <p>Três números, e não a série inteira: mínimo, máximo e média em
 * milissegundos. É o mesmo movimento de {@code CycleCost} — medir antes
 * de otimizar —, mas aqui a pergunta não é "onde" o ciclo gastou tempo,
 * é "quão estável foi o custo ao longo de duzentos ciclos". Uma média
 * que sobe sozinha não diz nada sobre deriva; um máximo muito acima da
 * média diz que algum ciclo específico pagou caro.
 */
public final class LatencySummary {

    private final long minMillis;

    private final long maxMillis;

    private final double meanMillis;

    private LatencySummary(long minMillis, long maxMillis, double meanMillis) {
        this.minMillis = minMillis;
        this.maxMillis = maxMillis;
        this.meanMillis = meanMillis;
    }

    /**
     * Resume as amostras, em milissegundos.
     *
     * <p>Uma corrida sem amostra nenhuma devolve tudo zero — não é erro,
     * é um resumo de nada, e "nada" é uma resposta válida para quem
     * chama antes do primeiro ciclo terminar.
     */
    public static LatencySummary of(long[] samplesMillis) {
        Objects.requireNonNull(samplesMillis, "samplesMillis");

        if (samplesMillis.length == 0) {
            return new LatencySummary(0L, 0L, 0.0);
        }

        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long sum = 0L;

        for (long sample : samplesMillis) {
            min = Math.min(min, sample);
            max = Math.max(max, sample);
            sum += sample;
        }

        return new LatencySummary(min, max, (double) sum / samplesMillis.length);
    }

    public long minMillis() {
        return minMillis;
    }

    public long maxMillis() {
        return maxMillis;
    }

    public double meanMillis() {
        return meanMillis;
    }

    @Override
    public String toString() {
        return "min " + minMillis + " ms, mean " + String.format("%.1f", meanMillis)
                + " ms, max " + maxMillis + " ms";
    }
}
