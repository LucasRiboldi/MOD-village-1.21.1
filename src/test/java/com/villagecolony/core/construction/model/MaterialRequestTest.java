package com.villagecolony.core.construction.model;

import com.villagecolony.core.construction.model.MaterialRequest.Source;
import com.villagecolony.core.construction.model.MaterialRequest.State;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O pedido de material de uma obra — ADR-035 §3, fase 1: diz o que a obra
 * espera, de onde a colônia tenta tirar e desde quando.
 */
class MaterialRequestTest {

    private static final ResourceId DOOR = new ResourceId("minecraft", "oak_door");

    private static final ResourceId GLASS = new ResourceId("minecraft", "glass_pane");

    @Test
    void onlyADeliveredRequestLetsTheWorkGoOn() {
        assertTrue(MaterialRequest.start(DOOR, State.DELIVERED, Source.CHEST, 0).delivered());
        assertFalse(MaterialRequest.start(DOOR, State.RESOLVING, Source.PROFESSION, 0).delivered());
        assertFalse(MaterialRequest.start(DOOR, State.NO_SOLUTION, Source.BARRIER, 0).delivered());
    }

    @Test
    void theSameSituationKeepsCountingFromTheFirstTick() {
        MaterialRequest first = MaterialRequest.start(DOOR, State.RESOLVING, Source.CRAFTSMAN, 100);

        MaterialRequest again = first.updatedTo(DOOR, State.RESOLVING, Source.CRAFTSMAN, 700);

        assertSame(first, again);
        assertEquals(600, again.waitingTicks(700));
    }

    @Test
    void aNewPieceStateOrSourceRestartsTheClock() {
        MaterialRequest first = MaterialRequest.start(DOOR, State.RESOLVING, Source.PROFESSION, 100);

        assertEquals(700, first.updatedTo(GLASS, State.RESOLVING, Source.PROFESSION, 700).sinceTick());
        assertEquals(700, first.updatedTo(DOOR, State.DELIVERED, Source.STOCKED, 700).sinceTick());
        assertEquals(700, first.updatedTo(DOOR, State.RESOLVING, Source.CRAFTSMAN, 700).sinceTick());
    }

    @Test
    void everySituationTheBuilderReachesHasItsOwnReason() {
        Set<String> reasons = new HashSet<>();

        for (MaterialRequest request : new MaterialRequest[] {
                MaterialRequest.start(DOOR, State.DELIVERED, Source.CHEST, 0),
                MaterialRequest.start(DOOR, State.DELIVERED, Source.STOCKED, 0),
                MaterialRequest.start(DOOR, State.RESOLVING, Source.PROFESSION, 0),
                MaterialRequest.start(DOOR, State.RESOLVING, Source.CRAFTSMAN, 0),
                MaterialRequest.start(DOOR, State.RESOLVING, Source.STOCKED, 0),
                MaterialRequest.start(DOOR, State.NO_SOLUTION, Source.STOCKED, 0),
                MaterialRequest.start(DOOR, State.NO_SOLUTION, Source.BARRIER, 0),
                MaterialRequest.start(DOOR, State.NO_SOLUTION, Source.NONE, 0)}) {

            assertNotEquals("", request.reason());
            assertTrue(reasons.add(request.reason()), "motivo repetido: " + request.reason());
        }
    }
}
