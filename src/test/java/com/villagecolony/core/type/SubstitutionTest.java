package com.villagecolony.core.type;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A preferência saiu do {@code ordinal()} para um campo em 2026-09-29.
 * Este teste fixa a sequência da ADR-009 §3.10, para que nenhuma
 * reordenação mude o que a colônia escolhe.
 */
class SubstitutionTest {

    private static final List<Substitution> CONTRACT = List.of(
            Substitution.PREFERRED,
            Substitution.ACCEPTABLE,
            Substitution.ALTERNATIVE,
            Substitution.FORBIDDEN);

    @Test
    void bestFirstKeepsTheContractedSequence() {
        List<Substitution> shuffled = new ArrayList<>(CONTRACT);
        Collections.reverse(shuffled);

        shuffled.sort(Substitution.BEST_FIRST);

        assertEquals(CONTRACT, shuffled);
    }

    @Test
    void everyLevelHasItsOwnPreference() {
        long distinct = Arrays.stream(Substitution.values())
                .mapToInt(Substitution::preference)
                .distinct()
                .count();

        assertEquals(Substitution.values().length, distinct);
    }

    @Test
    void isBetterThanAgreesWithTheComparator() {
        for (Substitution one : Substitution.values()) {
            for (Substitution other : Substitution.values()) {
                assertEquals(
                        Substitution.BEST_FIRST.compare(one, other) < 0,
                        one.isBetterThan(other),
                        one + " vs " + other);
            }
        }
    }
}
