package com.villagecolony.fabric.overlay;

import com.villagecolony.core.construction.model.RoadGraphMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regra 318 (ADR-057): a aresta sai com a cor do modo ligado; o trecho travado segue verde. */
class RoadMapRunsTest {

    @Test
    void theEdgeTakesTheColorOfTheModeAndTheFixedStepStaysGreen() {
        List<RoadMapPayload.Line> lines = new ArrayList<>();

        RoadMapSync.addRuns(List.of(1L, 2L, 3L, 4L), List.of(false, false, true, true), lines,
                RoadGraphMode.VIZINHANCA.edgeColor());

        assertEquals(List.of(RoadGraphMode.VIZINHANCA.edgeColor(), RoadMapSync.FIXED),
                lines.stream().map(RoadMapPayload.Line::color).toList());
    }

    @Test
    void withoutAModeTheEdgeStaysYellow() {
        List<RoadMapPayload.Line> lines = new ArrayList<>();

        RoadMapSync.addRuns(List.of(1L, 2L), List.of(false, false), lines);

        assertEquals(RoadMapSync.LINE, lines.getFirst().color());
    }
}
