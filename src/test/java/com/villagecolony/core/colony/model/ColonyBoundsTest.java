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

    @Test
    void attentionLastsFiveMinutesAfterThePlayerLeaves() {
        assertFalse(colony.isAttended(0), "sem jogador ainda, ninguém trabalha");

        colony.attend(1_000);

        assertTrue(colony.isAttended(1_000));
        assertTrue(colony.isAttended(1_000 + Colony.ATTENTION_TICKS));
        assertFalse(colony.isAttended(1_000 + Colony.ATTENTION_TICKS + 1),
                "passados 5 minutos sem jogador dentro, a vila para");
    }

    @Test
    void aClockThatWentBackDoesNotKeepTheVillageAwakeForever() {
        colony.attend(50_000);

        assertFalse(colony.isAttended(10));
    }
}
