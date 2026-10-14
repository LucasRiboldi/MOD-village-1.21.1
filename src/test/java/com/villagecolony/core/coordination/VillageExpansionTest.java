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
    void theVillageIsFullAfterACompleteSweepWithoutALot() {
        UUID colony = UUID.randomUUID();

        for (int sweep = 1; sweep < VillageExpansion.FULL_AFTER; sweep++) {
            assertFalse(VillageExpansion.sweptWithoutLot(colony));
        }

        assertTrue(VillageExpansion.sweptWithoutLot(colony));
    }

    /**
     * Com uma faixa anexada que não rendeu lote, a vila anexa de novo já na volta seguinte sem lote
     * — não espera mais que uma varredura completa (14-10): a vila crescia devagar demais e ficava
     * ociosa entre anexos. A salvaguarda contra inchaço é a qualidade do lado, não a espera.
     */
    @Test
    void anAnnexedStripThatGaveNoLotAnnexesAgainOnTheNextFullSweep() {
        UUID colony = UUID.randomUUID();
        VillageExpansion.annexed(colony, Side.EAST);

        // Uma faixa anexada não faz a vila esperar mais que o normal: a cadência com e sem anexo
        // pendente é a mesma (FULL_AGAIN_AFTER == FULL_AFTER).
        for (int sweep = 1; sweep < VillageExpansion.FULL_AGAIN_AFTER; sweep++) {
            assertFalse(VillageExpansion.sweptWithoutLot(colony), "anexou de novo na varredura " + sweep);
        }

        assertTrue(VillageExpansion.sweptWithoutLot(colony), "não re-anexou na volta que confirmou a falta de lote");
        assertEquals(VillageExpansion.FULL_AFTER, VillageExpansion.FULL_AGAIN_AFTER,
                "a espera com faixa pendente deve ser a mesma da primeira (sem atraso extra)");
    }

    @Test
    void aLotFoundStartsTheCountAgain() {
        UUID colony = UUID.randomUUID();
        VillageExpansion.annexed(colony, Side.EAST);

        VillageExpansion.sweptWithoutLot(colony);
        VillageExpansion.lotFound(colony);

        // Sem anexo pendente, a conta volta à de sempre.
        for (int sweep = 1; sweep < VillageExpansion.FULL_AFTER; sweep++) {
            assertFalse(VillageExpansion.sweptWithoutLot(colony));
        }

        assertTrue(VillageExpansion.sweptWithoutLot(colony));
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

    @Test
    void oneBuildInFiveLooksForALotOnTheEdge() {
        UUID colony = UUID.randomUUID();
        VillageBounds box = new VillageBounds(0, 60, 0, 99, 70, 99);

        for (int opened = 1; opened < VillageExpansion.EDGE_EVERY - 1; opened++) {
            VillageExpansion.buildOpened(colony);
            assertFalse(VillageExpansion.isEdgeTurn(colony), "vez da borda depois de " + opened + " obras");
        }

        VillageExpansion.buildOpened(colony);

        assertTrue(VillageExpansion.isEdgeTurn(colony));
        assertFalse(VillageExpansion.allowsLotAt(colony, box, 50, 50), "o miolo serviu na vez da borda");
        assertTrue(VillageExpansion.allowsLotAt(colony, box, 95, 50), "a faixa de dentro da borda não serviu");
        assertTrue(VillageExpansion.allowsLotAt(colony, box, 110, 50), "a faixa de fora da borda não serviu");
        assertFalse(VillageExpansion.allowsLotAt(colony, box, 130, 50), "longe da borda serviu");

        VillageExpansion.buildOpened(colony);

        assertFalse(VillageExpansion.isEdgeTurn(colony), "a vez da borda não acabou com a obra aberta");
        assertTrue(VillageExpansion.allowsLotAt(colony, box, 50, 50));
    }

    @Test
    void anEdgeTurnWithoutALotGivesWayAndDoesNotCountAsFull() {
        UUID colony = UUID.randomUUID();

        for (int opened = 0; opened < VillageExpansion.EDGE_EVERY - 1; opened++) {
            VillageExpansion.buildOpened(colony);
        }

        assertFalse(VillageExpansion.sweptWithoutLot(colony), "a varredura da borda contou para anexar");
        assertFalse(VillageExpansion.isEdgeTurn(colony));

        for (int sweep = 1; sweep < VillageExpansion.FULL_AFTER; sweep++) {
            assertFalse(VillageExpansion.sweptWithoutLot(colony));
        }

        assertTrue(VillageExpansion.sweptWithoutLot(colony));
    }
}
