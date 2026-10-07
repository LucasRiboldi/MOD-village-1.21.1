package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Cinco de cada peça por vez, em ciclo (ADR-038 P5b). */
class AdvanceStockTest {

    private static final List<ResourceType> PIECES =
            List.of(ResourceType.COBBLESTONE_SLAB, ResourceType.STONE_BRICKS);

    @AfterEach
    void forget() {
        AdvanceStock.clearAll();
    }

    @Test
    void fiveOfOnePieceThenTheNext() {
        UUID colony = UUID.randomUUID();
        Map<ResourceType, Integer> stock = new EnumMap<>(ResourceType.class);
        Map<ResourceType, Integer> goals = new HashMap<>();

        AdvanceStock.addTo(goals, colony, PIECES, type -> stock.getOrDefault(type, 0));
        assertEquals(5, goals.get(ResourceType.COBBLESTONE_SLAB));
        assertFalse(goals.containsKey(ResourceType.STONE_BRICKS), "duas peças ao mesmo tempo");

        stock.put(ResourceType.COBBLESTONE_SLAB, 5);
        goals.clear();
        AdvanceStock.addTo(goals, colony, PIECES, type -> stock.getOrDefault(type, 0));
        assertEquals(5, goals.get(ResourceType.STONE_BRICKS), "feitas as cinco, não trocou de peça");
        assertFalse(goals.containsKey(ResourceType.COBBLESTONE_SLAB));

        stock.put(ResourceType.STONE_BRICKS, 5);
        goals.clear();
        AdvanceStock.addTo(goals, colony, PIECES, type -> stock.getOrDefault(type, 0));
        assertEquals(10, goals.get(ResourceType.COBBLESTONE_SLAB), "o ciclo não voltou à primeira peça");
    }

    @Test
    void aPieceThatDoesNotMoveGivesWay() {
        UUID colony = UUID.randomUUID();
        Map<ResourceType, Integer> goals = new HashMap<>();

        for (int cycle = 0; cycle <= AdvanceStock.PATIENCE + 1; cycle++) {
            goals.clear();
            AdvanceStock.addTo(goals, colony, PIECES, type -> 0);
        }

        assertEquals(5, goals.get(ResourceType.STONE_BRICKS), "a peça sem material prendeu o ciclo");
    }

    @Test
    void eachTradeHasItsOwnCycle() {
        UUID colony = UUID.randomUUID();
        Map<ResourceType, Integer> goals = new HashMap<>();

        AdvanceStock.addTo(goals, colony, PIECES, type -> 0);
        AdvanceStock.addTo(goals, colony, "smelter", AdvanceStock.SMELTER_PIECES, type -> 0);

        assertEquals(5, goals.get(ResourceType.COBBLESTONE_SLAB));
        assertEquals(5, goals.get(ResourceType.GLASS), "a fila do fundidor não andou junto com a do pedreiro");
    }
}
