package com.villagecolony.core.telemetry.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O buffer circular do traço de atividade — decisão 7B, 2026-09-24.
 */
class ActivityTraceTest {

    private static ActivityTraceEvent event(int progress) {
        return new ActivityTraceEvent(
                UUID.randomUUID(),
                ActivityProfession.MINER,
                ActivityKind.MINING,
                ActivityState.IDLE,
                ControlledReason.NONE,
                TargetKind.STONE,
                progress);
    }

    @Test
    void emptyTraceHasNothingAndNoOverflow() {
        ActivityTrace trace = new ActivityTrace();

        assertTrue(trace.newestFirst(10).isEmpty());
        assertEquals(0, trace.overflowCount());
    }

    @Test
    void appendedEventComesBackNewestFirst() {
        ActivityTrace trace = new ActivityTrace();

        trace.append(event(1));
        trace.append(event(2));
        trace.append(event(3));

        List<ActivityTraceEvent> newest = trace.newestFirst(3);

        assertEquals(3, newest.get(0).progress());
        assertEquals(2, newest.get(1).progress());
        assertEquals(1, newest.get(2).progress());
    }

    @Test
    void newestFirstNeverReturnsMoreThanAsked() {
        ActivityTrace trace = new ActivityTrace();

        IntStream.range(0, 10).forEach(index -> trace.append(event(index)));

        assertEquals(3, trace.newestFirst(3).size());
    }

    @Test
    void traceKeepsNewest16384AndCountsOverflow() {
        ActivityTrace trace = new ActivityTrace();

        IntStream.range(0, 16_385).forEach(index -> trace.append(event(index)));

        assertEquals(16_384, trace.newestFirst(16_384).size());
        assertEquals(1, trace.overflowCount());
        // O evento mais antigo (progress=0) foi expulso; o mais novo é
        // 16384, o último inserido.
        assertEquals(16_384, trace.newestFirst(1).get(0).progress());
    }

    @Test
    void overflowKeepsAccumulatingPastTheFirstOne() {
        ActivityTrace trace = new ActivityTrace();

        IntStream.range(0, 16_384 + 50).forEach(index -> trace.append(event(index)));

        assertEquals(50, trace.overflowCount());
    }

    @Test
    void aFreshTraceNeverOverflows() {
        ActivityTrace trace = new ActivityTrace();

        IntStream.range(0, 16_384).forEach(index -> trace.append(event(index)));

        assertEquals(0, trace.overflowCount());
    }

    private static ActivityTraceEvent of(ActivityProfession profession, ActivityState state) {
        return new ActivityTraceEvent(UUID.randomUUID(), profession, ActivityKind.MINING, state,
                ControlledReason.WORK_STALLED, TargetKind.CROP, 0);
    }

    /**
     * "[TRAVADO]" é a última linha da profissão, não uma linha qualquer — o
     * /vc log de 2026-10-02 mostrava o fazendeiro travado colhendo.
     */
    @Test
    void endsStoppedReadsOnlyTheNewestLineOfThatProfession() {
        ActivityTrace trace = new ActivityTrace();

        assertFalse(trace.endsStopped(ActivityProfession.FARMER), "sem linha nenhuma não há interrupção");

        trace.append(of(ActivityProfession.FARMER, ActivityState.ABANDONED));
        trace.append(of(ActivityProfession.MINER, ActivityState.RECOVERED));

        assertTrue(trace.endsStopped(ActivityProfession.FARMER), "a desistência é a última do fazendeiro");
        assertFalse(trace.endsStopped(ActivityProfession.MINER));

        trace.append(of(ActivityProfession.FARMER, ActivityState.RECOVERED));

        assertFalse(trace.endsStopped(ActivityProfession.FARMER), "a volta ao trabalho encerra a interrupção");
    }
}
