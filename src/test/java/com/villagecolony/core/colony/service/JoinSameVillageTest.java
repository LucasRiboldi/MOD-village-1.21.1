package com.villagecolony.core.colony.service;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A vila espalhada não se parte: camas da mesma vila gerada são um aglomerado só (autor, 2026-10-20). */
class JoinSameVillageTest {

    private final VillageDetector detector = new VillageDetector();

    private static ColonyPos bed(int x, int z) {
        return new ColonyPos(x, 64, z);
    }

    /** A vila de deserto da bancada: 1 + 2 + 2 camas, pedaços a mais de 32 blocos uns dos outros. */
    private static final List<ColonyPos> DESERT = List.of(
            bed(-2532, 4660),
            bed(-2476, 4662), bed(-2474, 4664),
            bed(-2497, 4709), bed(-2495, 4711));

    @Test
    void theDesertVillageOfTheSimulationFallsApartByDistanceAlone() {
        // O defeito: pela distância, nenhum pedaço chega a MIN_BEDS.
        List<List<ColonyPos>> byDistance = detector.cluster(DESERT);

        assertEquals(3, byDistance.size());
        assertTrue(byDistance.stream().allMatch(cluster -> cluster.size() < VillageDetector.MIN_BEDS));
    }

    @Test
    void bedsOfTheSameGeneratedVillageJoinIntoOneCluster() {
        List<List<ColonyPos>> joined = VillageDetector.joinSameVillage(
                detector.cluster(DESERT), each -> Optional.of("village_desert"));

        assertEquals(1, joined.size());
        assertEquals(5, joined.get(0).size());
        assertTrue(detector.rejectionOf(joined.get(0), 7).isEmpty(), "a vila juntada ainda é recusada");
    }

    @Test
    void twoGeneratedVillagesStayApart() {
        Function<ColonyPos, Optional<String>> villageOf =
                each -> Optional.of(each.x() < -2490 ? "west" : "east");

        List<List<ColonyPos>> joined = VillageDetector.joinSameVillage(detector.cluster(DESERT), villageOf);

        assertEquals(2, joined.size());
    }

    @Test
    void aClusterWithoutAGeneratedVillageKeepsTheDistanceRule() {
        List<List<ColonyPos>> byDistance = detector.cluster(DESERT);

        List<List<ColonyPos>> joined = VillageDetector.joinSameVillage(byDistance, each -> Optional.<String>empty());

        assertEquals(byDistance, joined);
    }

    @Test
    void aPlayerBedNextToTheVillageJoinsByDistanceAndTheVillageBySameStructure() {
        // A cama do jogador encostada na vila entra pela distância; o pedaço de longe, pela estrutura.
        List<ColonyPos> beds = List.of(bed(0, 0), bed(10, 0), bed(20, 0), bed(100, 0));
        Function<ColonyPos, Optional<String>> villageOf =
                each -> each.x() == 20 ? Optional.empty() : Optional.of("v");

        List<List<ColonyPos>> joined = VillageDetector.joinSameVillage(detector.cluster(beds), villageOf);

        assertEquals(1, joined.size());
        assertEquals(4, joined.get(0).size());
    }
}
