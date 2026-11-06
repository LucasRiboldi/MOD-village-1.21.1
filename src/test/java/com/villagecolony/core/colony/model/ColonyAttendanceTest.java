package com.villagecolony.core.colony.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A vila descansa quando o jogador sai (ADR-036 item 11), e o marco do bosque não volta. */
@DisplayName("ColonyAttendance")
class ColonyAttendanceTest {

    private static final long GAP = Colony.PRESENCE_GAP_TICKS;

    private static Colony colony() {
        return Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));
    }

    /** Colônia em que o jogador ficou o bastante para ela trabalhar, e saiu em {@code left}. */
    private static Colony settledUntil(long left) {
        return visited(left - Colony.SETTLE_TICKS, left);
    }

    /** O jogador sem sair, de {@code from} a {@code to}. */
    private static Colony visited(long from, long to) {
        Colony colony = colony();

        for (long now = from; now < to; now += GAP) {
            colony.attend(now);
        }

        colony.attend(to);

        return colony;
    }

    @Test
    @DisplayName("sem nunca ter tido jogador, a vila não parou")
    void aVillageNobodyVisitedNeverStopped() {
        assertFalse(colony().stoppedWithin(1_000_000, 1_000_000));
    }

    @Test
    @DisplayName("o aviso de parada sai na janela logo depois do intervalo de presença")
    void theStopIsToldOnceRightAfterTheGap() {
        long left = 10_000;
        long step = 20;
        Colony colony = settledUntil(left);

        assertFalse(colony.stoppedWithin(left + GAP, step), "ainda dentro do intervalo de presença");
        assertTrue(colony.stoppedWithin(left + GAP + 1, step), "primeiro tique da parada");
        assertTrue(colony.stoppedWithin(left + GAP + step, step), "último tique da janela");
        assertFalse(colony.stoppedWithin(left + GAP + step + 1, step), "depois da janela não avisa de novo");
    }

    @Test
    @DisplayName("quem não chegou a assentar não avisa parada")
    void aVisitTooShortDoesNotCount() {
        long left = 10_000;
        Colony exact = settledUntil(left);
        Colony shortVisit = visited(left - Colony.SETTLE_TICKS + 1, left);

        assertTrue(exact.stoppedWithin(left + GAP + 1, 20), "assentou exatamente no limite");
        assertFalse(shortVisit.stoppedWithin(left + GAP + 1, 20), "um tique a menos não conta");
    }

    @Test
    @DisplayName("o marco do bosque é múltiplo de dez e nunca volta")
    void theForestMilestoneOnlyMovesForward() {
        Colony colony = colony();

        assertEquals(0, colony.forestPopulationMilestone());

        colony.markForestPopulationMilestone(0);
        colony.markForestPopulationMilestone(20);

        assertEquals(20, colony.forestPopulationMilestone());

        colony.markForestPopulationMilestone(20);

        assertThrows(IllegalArgumentException.class, () -> colony.markForestPopulationMilestone(10), "voltou");
        assertThrows(IllegalArgumentException.class, () -> colony.markForestPopulationMilestone(25), "não é múltiplo");
        assertThrows(IllegalArgumentException.class, () -> colony.markForestPopulationMilestone(-10), "negativo");
        assertEquals(20, colony.forestPopulationMilestone());
    }

    @Test
    @DisplayName("as voltas entre dois lados contam no sentido do relógio")
    void turnsCountClockwise() {
        for (Side side : Side.values()) {
            assertEquals(0, side.turnsTo(side));
            assertEquals(1, side.turnsTo(side.clockwise()));
            assertEquals(2, side.turnsTo(side.clockwise().clockwise()));
            assertEquals(3, side.turnsTo(side.clockwise().clockwise().clockwise()));
        }
    }
}
