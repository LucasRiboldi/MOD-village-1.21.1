package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O teto geral do estoque — regra do autor, 2026-10-11: 64 fabricados, 256 crus. */
class StockCeilingTest {

    @Test
    void aCraftedPieceStopsAt64AndARawBlockAt256() {
        Map<ResourceType, Integer> capped = StockCeiling.cap(Map.of(
                ResourceType.OAK_STAIRS, 363,
                ResourceType.SMOOTH_STONE_SLAB, 330,
                ResourceType.GLASS, 100,
                ResourceType.OAK_LOG, 751,
                ResourceType.WHEAT, 191), Map.of());

        assertEquals(64, capped.get(ResourceType.OAK_STAIRS));
        assertEquals(64, capped.get(ResourceType.SMOOTH_STONE_SLAB));
        assertEquals(64, capped.get(ResourceType.GLASS), "o fundido conta como fabricado");
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

            assertEquals(true, base == StockCeiling.CRAFTED || base == StockCeiling.RAW, type.name());
        }
    }
}
