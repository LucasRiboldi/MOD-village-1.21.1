package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.colony.service.ColonyService;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.coordination.VillageExpansion.SideSurvey;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O plano de expansão do terreno — Regra 56 (B, C e D). */
class VillageExpansionTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/plains_small_house_1");
    private static final ResourceId FARM = ResourceId.vanilla("village/plains/houses/plains_large_farm_1");

    @AfterEach
    void tearDown() {
        VillageExpansion.clearAll();
        VillageExpansion.useHouseRule(id -> true);
    }

    @Test
    void aWorkshopClaimsMoreGroundThanAHouse() {
        VillageExpansion.useHouseRule(HOUSE::equals);

        assertEquals(VillageExpansion.HOUSE_CLAIM, VillageExpansion.claimOf(HOUSE));
        assertEquals(VillageExpansion.WORK_CLAIM, VillageExpansion.claimOf(FARM));
    }

    @Test
    void aFarmByTheBorderPushesTheBorderByItsClaim() {
        VillageExpansion.useHouseRule(HOUSE::equals);
        ColonyService colonies = new ColonyService();
        Colony colony = colonies.createColony(new ColonyPos(50, 64, 50));
        colony.measure(new VillageBounds(0, 60, 0, 99, 70, 99));
        Building farm = new Building(UUID.randomUUID(), colony.id(), FARM,
                new ColonyPos(50, 64, 90), new ColonyPos(54, 68, 95));

        VillageBounds grown = VillageGrowth.byPiece(colonies, farm).orElseThrow();

        assertTrue(grown.maxZ() >= 95 + VillageExpansion.WORK_CLAIM, "a roça não reivindicou a folga de obra: " + grown);
    }

    @Test
    void theVillageIsFullAfterTwoSweepsWithoutALot() {
        UUID colony = UUID.randomUUID();

        assertFalse(VillageExpansion.sweptWithoutLot(colony));
        assertTrue(VillageExpansion.sweptWithoutLot(colony));
    }

    @Test
    void anAnnexedStripThatGaveNoLotWaitsLongerBeforeTheNext() {
        UUID colony = UUID.randomUUID();
        VillageExpansion.annexed(colony, Side.EAST);

        for (int sweep = 1; sweep < VillageExpansion.FULL_AGAIN_AFTER; sweep++) {
            assertFalse(VillageExpansion.sweptWithoutLot(colony), "anexou de novo na varredura " + sweep);
        }

        assertTrue(VillageExpansion.sweptWithoutLot(colony));
    }

    @Test
    void aLotFoundStartsTheCountAgain() {
        UUID colony = UUID.randomUUID();

        VillageExpansion.sweptWithoutLot(colony);
        VillageExpansion.lotFound(colony);

        assertFalse(VillageExpansion.sweptWithoutLot(colony));
    }

    @Test
    void theFlattestDrySideWinsAndAnotherColonyVetoesItsSide() {
        List<SideSurvey> surveys = List.of(
                new SideSurvey(Side.NORTH, 40, 38, 0, 0, true),
                new SideSurvey(Side.SOUTH, 40, 30, 8, 0, false),
                new SideSurvey(Side.EAST, 40, 34, 0, 1, false),
                new SideSurvey(Side.WEST, 40, 10, 0, 0, false));

        Optional<SideSurvey> best = VillageExpansion.choose(surveys);

        assertEquals(Optional.of(Side.EAST), best.map(SideSurvey::side));
    }

    @Test
    void noSideServesWhenNoneIsHalfFlat() {
        assertTrue(VillageExpansion.choose(List.of(
                new SideSurvey(Side.NORTH, 40, 19, 0, 0, false),
                new SideSurvey(Side.SOUTH, 0, 0, 0, 0, false))).isEmpty());
    }

    @Test
    void theStripLiesJustBeyondTheChosenSide() {
        VillageBounds box = new VillageBounds(0, 60, 0, 99, 70, 49);

        assertEquals(new VillageBounds(100, 60, 0, 115, 70, 49), VillageExpansion.stripOf(box, Side.EAST));
        assertEquals(new VillageBounds(0, 60, -16, 99, 70, -1), VillageExpansion.stripOf(box, Side.NORTH));
    }
}
