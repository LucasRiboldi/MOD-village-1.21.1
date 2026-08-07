package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nenhuma detecção enxerga a vila inteira: o raio é 64 e a vila é maior.
 *
 * <p>Estes testes travam a regra que impede uma observação parcial de
 * arrastar o centro. O oscilar foi observado em jogo antes da regra
 * existir, alternando entre uma visão de 12 camas e outra de 3.
 */
class PartialObservationTest {

    private ColonyService service;

    @BeforeEach
    void setUp() {
        service = new ColonyService();
    }

    private static VillageCandidate seen(int x, int z, int beds) {
        return new VillageCandidate(new ColonyPos(x, 64, z), beds);
    }

    @Test
    void partialViewDoesNotMoveTheCenter() {
        service.adopt(seen(1109, 730, 12));

        Colony colony = service.adopt(seen(1080, 733, 3));

        assertEquals(new ColonyPos(1109, 64, 730), colony.center());
        assertEquals(12, colony.observedBeds());
    }

    /** A sequência exata observada no log de 2026-08-06. */
    @Test
    void centerStopsOscillating() {
        service.adopt(seen(1109, 730, 12));

        for (int i = 0; i < 5; i++) {
            service.adopt(seen(1080, 733, 3));
            service.adopt(seen(1109, 730, 12));
        }

        Colony colony = service.all().iterator().next();

        assertEquals(1, service.count());
        assertEquals(new ColonyPos(1109, 64, 730), colony.center());
    }

    @Test
    void betterViewMovesTheCenter() {
        service.adopt(seen(1080, 733, 3));

        Colony colony = service.adopt(seen(1109, 730, 12));

        assertEquals(new ColonyPos(1109, 64, 730), colony.center());
        assertEquals(12, colony.observedBeds());
    }

    /** A vila pode se mover mantendo o mesmo número de camas. */
    @Test
    void equallyCompleteViewMovesTheCenter() {
        service.adopt(seen(0, 0, 5));

        Colony colony = service.adopt(seen(20, 0, 5));

        assertEquals(new ColonyPos(20, 64, 0), colony.center());
    }

    @Test
    void firstObservationAlwaysCounts() {
        Colony colony = service.adopt(seen(0, 0, 3));

        assertEquals(new ColonyPos(0, 64, 0), colony.center());
        assertEquals(3, colony.observedBeds());
    }

    @Test
    void observeReportsWhetherItMoved() {
        Colony colony = service.adopt(seen(0, 0, 5));

        assertFalse(colony.observe(new ColonyPos(0, 64, 0), 5), "mesmo centro");
        assertTrue(colony.observe(new ColonyPos(9, 64, 0), 5), "centro novo");
        assertFalse(colony.observe(new ColonyPos(50, 64, 0), 2), "visão pior");
    }

    @Test
    void worseViewKeepsTheStoredBedCount() {
        Colony colony = service.adopt(seen(0, 0, 10));

        colony.observe(new ColonyPos(5, 64, 5), 2);

        assertEquals(10, colony.observedBeds());
    }
}
