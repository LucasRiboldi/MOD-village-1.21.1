package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.integration.ColonyModels;
import com.villagecolony.fabric.integration.VillageStructures;
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
        // Regra 49 (10-02): o ferramenteiro é do mineiro.
        assertEquals(Optional.of(ProfessionType.MINER), ConstructionOrder.professionOf(
                ResourceId.vanilla("village/plains/houses/plains_tool_smith_1")));
        assertEquals(Optional.empty(), ConstructionOrder.professionOf(BUTCHER));
        assertEquals(Optional.empty(), ConstructionOrder.professionOf(TEMPLE));
    }

    @Test
    void onlyStorageNamedModelBelongsToItsRequestedProfession() {
        ResourceId barn = ResourceId.parse("villagecolony:colony/barn_majest");
        ResourceId storage = ResourceId.parse("villagecolony:colony/storage_majest");

        assertEquals(Optional.empty(), ColonyModels.professionOf(barn));
        assertEquals(Optional.of(ProfessionType.BUILDER), ColonyModels.professionOf(storage));
        assertEquals(Optional.empty(), ConstructionOrder.professionOf(barn));
        assertEquals(Optional.of(ProfessionType.BUILDER), ConstructionOrder.professionOf(storage));
        assertFalse(HousePlans.isHouse(barn));
        assertFalse(HousePlans.isHouse(storage));
        assertFalse(VillageStructures.buildableFor("plains").contains(barn));
        // O armazém do bioma entra no lugar do de todos os estilos (Regra 115).
        ResourceId plainsStorage = ResourceId.parse("villagecolony:colony/plains/storage_majest");

        assertEquals(Optional.of(ProfessionType.BUILDER), ColonyModels.professionOf(plainsStorage));
        assertTrue(VillageStructures.buildableFor("plains").contains(plainsStorage));
        assertFalse(VillageStructures.buildableFor("plains").contains(storage));
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

    /** O tipo novo vem antes de qualquer repetição: a repetição é de {@link ConstructionOrder#nextRepeat} — Regra 218. */
    @Test
    void withEveryTypeStandingTheNextTypeHasNothingToSay() {
        List<Building> buildings = List.of(
                built(FARM, true), built(MASON, true), built(BUTCHER, true), built(TEMPLE, true));

        assertEquals(Optional.empty(), next(buildings, List.of(BUTCHER, FARM, TEMPLE, MASON)));
    }

    private static Optional<String> repeat(List<Building> buildings, List<ResourceId> offered, int adults) {
        return ConstructionOrder.nextRepeat(
                buildings, offered.stream().map(HousePlans::constructionType).distinct().toList(),
                HousePlans::constructionType, adults);
    }

    /** Regra 218: a primeira repetição sai no 5º adulto, e não antes. */
    @Test
    void theFirstRepeatComesWithTheFifthAdult() {
        List<Building> buildings = List.of(built(MASON, true), built(BUTCHER, true));

        assertEquals(Optional.empty(), repeat(buildings, List.of(MASON, BUTCHER), 4));
        assertEquals(Optional.of("mason"), repeat(buildings, List.of(MASON, BUTCHER), 5));
    }

    /** Regra 218: repete o que a vila tem menos, e cada repetição de pé gasta a sua cota. */
    @Test
    void itRepeatsWhatTheVillageHasLeastUntilThePopulationIsServed() {
        List<Building> buildings = List.of(built(MASON, true), built(MASON, true), built(BUTCHER, true));

        assertEquals(Optional.empty(), repeat(buildings, List.of(MASON, BUTCHER), 9), "a cota de 9 adultos já foi gasta");
        assertEquals(Optional.of("butcher"), repeat(buildings, List.of(MASON, BUTCHER), 10));
    }

    /** Regra 218: obra abandonada não gasta a cota. */
    @Test
    void anAbandonedWorkDoesNotCountAsARepeat() {
        List<Building> buildings = List.of(built(MASON, true), built(MASON, false), built(BUTCHER, true));

        assertEquals(Optional.of("mason"), repeat(buildings, List.of(MASON, BUTCHER), 5));
    }

    /** Regra 218: roça e casa têm conta própria, nem saem nem gastam a cota. */
    @Test
    void farmsAndHousesAreNotRepeatsAndDoNotSpendTheQuota() {
        List<Building> farms = List.of(built(FARM, true), built(FARM, true), built(FARM, true), built(BUTCHER, true));

        assertEquals(Optional.empty(), repeat(farms, List.of(FARM, SMALL_HOUSE), 50));
        assertEquals(Optional.of("butcher"), repeat(farms, List.of(FARM, BUTCHER), 5));
    }

    @Test
    void onlyTradesTheCatalogOffersAreMissing() {
        assertEquals(List.of(ProfessionType.MASON),
                ConstructionOrder.missingWorkshops(List.of(), Set.of(ProfessionType.MASON)));
    }
}
