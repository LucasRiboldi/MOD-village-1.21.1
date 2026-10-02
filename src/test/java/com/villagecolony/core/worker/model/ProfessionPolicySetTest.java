package com.villagecolony.core.worker.model;

import com.villagecolony.core.worker.service.ProfessionAssigner;
import com.villagecolony.core.worker.service.WorkerService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfessionPolicySetTest {

    @Test
    void defaultOrderProjectsToTheHistoricalOrders() {
        ProfessionPolicySet policies = ProfessionPolicySet.defaults();

        assertEquals(ProfessionAssigner.PRODUCER_ORDER,
                policies.orderFor(ProfessionAssigner.PRODUCER_ORDER));
        assertEquals(ProfessionAssigner.FOUNDATION_ORDER,
                policies.orderFor(ProfessionAssigner.FOUNDATION_ORDER));
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
}
