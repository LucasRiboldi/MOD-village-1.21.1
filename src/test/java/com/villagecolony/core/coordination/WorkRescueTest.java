package com.villagecolony.core.coordination;

import com.villagecolony.fabric.work.MinerWork;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O socorro contra parada tem um prazo só para os ofícios que andam — F4, 2026-10-13. */
class WorkRescueTest {

    @Test
    void theRescueLimitIsOneMinuteAndTheMinerSharesIt() {
        assertEquals(1_200, WorkRescue.LIMIT, "um minuto de expediente andando sem chegar");
        assertEquals(WorkRescue.LIMIT, MinerWork.STALL_LIMIT, "o mineiro usa o prazo único");
    }
}
