package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PenPlansTest {

    @Test
    void onlyTheLargePenBlocksAnotherLargePen() {
        UUID colony = UUID.randomUUID();
        Building legacy = building(colony, new ResourceId("villagecolony", "colony/animal_pen"));
        Building large = building(colony, PenPlans.MODEL);

        assertFalse(PenPlans.hasLargePen(List.of(legacy)), "curral legado bloqueou o primeiro curral grande");
        assertTrue(PenPlans.hasLargePen(List.of(legacy, large)), "segundo curral grande não foi bloqueado");
    }

    private static Building building(UUID colony, ResourceId blueprint) {
        return new Building(UUID.randomUUID(), colony, blueprint,
                new ColonyPos(0, 64, 0), new ColonyPos(14, 66, 14));
    }
}
