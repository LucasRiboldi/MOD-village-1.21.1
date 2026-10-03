package com.villagecolony.core.worker.model;

import com.villagecolony.core.worker.service.ProfessionAssigner;
import com.villagecolony.core.worker.service.WorkerService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfessionPolicySetTest {

    @Test
    void defaultOrderProjectsToTheHistoricalOrders() {
        ProfessionPolicySet policies = ProfessionPolicySet.defaults();

        assertEquals(ProfessionAssigner.PRODUCER_ORDER,
                policies.orderFor(ProfessionAssigner.PRODUCER_ORDER));
        assertEquals(ProfessionAssigner.GROWTH_ORDER,
                policies.orderFor(ProfessionAssigner.GROWTH_ORDER));
        // A fundação não é projeção da ordem de contratação: o carpinteiro
        // logo depois do lenhador é decisão do autor de 2026-09-30, e o
        // ProfessionAssigner usa FOUNDATION_ORDER como está.
    }

    @Test
    void defaultPolicyDoesNotCapWorkersOrOverrideRadius() {
        ProfessionPolicy policy = ProfessionPolicySet.defaults().policyOf(ProfessionType.MINER);

        assertEquals(false, policy.limitsWorkers());
        assertEquals(false, policy.hasConfiguredRadius());
    }

    @Test
    void disabledProfessionIsNotAssignedToNewWorker() {
        ProfessionPolicySet defaults = ProfessionPolicySet.defaults();
        EnumMap<ProfessionType, ProfessionPolicy> policies = new EnumMap<>(defaults.policies());
        policies.put(ProfessionType.MINER, new ProfessionPolicy(false, 0, -1));
        ProfessionPolicySet configured = new ProfessionPolicySet(policies, defaults.hiringOrder());
        WorkerService workers = new WorkerService();
        UUID colony = UUID.randomUUID();
        var worker = workers.register(UUID.randomUUID(), colony);

        ProfessionAssigner.assignMissing(workers, colony, new HashSet<>(List.of(worker.villagerId())),
                1, ignored -> true, configured);

        assertEquals(ProfessionType.LUMBERJACK, worker.profession().orElseThrow());
    }

    @Test
    void maximumWorkerCapMovesTheNextGrowthSlotForward() {
        ProfessionPolicySet defaults = ProfessionPolicySet.defaults();
        EnumMap<ProfessionType, ProfessionPolicy> policies = new EnumMap<>(defaults.policies());
        policies.put(ProfessionType.MINER, new ProfessionPolicy(true, 1, -1));
        ProfessionPolicySet configured = new ProfessionPolicySet(policies, defaults.hiringOrder());
        WorkerService workers = new WorkerService();
        UUID colony = UUID.randomUUID();
        HashSet<UUID> ids = new HashSet<>();
        for (int index = 0; index < 16; index++) {
            ids.add(workers.register(UUID.randomUUID(), colony).villagerId());
        }

        ProfessionAssigner.assignMissing(workers, colony, ids, 16, ignored -> true, configured);

        long lumberjacks = workers.ofColony(colony).stream()
                .filter(worker -> worker.profession().filter(ProfessionType.LUMBERJACK::equals).isPresent())
                .count();
        assertEquals(2, lumberjacks);
    }

    @Test
    void disabledProducerReleasesItsGrowthSlotsToTheNextEnabledProfession() {
        ProfessionPolicySet defaults = ProfessionPolicySet.defaults();
        EnumMap<ProfessionType, ProfessionPolicy> policies = new EnumMap<>(defaults.policies());
        policies.put(ProfessionType.MINER, new ProfessionPolicy(false, 0, -1));
        ProfessionPolicySet configured = new ProfessionPolicySet(policies, defaults.hiringOrder());
        WorkerService workers = new WorkerService();
        UUID colony = UUID.randomUUID();
        HashSet<UUID> ids = new HashSet<>();
        for (int index = 0; index < 16; index++) {
            ids.add(workers.register(UUID.randomUUID(), colony).villagerId());
        }

        ProfessionAssigner.assignMissing(workers, colony, ids, 16, ignored -> true, configured);

        long lumberjacks = workers.ofColony(colony).stream()
                .filter(worker -> worker.profession().filter(ProfessionType.LUMBERJACK::equals).isPresent())
                .count();
        assertEquals(2, lumberjacks);
    }

    @Test
    void policyRejectsWorkerLimitsOutsideTheSafeRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProfessionPolicy(true, -1, ProfessionPolicy.AUTOMATIC_RADIUS));
        assertThrows(IllegalArgumentException.class,
                () -> new ProfessionPolicy(true, ProfessionPolicy.MAXIMUM_WORKERS_LIMIT + 1,
                        ProfessionPolicy.AUTOMATIC_RADIUS));
    }

    @Test
    void policySetRejectsSearchRadiusForAProfessionWithoutWorldSearch() {
        ProfessionPolicySet defaults = ProfessionPolicySet.defaults();
        EnumMap<ProfessionType, ProfessionPolicy> policies = new EnumMap<>(defaults.policies());
        policies.put(ProfessionType.MINER, new ProfessionPolicy(true, 0, 32));

        assertThrows(IllegalArgumentException.class,
                () -> new ProfessionPolicySet(policies, defaults.hiringOrder()));
    }
}
