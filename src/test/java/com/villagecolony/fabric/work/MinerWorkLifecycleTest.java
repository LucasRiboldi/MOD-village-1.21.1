package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.task.service.TaskService;
import com.villagecolony.core.type.ResourceType;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinerWorkLifecycleTest {

    @AfterEach
    void clearSharedState() {
        MinerWork.clearAll();
        MineClaims.clearAll();
    }

    @Test
    void aClosedJobReleasesItsMineClaimOnTheNextTick() {
        UUID colony = UUID.randomUUID();
        UUID worker = UUID.randomUUID();
        Task task = new TaskService().create(
                colony,
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                1);
        task.reserveFor(worker);
        MinerWork.JOBS.put(worker, new MinerWork.Job(task, BlockPos.ORIGIN));
        assertTrue(MineClaims.claimArm(colony, worker, Mine.ARMS).isPresent());

        task.start();
        task.complete();
        MinerWork.tick(null);

        assertEquals(0, MinerWork.activeJobs(), "o job fechado continuou ativo");
        assertTrue(
                MineClaims.diggerIn(colony).isEmpty(),
                "a claim do job fechado continuou bloqueando a mina");
    }
}
