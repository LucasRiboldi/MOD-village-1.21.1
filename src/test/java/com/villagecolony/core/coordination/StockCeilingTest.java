package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O teto do estoque — ADR-062, Regra 340: o fabricado na reserva da classe (4, 12 ou 1), o cru em 256. */
class StockCeilingTest {

    @Test
    void aCraftedPieceStopsAtTheReserveOfItsClassAndARawBlockAt256() {
        Map<ResourceType, Integer> capped = StockCeiling.cap(Map.of(
                ResourceType.OAK_STAIRS, 363,
                ResourceType.SMOOTH_STONE_SLAB, 330,
                ResourceType.GLASS, 100,
                ResourceType.OAK_LOG, 751,
                ResourceType.WHEAT, 191), Map.of());

        assertEquals(12, capped.get(ResourceType.OAK_STAIRS), "Regra 340: a escada das vilas guarda 12");
        assertEquals(12, capped.get(ResourceType.SMOOTH_STONE_SLAB));
        assertEquals(4, capped.get(ResourceType.GLASS), "Regra 340: o vidro não está nas vilas e guarda 4");
        assertEquals(256, capped.get(ResourceType.OAK_LOG));
        assertEquals(191, capped.get(ResourceType.WHEAT), "meta abaixo do teto fica como está");
    }

    @Test
    void theOpenBuildMayAskAboveTheCeiling() {
        Map<ResourceType, Integer> capped = StockCeiling.cap(
                Map.of(ResourceType.OAK_PLANKS, 120, ResourceType.COBBLESTONE, 400),
                Map.of(ResourceType.OAK_PLANKS, 100, ResourceType.COBBLESTONE, 300));

        assertEquals(100, capped.get(ResourceType.OAK_PLANKS));
        assertEquals(300, capped.get(ResourceType.COBBLESTONE));
    }

    @Test
    void theOrderOfTheGoalsIsKept() {
        Map<ResourceType, Integer> goals = new LinkedHashMap<>();
        goals.put(ResourceType.WHEAT, 10);
        goals.put(ResourceType.OAK_LOG, 999);
        goals.put(ResourceType.COBBLESTONE, 5);

        assertEquals(List.of(ResourceType.WHEAT, ResourceType.OAK_LOG, ResourceType.COBBLESTONE),
                List.copyOf(StockCeiling.cap(goals, Map.of()).keySet()));
    }

    @Test
    void everyResourceHasACeiling() {
        for (ResourceType type : ResourceType.values()) {
            int base = StockCeiling.base(type);

            assertEquals(true, base == BlockReserve.reserveOf(type) && base > 0, type.name());
        }
    }
}
