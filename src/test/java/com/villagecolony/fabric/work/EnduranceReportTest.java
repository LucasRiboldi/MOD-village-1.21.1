package com.villagecolony.fabric.work;

import com.villagecolony.core.telemetry.model.ActivityKind;
import com.villagecolony.core.telemetry.model.ActivityProfession;
import com.villagecolony.core.telemetry.model.ActivityState;
import com.villagecolony.core.telemetry.model.ActivityTrace;
import com.villagecolony.core.telemetry.model.ActivityTraceEvent;
import com.villagecolony.core.telemetry.model.ControlledReason;
import com.villagecolony.core.telemetry.model.TargetKind;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Um relatório reproduzível de falha de endurance — decisão 9B,
 * 2026-09-24.
 *
 * <p><b>Reporta, não reagenda.</b> Esta classe nunca decide rodar de
 * novo nem muda o comportamento do teste de endurance — ela só empacota
 * o que já aconteceu de um jeito que dá para reproduzir fora da máquina
 * que rodou primeiro.
 */
class EnduranceReportTest {

    private static ActivityTraceEvent event(int progress) {
        return new ActivityTraceEvent(
                UUID.randomUUID(),
                ActivityProfession.LUMBERJACK,
                ActivityKind.HARVESTING,
                ActivityState.WAITING,
                ControlledReason.NO_TARGET,
                TargetKind.WOOD,
                progress);
    }

    @Test
    void enduranceFailureIncludesSeedAndTraceExcerpt() {
        ActivityTrace trace = new ActivityTrace();
        IntStream.range(0, 100).forEach(index -> trace.append(event(index)));

        EnduranceReport report = EnduranceReport.failed(
                17L, 200_000L, 200L, 4_312, LatencySummary.of(new long[] {3, 5, 4}), trace);

        assertEquals(17L, report.seed());
        assertFalse(report.failureTrace().isEmpty());
    }

    @Test
    void failureTraceHasAtMost64Events() {
        ActivityTrace trace = new ActivityTrace();
        IntStream.range(0, 500).forEach(index -> trace.append(event(index)));

        EnduranceReport report = EnduranceReport.failed(
                1L, 1L, 1L, 1, LatencySummary.of(new long[] {1}), trace);

        assertEquals(64, report.failureTrace().size());
    }

    @Test
    void failureTraceIsNewestFirst() {
        ActivityTrace trace = new ActivityTrace();
        trace.append(event(1));
        trace.append(event(2));
        trace.append(event(3));

        EnduranceReport report = EnduranceReport.failed(
                1L, 1L, 1L, 1, LatencySummary.of(new long[] {1}), trace);

        assertEquals(3, report.failureTrace().get(0).progress());
    }

    @Test
    void reportRetainsDurationCyclesAndTaskCount() {
        EnduranceReport report = EnduranceReport.failed(
                42L, 120_000L, 200L, 9_001, LatencySummary.of(new long[] {2}), new ActivityTrace());

        assertEquals(120_000L, report.durationTicks());
        assertEquals(200L, report.cycles());
        assertEquals(9_001, report.taskCount());
    }

    @Test
    void succeededReportHasNoFailureContext() {
        EnduranceReport report = EnduranceReport.succeeded(
                EnduranceReport.DEFAULT_SEED, 120_000L, 200L, 500,
                LatencySummary.of(new long[] {5, 6, 7}));

        assertTrue(report.failureTrace().isEmpty());
    }

    @Test
    void defaultSeedIsFixedAndDocumented() {
        assertEquals(EnduranceReport.DEFAULT_SEED, EnduranceReport.DEFAULT_SEED);
    }

    @Test
    void latencySummaryReportsMinMeanMax() {
        LatencySummary summary = LatencySummary.of(new long[] {2, 4, 6});

        assertEquals(2L, summary.minMillis());
        assertEquals(6L, summary.maxMillis());
        assertEquals(4.0, summary.meanMillis());
    }

    @Test
    void latencySummaryOfNoSamplesIsAllZero() {
        LatencySummary summary = LatencySummary.of(new long[0]);

        assertEquals(0L, summary.minMillis());
        assertEquals(0L, summary.maxMillis());
        assertEquals(0.0, summary.meanMillis());
    }
}
