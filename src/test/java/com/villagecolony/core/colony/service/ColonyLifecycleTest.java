package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Detectar implica chunk carregado, e chunk carregado é a definição de
 * ACTIVE na ADR-002.
 */
class ColonyLifecycleTest {

    private static final ColonyPos CENTER = new ColonyPos(100, 64, 200);

    private ColonyService service;

    @BeforeEach
    void setUp() {
        service = new ColonyService();
    }

    /**
     * O defeito que motivou este teste: uma colônia lida do save nasce
     * DORMANT e nada a acordava. O loop de simulação, que só roda para
     * ACTIVE, ignoraria toda colônia persistida para sempre.
     */
    @Test
    void observingARestoredColonyWakesIt() {
        Colony restored = Colony.restore(
                UUID.randomUUID(), CENTER, ColonyState.PRODUCTION, ColonyLifecycle.DORMANT);

        service.register(restored);

        Colony adopted = service.adopt(new VillageCandidate(CENTER, 8));

        assertEquals(restored.id(), adopted.id());
        assertEquals(ColonyLifecycle.ACTIVE, adopted.lifecycle());
        assertTrue(adopted.isActive());
    }

    @Test
    void newlyDetectedColonyIsActive() {
        Colony colony = service.adopt(new VillageCandidate(CENTER, 5));

        assertEquals(ColonyLifecycle.ACTIVE, colony.lifecycle());
    }

    /** Acordar não pode apagar o que a colônia estava fazendo. */
    @Test
    void wakingPreservesTheColonyState() {
        Colony restored = Colony.restore(
                UUID.randomUUID(), CENTER, ColonyState.EXPANSION, ColonyLifecycle.DORMANT);

        service.register(restored);
        service.adopt(new VillageCandidate(CENTER, 8));

        assertEquals(ColonyState.EXPANSION, restored.state());
    }

    /** Observação pior não move o centro, mas ainda acorda a colônia. */
    @Test
    void worseObservationStillWakesTheColony() {
        Colony colony = service.adopt(new VillageCandidate(CENTER, 12));
        colony.setLifecycle(ColonyLifecycle.DORMANT);

        service.adopt(new VillageCandidate(new ColonyPos(110, 64, 205), 2));

        assertEquals(ColonyLifecycle.ACTIVE, colony.lifecycle());
        assertEquals(CENTER, colony.center());
    }
}
