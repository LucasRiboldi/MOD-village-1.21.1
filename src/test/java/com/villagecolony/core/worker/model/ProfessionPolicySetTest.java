package com.villagecolony.core.worker.model;

import com.villagecolony.core.worker.service.ProfessionAssigner;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
