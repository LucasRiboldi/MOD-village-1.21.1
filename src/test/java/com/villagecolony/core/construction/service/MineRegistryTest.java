package com.villagecolony.core.construction.service;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A mina é da colônia, e ela dura — 2026-08-20.
 *
 * <p>Duas propriedades, e as duas custaram trabalho de mineiro antes de
 * existirem: a colônia tem <b>uma</b> mina, e a fronteira do que já foi
 * cavado não volta a zero.
 */
class MineRegistryTest {

    private static final ColonyPos MOUTH = new ColonyPos(40, 64, 0);

    private static MineShaft shaft() {
        return MineShaft.from(MOUTH, Side.EAST);
    }

    @Test
    void aColonyWithoutAMineHasNone() {
        assertTrue(new MineRegistry().of(UUID.randomUUID()).isEmpty());
    }

    @Test
    void theSecondMinerFindsTheSameMine() {
        MineRegistry registry = new MineRegistry();

        UUID colonyId = UUID.randomUUID();

        Mine first = registry.open(colonyId, shaft());
        Mine second = registry.open(colonyId, MineShaft.from(new ColonyPos(-40, 64, 0), Side.WEST));

        // O segundo mineiro chegou com outra boca debaixo do braço, e a
        // colônia recusou: uma segunda escada apagaria a fronteira da
        // primeira, e ele recomeçaria dentro da mina já cavada.
        assertSame(first, second);
        assertEquals(MOUTH, second.entry());
        assertEquals(1, registry.count());
    }

    @Test
    void twoColoniesDigTheirOwnMines() {
        MineRegistry registry = new MineRegistry();

        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        registry.open(one, shaft());
        registry.open(other, MineShaft.from(new ColonyPos(-40, 64, 0), Side.WEST));

        assertEquals(2, registry.count());
        assertNotEquals(
                registry.of(one).orElseThrow().entry(),
                registry.of(other).orElseThrow().entry());
    }

    @Test
    void theCursorAdvancesAndIsRemembered() {
        MineRegistry registry = new MineRegistry();

        UUID colonyId = UUID.randomUUID();

        Mine mine = registry.open(colonyId, shaft());

        for (int i = 0; i < 5; i++) {
            mine.nextPosition();
        }

        assertEquals(5, registry.of(colonyId).orElseThrow().cut());
    }

    /**
     * A posição sai na ordem de cavar, e a fronteira é onde se parou.
     *
     * <p>Prova a razão de o cursor existir: a sessão seguinte pergunta a
     * posição de número {@code cut} e recebe a que vem depois da última
     * olhada — sem revarrer degrau por degrau o que já está aberto.
     */
    @Test
    void theFrontierIsWhereTheNextCutBegins() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        ColonyPos first = mine.nextPosition();
        ColonyPos second = mine.nextPosition();

        assertEquals(shaft().positionAt(0), first);
        assertEquals(shaft().positionAt(1), second);

        Mine reopened = Mine.restore(UUID.randomUUID(), shaft(), mine.cut());

        assertEquals(shaft().positionAt(2), reopened.nextPosition());
    }

    @Test
    void theGalleryKeepsTheTurnItTook() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        Side before = mine.shaft().gallery();

        mine.turn();

        assertNotEquals(before, mine.shaft().gallery());
        assertEquals(MOUTH, mine.entry());
    }

    @Test
    void aRestoredMineNeverStartsBehindTheStart() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Mine.restore(UUID.randomUUID(), shaft(), -1));
    }

    @Test
    void aColonyThatGoesAwayTakesItsMine() {
        MineRegistry registry = new MineRegistry();

        UUID colonyId = UUID.randomUUID();

        registry.open(colonyId, shaft());
        registry.removeOfColony(colonyId);

        assertTrue(registry.of(colonyId).isEmpty());
        assertFalse(registry.all().iterator().hasNext());
    }
}
