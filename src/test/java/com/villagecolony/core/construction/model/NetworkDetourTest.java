package com.villagecolony.core.construction.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O atalho ao chafariz — decisão D5: só quando a rede passa de duas vezes o direto. */
class NetworkDetourTest {

    /** Uma rua em L: de (0, 0) para leste até (20, 0), e para o norte até o chafariz em (20, -20). */
    private static Set<Long> elbow() {
        Set<Long> roads = new HashSet<>();

        for (int x = 0; x <= 20; x++) {
            roads.add(ColonyRoads.column(x, 0));
        }

        for (int z = 0; z >= -20; z--) {
            roads.add(ColonyRoads.column(20, z));
        }

        return roads;
    }

    @Test
    void theStepsAlongTheNetworkAreCounted() {
        assertEquals(OptionalInt.of(37), NetworkDetour.stepsToHub(elbow(), 0, 0, 20, -20),
                "20 a leste e 17 ao norte, até a três do chafariz");
        assertTrue(NetworkDetour.stepsToHub(elbow(), 5, 5, 20, -20).isEmpty(), "fora da rede não há passos");
    }

    @Test
    void aNetworkThatDoesNotReachTheHubAsksForAShortcut() {
        Set<Long> island = Set.of(ColonyRoads.column(0, 0), ColonyRoads.column(1, 0));

        assertTrue(NetworkDetour.stepsToHub(island, 0, 0, 40, 40).isEmpty());
        assertTrue(NetworkDetour.needsShortcut(2, OptionalInt.empty(), 50));
    }

    @Test
    void theShortcutComesOnlyPastTwiceTheDirectDistance() {
        assertFalse(NetworkDetour.needsShortcut(5, OptionalInt.of(15), 10), "20 contra 10 é o dobro, não passa");
        assertTrue(NetworkDetour.needsShortcut(5, OptionalInt.of(16), 10), "21 contra 10 passa do dobro");
    }
}
