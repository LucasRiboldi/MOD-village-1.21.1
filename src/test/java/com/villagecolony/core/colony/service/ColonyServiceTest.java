package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColonyServiceTest {

    private static final ColonyPos ORIGIN = new ColonyPos(0, 64, 0);

    private ColonyService service;

    @BeforeEach
    void setUp() {
        service = new ColonyService();
    }

    @Test
    void startsEmpty() {
        assertEquals(0, service.count());
        assertTrue(service.all().isEmpty());
    }

    @Test
    void createColonyRegistersIt() {
        Colony colony = service.createColony(ORIGIN);

        assertEquals(1, service.count());
        assertEquals(Optional.of(colony), service.find(colony.id()));
    }

    @Test
    void createColonyGivesDistinctIds() {
        Colony one = service.createColony(ORIGIN);
        Colony other = service.createColony(ORIGIN);

        assertEquals(2, service.count());
        assertFalse(one.id().equals(other.id()));
    }

    @Test
    void createColonyRejectsNullCenter() {
        assertThrows(NullPointerException.class, () -> service.createColony(null));
    }

    @Test
    void registerAcceptsRestoredColony() {
        Colony restored = Colony.restore(
                UUID.randomUUID(), ORIGIN, ColonyState.EXPANSION, ColonyLifecycle.DORMANT);

        service.register(restored);

        assertSame(restored, service.find(restored.id()).orElseThrow());
    }

    /** Sobrescrever em silêncio esconderia save corrompido. */
    @Test
    void registerRejectsDuplicateId() {
        Colony colony = service.createColony(ORIGIN);

        assertThrows(IllegalStateException.class, () -> service.register(colony));
        assertEquals(1, service.count());
    }

    @Test
    void findMissingIdIsEmpty() {
        assertEquals(Optional.empty(), service.find(UUID.randomUUID()));
    }

    @Test
    void findNullIdIsEmpty() {
        assertEquals(Optional.empty(), service.find(null));
    }

    @Test
    void findNearestPicksTheClosest() {
        service.createColony(new ColonyPos(500, 64, 0));
        Colony near = service.createColony(new ColonyPos(30, 64, 0));
        service.createColony(new ColonyPos(-800, 64, 0));

        assertEquals(Optional.of(near), service.findNearest(ORIGIN, 100));
    }

    @Test
    void findNearestIgnoresColoniesOutsideRadius() {
        service.createColony(new ColonyPos(500, 64, 0));

        assertEquals(Optional.empty(), service.findNearest(ORIGIN, 100));
    }

    @Test
    void findNearestIncludesColonyExactlyOnRadius() {
        Colony onEdge = service.createColony(new ColonyPos(100, 64, 0));

        assertEquals(Optional.of(onEdge), service.findNearest(ORIGIN, 100));
    }

    /** Altura não deve afastar uma colônia: a distância é no plano. */
    @Test
    void findNearestIgnoresHeight() {
        Colony hilltop = service.createColony(new ColonyPos(10, 250, 0));

        assertEquals(Optional.of(hilltop), service.findNearest(ORIGIN, 20));
    }

    @Test
    void findNearestOnEmptyRegistryIsEmpty() {
        assertEquals(Optional.empty(), service.findNearest(ORIGIN, 1000));
    }

    @Test
    void findNearestWithNegativeRadiusIsEmpty() {
        service.createColony(ORIGIN);

        assertEquals(Optional.empty(), service.findNearest(ORIGIN, -1));
    }

    @Test
    void findNearestRejectsNullPosition() {
        assertThrows(NullPointerException.class, () -> service.findNearest(null, 100));
    }

    @Test
    void allKeepsInsertionOrder() {
        Colony first = service.createColony(new ColonyPos(1, 64, 0));
        Colony second = service.createColony(new ColonyPos(2, 64, 0));
        Colony third = service.createColony(new ColonyPos(3, 64, 0));

        assertEquals(List.of(first, second, third), List.copyOf(service.all()));
    }

    @Test
    void allIsUnmodifiable() {
        Colony colony = service.createColony(ORIGIN);

        assertThrows(
                UnsupportedOperationException.class,
                () -> service.all().remove(colony));
    }

    @Test
    void removeDropsTheColony() {
        Colony colony = service.createColony(ORIGIN);

        assertTrue(service.remove(colony.id()));
        assertEquals(0, service.count());
        assertEquals(Optional.empty(), service.find(colony.id()));
    }

    @Test
    void removeMissingIdReportsFalse() {
        assertFalse(service.remove(UUID.randomUUID()));
        assertFalse(service.remove(null));
    }

    @Test
    void clearEmptiesTheRegistry() {
        service.createColony(new ColonyPos(1, 64, 0));
        service.createColony(new ColonyPos(2, 64, 0));

        service.clear();

        assertEquals(0, service.count());
    }

    // ----------------------------------------------------------------
    // Uma observação por colônia por varredura — E2, 2026-08-11
    // ----------------------------------------------------------------

    /**
     * Uma varredura, dois aglomerados, a mesma colônia: vence o maior.
     *
     * <p>Vila grande e um punhado de camas a quarenta blocos — longe
     * demais para entrar no mesmo cluster, perto demais para ser outra
     * colônia. A varredura devolve os dois, e {@code findNearest} manda
     * os dois para a mesma colônia.
     *
     * <p>Sem esta escolha, os dois viravam observação: o primeiro
     * gravava a leitura da sonda e o segundo era confirmado por ela no
     * mesmo tick — a regra da sonda foi feita para ciclos sucessivos, e
     * nada exigia que fossem sucessivos. Ver §17, E2.
     */
    @Test
    void oneScanGivesEachColonyASingleObservation() {
        service.createColony(ORIGIN);

        VillageCandidate big = new VillageCandidate(ORIGIN, 28, false, ORIGIN);
        VillageCandidate small =
                new VillageCandidate(new ColonyPos(40, 64, 0), 5, false, ORIGIN);

        List<VillageCandidate> kept = service.bestPerColony(List.of(big, small));

        assertEquals(1, kept.size(), "a colônia recebeu duas leituras da mesma varredura");
        assertEquals(28, kept.get(0).bedCount(), "ficou com a leitura pobre");
    }

    /**
     * O aglomerado satélite não encolhe a colônia.
     *
     * <p>É o E2 inteiro em uma asserção: 31 camas registradas, uma
     * varredura que enxerga 28 da vila e 5 do vizinho, e a colônia
     * continua com 31 e no lugar em que estava.
     */
    @Test
    void aSatelliteClusterCannotShrinkTheColony() {
        Colony colony = service.createColony(ORIGIN);
        colony.observe(ORIGIN, 31);

        VillageCandidate big = new VillageCandidate(ORIGIN, 28, false, ORIGIN);
        VillageCandidate small =
                new VillageCandidate(new ColonyPos(40, 64, 0), 5, false, ORIGIN);

        for (VillageCandidate candidate : service.bestPerColony(List.of(big, small))) {
            service.adopt(candidate);
        }

        assertEquals(31, colony.observedBeds(), "a colônia encolheu numa varredura só");
        assertEquals(ORIGIN, colony.center(), "o centro pulou para o satélite");
    }

    /** A ordem em que a varredura devolve os aglomerados não importa. */
    @Test
    void theOrderOfTheClustersDoesNotMatter() {
        Colony colony = service.createColony(ORIGIN);
        colony.observe(ORIGIN, 31);

        VillageCandidate big = new VillageCandidate(ORIGIN, 28, false, ORIGIN);
        VillageCandidate small =
                new VillageCandidate(new ColonyPos(40, 64, 0), 5, false, ORIGIN);

        for (VillageCandidate candidate : service.bestPerColony(List.of(small, big))) {
            service.adopt(candidate);
        }

        assertEquals(31, colony.observedBeds());
    }

    /**
     * Vilas de colônias diferentes continuam passando as duas.
     *
     * <p>O agrupamento é por colônia, e não um "só o maior da
     * varredura": duas vilas distantes descobertas na mesma varredura
     * são duas colônias, e recusar uma delas faria a detecção perder
     * vila.
     */
    @Test
    void candidatesOfDifferentColoniesAllSurvive() {
        VillageCandidate here = new VillageCandidate(ORIGIN, 12, false, ORIGIN);
        VillageCandidate faraway =
                new VillageCandidate(new ColonyPos(500, 64, 0), 4, false, ORIGIN);

        assertEquals(2, service.bestPerColony(List.of(here, faraway)).size());
    }

    /** Sem colônia conhecida, todo candidato passa: cada um vira uma. */
    @Test
    void unknownCandidatesAreAllKept() {
        VillageCandidate one = new VillageCandidate(ORIGIN, 8, false, ORIGIN);
        VillageCandidate two =
                new VillageCandidate(new ColonyPos(400, 64, 0), 6, false, ORIGIN);

        assertEquals(2, service.bestPerColony(List.of(one, two)).size());
    }

    /** Após remover, o mesmo id pode ser registrado de novo. */
    @Test
    void removeThenRegisterIsAllowed() {
        Colony colony = service.createColony(ORIGIN);
        service.remove(colony.id());

        service.register(colony);

        assertEquals(1, service.count());
    }
}
