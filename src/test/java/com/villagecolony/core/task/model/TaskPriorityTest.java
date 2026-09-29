package com.villagecolony.core.task.model;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A urgência saiu do {@code ordinal()} para um campo em 2026-09-29. Este
 * teste fixa a sequência que valia antes, para que a troca e qualquer
 * nível novo não mudem em silêncio o que a colônia faz primeiro.
 */
class TaskPriorityTest {

    private static final List<TaskPriority> CONTRACT = List.of(
            TaskPriority.SURVIVAL,
            TaskPriority.CONSTRUCTION_MATERIAL,
            TaskPriority.PRODUCTION,
            TaskPriority.CONSTRUCTION);

    @Test
    void mostUrgentFirstKeepsTheContractedSequence() {
        List<TaskPriority> shuffled = new ArrayList<>(CONTRACT);
        Collections.reverse(shuffled);

        shuffled.sort(TaskPriority.MOST_URGENT_FIRST);

        assertEquals(CONTRACT, shuffled);
    }

    @Test
    void everyLevelHasItsOwnUrgency() {
        long distinct = Arrays.stream(TaskPriority.values())
                .mapToInt(TaskPriority::urgency)
                .distinct()
                .count();

        assertEquals(TaskPriority.values().length, distinct);
    }

    @Test
    void isHigherThanAgreesWithTheComparator() {
        for (TaskPriority one : TaskPriority.values()) {
            for (TaskPriority other : TaskPriority.values()) {
                assertEquals(
                        TaskPriority.MOST_URGENT_FIRST.compare(one, other) < 0,
                        one.isHigherThan(other),
                        one + " vs " + other);
            }
        }
    }
}
