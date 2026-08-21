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

    /**
     * A posição do túnel espera quando a picareta vai ao minério.
     *
     * <p>O caso é real: o túnel chegou a uma pedra cavável e havia carvão
     * colado nela. Sem desandar o cursor, ele passaria por cima da pedra
     * e o túnel ficaria com um bloco no meio para sempre.
     */
    @Test
    void theTunnelPositionWaitsWhileTheOreIsTaken() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        ColonyPos first = mine.nextPosition();

        mine.holdPosition();

        assertEquals(first, mine.nextPosition());
    }

    /** Desandar do começo não leva a picareta para antes do primeiro degrau. */
    @Test
    void theCursorNeverGoesBehindTheStart() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        mine.holdPosition();

        assertEquals(0, mine.cut());
    }

    /**
     * A veia é lembrada até acabar, e a virada da galeria não a leva.
     *
     * <p>Minério não vem sozinho, e voltar ao túnel com a veia pela
     * metade faria o aldeão andar até lá outra vez na passagem seguinte.
     */
    @Test
    void theVeinIsRememberedUntilItRunsOut() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        assertTrue(mine.vein().isEmpty());

        ColonyPos ore = new ColonyPos(41, 54, 3);

        mine.followVein(ore);

        assertEquals(ore, mine.vein().orElseThrow());

        mine.veinExhausted();

        assertTrue(mine.vein().isEmpty());
    }

    /**
     * A galeria vira depois de oito recusas seguidas, e não de dezesseis.
     *
     * <p>A contagem é da mina, e não do mineiro: dois na mesma escada
     * esbarram na mesma lava, e duas contagens separadas pediriam o dobro
     * de recusas para uma curva que precisa de oito.
     */
    @Test
    void theGalleryTurnsOnceTheRefusalsAddUp() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        Side before = mine.shaft().gallery();

        for (int i = 0; i < 7; i++) {
            assertFalse(mine.blockedAgain(8), "virou cedo demais, na recusa " + (i + 1));
        }

        assertTrue(mine.blockedAgain(8));
        assertNotEquals(before, mine.shaft().gallery());

        // E a contagem recomeça: a curva seguinte também pede oito.
        assertFalse(mine.blockedAgain(8));
    }

    /** Picareta que pega zera a conta das recusas. */
    @Test
    void diggingClearsTheRefusals() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        Side before = mine.shaft().gallery();

        for (int i = 0; i < 7; i++) {
            mine.blockedAgain(8);
        }

        mine.digging();

        assertFalse(mine.blockedAgain(8));
        assertEquals(before, mine.shaft().gallery());
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
