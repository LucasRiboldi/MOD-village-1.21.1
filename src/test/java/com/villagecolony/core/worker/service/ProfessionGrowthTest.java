package com.villagecolony.core.worker.service;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfessionGrowthTest {

    private static final UUID COLONY = UUID.randomUUID();

    private WorkerService workers;

    @BeforeEach
    void setUp() {
        workers = new WorkerService();
    }

    @Test
    void sevenAdultsReceiveFoundationThenTheFirstGrowthRole() {
        addWorkers(7);

        assertEquals(7, assign(7));
        java.util.List<ProfessionType> assigned = assignedInOrder();
        int foundationSize = ProfessionAssigner.FOUNDATION_ORDER.size();
        assertEquals(ProfessionAssigner.FOUNDATION_ORDER,
                assigned.subList(0, foundationSize));
        assertEquals(ProfessionType.CARPENTER, assigned.get(foundationSize));
    }

    /** Agricultor e carpinteiro continuam sendo vagas reais de crescimento. */
    @Test
    void farmerAndCarpenterRemainAvailableOutsideTheFoundation() {
        addWorkers(13);

        assign(13);

        assertTrue(ProfessionAssigner.PRODUCER_ORDER.contains(ProfessionType.CARPENTER));
        assertTrue(ProfessionAssigner.PRODUCER_ORDER.contains(ProfessionType.FARMER));
        assertFalse(ProfessionAssigner.FOUNDATION_ORDER.contains(ProfessionType.CARPENTER));
        assertFalse(ProfessionAssigner.FOUNDATION_ORDER.contains(ProfessionType.FARMER));
        assertTrue(count(ProfessionType.CARPENTER) >= 1,
                "o carpinteiro foi removido do crescimento");
        assertTrue(count(ProfessionType.FARMER) >= 1,
                "o agricultor foi removido do crescimento");
    }

    @Test
    void populationFifteenKeepsOneOfEachAndSixteenAddsTheSecondMiner() {
        addWorkers(16);

        assign(15);
        assertEquals(8, employedCount());

        assertEquals(1, assign(16));
        assertEquals(2, count(ProfessionType.MINER));
        assertEquals(1, count(ProfessionType.LUMBERJACK));
    }

    @Test
    void populationThirtyHasTwoOfEachAndAdultThirtyOneAddsThirdMiner() {
        addWorkers(31);

        assign(30);
        assertEquals(15, employedCount());
        assertEquals(2, count(ProfessionType.BREEDER));

        assertEquals(1, assign(31));
        assertEquals(3, count(ProfessionType.MINER));
        assertEquals(2, count(ProfessionType.LUMBERJACK));
    }

    @Test
    void adultThirtyTwoAddsThirdLumberjack() {
        addWorkers(32);

        assign(30);
        assign(31);
        assign(32);

        assertEquals(3, count(ProfessionType.LUMBERJACK));
        assertEquals(2, count(ProfessionType.MASON));
    }

    @Test
    void existingProfessionIsNeverRemovedWhenPopulationFalls() {
        addWorkers(31);
        assign(31);

        assertTrue(ProfessionAssigner.enforceVacancies(workers, COLONY).isEmpty());
        assertEquals(3, count(ProfessionType.MINER));
    }

    @Test
    void legacyShepherdCountsTowardTheBreederQuota() {
        Worker legacyShepherd = workers.register(UUID.randomUUID(), COLONY);
        legacyShepherd.assign(ProfessionType.SHEPHERD);
        addWorkers(15);

        assertEquals(7, assign(15));
        assertEquals(0, count(ProfessionType.BREEDER));
        assertEquals(1, count(ProfessionType.SHEPHERD));
    }

    private void addWorkers(int count) {
        for (int i = 0; i < count; i++) {
            workers.register(UUID.randomUUID(), COLONY);
        }
    }

    private int assign(int adults) {
        Set<UUID> employable = new HashSet<>();
        for (Worker worker : workers.ofColony(COLONY)) {
            employable.add(worker.villagerId());
        }
        return ProfessionAssigner.assignMissing(workers, COLONY, employable, adults);
    }

    private int count(ProfessionType type) {
        return (int) workers.ofColony(COLONY).stream()
                .filter(worker -> worker.profession().filter(type::equals).isPresent())
                .count();
    }

    private int employedCount() {
        return (int) workers.ofColony(COLONY).stream().filter(Worker::hasProfession).count();
    }

    private java.util.List<ProfessionType> assignedInOrder() {
        return workers.ofColony(COLONY).stream()
                .map(worker -> worker.profession().orElseThrow())
                .toList();
    }
}
