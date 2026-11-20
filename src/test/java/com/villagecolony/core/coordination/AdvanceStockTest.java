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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cinco de cada peça por vez, em ciclo (ADR-038 P5b). */
class AdvanceStockTest {

    private static final List<ResourceType> PIECES =
            List.of(ResourceType.SANDSTONE_SLAB, ResourceType.SMOOTH_STONE_SLAB);

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
        assertEquals(5, goals.get(ResourceType.SANDSTONE_SLAB));
        assertFalse(goals.containsKey(ResourceType.SMOOTH_STONE_SLAB), "duas peças ao mesmo tempo");

        stock.put(ResourceType.SANDSTONE_SLAB, 5);
        goals.clear();
        AdvanceStock.addTo(goals, colony, PIECES, type -> stock.getOrDefault(type, 0));
        assertEquals(5, goals.get(ResourceType.SMOOTH_STONE_SLAB), "feitas as cinco, não trocou de peça");
        assertFalse(goals.containsKey(ResourceType.SANDSTONE_SLAB));

        stock.put(ResourceType.SMOOTH_STONE_SLAB, 5);
        goals.clear();
        AdvanceStock.addTo(goals, colony, PIECES, type -> stock.getOrDefault(type, 0));
        assertEquals(10, goals.get(ResourceType.SANDSTONE_SLAB), "o ciclo não voltou à primeira peça");
    }

    @Test
    void aPieceThatDoesNotMoveGivesWay() {
        UUID colony = UUID.randomUUID();
        Map<ResourceType, Integer> goals = new HashMap<>();

        for (int cycle = 0; cycle <= AdvanceStock.PATIENCE + 1; cycle++) {
            goals.clear();
            AdvanceStock.addTo(goals, colony, PIECES, type -> 0);
        }

        assertEquals(5, goals.get(ResourceType.SMOOTH_STONE_SLAB), "a peça sem material prendeu o ciclo");
    }

    @Test
    void theAdvanceStopsAtTheCeilingOfTheNextBuild() {
        UUID colony = UUID.randomUUID();
        Map<ResourceType, Integer> stock = new EnumMap<>(ResourceType.class);
        Map<ResourceType, Integer> goals = new HashMap<>();

        AdvanceStock.ceilings(colony, Map.of(ResourceType.SANDSTONE_SLAB, 7, ResourceType.SMOOTH_STONE_SLAB, 3));

        AdvanceStock.addTo(goals, colony, PIECES, type -> stock.getOrDefault(type, 0));
        assertEquals(5, goals.get(ResourceType.SANDSTONE_SLAB));

        stock.put(ResourceType.SANDSTONE_SLAB, 5);
        stock.put(ResourceType.SMOOTH_STONE_SLAB, 3);
        goals.clear();
        AdvanceStock.addTo(goals, colony, PIECES, type -> stock.getOrDefault(type, 0));
        assertEquals(7, goals.get(ResourceType.SANDSTONE_SLAB),
                "a meta passou do que a próxima obra pede (teto 7)");
        assertFalse(goals.containsKey(ResourceType.SMOOTH_STONE_SLAB), "o tijolo já está no teto");

        stock.put(ResourceType.SANDSTONE_SLAB, 7);
        goals.clear();
        AdvanceStock.addTo(goals, colony, PIECES, type -> stock.getOrDefault(type, 0));
        assertTrue(goals.isEmpty(), "com as duas peças no teto, nada se adianta: " + goals);
    }

    @Test
    void aPieceTheNextBuildDoesNotUseIsNeverAdvanced() {
        UUID colony = UUID.randomUUID();
        Map<ResourceType, Integer> goals = new HashMap<>();

        AdvanceStock.ceilings(colony, Map.of(ResourceType.SMOOTH_STONE_SLAB, 4));

        AdvanceStock.addTo(goals, colony, PIECES, type -> 0);

        assertFalse(goals.containsKey(ResourceType.SANDSTONE_SLAB), "laje sem uso na obra");
        assertEquals(4, goals.get(ResourceType.SMOOTH_STONE_SLAB));
    }

    @Test
    void eachTradeHasItsOwnCycle() {
        UUID colony = UUID.randomUUID();
        Map<ResourceType, Integer> goals = new HashMap<>();

        AdvanceStock.addTo(goals, colony, PIECES, type -> 0);
        AdvanceStock.addTo(goals, colony, "smelter", AdvanceStock.SMELTER_PIECES, type -> 0);

        assertEquals(5, goals.get(ResourceType.SANDSTONE_SLAB));
        assertEquals(BlockReserve.reserveOf(ResourceType.GLASS), goals.get(ResourceType.GLASS),
                "a fila do fundidor não andou junto com a do pedreiro (o vidro para na reserva, Regra 340)");
    }
}
