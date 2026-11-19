package com.villagecolony.core.construction.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regra 318 (ADR-057): os três modos do grafo têm nome de comando e cor de aresta própria. */
class RoadGraphModeTest {

    @Test
    void theCommandNamesFindTheirModeIgnoringCase() {
        assertEquals(Optional.of(RoadGraphMode.ATUAL), RoadGraphMode.parse("atual"));
        assertEquals(Optional.of(RoadGraphMode.REGIOES), RoadGraphMode.parse(" Regioes "));
        assertEquals(Optional.of(RoadGraphMode.VIZINHANCA), RoadGraphMode.parse("VIZINHANCA"));
        assertEquals(Optional.empty(), RoadGraphMode.parse("trilha"));
        assertEquals(Optional.empty(), RoadGraphMode.parse(null));
    }

    @Test
    void everyModeHasItsOwnEdgeColorAndTheCurrentOneKeepsYellow() {
        assertEquals(RoadGraphMode.values().length,
                Arrays.stream(RoadGraphMode.values()).mapToInt(RoadGraphMode::edgeColor).distinct().count());
        assertEquals(0xFFFF00, RoadGraphMode.ATUAL.edgeColor(), "o modo de sempre segue amarelo no overlay");
    }
}
