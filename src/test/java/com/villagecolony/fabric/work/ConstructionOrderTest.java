package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A ordem das obras — N9, 2026-09-24: casa, a oficina de cada ofício, e
 * só então o que sobrou.
 */
class ConstructionOrderTest {

    private static final UUID COLONY = UUID.randomUUID();

    private static final ResourceId SMALL_HOUSE =
            ResourceId.parse("minecraft:village/plains/houses/plains_small_house_1");

    private static final ResourceId FARM =
            ResourceId.parse("minecraft:village/plains/houses/plains_small_farm_1");

    private static final ResourceId MASON =
            ResourceId.parse("minecraft:village/taiga/houses/taiga_masons_house_1");

    private static final ResourceId ARMORER =
            ResourceId.parse("minecraft:village/plains/houses/plains_armorer_house_1");

    private static final ResourceId BUTCHER =
            ResourceId.parse("minecraft:village/savanna/houses/savanna_butchers_shop_1");

    private static final ResourceId TEMPLE =
            ResourceId.parse("minecraft:village/plains/houses/plains_temple_4");

    private static Building built(ResourceId id, boolean finished) {
        return new Building(UUID.randomUUID(), COLONY, id,
                new ColonyPos(0, 64, 0), new ColonyPos(8, 70, 8), finished);
    }

    /** Os tipos oferecidos, na ordem do catálogo, e o ofício de cada um. */
    private static Optional<String> next(List<Building> buildings, List<ResourceId> offered) {
        List<String> types = offered.stream().map(HousePlans::constructionType).distinct().toList();
        Map<String, Optional<ProfessionType>> professions = new java.util.HashMap<>();

        for (ResourceId id : offered) {
            professions.putIfAbsent(HousePlans.constructionType(id), ConstructionOrder.professionOf(id));
        }

        return ConstructionOrder.nextType(buildings, types, professions, HousePlans::constructionType);
    }

    @Test
    void aWorkshopIsNotAHouse() {
        assertTrue(HousePlans.isHouse(SMALL_HOUSE));
        assertFalse(HousePlans.isHouse(MASON), "a oficina do pedreiro saia na vez da casa");
        assertFalse(HousePlans.isHouse(BUTCHER));
        assertTrue(HousePlans.isDwelling(MASON), "a oficina continua sendo moradia para o resto do mod");
    }

    @Test
    void eachWorkshopBelongsToItsTrade() {
        assertEquals(Optional.of(ProfessionType.FARMER), ConstructionOrder.professionOf(FARM));
        assertEquals(Optional.of(ProfessionType.MASON), ConstructionOrder.professionOf(MASON));
        assertEquals(Optional.of(ProfessionType.SMELTER), ConstructionOrder.professionOf(ARMORER));
        assertEquals(Optional.empty(), ConstructionOrder.professionOf(BUTCHER));
        assertEquals(Optional.empty(), ConstructionOrder.professionOf(TEMPLE));
    }

    @Test
    void theOtherTurnGoesToTheFirstTradeWithoutAWorkshop() {
        // O templo vem antes no catálogo, e ainda assim a vez é da roça.
        assertEquals(Optional.of("farm"), next(List.of(), List.of(TEMPLE, MASON, FARM)));
    }

    @Test
    void aTradeWithAWorkshopStandingGivesTheTurnToTheNext() {
        List<Building> buildings = List.of(built(FARM, true));

        assertEquals(Optional.of("mason"), next(buildings, List.of(TEMPLE, ARMORER, MASON)));
    }

    @Test
    void anAbandonedWorkshopStillCountsAsMissing() {
        List<Building> buildings = List.of(built(MASON, false));

        assertEquals(Optional.of("mason"), next(buildings, List.of(TEMPLE, MASON)));
    }

    @Test
    void withEveryTradeServedTheVillageBuildsWhatItDoesNotHaveYet() {
        List<Building> buildings = List.of(
                built(FARM, true), built(MASON, true), built(ARMORER, true), built(BUTCHER, true));

        assertEquals(Optional.of("temple"), next(buildings, List.of(BUTCHER, FARM, TEMPLE)));
    }

    @Test
    void onlyTradesTheCatalogOffersAreMissing() {
        assertEquals(List.of(ProfessionType.MASON),
                ConstructionOrder.missingWorkshops(List.of(), Set.of(ProfessionType.MASON)));
    }
}
