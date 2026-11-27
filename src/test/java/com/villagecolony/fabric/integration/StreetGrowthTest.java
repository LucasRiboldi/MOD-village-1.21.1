package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Regra 389 (ADR-065, emenda de 27-11): a volta da busca de lote que fecha sem lote faz crescer o braço da malha do lado
 * com mais recusas, e só uma vez por volta.
 */
class StreetGrowthTest {

    private static final ColonyPos CENTER = new ColonyPos(0, 64, 0);

    @AfterEach
    void clear() {
        StreetGrowth.clearAll();
    }

    @Test
    @DisplayName("Regra 389: o lado com mais recusas cresce na volta sem lote, e a vez é gasta")
    void theSideWithMostRefusalsGrows() {
        UUID colony = UUID.randomUUID();

        StreetGrowth.count(colony, CENTER, 40, 2);
        StreetGrowth.count(colony, CENTER, -3, 50);
        StreetGrowth.count(colony, CENTER, 4, 60);

        assertArrayEquals(new boolean[4], StreetGrowth.take(colony), "cresceu sem volta sem lote");

        StreetGrowth.noLot(colony);

        assertArrayEquals(new boolean[] {false, false, true, false}, StreetGrowth.take(colony),
                "Regra 389: o lado sul, com mais recusas, não cresceu");
        assertArrayEquals(new boolean[4], StreetGrowth.take(colony), "a mesma volta sem lote cresceu duas vezes");

        StreetGrowth.noLot(colony);

        assertArrayEquals(new boolean[4], StreetGrowth.take(colony), "a conta das recusas não recomeçou depois da volta");
    }
}
