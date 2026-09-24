package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A placa do lote flutua cinco blocos acima do topo da planta — N7, 2026-09-24. */
class SiteMarkerHeightTest {

    @Test
    void theLabelFloatsFiveBlocksAboveTheTopOfTheBlueprint() {
        ColonyPos origin = new ColonyPos(100, 64, -20);

        // A desert_small_house_6, a mais alta do catálogo, tem 18 de altura.
        assertEquals(64 + 18 + 5, SiteMarker.labelY(origin, new ColonyPos(9, 18, 9)));
        assertEquals(64 + 5 + 5, SiteMarker.labelY(origin, new ColonyPos(7, 5, 7)));
    }
}
