package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra 382 (autor 26-11, K2 do jogo da 0.4.148: "adiciona a necessidade de construir a casa do fazendeiro perto da fazenda
 * grande"): com a plantação grande pronta e nenhuma casa a até {@link FarmerHomePlans#RADIUS} blocos dela, a vila deve a
 * casa do fazendeiro ali perto.
 */
class FarmerHomeTest {

    private static final UUID COLONY = UUID.randomUUID();

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/plains_small_house_1");

    private static Building at(ResourceId blueprint, int x, int z, int side) {
        return new Building(UUID.randomUUID(), COLONY, blueprint, new ColonyPos(x, 64, z),
                new ColonyPos(x + side - 1, 70, z + side - 1), true);
    }

    @Test
    @DisplayName("Regra 382: a plantação grande sem casa perto pede a casa do fazendeiro")
    void aLargeFarmWithoutAHouseNearbyOwesTheFarmersHouse() {
        Building farm = at(LargeFarmPlans.MODEL, 0, 0, 16);

        assertFalse(FarmerHomePlans.isOwed(List.of()), "sem plantação grande, a casa do fazendeiro foi pedida");
        assertTrue(FarmerHomePlans.isOwed(List.of(farm)), "Regra 382: a plantação grande sem casa não pediu a casa");
        assertTrue(FarmerHomePlans.isOwed(List.of(farm, at(HOUSE, 60, 60, 7))),
                "Regra 382: a casa a 44 blocos da plantação contou como a casa do fazendeiro");
        assertFalse(FarmerHomePlans.isOwed(List.of(farm, at(HOUSE, 20, 0, 7))),
                "a casa a 4 blocos da plantação não bastou");
    }
}
