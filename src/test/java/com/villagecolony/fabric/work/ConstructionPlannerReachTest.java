package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A busca de lote cresce com a vila, sem teto — 2026-10-01, pedido do autor:
 * <i>"a vila deve crescer infinitamente para qualquer lado possível"</i>.
 *
 * <p>Era 64 do centro, fixo; com o centro no meio da caixa, a vila parava em
 * uns 64 para cada lado. Agora é metade da diagonal da caixa mais a margem de
 * crescimento: alcança os cantos e passa da borda, e cresce junto com ela.
 */
class ConstructionPlannerReachTest {

    @AfterEach
    void restore() {
        ConstructionPlanner.restoreSearch();
    }

    private static VillageBounds square(int side) {
        return new VillageBounds(0, 60, 0, side - 1, 80, side - 1);
    }

    @Test
    void aTypicalVillageSearchesPastTheOld64() {
        // 144 de lado: metade da diagonal é 101,8 — os cantos, mais 15.
        assertEquals(117, ConstructionPlanner.reachOf(square(144)));
    }

    @Test
    void theSearchCoversTheCornersAndPassesTheEdge() {
        VillageBounds box = new VillageBounds(0, 60, 0, 199, 80, 99);
        double corner = Math.hypot(box.sizeX() / 2.0, box.sizeZ() / 2.0);

        int reach = ConstructionPlanner.reachOf(box);

        assertTrue(reach >= corner + VillageBounds.GROWTH_MARGIN,
                "o raio " + reach + " não passa do canto (" + corner + ") com a margem");
    }

    @Test
    void aSmallVillageNeverSearchesLessThanBefore() {
        assertEquals(VillageDetector.SEARCH_RADIUS, ConstructionPlanner.reachOf(square(40)));
    }

    @Test
    void thereIsNoCeilingTheSearchGrowsWithTheVillage() {
        int previous = 0;

        for (int side = 144; side <= 4_608; side *= 2) {
            int reach = ConstructionPlanner.reachOf(square(side));

            assertTrue(reach > previous, "o raio parou de crescer numa vila de " + side);
            previous = reach;
        }

        // Uma vila de 4.608 de lado (144 dobrado cinco vezes) busca a mais de 2.900 do centro.
        assertTrue(previous > 2_900, "teto escondido: " + previous);
    }

    @Test
    void aMeasuredVillageUsesItsBoxAndAnUnmeasuredOneKeeps64() {
        Colony unmeasured = Colony.create(UUID.randomUUID(), new ColonyPos(72, 64, 72));
        Colony measured = Colony.create(UUID.randomUUID(), new ColonyPos(72, 64, 72));

        measured.measure(square(144));

        assertEquals(VillageDetector.SEARCH_RADIUS, ConstructionPlanner.searchRadius(unmeasured));
        // Medida, a caixa de 144 vira 145 (centro num bloco só): 102,5 + 15.
        assertEquals(118, ConstructionPlanner.searchRadius(measured));
    }

    @Test
    void theVillageGrowingMakesTheSearchGrow() {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(72, 64, 72));

        colony.measure(square(144));
        int before = ConstructionPlanner.searchRadius(colony);

        // Uma construção perto da borda leste empurra a caixa (Emenda 6).
        colony.grow(VillageBounds.aroundPiece(new ColonyPos(150, 64, 70), new ColonyPos(160, 70, 80)));

        assertTrue(ConstructionPlanner.searchRadius(colony) > before,
                "a vila cresceu e a busca não acompanhou: " + before + " -> "
                        + ConstructionPlanner.searchRadius(colony));
    }

    @Test
    void theTestsCanStillShortenTheSearch() {
        Colony measured = Colony.create(UUID.randomUUID(), new ColonyPos(72, 64, 72));
        measured.measure(square(144));

        ConstructionPlanner.shortenSearchTo(20);

        assertEquals(20, ConstructionPlanner.searchRadius(measured));
    }
}
