package com.villagecolony.core.resource.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColonyResourcesTest {

    private static final ColonyPos CHEST_A = new ColonyPos(10, 64, 20);
    private static final ColonyPos CHEST_B = new ColonyPos(30, 64, 40);

    private static ResourceTally tally(ResourceType type, int amount) {
        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        counts.put(type, amount);

        return ResourceTally.of(counts);
    }

    /** Resource-System.md: 40 num baú, 24 noutro, total 64. */
    @Test
    void sumsTheChests() {
        Map<ColonyPos, ResourceTally> byChest = new LinkedHashMap<>();
        byChest.put(CHEST_A, tally(ResourceType.OAK_LOG, 40));
        byChest.put(CHEST_B, tally(ResourceType.OAK_LOG, 24));

        ColonyResources resources = ColonyResources.of(byChest);

        assertEquals(64, resources.amountOf(ResourceType.OAK_LOG));
        assertEquals(2, resources.byChest().size());
    }

    /** O total sozinho não diz para onde o trabalhador deve andar. */
    @Test
    void saysWhereEachPartIs() {
        Map<ColonyPos, ResourceTally> byChest = new LinkedHashMap<>();
        byChest.put(CHEST_A, tally(ResourceType.OAK_LOG, 40));
        byChest.put(CHEST_B, tally(ResourceType.OAK_LOG, 24));

        Map<ColonyPos, Integer> locations =
                ColonyResources.of(byChest).locationsOf(ResourceType.OAK_LOG);

        assertEquals(40, locations.get(CHEST_A));
        assertEquals(24, locations.get(CHEST_B));
        assertEquals(List.of(CHEST_A, CHEST_B), List.copyOf(locations.keySet()));
    }

    /** Um baú que não tem o recurso não é destino para ninguém. */
    @Test
    void aChestWithoutTheResourceIsNotALocation() {
        Map<ColonyPos, ResourceTally> byChest = new LinkedHashMap<>();
        byChest.put(CHEST_A, tally(ResourceType.OAK_LOG, 10));
        byChest.put(CHEST_B, tally(ResourceType.COBBLESTONE, 5));

        Map<ColonyPos, Integer> locations =
                ColonyResources.of(byChest).locationsOf(ResourceType.OAK_LOG);

        assertEquals(1, locations.size());
        assertTrue(locations.containsKey(CHEST_A));
    }

    /**
     * Baú vazio não entra: senão "a colônia tem três baús com madeira"
     * contaria baús sem madeira.
     */
    @Test
    void emptyChestsAreDropped() {
        Map<ColonyPos, ResourceTally> byChest = new LinkedHashMap<>();
        byChest.put(CHEST_A, tally(ResourceType.OAK_LOG, 10));
        byChest.put(CHEST_B, ResourceTally.empty());

        ColonyResources resources = ColonyResources.of(byChest);

        assertEquals(1, resources.byChest().size());
        assertEquals(10, resources.amountOf(ResourceType.OAK_LOG));
    }

    @Test
    void aColonyWithNoChestsHasNothing() {
        assertTrue(ColonyResources.empty().isEmpty());
        assertEquals(0, ColonyResources.empty().amountOf(ResourceType.OAK_LOG));
        assertTrue(ColonyResources.of(new LinkedHashMap<>()).isEmpty());
        assertTrue(ColonyResources.empty().locationsOf(ResourceType.OAK_LOG).isEmpty());
    }

    @Test
    void differentResourcesAddUpSeparately() {
        Map<ColonyPos, ResourceTally> byChest = new LinkedHashMap<>();
        byChest.put(CHEST_A, tally(ResourceType.OAK_LOG, 10));
        byChest.put(CHEST_B, tally(ResourceType.OAK_PLANKS, 8));

        ColonyResources resources = ColonyResources.of(byChest);

        assertEquals(10, resources.amountOf(ResourceType.OAK_LOG));
        assertEquals(8, resources.amountOf(ResourceType.OAK_PLANKS));
        assertEquals(0, resources.amountOf(ResourceType.COBBLESTONE));
    }

    @Test
    void isReadOnly() {
        Map<ColonyPos, ResourceTally> byChest = new LinkedHashMap<>();
        byChest.put(CHEST_A, tally(ResourceType.OAK_LOG, 10));

        ColonyResources resources = ColonyResources.of(byChest);

        assertThrows(UnsupportedOperationException.class,
                () -> resources.byChest().clear());

        assertThrows(UnsupportedOperationException.class,
                () -> resources.locationsOf(ResourceType.OAK_LOG).clear());
    }

    /** Alterar o mapa depois não pode mexer no que já foi agregado. */
    @Test
    void doesNotFollowTheSourceMap() {
        Map<ColonyPos, ResourceTally> byChest = new LinkedHashMap<>();
        byChest.put(CHEST_A, tally(ResourceType.OAK_LOG, 10));

        ColonyResources resources = ColonyResources.of(byChest);

        byChest.put(CHEST_B, tally(ResourceType.OAK_LOG, 999));

        assertEquals(10, resources.amountOf(ResourceType.OAK_LOG));
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> ColonyResources.of(null));

        Map<ColonyPos, ResourceTally> withNullTally = new LinkedHashMap<>();
        withNullTally.put(CHEST_A, null);

        assertThrows(NullPointerException.class, () -> ColonyResources.of(withNullTally));
    }
}
