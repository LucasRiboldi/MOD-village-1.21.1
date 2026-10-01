package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.colony.service.ColonyService;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.service.BuildingRegistry;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Um aglomerado de camas da mesma vila não vira colônia nova — E51, 2026-09-30.
 *
 * <p>No playtest de 30-09 a vila do autor, com mais de 128 blocos de ponta a
 * ponta, teve aglomerados a 65+ blocos do centro adotados como colônias
 * novas: cada uma ergueu uma BigHouseMOD e criou 7 adultos antes de a fusão
 * do fim do ciclo as absorver. Quatro casas-base a mais em duas sessões.
 */
class ColonyIdentityTest {

    private ColonyService colonies;
    private BuildingRegistry buildings;

    @BeforeEach
    void setUp() {
        colonies = new ColonyService();
        buildings = new BuildingRegistry();
    }

    private static VillageCandidate clusterAt(int x, int z) {
        List<ColonyPos> beds = List.of(
                new ColonyPos(x, 64, z), new ColonyPos(x + 2, 64, z), new ColonyPos(x + 4, 64, z));
        return new VillageCandidate(new ColonyPos(x + 2, 64, z), 3, true, null, beds);
    }

    private void buildingOf(Colony colony, int minX, int minZ, int maxX, int maxZ) {
        buildings.register(new Building(UUID.randomUUID(), colony.id(), ResourceId.vanilla("house"),
                new ColonyPos(minX, 64, minZ), new ColonyPos(maxX, 70, maxZ)));
    }

    private Optional<ColonyIdentity.Owner> ownerOf(VillageCandidate candidate) {
        return ColonyIdentity.ownerOf(candidate, colonies, buildings, colony -> false);
    }

    @Test
    void aClusterNearTheCenterBelongsToTheColonyAsBefore() {
        Colony colony = colonies.createColony(new ColonyPos(0, 64, 0));

        Optional<ColonyIdentity.Owner> owner = ownerOf(clusterAt(40, 0));

        assertEquals(colony, owner.orElseThrow().colony());
        assertEquals(ColonyIdentity.Reason.NEAR_ITS_CENTER, owner.get().reason());
    }

    @Test
    void aFarClusterBesideOneOfItsBuildingsIsTheSameVillage() {
        Colony colony = colonies.createColony(new ColonyPos(0, 64, 0));
        buildingOf(colony, 60, -4, 70, 4);

        Optional<ColonyIdentity.Owner> owner = ownerOf(clusterAt(95, 0));

        assertEquals(colony, owner.orElseThrow().colony(),
                "aglomerado a 25 blocos de uma casa da colônia virou vila nova");
        assertEquals(ColonyIdentity.Reason.NEAR_ITS_BUILDINGS, owner.get().reason());
    }

    @Test
    void theSameGeneratedVillageIsTheSameColonyAtAnyDistance() {
        Colony colony = colonies.createColony(new ColonyPos(0, 64, 0));

        Optional<ColonyIdentity.Owner> owner = ColonyIdentity.ownerOf(
                clusterAt(200, 0), colonies, buildings, known -> known.equals(colony));

        assertEquals(colony, owner.orElseThrow().colony());
        assertEquals(ColonyIdentity.Reason.SAME_GENERATED_VILLAGE, owner.get().reason());
    }

    @Test
    void aClusterFarFromEverythingIsANewVillage() {
        Colony colony = colonies.createColony(new ColonyPos(0, 64, 0));
        buildingOf(colony, 60, -4, 70, 4);

        assertTrue(ownerOf(clusterAt(103, 0)).isEmpty(),
                "a 33 blocos da última casa já é outra vila");
    }

    @Test
    void aBuildingOfNoColonyMakesNothingOwned() {
        buildings.register(new Building(UUID.randomUUID(), UUID.randomUUID(),
                ResourceId.vanilla("house"), new ColonyPos(60, 64, -4), new ColonyPos(70, 70, 4)));

        assertTrue(ownerOf(clusterAt(95, 0)).isEmpty());
    }

    @Test
    void twoColoniesWhoseBuildingsAreAFewBlocksApartAreNeighbours() {
        Colony a = colonies.createColony(new ColonyPos(0, 64, 0));
        Colony b = colonies.createColony(new ColonyPos(200, 64, 0));
        buildingOf(a, 60, -4, 70, 4);
        buildingOf(b, 90, -4, 100, 4);

        assertTrue(ColonyIdentity.buildingsNear(a.id(), b.id(), buildings));
    }

    @Test
    void twoColoniesWhoseBuildingsAreFarApartAreNotNeighbours() {
        Colony a = colonies.createColony(new ColonyPos(0, 64, 0));
        Colony b = colonies.createColony(new ColonyPos(200, 64, 0));
        buildingOf(a, 60, -4, 70, 4);
        buildingOf(b, 104, -4, 110, 4);

        assertTrue(!ColonyIdentity.buildingsNear(a.id(), b.id(), buildings));
    }
}
