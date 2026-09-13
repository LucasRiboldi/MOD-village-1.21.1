package com.villagecolony.fabric.work;

import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.integration.TreeHarvester;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeChoiceTest {

    @Test
    void anUntouchedTreeIsMarkedOutOfReachWhenTheLumberjackGivesUp() {
        LumberjackWork.Job job = jobWithTree();

        assertTrue(
                TreeChoice.shouldMarkUnreachable(job),
                "árvore intacta precisa sair da escolha para o próximo lenhador não travar nela");
    }

    @Test
    void aPartlyCutTreeIsNotMarkedOutOfReach() {
        LumberjackWork.Job job = jobWithTree();

        job.index = 1;

        assertFalse(
                TreeChoice.shouldMarkUnreachable(job),
                "árvore já cortada pela metade não pode ficar em castigo longo,"
                        + " senão a copa some e o resto vira tronco órfão");
    }

    private static LumberjackWork.Job jobWithTree() {
        Task task = Task.create(
                UUID.randomUUID(),
                TaskType.COLLECT_WOOD,
                TaskPriority.PRODUCTION,
                ResourceType.OAK_LOG,
                64);

        LumberjackWork.Job job = new LumberjackWork.Job(task, BlockPos.ORIGIN);
        job.plan = new TreeHarvester.Plan(
                null,
                BlockPos.ORIGIN,
                java.util.List.of(BlockPos.ORIGIN, BlockPos.ORIGIN.up()),
                2,
                0);

        return job;
    }
}
