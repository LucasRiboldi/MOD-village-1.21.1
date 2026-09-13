package com.villagecolony.fabric.work;

import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.task.service.TaskService;
import com.villagecolony.core.type.ResourceType;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinerRequestedResourceTest {

    @Test
    void sandTaskCountsSandInsteadOfTheVillageStone() {
        UUID colony = UUID.randomUUID();
        Task task = new TaskService().create(
                colony,
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.SAND,
                3);

        MinerWork.Job job = new MinerWork.Job(task, new BlockPos(0, 64, 0));

        assertEquals(
                ResourceType.SAND,
                job.wanted,
                "the miner never fills its sand order if progress tracks the village stone");
    }
}
