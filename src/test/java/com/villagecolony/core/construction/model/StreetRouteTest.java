package com.villagecolony.core.construction.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O trajeto longo vai pela rede de ruas — autor, 2026-10-28, depois do jogo da 0.3.104 (escolha "as duas juntas"):
 * acima de {@link StreetRoute#LONG} blocos, o aldeão vai até a rua mais perto, anda por ela até o ponto mais perto do
 * destino e sai dali.
 */
class StreetRouteTest {

    /** Rua em L: de (0, 0) a (60, 0) e de (60, 0) a (60, 60). */
    private static Set<Long> elbow() {
        Set<Long> roads = new HashSet<>();

        for (int i = 0; i <= 60; i++) {
            roads.add(ColonyRoads.column(i, 0));
            roads.add(ColonyRoads.column(60, i));
        }

        return roads;
    }

    @Test
    void aShortTripGoesStraight() {
        assertEquals(Optional.empty(), StreetRoute.nextWaypoint(elbow(), 0, 3, 20, 3));
    }

    @Test
    void offTheStreetTheFirstStopIsTheNearestStreet() {
        Optional<Long> next = StreetRoute.nextWaypoint(elbow(), 5, 4, 63, 50);

        assertEquals(Optional.of(ColonyRoads.column(5, 0)), next);
    }

    @Test
    void onTheStreetItWalksALegAlongTheNetworkTowardTheExit() {
        Optional<Long> next = StreetRoute.nextWaypoint(elbow(), 5, 0, 63, 50);

        assertTrue(next.isPresent());
        assertEquals(ColonyRoads.column(5 + StreetRoute.LEG, 0), next.get());
    }

    @Test
    void aLegStopsAtTheExitWhenTheExitIsCloserThanALeg() {
        Optional<Long> next = StreetRoute.nextWaypoint(elbow(), 60, 30, 63, 58);

        assertEquals(Optional.of(ColonyRoads.column(60, 58)), next);
    }

    @Test
    void atTheExitItLeavesTheStreetForTheTarget() {
        assertEquals(Optional.empty(), StreetRoute.nextWaypoint(elbow(), 60, 50, 63, 50));
    }

    @Test
    void streetsThatDoNotMeetAreNotARoute() {
        Set<Long> roads = new HashSet<>();

        for (int i = 0; i <= 20; i++) {
            roads.add(ColonyRoads.column(i, 0));
            roads.add(ColonyRoads.column(i + 40, 40));
        }

        assertEquals(Optional.empty(), StreetRoute.nextWaypoint(roads, 0, 2, 60, 42));
    }

    @Test
    void aStreetThatGoesTooFarAroundIsNotARoute() {
        // Do (0, 2) ao (0, 40): direto são 38; pela rua, 60 + 60 + 60 + 19 até a saída em (0, 41) — mais de 2 vezes.
        Set<Long> roads = new HashSet<>();

        for (int i = 0; i <= 60; i++) {
            roads.add(ColonyRoads.column(i, 0));
            roads.add(ColonyRoads.column(60, i));
            roads.add(ColonyRoads.column(i, 60));
        }

        for (int z = 41; z <= 60; z++) {
            roads.add(ColonyRoads.column(0, z));
        }

        assertEquals(Optional.empty(), StreetRoute.nextWaypoint(roads, 0, 2, 0, 40));
    }

    @Test
    void farFromAnyStreetItGoesStraight() {
        assertEquals(Optional.empty(), StreetRoute.nextWaypoint(elbow(), 20, 30, 20, 90));
    }
}
