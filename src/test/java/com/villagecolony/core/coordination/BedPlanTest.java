package com.villagecolony.core.coordination;

import com.villagecolony.core.coordination.BedPlan.Move;
import com.villagecolony.core.coordination.BedPlan.Sleeper;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A cama mais perto do trabalho — Regra 74. */
class BedPlanTest {

    private static final UUID MINER = UUID.randomUUID();
    private static final UUID BUILDER = UUID.randomUUID();
    private static final UUID IDLE = UUID.randomUUID();

    private static ColonyPos at(int x, int y, int z) {
        return new ColonyPos(x, y, z);
    }

    @Test
    void theDistanceCountsTheHeightToo() {
        assertEquals(5.0, BedPlan.distance(at(0, 0, 0), at(3, 4, 0)), 1e-9);
        assertEquals(13.0, BedPlan.distance(at(1, 2, 3), at(4, 6, 15)), 1e-9);
    }

    @Test
    void aWorkerTakesTheFreeBedNearHisWork() {
        Sleeper miner = new Sleeper(MINER, at(100, 80, 0), at(0, 60, 0), true);

        List<Move> moves = BedPlan.plan(List.of(miner), List.of(at(5, 62, 0), at(60, 70, 0)));

        assertEquals(List.of(new Move(MINER, at(100, 80, 0), at(5, 62, 0))), moves);
    }

    @Test
    void aGainBelowTheMinimumIsNotWorthTheMove() {
        Sleeper miner = new Sleeper(MINER, at(20, 60, 0), at(0, 60, 0), true);

        assertTrue(BedPlan.plan(List.of(miner), List.of(at(5, 60, 0))).isEmpty(), "ganho de 15 não vale a mudança");
        assertEquals(1, BedPlan.plan(List.of(miner), List.of(at(4, 60, 0))).size(), "ganho de 16 vale");
    }

    /** O mineiro dorme em cima, ao lado da obra, e cava embaixo, ao lado da cama do construtor. */
    @Test
    void theMinerAndTheBuilderSwapWhenBothWalkLess() {
        Sleeper miner = new Sleeper(MINER, at(100, 80, 0), at(0, 56, 0), true);
        Sleeper builder = new Sleeper(BUILDER, at(5, 63, 0), at(95, 82, 0), true);

        List<Move> moves = BedPlan.plan(List.of(miner, builder), List.of());

        assertEquals(Set.of(
                new Move(MINER, at(100, 80, 0), at(5, 63, 0)),
                new Move(BUILDER, at(5, 63, 0), at(100, 80, 0))), Set.copyOf(moves));
    }

    @Test
    void anOwnerWhoWouldWalkMoreKeepsHisBed() {
        Sleeper miner = new Sleeper(MINER, at(100, 60, 0), at(0, 60, 0), true);
        // O construtor trabalha ao lado da própria cama: ir para a do mineiro o afastaria.
        Sleeper builder = new Sleeper(BUILDER, at(10, 60, 0), at(12, 60, 0), true);

        assertTrue(BedPlan.plan(List.of(miner, builder), List.of()).isEmpty());
        assertEquals(List.of(new Move(MINER, at(100, 60, 0), at(30, 60, 0))),
                BedPlan.plan(List.of(miner, builder), List.of(at(30, 60, 0))), "segue para a próxima cama");
    }

    @Test
    void aVillagerWithoutAJobGivesUpHisBed() {
        Sleeper miner = new Sleeper(MINER, at(100, 60, 0), at(0, 60, 0), true);
        Sleeper idle = new Sleeper(IDLE, at(3, 60, 0), null, true);

        assertEquals(Set.of(
                new Move(MINER, at(100, 60, 0), at(3, 60, 0)),
                new Move(IDLE, at(3, 60, 0), at(100, 60, 0))), Set.copyOf(BedPlan.plan(List.of(miner, idle), List.of())));
    }

    @Test
    void whoeverSleepsNowStaysAndKeepsHisBed() {
        Sleeper sleepingMiner = new Sleeper(MINER, at(100, 60, 0), at(0, 60, 0), false);
        Sleeper builder = new Sleeper(BUILDER, at(200, 60, 0), at(0, 60, 0), true);
        Sleeper sleepingIdle = new Sleeper(IDLE, at(2, 60, 0), null, false);

        assertTrue(BedPlan.plan(List.of(sleepingMiner), List.of(at(1, 60, 0))).isEmpty(), "quem dorme não muda");
        assertTrue(BedPlan.plan(List.of(builder, sleepingIdle), List.of()).isEmpty(), "a cama de quem dorme não se oferece");
    }

    @Test
    void whoeverWalksMoreChoosesFirst() {
        Sleeper near = new Sleeper(MINER, at(40, 60, 0), at(0, 60, 0), true);
        Sleeper far = new Sleeper(BUILDER, at(90, 60, 0), at(0, 60, 0), true);

        assertEquals(List.of(new Move(BUILDER, at(90, 60, 0), at(1, 60, 0))),
                BedPlan.plan(List.of(near, far), List.of(at(1, 60, 0))));
    }

    @Test
    void theBedLeftBehindIsFreeForTheNextAndNobodyMovesTwice() {
        Sleeper first = new Sleeper(BUILDER, at(120, 60, 0), at(0, 60, 0), true);
        Sleeper second = new Sleeper(MINER, at(200, 60, 0), at(115, 60, 0), true);

        assertEquals(List.of(
                new Move(BUILDER, at(120, 60, 0), at(1, 60, 0)),
                new Move(MINER, at(200, 60, 0), at(120, 60, 0))),
                BedPlan.plan(List.of(first, second), List.of(at(1, 60, 0))));
    }

    @Test
    void aWorkerWithoutMeasuredWorkStays() {
        Sleeper idle = new Sleeper(IDLE, at(100, 60, 0), null, true);

        assertTrue(BedPlan.plan(List.of(idle), List.of(at(1, 60, 0))).isEmpty());
    }

    @Test
    void aBedWithAnOwnerIsNeverOfferedAsFree() {
        Sleeper miner = new Sleeper(MINER, at(100, 60, 0), at(0, 60, 0), true);
        Sleeper sleeping = new Sleeper(IDLE, at(2, 60, 0), null, false);

        assertTrue(BedPlan.plan(List.of(miner, sleeping), List.of(at(2, 60, 0))).isEmpty(),
                "a cama ocupada que veio na lista das livres não é livre");
    }
}
