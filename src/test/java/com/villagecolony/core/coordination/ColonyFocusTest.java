package com.villagecolony.core.coordination;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A vila foco é a de maior presença do jogador, e ela segue o jogador. */
class ColonyFocusTest {

    private final UUID home = UUID.randomUUID();
    private final UUID visited = UUID.randomUUID();
    private final List<UUID> known = List.of(home, visited);

    @Test
    void nobodyVisitedMeansNoFocus() {
        assertEquals(Optional.empty(), new ColonyFocus().focus());
    }

    @Test
    void theVillageWhereThePlayerStaysIsTheFocus() {
        ColonyFocus focus = new ColonyFocus();

        for (int cycle = 0; cycle < 100; cycle++) {
            focus.record(known, Set.of(home));
        }

        for (int cycle = 0; cycle < 10; cycle++) {
            focus.record(known, Set.of(visited));
        }

        assertEquals(Optional.of(home), focus.focus(), "uma visita curta roubou o foco");
    }

    @Test
    void movingToAnotherVillageEventuallyMovesTheFocus() {
        ColonyFocus focus = new ColonyFocus();

        for (int cycle = 0; cycle < 100; cycle++) {
            focus.record(known, Set.of(home));
        }

        for (int cycle = 0; cycle < 400; cycle++) {
            focus.record(known, Set.of(visited));
        }

        assertEquals(Optional.of(visited), focus.focus(), "o foco não seguiu o jogador que se mudou");
    }

    @Test
    void presenceDecaysByHalfEveryHalfLife() {
        ColonyFocus focus = new ColonyFocus();
        focus.record(known, Set.of(home));
        double start = focus.presenceOf(home);

        for (int cycle = 0; cycle < ColonyFocus.HALF_LIFE_CYCLES; cycle++) {
            focus.record(known, Set.of());
        }

        assertEquals(start / 2, focus.presenceOf(home), 1e-6);
    }

    @Test
    void aColonyThatNoLongerExistsIsForgotten() {
        ColonyFocus focus = new ColonyFocus();
        focus.record(known, Set.of(home));
        focus.record(List.of(visited), Set.of());

        assertEquals(0.0, focus.presenceOf(home));
        assertTrue(focus.focus().isEmpty());
    }
}
