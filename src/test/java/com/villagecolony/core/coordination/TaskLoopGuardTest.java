package com.villagecolony.core.coordination;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O pedido que fecha sem resultado entra em espera — Regra 191. */
@DisplayName("TaskLoopGuard")
class TaskLoopGuardTest {

    private static Task taskOf(UUID colony, ResourceType target) {
        return Task.create(colony, TaskType.SMELT_MATERIAL, TaskPriority.CONSTRUCTION_MATERIAL, target, 2);
    }

    private static void passCycles(UUID colony, int cycles) {
        for (int cycle = 0; cycle < cycles; cycle++) {
            TaskLoopGuard.aCycleWentBy(colony);
        }
    }

    private static void strikeOut(Task task) {
        for (int strike = 0; strike < TaskLoopGuard.STRIKES; strike++) {
            TaskLoopGuard.fruitless(task);
        }
    }

    @Test
    @DisplayName("só a terceira falta seguida segura o pedido")
    void theThirdStrikeHolds() {
        Task task = taskOf(UUID.randomUUID(), ResourceType.GLASS);

        assertEquals(0, TaskLoopGuard.fruitless(task));
        assertEquals(0, TaskLoopGuard.fruitless(task));
        assertFalse(TaskLoopGuard.isHeld(task), "duas faltas ainda não seguram");
        assertEquals(TaskLoopGuard.FIRST_HOLD, TaskLoopGuard.fruitless(task));
        assertTrue(TaskLoopGuard.isHeld(task), "a terceira falta segura");
    }

    @Test
    @DisplayName("a espera acaba quando os ciclos passam")
    void theHoldEndsWithTheCycles() {
        UUID colony = UUID.randomUUID();
        Task task = taskOf(colony, ResourceType.GLASS);

        strikeOut(task);
        passCycles(colony, TaskLoopGuard.FIRST_HOLD - 1);
        assertTrue(TaskLoopGuard.isHeld(task), "um ciclo antes ainda segura");
        passCycles(colony, 1);
        assertFalse(TaskLoopGuard.isHeld(task), "os ciclos passaram");
    }

    @Test
    @DisplayName("cada nova falta dobra a espera, até o teto")
    void eachNewStrikeDoublesUpToTheCeiling() {
        UUID colony = UUID.randomUUID();
        Task task = taskOf(colony, ResourceType.GLASS);

        strikeOut(task);

        int hold = TaskLoopGuard.FIRST_HOLD;

        while (hold < TaskLoopGuard.LONGEST_HOLD) {
            passCycles(colony, hold);
            hold = Math.min(hold * 2, TaskLoopGuard.LONGEST_HOLD);
            assertEquals(hold, TaskLoopGuard.fruitless(task));
        }

        passCycles(colony, hold);
        assertEquals(TaskLoopGuard.LONGEST_HOLD, TaskLoopGuard.fruitless(task), "o teto não passa");
    }

    @Test
    @DisplayName("peça feita zera a conta")
    void aFruitfulTaskStartsOver() {
        Task task = taskOf(UUID.randomUUID(), ResourceType.GLASS);

        strikeOut(task);
        TaskLoopGuard.fruitful(task);
        assertFalse(TaskLoopGuard.isHeld(task), "rendeu, e continuou em espera");
        assertEquals(0, TaskLoopGuard.fruitless(task), "a conta não recomeçou");
    }

    @Test
    @DisplayName("o cru aumentar nos baús solta a espera")
    void moreRawReleasesTheHold() {
        UUID colony = UUID.randomUUID();
        Task task = taskOf(colony, ResourceType.GLASS);
        ResourceType raw = StockRules.rawOf(ResourceType.GLASS).orElseThrow();

        strikeOut(task);
        TaskLoopGuard.reviewStock(colony, ResourceTally.of(Map.of(raw, 3)));
        assertTrue(TaskLoopGuard.isHeld(task), "a primeira leitura só anota");
        TaskLoopGuard.reviewStock(colony, ResourceTally.of(Map.of(raw, 3)));
        assertTrue(TaskLoopGuard.isHeld(task), "o cru não aumentou");
        TaskLoopGuard.reviewStock(colony, ResourceTally.of(Map.of(raw, 4)));
        assertFalse(TaskLoopGuard.isHeld(task), "o cru aumentou, e continuou em espera");
    }

    @Test
    @DisplayName("obra nova solta só a colônia dela, e cada alvo conta sozinho")
    void aNewBuildReleasesOnlyItsColony() {
        UUID colony = UUID.randomUUID();
        Task here = taskOf(colony, ResourceType.GLASS);
        Task other = taskOf(colony, ResourceType.TERRACOTTA);
        Task elsewhere = taskOf(UUID.randomUUID(), ResourceType.GLASS);

        strikeOut(here);
        strikeOut(elsewhere);
        assertFalse(TaskLoopGuard.isHeld(other), "outro alvo pegou a espera emprestada");

        TaskLoopGuard.clear(colony);
        assertFalse(TaskLoopGuard.isHeld(here), "a obra nova não soltou a espera");
        assertTrue(TaskLoopGuard.isHeld(elsewhere), "a obra de uma vila soltou a de outra");
    }
}
