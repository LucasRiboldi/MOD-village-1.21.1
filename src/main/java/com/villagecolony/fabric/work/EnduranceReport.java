package com.villagecolony.fabric.work;

import com.villagecolony.core.telemetry.model.ActivityTrace;
import com.villagecolony.core.telemetry.model.ActivityTraceEvent;

import java.util.List;
import java.util.Objects;

/**
 * O relatório de uma corrida de endurance — decisão 9B, 2026-09-24.
 *
 * <p><b>Reporta, não reagenda.</b> Esta classe nunca decide rodar de
 * novo, nunca escolhe outra seed sozinha, nunca muda o comportamento do
 * teste de endurance. Ela só empacota o que já aconteceu — seed,
 * duração, ciclos, contagem de tarefas, latência e um recorte do traço
 * de atividade — de um jeito que dá para reproduzir fora da máquina que
 * rodou primeiro.
 *
 * <p>Consome {@link ActivityTrace} (decisão 7B). A prova de que uma
 * corrida <b>degradou</b> continua sendo
 * {@code ColonyEnduranceGameTest.assertDidNotDrift}; este relatório
 * existe para o instante em que ela falhar, não no lugar dela.
 */
public record EnduranceReport(
        long seed,
        long durationTicks,
        long cycles,
        int taskCount,
        LatencySummary latency,
        List<ActivityTraceEvent> failureTrace) {

    /**
     * A seed padrão de toda corrida de endurance — decisão 9B.
     *
     * <p>Fixa, e não aleatória: uma corrida que falha precisa ser
     * reproduzível por quem lê o relatório depois, na própria máquina. A
     * Task 9B aceita uma seed alternativa via propriedade de sistema
     * nomeada, revisada — nunca escolhida sozinha pelo runner.
     */
    public static final long DEFAULT_SEED = 2026_09_24L;

    /** O nome da propriedade de sistema para uma seed alternativa revisada. */
    public static final String SEED_PROPERTY = "villagecolony.endurance.seed";

    /** Quantos eventos do traço um relatório de falha guarda. */
    private static final int FAILURE_TRACE_SIZE = 64;

    public EnduranceReport {
        Objects.requireNonNull(latency, "latency");
        failureTrace = List.copyOf(failureTrace);
    }

    /**
     * A seed desta corrida: {@link #DEFAULT_SEED}, ou a propriedade de
     * sistema {@value #SEED_PROPERTY} quando presente.
     */
    public static long seedForThisRun() {
        String override = System.getProperty(SEED_PROPERTY);

        if (override == null || override.isBlank()) {
            return DEFAULT_SEED;
        }

        return Long.parseLong(override.trim());
    }

    /**
     * O relatório de uma corrida que passou. Sem recorte de traço: uma
     * corrida sem falha não tem o que reproduzir.
     */
    public static EnduranceReport succeeded(
            long seed, long durationTicks, long cycles, int taskCount, LatencySummary latency) {

        return new EnduranceReport(
                seed, durationTicks, cycles, taskCount, latency, List.of());
    }

    /**
     * O relatório de uma corrida que falhou, com os
     * {@value #FAILURE_TRACE_SIZE} eventos mais recentes do traço para
     * reprodução.
     */
    public static EnduranceReport failed(
            long seed,
            long durationTicks,
            long cycles,
            int taskCount,
            LatencySummary latency,
            ActivityTrace trace) {

        Objects.requireNonNull(trace, "trace");

        return new EnduranceReport(
                seed, durationTicks, cycles, taskCount, latency,
                trace.newestFirst(FAILURE_TRACE_SIZE));
    }

    /** Se este relatório carrega contexto de falha. */
    public boolean isFailure() {
        return !failureTrace.isEmpty();
    }

    /**
     * A linha que vai para o log — tudo que é preciso para reproduzir a
     * corrida, numa frase só.
     */
    public String summary() {
        return "endurance seed=" + seed
                + " duration=" + durationTicks + "t"
                + " cycles=" + cycles
                + " tasks=" + taskCount
                + " latency={" + latency + "}"
                + (isFailure() ? " failureTraceEvents=" + failureTrace.size() : "");
    }
}
