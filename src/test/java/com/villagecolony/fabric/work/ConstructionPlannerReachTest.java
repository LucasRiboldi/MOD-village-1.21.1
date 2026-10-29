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
 * crescimento: alcança os cantos e passa da borda, e cresce junto com ela. Com o
 * centro fixo (Regra 141), a conta parte do centro até o canto mais longe.
 */
class ConstructionPlannerReachTest {

    @AfterEach
    void restore() {
        ConstructionPlanner.restoreSearch();
    }

    private static VillageBounds square(int side) {
        return new VillageBounds(0, 60, 0, side - 1, 80, side - 1);
    }

    private static int reachFromTheMiddle(VillageBounds box) {
        return ConstructionPlanner.reachOf(box, new ColonyPos(box.centerX(), 64, box.centerZ()));
    }

    @Test
    void aTypicalVillageSearchesPastTheOld64() {
        // 144 de lado: metade da diagonal é 101,8 — os cantos, mais 15.
        assertEquals(117, reachFromTheMiddle(square(144)));
    }

    @Test
    void theSearchCoversTheCornersAndPassesTheEdge() {
        VillageBounds box = new VillageBounds(0, 60, 0, 199, 80, 99);
        double corner = Math.hypot(box.sizeX() / 2.0, box.sizeZ() / 2.0);

        int reach = reachFromTheMiddle(box);

        assertTrue(reach >= corner + VillageBounds.GROWTH_MARGIN,
                "o raio " + reach + " não passa do canto (" + corner + ") com a margem");
    }

    @Test
    void aSmallVillageNeverSearchesLessThanBefore() {
        assertEquals(VillageDetector.SEARCH_RADIUS, reachFromTheMiddle(square(40)));
    }

    @Test
    void thereIsNoCeilingTheSearchGrowsWithTheVillage() {
        int previous = 0;

        for (int side = 144; side <= 4_608; side *= 2) {
            int reach = reachFromTheMiddle(square(side));

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
        // Medida, a caixa de 144 vira 145 com o centro em 72: 72 para cada lado, 101,8 + 15.
        assertEquals(117, ConstructionPlanner.searchRadius(measured));
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
    void aBoxThatGrewToOneSideIsReachedFromTheFixedCenter() {
        // Regra 141: o centro fica em 72 e a caixa cresceu 100 a leste — o canto leste está a mais que
        // a metade da diagonal.
        VillageBounds grown = new VillageBounds(0, 60, 0, 243, 80, 143);
        int reach = ConstructionPlanner.reachOf(grown, new ColonyPos(72, 64, 72));

        assertTrue(reach >= Math.hypot(243 - 72, 72) + VillageBounds.GROWTH_MARGIN, "o raio não chega ao canto leste");
    }

    @Test
    void theTestsCanStillShortenTheSearch() {
        Colony measured = Colony.create(UUID.randomUUID(), new ColonyPos(72, 64, 72));
        measured.measure(square(144));

        ConstructionPlanner.shortenSearchTo(20);

        assertEquals(20, ConstructionPlanner.searchRadius(measured));
    }
}
