package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.ColonySweepCursor;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.SweepPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra 373 (notas do jogo da 0.4.145, J2 🅲: "por que este espaço todo livre da vila não teve construção?", nove
 * vezes): a plantação sem lote ao alcance do fazendeiro espera a busca da casa terminar. Antes ela voltava a cada
 * minuto, achava o mesmo lote longe e apagava o cursor da busca da casa, que recomeçava do centro e nunca acabava.
 */
class FarmPostponeTest {

    private final UUID colony = UUID.randomUUID();

    @AfterEach
    void forget() {
        BuildSiteScanner.clear(colony);
    }

    @Test
    @DisplayName("Regra 373: a plantação adiada espera a busca da casa terminar")
    void theFarmWaitsWhileTheHouseSweepIsRunning() {
        FarmPlans.postpone(colony, 1_000);
        SweepPersistence.restore(new ColonySweepCursor(colony, new ColonyPos(0, 64, 0), 3, 10, List.of()));

        long later = 1_000 + VillageDetector.CYCLE_TICKS * 3L;

        assertTrue(FarmPlans.postponed(colony, later),
                "Regra 373: a plantação voltou no meio da busca da casa e apagaria o cursor dela");

        BuildSiteScanner.clear(colony);

        assertFalse(FarmPlans.postponed(colony, later), "a plantação ficou adiada com a busca da casa terminada");
    }
}
