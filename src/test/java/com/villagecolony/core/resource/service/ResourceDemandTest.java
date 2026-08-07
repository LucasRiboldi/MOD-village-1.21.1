package com.villagecolony.core.resource.service;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceDemandTest {

    private static Map<ResourceType, Integer> goal(ResourceType type, int amount) {
        Map<ResourceType, Integer> goal = new EnumMap<>(ResourceType.class);
        goal.put(type, amount);

        return goal;
    }

    private static ResourceTally owned(ResourceType type, int amount) {
        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        counts.put(type, amount);

        return ResourceTally.of(counts);
    }

    /** Resource-System.md: precisa 64, tem 20, déficit 44. */
    @Test
    void theDocumentedExample() {
        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                goal(ResourceType.OAK_PLANKS, 64),
                owned(ResourceType.OAK_PLANKS, 20));

        assertEquals(44, missing.get(ResourceType.OAK_PLANKS));
    }

    /** Nada no baú: falta a meta inteira. */
    @Test
    void owningNothingLacksEverything() {
        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                goal(ResourceType.OAK_LOG, 32), ResourceTally.empty());

        assertEquals(32, missing.get(ResourceType.OAK_LOG));
    }

    /**
     * Recurso em dia não entra com zero: um mapa que lista o que não
     * falta obriga todo chamador a filtrar, e quem esquecer vai mandar
     * buscar nada.
     */
    @Test
    void whatIsNotMissingIsAbsent() {
        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                goal(ResourceType.OAK_LOG, 32),
                owned(ResourceType.OAK_LOG, 32));

        assertTrue(missing.isEmpty());
    }

    /** Sobra é déficit zero, não negativo. */
    @Test
    void surplusIsNotANegativeDeficit() {
        assertTrue(ResourceDemand.deficit(
                goal(ResourceType.OAK_PLANKS, 64),
                owned(ResourceType.OAK_PLANKS, 100)).isEmpty());

        assertEquals(0, ResourceDemand.deficitOf(
                ResourceType.OAK_PLANKS, 64, owned(ResourceType.OAK_PLANKS, 100)));
    }

    @Test
    void onlyTheMissingOnesAreListed() {
        Map<ResourceType, Integer> goal = new EnumMap<>(ResourceType.class);
        goal.put(ResourceType.OAK_LOG, 32);
        goal.put(ResourceType.OAK_PLANKS, 64);
        goal.put(ResourceType.COBBLESTONE, 16);

        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        counts.put(ResourceType.OAK_LOG, 40);
        counts.put(ResourceType.OAK_PLANKS, 20);

        Map<ResourceType, Integer> missing =
                ResourceDemand.deficit(goal, ResourceTally.of(counts));

        assertEquals(2, missing.size());
        assertEquals(44, missing.get(ResourceType.OAK_PLANKS));
        assertEquals(16, missing.get(ResourceType.COBBLESTONE));
        assertFalse(missing.containsKey(ResourceType.OAK_LOG));
    }

    /** Recurso fora da meta não é déficit, mesmo com estoque zero. */
    @Test
    void whatWasNotAskedForIsNotMissing() {
        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                goal(ResourceType.OAK_LOG, 10), ResourceTally.empty());

        assertFalse(missing.containsKey(ResourceType.COBBLESTONE));
    }

    @Test
    void aColonyThatHasEverythingIsSatisfied() {
        assertTrue(ResourceDemand.isSatisfied(
                goal(ResourceType.OAK_LOG, 10), owned(ResourceType.OAK_LOG, 10)));

        assertFalse(ResourceDemand.isSatisfied(
                goal(ResourceType.OAK_LOG, 10), owned(ResourceType.OAK_LOG, 9)));

        assertTrue(ResourceDemand.isSatisfied(
                new EnumMap<>(ResourceType.class), ResourceTally.empty()));
    }

    @Test
    void deficitOfASingleResource() {
        assertEquals(44, ResourceDemand.deficitOf(
                ResourceType.OAK_PLANKS, 64, owned(ResourceType.OAK_PLANKS, 20)));

        assertEquals(0, ResourceDemand.deficitOf(
                ResourceType.OAK_PLANKS, 0, ResourceTally.empty()));
    }

    @Test
    void theResultIsReadOnly() {
        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                goal(ResourceType.OAK_LOG, 10), ResourceTally.empty());

        assertThrows(UnsupportedOperationException.class, () -> missing.clear());
    }

    /** Meta negativa é erro de quem chamou, não estoque a mais. */
    @Test
    void rejectsNegativeGoals() {
        assertThrows(IllegalArgumentException.class, () -> ResourceDemand.deficit(
                goal(ResourceType.OAK_LOG, -1), ResourceTally.empty()));

        assertThrows(IllegalArgumentException.class, () -> ResourceDemand.deficitOf(
                ResourceType.OAK_LOG, -1, ResourceTally.empty()));
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class,
                () -> ResourceDemand.deficit(null, ResourceTally.empty()));

        assertThrows(NullPointerException.class,
                () -> ResourceDemand.deficit(goal(ResourceType.OAK_LOG, 1), null));

        Map<ResourceType, Integer> withNullKey = new HashMap<>();
        withNullKey.put(null, 1);

        assertThrows(NullPointerException.class,
                () -> ResourceDemand.deficit(withNullKey, ResourceTally.empty()));
    }
}
