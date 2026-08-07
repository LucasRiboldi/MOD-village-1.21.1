package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillageDetectorTest {

    private VillageDetector detector;

    @BeforeEach
    void setUp() {
        detector = new VillageDetector();
    }

    private static ColonyPos bed(int x, int z) {
        return new ColonyPos(x, 64, z);
    }

    private static List<ColonyPos> beds(int count, int spacing) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> bed(i * spacing, 0))
                .toList();
    }

    // --- clusterização ---

    @Test
    void noBedsMeansNoClusters() {
        assertTrue(detector.cluster(List.of()).isEmpty());
    }

    @Test
    void nearbyBedsFormOneCluster() {
        List<List<ColonyPos>> clusters = detector.cluster(
                List.of(bed(0, 0), bed(10, 0), bed(20, 0)));

        assertEquals(1, clusters.size());
        assertEquals(3, clusters.get(0).size());
    }

    @Test
    void distantBedsFormSeparateClusters() {
        List<List<ColonyPos>> clusters = detector.cluster(
                List.of(bed(0, 0), bed(500, 0)));

        assertEquals(2, clusters.size());
    }

    /** A-B e B-C juntam as três, mesmo com A-C além do limite. */
    @Test
    void clusteringIsTransitive() {
        List<List<ColonyPos>> clusters = detector.cluster(
                List.of(bed(0, 0), bed(30, 0), bed(60, 0)));

        assertEquals(1, clusters.size());
        assertEquals(3, clusters.get(0).size());
    }

    @Test
    void bedExactlyOnClusterDistanceJoins() {
        List<List<ColonyPos>> clusters = detector.cluster(
                List.of(bed(0, 0), bed(VillageDetector.CLUSTER_DISTANCE, 0)));

        assertEquals(1, clusters.size());
    }

    @Test
    void bedOneBlockBeyondClusterDistanceSplits() {
        List<List<ColonyPos>> clusters = detector.cluster(
                List.of(bed(0, 0), bed(VillageDetector.CLUSTER_DISTANCE + 1, 0)));

        assertEquals(2, clusters.size());
    }

    /** Altura não separa vila: a cama do sótão é da mesma casa. */
    @Test
    void heightDoesNotSplitCluster() {
        List<List<ColonyPos>> clusters = detector.cluster(
                List.of(new ColonyPos(0, 64, 0), new ColonyPos(2, 200, 0)));

        assertEquals(1, clusters.size());
    }

    @Test
    void everyBedLandsInExactlyOneCluster() {
        List<ColonyPos> all = List.of(
                bed(0, 0), bed(10, 0), bed(500, 0), bed(510, 0), bed(1000, 0));

        int total = detector.cluster(all).stream().mapToInt(List::size).sum();

        assertEquals(all.size(), total);
    }

    // --- validação ---

    @Test
    void tooFewBedsIsNotAVillage() {
        List<ColonyPos> cluster = beds(VillageDetector.MIN_BEDS - 1, 5);

        assertEquals(Optional.empty(), detector.evaluate(cluster, 10, Optional.empty()));
    }

    @Test
    void tooFewVillagersIsNotAVillage() {
        List<ColonyPos> cluster = beds(VillageDetector.MIN_BEDS, 5);

        assertEquals(
                Optional.empty(),
                detector.evaluate(cluster, VillageDetector.MIN_VILLAGERS - 1, Optional.empty()));
    }

    @Test
    void minimumBedsAndVillagersQualify() {
        List<ColonyPos> cluster = beds(VillageDetector.MIN_BEDS, 5);

        Optional<VillageCandidate> candidate =
                detector.evaluate(cluster, VillageDetector.MIN_VILLAGERS, Optional.empty());

        assertTrue(candidate.isPresent());
        assertEquals(VillageDetector.MIN_BEDS, candidate.orElseThrow().bedCount());
    }

    @Test
    void emptyClusterIsNotAVillage() {
        assertEquals(Optional.empty(), detector.evaluate(List.of(), 10, Optional.empty()));
    }

    // --- centro ---

    @Test
    void centerIsTheAverageOfBeds() {
        List<ColonyPos> cluster = List.of(bed(0, 0), bed(10, 0), bed(20, 0));

        ColonyPos center = detector.evaluate(cluster, 5, Optional.empty()).orElseThrow().center();

        assertEquals(bed(10, 0), center);
    }

    /** O sino é o centro social real da vila Vanilla. */
    @Test
    void meetingPointOverridesTheAverage() {
        List<ColonyPos> cluster = List.of(bed(0, 0), bed(10, 0), bed(20, 0));
        ColonyPos bell = bed(7, 3);

        ColonyPos center = detector.evaluate(cluster, 5, Optional.of(bell)).orElseThrow().center();

        assertEquals(bell, center);
    }

    /** Somar 64 camas em coordenada extrema estoura int. */
    @Test
    void centerSurvivesExtremeCoordinates() {
        List<ColonyPos> cluster = List.of(
                new ColonyPos(29_999_990, 64, 29_999_990),
                new ColonyPos(29_999_995, 64, 29_999_995),
                new ColonyPos(30_000_000, 64, 30_000_000));

        ColonyPos center = detector.evaluate(cluster, 5, Optional.empty()).orElseThrow().center();

        assertEquals(new ColonyPos(29_999_995, 64, 29_999_995), center);
    }

    // --- constantes da ADR-003 §8 ---

    @Test
    void constantsMatchTheDecisionRecord() {
        assertEquals(64, VillageDetector.SEARCH_RADIUS);
        assertEquals(32, VillageDetector.CLUSTER_DISTANCE);
        assertEquals(3, VillageDetector.MIN_BEDS);
        assertEquals(2, VillageDetector.MIN_VILLAGERS);
        assertEquals(64, VillageDetector.DUPLICATE_DISTANCE);
        assertEquals(600, VillageDetector.CYCLE_TICKS);
    }
}
