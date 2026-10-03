package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.LastShortage;
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

/**
 * O pedido seguinte nasce quando o anterior termina — F-2, 2026-10-02.
 */
class TaskChainTest {

    private static final UUID COLONY = UUID.randomUUID();

    @BeforeEach
    @AfterEach
    void forget() {
        VillageColonyMod.TASKS.clear();
        LastShortage.clearAll();
    }

    private static Task doneWood() {
        Task task = VillageColonyMod.TASKS.create(
                COLONY, TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION, ResourceType.OAK_LOG, 16);

        task.reserveFor(UUID.randomUUID());
        task.start();
        task.complete();

        return task;
    }

    private static long waitingWood() {
        return VillageColonyMod.TASKS.availableFor(COLONY).stream()
                .filter(task -> task.targetResource() == ResourceType.OAK_LOG)
                .count();
    }

    @Test
    void aDoneRequestWhoseShortageRemainsOpensTheNextOneAtOnce() {
        LastShortage.measured(COLONY, Set.of(ResourceType.OAK_LOG));

        Task done = doneWood();

        TaskChain.next(done);

        assertEquals(1, waitingWood(), "a falta continua: o próximo pedido devia estar à espera");

        Task next = VillageColonyMod.TASKS.availableFor(COLONY).get(0);

        assertEquals(TaskType.COLLECT_WOOD, next.type());
        assertEquals(done.amount(), next.amount());
        assertEquals(done.priority(), next.priority());
    }

    @Test
    void itNeverPilesUpRequests() {
        LastShortage.measured(COLONY, Set.of(ResourceType.OAK_LOG));

        TaskChain.next(doneWood());
        TaskChain.next(doneWood());

        assertEquals(1, waitingWood(), "já havia um à espera: não abre outro");
    }

    @Test
    void withoutTheShortageNothingIsChained() {
        LastShortage.measured(COLONY, Set.of(ResourceType.SAND));

        TaskChain.next(doneWood());

        assertEquals(0, waitingWood(), "a madeira não faltava no último ciclo");
    }

    @Test
    void aBuildTaskIsNotARequestAndIsNeverChained() {
        LastShortage.measured(COLONY, Set.of(ResourceType.OAK_LOG));

        Task build = VillageColonyMod.TASKS.create(
                COLONY, TaskType.BUILD, TaskPriority.CONSTRUCTION, ResourceType.OAK_LOG, 1);

        build.reserveFor(UUID.randomUUID());
        build.start();
        build.complete();

        TaskChain.next(build);

        assertEquals(0, VillageColonyMod.TASKS.availableFor(COLONY).size());
    }
}
