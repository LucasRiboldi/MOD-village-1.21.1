package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.colony.service.ColonyService;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.service.BuildingRegistry;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O que faz a vila crescer — decisão do autor, 2026-09-30. */
class VillageGrowthTest {

    private ColonyService colonies;
    private Colony colony;

    @BeforeEach
    void setUp() {
        colonies = new ColonyService();
        colony = colonies.createColony(new ColonyPos(50, 64, 50));
        colony.measure(new VillageBounds(0, 60, 0, 99, 70, 99));
    }

    private Building house(int minZ, int maxZ) {
        return new Building(UUID.randomUUID(), colony.id(), ResourceId.vanilla("house"),
                new ColonyPos(50, 64, minZ), new ColonyPos(54, 68, maxZ));
    }

    @Test
    void aBuildingByTheBorderGrowsTheVillage() {
        assertTrue(VillageGrowth.byPiece(colonies, house(90, 95)).isPresent());

        // A vila medida tem 0..100 (lado ímpar); a casa até 95 mais os 15 leva o sul a 110.
        assertEquals(111, colony.bounds().orElseThrow().sizeZ());
    }

    @Test
    void aRoadChainOutsideGrowsTheVillageToItsFarthestBlock() {
        VillageGrowth.byRoad(colonies, colony.id(),
                List.of(new ColonyPos(50, 64, 101), new ColonyPos(50, 64, 105), new ColonyPos(50, 64, 103)));

        // Até o bloco 105 (0..105 = 106, par): mais uma linha ao sul, 107.
        assertEquals(106, colony.bounds().orElseThrow().maxZ());
        assertEquals(107, colony.bounds().orElseThrow().sizeZ());
    }

    @Test
    void aPieceOfAnUnknownColonyGrowsNothing() {
        Building stray = new Building(UUID.randomUUID(), UUID.randomUUID(), ResourceId.vanilla("house"),
                new ColonyPos(500, 64, 500), new ColonyPos(504, 68, 504));

        assertTrue(VillageGrowth.byPiece(colonies, stray).isEmpty());
    }

    @Test
    void theBuildingRegistryAnnouncesEveryRegisteredBuilding() {
        BuildingRegistry registry = new BuildingRegistry();
        registry.whenRegistered(building -> VillageGrowth.byPiece(colonies, building));

        registry.register(house(90, 95));

        assertEquals(111, colony.bounds().orElseThrow().sizeZ(),
                "a construção registrada não fez a vila crescer");
    }
}
