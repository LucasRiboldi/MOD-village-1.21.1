package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A vila trabalha com o jogador num perímetro em volta dela — autor, 2026-10-12. */
class VillageFocusTest {

    @Test
    void aPlayerAroundTheVillageKeepsItWorking() {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(50, 64, 50));
        colony.measure(new VillageBounds(0, 60, 0, 99, 70, 99));

        int edge = colony.bounds().orElseThrow().maxX();

        assertTrue(VillageFocus.isInside(colony, 50, 50), "dentro da caixa");
        assertTrue(VillageFocus.isInside(colony, edge + VillageFocus.PERIMETER, 50), "no perímetro em volta");
        assertFalse(VillageFocus.isInside(colony, edge + VillageFocus.PERIMETER + 1, 50), "além do perímetro");
    }
}
