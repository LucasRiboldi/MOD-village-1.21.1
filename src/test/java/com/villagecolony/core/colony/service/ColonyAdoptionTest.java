package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Anti-duplicata da ADR-003 §6 e estabilidade de identidade da §4.
 */
class ColonyAdoptionTest {

    private ColonyService service;

    @BeforeEach
    void setUp() {
        service = new ColonyService();
    }

    private static VillageCandidate at(int x, int z) {
        return new VillageCandidate(new ColonyPos(x, 64, z), 5);
    }

    /** Leitura da sonda: a varredura ancorada no centro da colônia. */
    private static VillageCandidate probe(ColonyPos anchor, int x, int z) {
        return new VillageCandidate(new ColonyPos(x, 64, z), 5, false, anchor);
    }

    @Test
    void firstDetectionCreatesAColony() {
        Colony colony = service.adopt(at(0, 0));

        assertEquals(1, service.count());
        assertEquals(new ColonyPos(0, 64, 0), colony.center());
    }

    /** Reentrar na área não pode criar uma segunda colônia. */
    @Test
    void detectingTheSameVillageAgainDoesNotDuplicate() {
        Colony first = service.adopt(at(0, 0));
        Colony second = service.adopt(at(0, 0));

        assertEquals(1, service.count());
        assertSame(first, second);
    }

    /**
     * O centro se move, o UUID não.
     *
     * <p>Quem o move é a sonda, desde a Emenda 4 da ADR-003. A
     * varredura que não é sonda continua achando a mesma colônia — que
     * é o que a §4 promete — e deixa a posição onde está.
     */
    @Test
    void centerMovesButIdentityHolds() {
        Colony first = service.adopt(at(0, 0));
        java.util.UUID originalId = first.id();

        Colony updated = service.adopt(probe(first.center(), 20, 0));

        assertEquals(1, service.count());
        assertEquals(originalId, updated.id());
        assertEquals(new ColonyPos(20, 64, 0), updated.center());
    }

    /** A mesma varredura, sem âncora: mesma colônia, mesmo lugar. */
    @Test
    void aScanThatIsNotAProbeKeepsTheCenterWhereItIs() {
        Colony first = service.adopt(at(0, 0));

        Colony updated = service.adopt(at(20, 0));

        assertEquals(1, service.count());
        assertEquals(first.id(), updated.id());
        assertEquals(new ColonyPos(0, 64, 0), updated.center());
    }

    @Test
    void villageJustInsideDuplicateDistanceUpdates() {
        Colony first = service.adopt(at(0, 0));

        Colony second = service.adopt(at(VillageDetector.DUPLICATE_DISTANCE, 0));

        assertEquals(1, service.count());
        assertEquals(first.id(), second.id());
    }

    @Test
    void villageBeyondDuplicateDistanceBecomesANewColony() {
        Colony first = service.adopt(at(0, 0));

        Colony second = service.adopt(at(VillageDetector.DUPLICATE_DISTANCE + 1, 0));

        assertEquals(2, service.count());
        assertNotEquals(first.id(), second.id());
    }

    /** Com duas colônias próximas, a adoção escolhe a mais perto. */
    @Test
    void adoptUpdatesTheNearestColony() {
        Colony far = service.adopt(at(0, 0));
        Colony near = service.adopt(at(1000, 0));

        Colony updated = service.adopt(at(1010, 0));

        assertEquals(2, service.count());
        assertEquals(near.id(), updated.id());
        assertNotEquals(far.id(), updated.id());
    }

    @Test
    void adoptRejectsNull() {
        assertThrows(NullPointerException.class, () -> service.adopt(null));
    }

    @Test
    void candidateRejectsNonPositiveBedCount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VillageCandidate(new ColonyPos(0, 64, 0), 0));
    }

    @Test
    void candidateRejectsNullCenter() {
        assertThrows(NullPointerException.class, () -> new VillageCandidate(null, 3));
    }
}
