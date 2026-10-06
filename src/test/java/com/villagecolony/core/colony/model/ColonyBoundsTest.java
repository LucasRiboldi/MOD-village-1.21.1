package com.villagecolony.core.colony.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A colônia com caixa e com atenção — decisão do autor, 2026-09-30.
 *
 * <p>O centro passa a ser o meio da caixa e só anda quando ela cresce; e a
 * colônia só trabalha enquanto o jogador está dentro dela, e por 5 minutos
 * depois que ele sai.
 */
class ColonyBoundsTest {

    private static final VillageBounds HUNDRED = new VillageBounds(0, 60, 0, 99, 70, 99);

    private final Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(10, 64, 10));

    @Test
    void measuringTheVillageMovesTheCenterToTheMiddleOfTheBox() {
        assertTrue(colony.measure(HUNDRED));

        assertEquals(new ColonyPos(50, 64, 50), colony.center(), "a altura do centro fica a de antes");
        // 100 de lado é par: ganha uma linha para o centro ser um bloco só — 2026-10-03.
        assertEquals(HUNDRED.centered(), colony.bounds().orElseThrow());
        assertEquals(101, colony.bounds().orElseThrow().sizeX());
    }

    @Test
    void growingFollowsTheBoxAndNeverShrinks() {
        colony.measure(HUNDRED);

        assertTrue(colony.grow(VillageBounds.block(new ColonyPos(50, 64, 120))));
        assertEquals(60, colony.center().z());

        assertFalse(colony.grow(new VillageBounds(10, 64, 10, 20, 66, 20)), "peça de dentro não muda nada");
        assertEquals(121, colony.bounds().orElseThrow().sizeZ());
    }

    @Test
    void aColonyNotMeasuredYetDoesNotGrowFromAPiece() {
        assertFalse(colony.grow(VillageBounds.block(new ColonyPos(500, 64, 500))));
        assertTrue(colony.bounds().isEmpty());
    }

    @Test
    void theProbeNoLongerMovesTheCenterOfAMeasuredVillage() {
        colony.measure(HUNDRED);

        colony.observe(new ColonyPos(80, 64, 80), 12, true, colony.center());

        assertEquals(new ColonyPos(50, 64, 50), colony.center(),
                "a média das camas não manda mais no centro da vila medida");
        assertEquals(12, colony.observedBeds());
    }

    @Test
    void theProbeStillMovesTheCenterOfAColonyNotMeasuredYet() {
        colony.observe(new ColonyPos(30, 64, 30), 5, true, colony.center());

        assertEquals(new ColonyPos(30, 64, 30), colony.center());
    }

    /** O jogador visto a cada 20 tiques, como o VillageFocus faz, de {@code from} a {@code to}. */
    private void stay(long from, long to) {
        for (long tick = from; tick <= to; tick += 20) {
            colony.attend(tick);
        }
    }

    /** ADR-036 item 11: a vila só trabalha depois de um minuto com o jogador dentro. */
    @Test
    void theVillageWorksOnlyAfterAMinuteWithThePlayerInside() {
        assertFalse(colony.isAttended(0), "sem jogador ainda, ninguém trabalha");

        stay(1_000, 2_180);
        assertFalse(colony.isAttended(2_180), "59 segundos dentro ainda não bastam");

        stay(2_200, 2_200);
        assertTrue(colony.isAttended(2_200), "um minuto dentro e a vila trabalha");
    }

    /** Saiu, parou: não há mais os cinco minutos de sobra. */
    @Test
    void theVillageStopsAsSoonAsThePlayerLeaves() {
        stay(1_000, 3_000);

        assertTrue(colony.isAttended(3_000 + Colony.PRESENCE_GAP_TICKS));
        assertFalse(colony.isAttended(3_000 + Colony.PRESENCE_GAP_TICKS + 1),
                "o jogador saiu e a vila continuou trabalhando");
    }

    /** Quem sai e volta começa o minuto de novo. */
    @Test
    void comingBackStartsTheMinuteAgain() {
        stay(1_000, 3_000);
        stay(5_000, 5_000);

        assertFalse(colony.isAttended(5_000), "a volta contou o tempo da visita anterior");

        stay(5_020, 6_200);
        assertTrue(colony.isAttended(6_200));
    }

    @Test
    void aClockThatWentBackDoesNotKeepTheVillageAwakeForever() {
        colony.attend(50_000);

        assertFalse(colony.isAttended(10));
    }
}
