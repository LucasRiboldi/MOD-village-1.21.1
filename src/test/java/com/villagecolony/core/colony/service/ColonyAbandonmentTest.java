package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quando a colônia deixa de ter vila — e quando volta a ter.
 *
 * <p>ADR-003 §6. A colônia não é apagada: apagá-la destruiria o registro
 * de Buildings, contra o PROJECT_CONSTITUTION.md §10. Ela é marcada.
 */
class ColonyAbandonmentTest {

    private static final ColonyPos CENTER = new ColonyPos(0, 64, 0);

    private static Colony colonyAt(ColonyPos center) {
        return Colony.create(UUID.randomUUID(), center);
    }

    private static VillageCandidate villageAt(ColonyPos center) {
        return new VillageCandidate(center, VillageDetector.MIN_BEDS);
    }

    @Test
    void aColonyThatStillSeesItsVillageIsNotTouched() {
        Colony colony = colonyAt(CENTER);

        assertTrue(ColonyAbandonment.judge(
                colony, CENTER, List.of(villageAt(CENTER)), false).isEmpty());
    }

    @Test
    void aColonyWhoseProbeFindsNothingIsAbandoned() {
        Colony colony = colonyAt(CENTER);

        assertEquals(
                Optional.of(ColonyState.ABANDONED),
                ColonyAbandonment.judge(colony, CENTER, List.of(), false));
    }

    /**
     * O veredito se desfaz sozinho.
     *
     * <p>É por isso que ele não exige confirmação em dois ciclos, ao
     * contrário do encolhimento: nada se perde, e a primeira varredura
     * que enxergar vila devolve a colônia ao ponto de partida.
     */
    @Test
    void anAbandonedColonyComesBackWhenTheVillageDoes() {
        Colony colony = colonyAt(CENTER);
        colony.setState(ColonyState.ABANDONED);

        assertEquals(
                Optional.of(ColonyState.STABLE),
                ColonyAbandonment.judge(colony, CENTER, List.of(villageAt(CENTER)), false));
    }

    /** Uma vez marcada, não se marca de novo a cada ciclo. */
    @Test
    void anAbandonedColonyIsNotAnnouncedTwice() {
        Colony colony = colonyAt(CENTER);
        colony.setState(ColonyState.ABANDONED);

        assertTrue(ColonyAbandonment.judge(colony, CENTER, List.of(), false).isEmpty());
    }

    /**
     * Bioma recusado não é vila morta.
     *
     * <p>ADR-003 §5: aglomerado fora de PLAINS é ignorado, e isso é
     * limite do MVP. Um centro que caminhou para a borda do bioma
     * condenaria uma vila cheia de gente.
     */
    @Test
    void aClusterIgnoredByBiomeDoesNotCondemnTheColony() {
        Colony colony = colonyAt(CENTER);

        assertTrue(ColonyAbandonment.judge(colony, CENTER, List.of(), true).isEmpty());
    }

    /** Vila de outra colônia, longe demais, não conta como esta. */
    @Test
    void aVillageBeyondTheDuplicateDistanceIsSomebodyElses() {
        Colony colony = colonyAt(CENTER);

        ColonyPos faraway = new ColonyPos(VillageDetector.DUPLICATE_DISTANCE + 10, 64, 0);

        assertEquals(
                Optional.of(ColonyState.ABANDONED),
                ColonyAbandonment.judge(colony, CENTER, List.of(villageAt(faraway)), false));
    }

    /**
     * A pergunta é sobre de onde a sonda partiu, não sobre onde a colônia
     * está agora.
     *
     * <p>A adoção move centros dentro do mesmo ciclo. Medir do centro
     * novo compararia a leitura com um ponto que não existia quando ela
     * foi feita.
     */
    @Test
    void theProbeOriginIsWhatCounts() {
        Colony colony = colonyAt(CENTER);

        ColonyPos probedFrom = new ColonyPos(1000, 64, 1000);

        assertEquals(
                Optional.of(ColonyState.ABANDONED),
                ColonyAbandonment.judge(colony, probedFrom, List.of(villageAt(CENTER)), false));
    }

    /**
     * <b>Colônia sem vila não planeja obra</b> — 2026-09-02.
     *
     * <p>O planejamento é onde moram a varredura de lote e o crescimento
     * de rua, e a varredura tem teto de mil e vinte e quatro colunas por
     * passagem. Colônia abandonada cobrava isso por ciclo sem ter o que
     * construir.
     */
    @Test
    void anAbandonedColonyDoesNotPlanConstruction() {
        Colony colony = colonyAt(CENTER);

        colony.setState(ColonyState.ABANDONED);

        assertFalse(
                ColonyAbandonment.plansConstruction(colony),
                "a colônia abandonada continua pagando a varredura de lote");
    }

    /** E a que tem vila continua planejando, que é o caso normal. */
    @Test
    void aColonyWithAVillageStillPlans() {
        assertTrue(ColonyAbandonment.plansConstruction(colonyAt(CENTER)));
    }
}
