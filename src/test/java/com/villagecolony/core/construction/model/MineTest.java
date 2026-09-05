package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A mina em uso: o cursor, a curva e o braço.
 *
 * <p>{@link MineShaftTest} afirma a <b>forma</b>; aqui se afirma o que a
 * colônia faz com ela ao longo de uma sessão.
 */
class MineTest {

    private static final ColonyPos ENTRY = new ColonyPos(100, 64, 200);

    private static Mine opened() {
        return Mine.open(UUID.randomUUID(), MineShaft.from(ENTRY, Side.NORTH));
    }

    /**
     * A curva devolve o cursor ao começo da galeria — decisão do autor,
     * 2026-09-04.
     *
     * <p><b>Era o defeito de verdade por trás do "out of reach".</b> A
     * galeria já sabia virar desde 2026-09-02, e virar não adiantava: o
     * cursor era guardado, então a direção nova começava na mesma
     * distância. Na sessão de 09-04 isso pôs o mineiro a 70,7 blocos da
     * frente, parado, por vinte minutos.
     */
    @Test
    void turningBringsTheCursorBackToTheRoom() {
        Mine mine = opened();

        while (mine.cut() < MineShaft.CARVED + 100) {
            mine.nextPosition();
        }

        assertTrue(mine.cut() > MineShaft.CARVED,
                "o cursor não chegou a entrar na galeria");

        mine.turn();

        assertEquals(MineShaft.CARVED, mine.cut(),
                "a curva manteve o cursor longe da sala");
    }

    /**
     * E o poço não é recavado: o cursor volta ao <b>começo da galeria</b>,
     * não ao começo da mina.
     *
     * <p>Voltar a zero mandaria o mineiro cavar de novo a escada e as duas
     * salas, que já estão abertas — o {@code findTheFrontier} passaria por
     * elas, mas gastando passagem atrás de passagem.
     */
    @Test
    void turningDoesNotSendThePickaxeBackUpTheStair() {
        Mine mine = opened();

        while (mine.cut() < MineShaft.CARVED + 50) {
            mine.nextPosition();
        }

        mine.turn();

        assertTrue(mine.cut() >= MineShaft.CARVED,
                "o cursor voltou para dentro do poço, que já está aberto");
    }

    /**
     * O braço acaba, e a mina avisa antes de gastar a posição.
     *
     * <p>É o que o {@code MineDigging} pergunta a cada olhada, e é onde o
     * teto de raio do autor vira comportamento.
     */
    @Test
    void theMineSaysWhenTheArmIsOver() {
        Mine mine = opened();

        assertFalse(mine.reachedTheEndOfTheArm(),
                "a mina recém-aberta já se diz no fim do braço");

        while (!mine.reachedTheEndOfTheArm()) {
            mine.nextPosition();

            assertTrue(mine.cut() < MineShaft.CARVED + 10_000,
                    "o braço nunca acabou — o teto de raio não pegou");
        }

        assertTrue(mine.cut() > MineShaft.CARVED,
                "o fim do braço caiu antes de a galeria começar");
    }

    /**
     * Quatro curvas fecham o nível e a mina desce — e é aí, e só aí, que
     * o cursor volta a zero.
     *
     * <p>O nível novo tem poço e salas próprios: eles ainda são rocha, e
     * por isso a ordem recomeça do primeiro degrau.
     */
    @Test
    void theFourthTurnGoesDownAndStartsTheOrderOver() {
        Mine mine = opened();

        int y = mine.shaft().positionAt(MineShaft.CARVED).y();

        for (int turn = 0; turn < Mine.TURNS_PER_LEVEL; turn++) {
            mine.nextPosition();
            mine.turn();
        }

        assertEquals(0, mine.cut(),
                "a descida não recomeçou a ordem de cavar");

        assertTrue(mine.shaft().positionAt(MineShaft.CARVED).y() < y,
                "a quarta curva não desceu de nível");
    }
}
