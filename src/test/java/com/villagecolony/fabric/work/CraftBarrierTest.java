package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.LastShortage;
import com.villagecolony.core.coordination.TaskLoopGuard;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra 339: o pedido de oficina parado por uma barreira (a tora guardada, a tora que acabou) não se reabre na hora e
 * conta como pedido sem resultado, mesmo com uma peça feita — o carpinteiro do jogo da 0.4.93 pegava, fazia 0 ou 1 peça,
 * parava e recebia o mesmo pedido de volta o dia inteiro.
 */
class CraftBarrierTest {

    private static final UUID COLONY = UUID.randomUUID();

    @BeforeEach
    @AfterEach
    void forget() {
        VillageColonyMod.TASKS.clear();
        LastShortage.clearAll();
        TaskLoopGuard.clear(COLONY);
    }

    private static CraftingWork.Job stairs(int crafted) {
        Task task = VillageColonyMod.TASKS.create(
                COLONY, TaskType.CRAFT_WOOD_MATERIAL, TaskPriority.PRODUCTION, ResourceType.OAK_STAIRS, 16);

        task.reserveFor(UUID.randomUUID());
        task.start();

        CraftingWork.Job job = new CraftingWork.Job(task);
        job.crafted = crafted;

        return job;
    }

    private static long waitingStairs() {
        return VillageColonyMod.TASKS.availableFor(COLONY).stream()
                .filter(task -> task.targetResource() == ResourceType.OAK_STAIRS)
                .count();
    }

    @Test
    void aRequestStoppedByTheLogReserveIsNotChainedEvenWithOnePieceMade() {
        LastShortage.measured(COLONY, Set.of(ResourceType.OAK_STAIRS));

        CraftingWork.finish(stairs(1), UUID.randomUUID(), "half of the colony's wood stays in logs");

        assertEquals(0, waitingStairs(), "Regra 339: o pedido barrado pela tora guardada se reabriu na hora");
    }

    @Test
    void threeStopsByABarrierHoldTheRequest() {
        LastShortage.measured(COLONY, Set.of(ResourceType.OAK_STAIRS));

        for (int stop = 0; stop < 3; stop++) {
            CraftingWork.finish(stairs(1), UUID.randomUUID(), "no logs left in the colony chests");
        }

        Task next = VillageColonyMod.TASKS.create(
                COLONY, TaskType.CRAFT_WOOD_MATERIAL, TaskPriority.PRODUCTION, ResourceType.OAK_STAIRS, 16);

        assertTrue(TaskLoopGuard.isHeld(next), "Regra 339: três paradas na barreira não seguraram o pedido");
    }

    @Test
    void aRequestThatMadeWhatItWasAskedIsStillChained() {
        LastShortage.measured(COLONY, Set.of(ResourceType.OAK_STAIRS));

        CraftingWork.finish(stairs(16), UUID.randomUUID(), "made the oak_stairs it was asked");

        assertEquals(1, waitingStairs(), "o pedido cumprido com a falta ainda aberta devia se encadear");
    }
}
