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

    /**
     * Bloco que não deu para alcançar volta a ser oferecido —
     * 2026-08-27.
     *
     * <p><b>O autor foi olhar em jogo, e a frase dele fecha o caso:</b>
     * <i>"tive que cavar até lá"</i>. O mod dizia estar cavando a galeria
     * havia três sessões, e no mundo estava rocha maciça.
     *
     * <p>{@link Mine#nextPosition()} avança o cursor <b>sempre</b>. Quando
     * o mineiro não conseguia chegar na pedra, a tarefa voltava para a
     * fila e a posição ficava para trás: o cursor marchava por dentro da
     * rocha, coluna após coluna, e o túnel nunca era aberto. É o mesmo
     * defeito que o {@code holdPosition} já conserta quando a picareta
     * desvia para o minério — <i>"o túnel ficaria com um bloco no meio
     * para sempre"</i> —, e ninguém o chamava na desistência.
     */
    @Test
    void aStoneThatCouldNotBeReachedIsOfferedAgain() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        ColonyPos unreachable = mine.nextPosition();

        assertTrue(mine.holdPositionAt(unreachable), "o cursor não desandou");

        assertEquals(unreachable, mine.nextPosition());
    }

    /**
     * Só desanda quando a posição abandonada é a última entregue.
     *
     * <p>A mina é da colônia e dois mineiros a partilham. Desandar às
     * cegas devolveria o cursor por cima do bloco que o <b>outro</b>
     * acabou de pegar, e os dois passariam a brigar pelo mesmo ponto.
     */
    @Test
    void onlyTheLastHandedOutPositionRollsBack() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        ColonyPos mine_ = mine.nextPosition();
        ColonyPos theOther = mine.nextPosition();

        assertFalse(
                mine.holdPositionAt(mine_),
                "desandou por cima do bloco que o outro mineiro pegou");

        assertTrue(mine.holdPositionAt(theOther));

        assertEquals(theOther, mine.nextPosition());
    }

    /**
     * A galeria sabe recuar até a frente de verdade — 2026-08-27.
     *
     * <p>Quando o cursor marchou por dentro da rocha, a posição que ele
     * aponta não tem túnel atrás dela: não há de onde alcançá-la, e o
     * mineiro é mandado para dentro da pedra. Recuar é o único caminho
     * de volta — a ordem de cavar é um caminho para fora da boca, então
     * a posição anterior está sempre mais perto do que já está aberto.
     */
    @Test
    void theGalleryCanBackUpToWhereItReallyEnds() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        for (int i = 0; i < 10; i++) {
            mine.nextPosition();
        }

        assertEquals(10, mine.cut());

        mine.backUp();
        mine.backUp();

        assertEquals(8, mine.cut());
        assertEquals(mine.shaft().positionAt(8), mine.nextPosition());
    }

    /** Recuar do começo não leva a picareta para antes do primeiro degrau. */
    @Test
    void backingUpNeverGoesBehindTheStart() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        mine.backUp();
        mine.backUp();

        assertEquals(0, mine.cut());
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
