package com.villagecolony.fabric.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A linha {@code VC_COST_PHASES}: as subfases do {@code planner} de uma colônia, no primeiro ciclo de trabalho dela e
 * sempre que o {@code planner} passa de {@link PlannerPhases#SLOW_MS} — VF-001 A.
 */
class PlannerPhasesTest {

    private static final UUID COLONY = UUID.fromString("9105dea9-0000-0000-0000-000000000001");

    private static long ms(long millis) {
        return millis * 1_000_000L;
    }

    @BeforeEach
    void forgetFirstCycles() {
        PlannerPhases.clearAll();
    }

    @Test
    void theLineNamesEverySubphaseInMicrosecondsAndTheRestAsOther() {
        PlannerPhases phases = new PlannerPhases();

        phases.add(PlannerPhases.Sub.PLAN, ms(15));
        phases.add(PlannerPhases.Sub.SMELTED, ms(300));
        phases.add(PlannerPhases.Sub.PROBE, ms(2));

        String line = phases.line(COLONY, ms(400), true);

        assertEquals("VC_COST_PHASES version=1 colony=" + COLONY + " first=true planner_us=400000"
                + " probe_us=2000 plan_us=15000 planks_us=0 palette_us=0 biome_pieces_us=0 stone_us=0"
                + " wool_us=0 glass_us=0 coal_us=0 iron_us=0 smelted_us=300000 surface_us=0 mined_us=0"
                + " raw_wood_us=0 other_us=83000", line);
    }

    @Test
    void theFirstWorkCycleOfAColonyAlwaysReportsEvenWhenCheap() {
        Optional<String> first = new PlannerPhases().lineIfDue(COLONY, ms(1));
        Optional<String> second = new PlannerPhases().lineIfDue(COLONY, ms(1));

        assertTrue(first.isPresent(), "o primeiro ciclo de trabalho da colônia sempre sai");
        assertTrue(first.get().contains(" first=true "), first.get());
        assertTrue(second.isEmpty(), "o segundo ciclo barato não sai: " + second);
    }

    @Test
    void aLaterCycleReportsOnlyAboveTheLimit() {
        new PlannerPhases().lineIfDue(COLONY, ms(1));

        assertTrue(new PlannerPhases().lineIfDue(COLONY, ms(PlannerPhases.SLOW_MS)).isEmpty(),
                "no limite exato não sai");

        Optional<String> slow = new PlannerPhases().lineIfDue(COLONY, ms(PlannerPhases.SLOW_MS + 1));

        assertTrue(slow.isPresent(), "acima do limite sai");
        assertTrue(slow.get().contains(" first=false "), slow.get());
    }

    @Test
    void forgettingTheServerMemoryMakesTheNextCycleFirstAgain() {
        new PlannerPhases().lineIfDue(COLONY, ms(1));
        PlannerPhases.clearAll();

        assertTrue(new PlannerPhases().lineIfDue(COLONY, ms(1)).isPresent(),
                "ao reabrir o mundo, o primeiro ciclo volta a sair");
    }

    @Test
    void otherNeverGoesNegative() {
        PlannerPhases phases = new PlannerPhases();

        phases.add(PlannerPhases.Sub.PLAN, ms(50));

        assertTrue(phases.line(COLONY, ms(10), false).endsWith(" other_us=0"));
    }
}
