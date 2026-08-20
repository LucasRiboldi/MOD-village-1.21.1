package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A mina — a Regra 29, e ela é geometria.
 *
 * <p>Fora do jogo de propósito: a mina tem cento e cinquenta e duas
 * posições antes da galeria, e conferi-las cavando levaria uma sessão.
 * Aqui levam milissegundos, e o que se afirma é a forma — que é o que o
 * autor descreveu.
 */
class MineShaftTest {

    private static final ColonyPos ENTRY = new ColonyPos(100, 64, 200);

    private static MineShaft shaft() {
        return MineShaft.from(ENTRY, Side.EAST);
    }

    /**
     * A escada desce um por passo, e nunca dois.
     *
     * <p>É a frase do autor: <i>"de modo que ele possa subir de volta"</i>.
     * Um degrau de dois blocos é um poço com aparência de escada.
     */
    @Test
    void theStairDropsOneBlockPerStep() {
        MineShaft mine = shaft();

        for (int step = 1; step <= MineShaft.DESCENT; step++) {
            ColonyPos feet = mine.positionAt((step - 1) * MineShaft.HEADROOM);

            assertEquals(ENTRY.y() - step + 1, feet.y(),
                    "o degrau " + step + " não desceu um bloco");

            assertEquals(ENTRY.x() + step, feet.x(), "o degrau " + step + " não andou um bloco");
        }
    }

    /** Cada degrau abre dois blocos: os pés e a cabeça. */
    @Test
    void everyStepIsTwoBlocksTall() {
        MineShaft mine = shaft();

        for (int step = 1; step <= MineShaft.DESCENT; step++) {
            ColonyPos feet = mine.positionAt((step - 1) * MineShaft.HEADROOM);
            ColonyPos head = mine.positionAt((step - 1) * MineShaft.HEADROOM + 1);

            assertEquals(feet.y() + 1, head.y(), "a cabeça do degrau " + step + " não abriu");
            assertEquals(feet.x(), head.x());
            assertEquals(feet.z(), head.z());
        }
    }

    /** Dez degraus levam a dez blocos abaixo da entrada. */
    @Test
    void tenStepsReachTenBlocksDown() {
        ColonyPos last = shaft().positionAt(MineShaft.DESCENT * MineShaft.HEADROOM - 2);

        assertEquals(ENTRY.y() - MineShaft.DESCENT + 1, last.y());
    }

    /**
     * O segundo lance vira, e não continua reto.
     *
     * <p>Continuar reto daria um corredor inclinado de vinte blocos; a
     * curva é o que mantém a mina compacta e a subida curta.
     */
    @Test
    void theSecondFlightTurns() {
        MineShaft mine = shaft();

        ColonyPos first = mine.positionAt(0);
        ColonyPos second = mine.positionAt(MineShaft.CARVED - 1);

        assertNotEquals(first.z(), second.z(), "o segundo lance não virou para lado nenhum");
    }

    /** A sala é sete por quatro, e de dois de altura. */
    @Test
    void eachRoomIsSevenByFourAndTwoTall() {
        MineShaft mine = shaft();

        Set<String> floor = new HashSet<>();
        Set<Integer> heights = new HashSet<>();

        int from = MineShaft.DESCENT * MineShaft.HEADROOM;
        int upTo = from + MineShaft.ROOM_LONG * MineShaft.ROOM_WIDE * MineShaft.HEADROOM;

        for (int i = from; i < upTo; i++) {
            ColonyPos at = mine.positionAt(i);

            floor.add(at.x() + ":" + at.z());
            heights.add(at.y());
        }

        assertEquals(MineShaft.ROOM_LONG * MineShaft.ROOM_WIDE, floor.size(),
                "a sala não tem sete por quatro de chão");

        assertEquals(MineShaft.HEADROOM, heights.size(), "a sala não tem dois de altura");
    }

    /** A segunda sala fica vinte blocos abaixo da entrada. */
    @Test
    void theSecondRoomSitsTwentyBlocksDown() {
        ColonyPos at = shaft().positionAt(MineShaft.CARVED - 1);

        assertTrue(
                at.y() <= ENTRY.y() - 2 * MineShaft.DESCENT + MineShaft.HEADROOM,
                "a segunda sala ficou em " + at.y() + ", e devia estar por volta de "
                        + (ENTRY.y() - 2 * MineShaft.DESCENT));
    }

    /**
     * A galeria não acaba, e fica toda no nível da segunda sala.
     *
     * <p>É a frase do autor: <i>"na camada 20 ele começa a recolher na
     * altura do aldeão mais 1 infinitamente"</i>.
     */
    @Test
    void theGalleryRunsForeverOnOneLevel() {
        MineShaft mine = shaft();

        int level = mine.positionAt(MineShaft.CARVED).y();

        for (int i = MineShaft.CARVED; i < MineShaft.CARVED + 2_000; i++) {
            int y = mine.positionAt(i).y();

            assertTrue(
                    y == level || y == level + 1,
                    "a galeria saiu do nível dela em " + y);
        }
    }

    /** E cada passo dela avança de verdade, sem repetir posição. */
    @Test
    void theGalleryAdvancesInsteadOfDiggingTheSameHole() {
        MineShaft mine = shaft();

        Set<ColonyPos> seen = new HashSet<>();

        for (int i = MineShaft.CARVED; i < MineShaft.CARVED + 200; i++) {
            assertTrue(seen.add(mine.positionAt(i)), "a galeria repetiu uma posição");
        }
    }

    /**
     * Barreira à frente: a galeria vira, e a escada fica onde estava.
     *
     * <p>Virar a mina inteira jogaria fora os cento e cinquenta e dois
     * blocos já cavados e o caminho de volta do aldeão.
     */
    @Test
    void aBlockedGalleryTurnsWithoutMovingTheStair() {
        MineShaft mine = shaft();
        MineShaft turned = mine.turned();

        assertEquals(mine.positionAt(0), turned.positionAt(0), "a escada mudou de lugar");

        assertNotEquals(
                mine.positionAt(MineShaft.CARVED + 4),
                turned.positionAt(MineShaft.CARVED + 4),
                "a galeria não virou");
    }

    /** Nenhuma posição da parte cavada se repete. */
    @Test
    void theCarvedPartNeverDigsTheSameBlockTwice() {
        MineShaft mine = shaft();

        Set<ColonyPos> seen = new HashSet<>();

        for (int i = 0; i < MineShaft.CARVED; i++) {
            assertTrue(seen.add(mine.positionAt(i)), "a mina repetiu a posição de índice " + i);
        }
    }
}
