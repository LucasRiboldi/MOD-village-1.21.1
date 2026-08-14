package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.ClusterRejection;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O que a detecção diz quando recusa um aglomerado.
 *
 * <p>A recusa existe para que {@code ColonyState.ABANDONED} tenha quem o
 * atribua: sem ela, "a vila deixou de ser viável" e "a vila não foi
 * observada" chegavam iguais ao lugar que decide. Ver ADR-003 §6.
 */
class ClusterRejectionTest {

    private VillageDetector detector;

    @BeforeEach
    void setUp() {
        detector = new VillageDetector();
    }

    private static ColonyPos bed(int x, int z) {
        return new ColonyPos(x, 64, z);
    }

    private static List<ColonyPos> beds(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> bed(i * 2, 0))
                .toList();
    }

    @Test
    void aRealVillageIsNotRejected() {
        assertTrue(detector.rejectionOf(beds(VillageDetector.MIN_BEDS),
                VillageDetector.MIN_VILLAGERS).isEmpty());
    }

    @Test
    void tooFewBedsIsRejectedAsACamp() {
        Optional<ClusterRejection> rejection =
                detector.rejectionOf(beds(VillageDetector.MIN_BEDS - 1), 10);

        assertTrue(rejection.isPresent());
        assertEquals(ClusterRejection.Reason.TOO_FEW_BEDS, rejection.get().reason());
    }

    /** Casas de sobra e ninguém dentro: vila esvaziada, não acampamento. */
    @Test
    void enoughBedsWithoutPeopleIsAnEmptyVillage() {
        Optional<ClusterRejection> rejection =
                detector.rejectionOf(beds(VillageDetector.MIN_BEDS), 0);

        assertTrue(rejection.isPresent());
        assertEquals(ClusterRejection.Reason.TOO_FEW_VILLAGERS, rejection.get().reason());
        assertEquals(0, rejection.get().villagerCount());
    }

    /**
     * Camas de menos vence a contagem de aldeões.
     *
     * <p>Duas camas sem ninguém em volta é acampamento. Chamá-lo de "vila
     * sem população" daria a entender que houve vila ali.
     */
    @Test
    void tooFewBedsWinsOverTooFewVillagers() {
        Optional<ClusterRejection> rejection = detector.rejectionOf(beds(1), 0);

        assertEquals(ClusterRejection.Reason.TOO_FEW_BEDS, rejection.orElseThrow().reason());
    }

    /** Raio sem cama alguma não afirma nada sobre vila nenhuma. */
    @Test
    void noBedsIsNotARejection() {
        assertTrue(detector.rejectionOf(List.of(), 0).isEmpty());
    }

    @Test
    void theRejectionSaysWhereTheClusterWas() {
        ClusterRejection rejection = detector
                .rejectionOf(List.of(bed(0, 0), bed(10, 0)), 5)
                .orElseThrow();

        assertEquals(bed(5, 0), rejection.center());
        assertEquals(2, rejection.bedCount());
    }

    /**
     * Aprovar e recusar não podem discordar.
     *
     * <p>São a mesma regra da ADR-003 §3, e duas cópias dela divergiriam
     * no dia em que alguém mexesse numa só.
     */
    @Test
    void everyClusterIsEitherAcceptedOrRejected() {
        for (int bedCount = 0; bedCount <= VillageDetector.MIN_BEDS + 1; bedCount++) {
            for (int villagers = 0; villagers <= VillageDetector.MIN_VILLAGERS + 1; villagers++) {
                List<ColonyPos> cluster = beds(bedCount);

                boolean accepted = detector
                        .evaluate(cluster, villagers, Optional.empty(), Optional.empty())
                        .isPresent();

                boolean rejected = detector.rejectionOf(cluster, villagers).isPresent();

                // O aglomerado vazio é o único que não é nem uma coisa
                // nem outra: não há o que aprovar nem lugar de que falar.
                if (cluster.isEmpty()) {
                    assertTrue(!accepted && !rejected, "aglomerado vazio afirmou algo");

                    continue;
                }

                assertEquals(accepted, !rejected,
                        bedCount + " camas e " + villagers + " aldeões");
            }
        }
    }

    @Test
    void uncountedVillagersAreNotReportedAsZero() {
        ClusterRejection rejection = detector
                .rejectionOf(beds(1), ClusterRejection.VILLAGERS_NOT_COUNTED)
                .orElseThrow();

        assertEquals("not counted", rejection.villagersAsText());
    }
}
