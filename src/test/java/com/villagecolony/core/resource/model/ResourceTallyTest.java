package com.villagecolony.core.resource.model;

import com.villagecolony.core.type.ResourceCategory;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceTallyTest {

    private static ResourceTally tally(ResourceType type, int amount) {
        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        counts.put(type, amount);

        return ResourceTally.of(counts);
    }

    @Test
    void countsWhatItWasGiven() {
        ResourceTally result = tally(ResourceType.OAK_LOG, 64);

        assertEquals(64, result.amountOf(ResourceType.OAK_LOG));
        assertTrue(result.has(ResourceType.OAK_LOG));
    }

    @Test
    void countsIdsOutsideTheClosedResourceCatalog() {
        ResourceId dirt = ResourceId.vanilla("dirt");
        ResourceTally result = ResourceTally.ofIds(Map.of(dirt, 12));

        assertEquals(12, result.amountOf(dirt));
        assertEquals(Map.of(dirt, 12), result.idCounts());
        assertTrue(result.counts().isEmpty());
    }

    @Test
    void combinesGenericIdsWithAnEmptyTypedView() {
        ResourceId dirt = ResourceId.vanilla("dirt");
        ResourceTally result = ResourceTally.of(Map.of(), Map.of(dirt, 12));

        assertEquals(12, result.amountOf(dirt));
        assertTrue(result.counts().isEmpty());
    }

    @Test
    void omitsZeroGenericCountsAndRejectsNegativeOnes() {
        ResourceId dirt = ResourceId.vanilla("dirt");
        assertTrue(ResourceTally.ofIds(Map.of(dirt, 0)).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> ResourceTally.ofIds(Map.of(dirt, -1)));
    }

    @Test
    void genericCountsAreImmutableSnapshots() {
        ResourceId dirt = ResourceId.vanilla("dirt");
        Map<ResourceId, Integer> source = new HashMap<>();
        source.put(dirt, 3);

        ResourceTally result = ResourceTally.ofIds(source);
        source.put(dirt, 8);

        assertEquals(3, result.amountOf(dirt));
        assertThrows(UnsupportedOperationException.class,
                () -> result.idCounts().clear());
    }

    @Test
    void typedCountsAlsoHaveAnIdViewWithoutBeingCountedTwice() {
        ResourceTally result = tally(ResourceType.OAK_LOG, 12);

        assertEquals(12, result.amountOf(ResourceId.vanilla("oak_log")));
        assertEquals(Map.of(ResourceId.vanilla("oak_log"), 12), result.idCounts());
    }

    @Test
    void mergingTalliesPreservesTypedAndGenericCounts() {
        ResourceId dirt = ResourceId.vanilla("dirt");
        ResourceTally result = tally(ResourceType.OAK_LOG, 5)
                .plus(ResourceTally.ofIds(Map.of(dirt, 7)));

        assertEquals(5, result.amountOf(ResourceType.OAK_LOG));
        assertEquals(5, result.amountOf(ResourceId.vanilla("oak_log")));
        assertEquals(7, result.amountOf(dirt));
    }

    @Test
    void genericTalliesCanBeMergedWithEachOther() {
        ResourceId dirt = ResourceId.vanilla("dirt");
        ResourceTally result = ResourceTally.ofIds(Map.of(dirt, 3))
                .plus(ResourceTally.ofIds(Map.of(dirt, 4)));

        assertEquals(7, result.amountOf(dirt));
    }

    /** Ausência é zero, não erro: um baú sem tábua tem zero tábuas. */
    @Test
    void whatWasNotCountedIsZero() {
        ResourceTally result = tally(ResourceType.OAK_LOG, 64);

        assertEquals(0, result.amountOf(ResourceType.COBBLESTONE));
        assertFalse(result.has(ResourceType.COBBLESTONE));
    }

    @Test
    void anEmptyChestCountsNothing() {
        assertTrue(ResourceTally.empty().isEmpty());
        assertEquals(0, ResourceTally.empty().amountOf(ResourceType.OAK_LOG));
        assertTrue(ResourceTally.of(new EnumMap<>(ResourceType.class)).isEmpty());
    }

    /** É o que transforma os baús de uma colônia num total. */
    @Test
    void tallysAddUp() {
        ResourceTally sum = tally(ResourceType.OAK_LOG, 40)
                .plus(tally(ResourceType.OAK_LOG, 24));

        assertEquals(64, sum.amountOf(ResourceType.OAK_LOG));
    }

    @Test
    void addingKeepsWhatOnlyOneSideHad() {
        ResourceTally sum = tally(ResourceType.OAK_LOG, 10)
                .plus(tally(ResourceType.COBBLESTONE, 5));

        assertEquals(10, sum.amountOf(ResourceType.OAK_LOG));
        assertEquals(5, sum.amountOf(ResourceType.COBBLESTONE));
    }

    @Test
    void addingEmptyChangesNothing() {
        ResourceTally one = tally(ResourceType.OAK_PLANKS, 3);

        assertEquals(one, one.plus(ResourceTally.empty()));
        assertEquals(one, ResourceTally.empty().plus(one));
    }

    /** Somar não pode alterar as parcelas — a contagem é uma fotografia. */
    @Test
    void addingDoesNotMutateEitherSide() {
        ResourceTally one = tally(ResourceType.OAK_LOG, 10);
        ResourceTally other = tally(ResourceType.OAK_LOG, 5);

        one.plus(other);

        assertEquals(10, one.amountOf(ResourceType.OAK_LOG));
        assertEquals(5, other.amountOf(ResourceType.OAK_LOG));
    }

    /** Zero e ausente dizem o mesmo, então têm de ser iguais. */
    @Test
    void zeroIsTheSameAsAbsent() {
        Map<ResourceType, Integer> withZero = new EnumMap<>(ResourceType.class);
        withZero.put(ResourceType.OAK_LOG, 5);
        withZero.put(ResourceType.COBBLESTONE, 0);

        assertEquals(tally(ResourceType.OAK_LOG, 5), ResourceTally.of(withZero));
    }

    @Test
    void differentCountsAreNotEqual() {
        assertNotEquals(
                tally(ResourceType.OAK_LOG, 5),
                tally(ResourceType.OAK_LOG, 6));
    }

    /** Um baú não tem quantidade negativa; se chegar uma, algo mentiu. */
    @Test
    void rejectsNegativeAmounts() {
        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        counts.put(ResourceType.OAK_LOG, -1);

        assertThrows(IllegalArgumentException.class, () -> ResourceTally.of(counts));
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> ResourceTally.of(null));
        assertThrows(NullPointerException.class, () -> ResourceTally.empty().plus(null));

        Map<ResourceType, Integer> withNullKey = new HashMap<>();
        withNullKey.put(null, 1);

        assertThrows(NullPointerException.class, () -> ResourceTally.of(withNullKey));
    }

    @Test
    void countsAreReadOnly() {
        var counts = tally(ResourceType.OAK_LOG, 1).counts();

        assertThrows(UnsupportedOperationException.class, () -> counts.clear());
    }

    /** Resource-System.md: a tábua é processada, o tronco é natural. */
    @Test
    void categoriesMatchTheDesign() {
        assertEquals(ResourceCategory.NATURAL, ResourceType.OAK_LOG.category());
        assertEquals(ResourceCategory.NATURAL, ResourceType.COBBLESTONE.category());
        assertEquals(ResourceCategory.PROCESSED, ResourceType.OAK_PLANKS.category());
    }
}
