package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A mina em uso: os ramais, os cursores e a descida.
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
     * Cada ramal tem o cursor dele — decisão do autor, 2026-09-04.
     *
     * <p><b>É a propriedade inteira do pedido.</b> Havia um cursor só, e
     * era por isso que a mina tinha um dono: dois mineiros perguntando na
     * mesma passagem recebiam a mesma posição. Na sessão de 09-04 o
     * terceiro mineiro passou os trinta e sete minutos em {@code waiting
     * for the shaft}.
     *
     * <p>Cavar num ramal não pode mover a frente de nenhum outro.
     */
    @Test
    void diggingInOneBranchDoesNotMoveTheOthers() {
        Mine mine = opened();

        for (int i = 0; i < 40; i++) {
            mine.arm(0).nextPosition();
        }

        assertEquals(40, mine.arm(0).cut());

        for (int index = 1; index < Mine.ARMS; index++) {
            assertEquals(0, mine.arm(index).cut(),
                    "o ramal " + index + " andou junto com o primeiro");
        }
    }

    /**
     * E dois ramais na mesma posição da ordem cavam blocos diferentes.
     *
     * <p>É o que faz deles ramais, e não turnos: o índice é o mesmo, o
     * rumo da galeria não é. Abaixo de {@link MineShaft#CARVED} eles
     * <b>coincidem</b> de propósito — a escada e as salas são do poço, e
     * quem chegar primeiro as abre.
     */
    @Test
    void twoBranchesAtTheSameIndexDigDifferentBlocks() {
        Mine mine = opened();

        int gallery = MineShaft.CARVED + 4;

        assertEquals(
                mine.arm(0).shaft().positionAt(MineShaft.CARVED - 1),
                mine.arm(1).shaft().positionAt(MineShaft.CARVED - 1),
                "os ramais cavaram escadas diferentes");

        for (int index = 1; index < Mine.ARMS; index++) {
            assertFalse(
                    mine.arm(0).shaft().positionAt(gallery)
                            .equals(mine.arm(index).shaft().positionAt(gallery)),
                    "o ramal " + index + " cava o mesmo bloco que o primeiro");
        }
    }

    /**
     * O ramal acaba, e a mina avisa antes de gastar a posição.
     *
     * <p>É o que o {@code MineDigging} pergunta a cada olhada, e é onde o
     * teto de raio do autor vira comportamento.
     */
    @Test
    void theBranchSaysWhenItIsOver() {
        MineArm arm = opened().arm(0);

        assertFalse(arm.reachedTheEndOfTheArm(),
                "a mina recém-aberta já se diz no fim do braço");

        while (!arm.reachedTheEndOfTheArm()) {
            arm.nextPosition();

            assertTrue(arm.cut() < MineShaft.CARVED + 10_000,
                    "o braço nunca acabou — o teto de raio não pegou");
        }

        assertTrue(arm.cut() > MineShaft.CARVED,
                "o fim do braço caiu antes de a galeria começar");
    }

    /**
     * Um ramal que fecha não fecha os outros.
     *
     * <p>A lava é de um lugar. Encerrar os quatro tiraria três mineiros
     * de frentes que estão secas — e foi por isso que a contagem de
     * recusas saiu da {@link Mine} e passou para o {@link MineArm}.
     */
    @Test
    void aFinishedBranchLeavesTheOthersOpen() {
        Mine mine = opened();

        mine.arm(0).finish();

        assertTrue(mine.arm(0).isDone());
        assertFalse(mine.everyArmIsDone(), "um ramal fechado fechou a mina inteira");

        assertEquals(1, mine.firstArmStillOpen().orElseThrow(),
                "o mineiro seguinte não foi mandado ao primeiro ramal livre");
    }

    /**
     * Fechados os quatro, a mina desce e a ordem recomeça.
     *
     * <p>É a regra das quatro curvas de 2026-09-02, contada de outro
     * jeito: os mesmos quatro rumos, agora podendo ser fechados por
     * quatro aldeões ao mesmo tempo em vez de um só, quatro vezes.
     */
    @Test
    void theFourthFinishedBranchTakesTheMineDown() {
        Mine mine = opened();

        int y = mine.shaft().positionAt(MineShaft.CARVED).y();

        for (int index = 0; index < Mine.ARMS - 1; index++) {
            mine.arm(index).finish();

            assertFalse(mine.deepenIfEveryArmIsDone(),
                    "ela desceu com o ramal " + index + " ainda aberto");
        }

        mine.arm(Mine.ARMS - 1).finish();

        assertTrue(mine.deepenIfEveryArmIsDone(), "o quarto ramal fechou e ela não desceu");

        assertTrue(mine.shaft().positionAt(MineShaft.CARVED).y() < y,
                "a mina não desceu de nível");

        for (int index = 0; index < Mine.ARMS; index++) {
            assertEquals(0, mine.arm(index).cut(),
                    "o ramal " + index + " não recomeçou do primeiro degrau");

            assertFalse(mine.arm(index).isDone(),
                    "o ramal " + index + " desceu já fechado");
        }
    }

    /**
     * O save de um ramal só reabre os outros três no primeiro degrau.
     *
     * <p>É o caminho do disco anterior a 2026-09-04. Nenhum dos três
     * aponta para lugar errado: são rumos que ninguém cavou ainda.
     */
    @Test
    void anOldSaveComesBackWithOneBranchAdvanced() {
        Mine mine = Mine.restore(UUID.randomUUID(), MineShaft.from(ENTRY, Side.NORTH), 437);

        assertEquals(437, mine.arm(0).cut());

        for (int index = 1; index < Mine.ARMS; index++) {
            assertEquals(0, mine.arm(index).cut(),
                    "o ramal " + index + " herdou a fronteira do primeiro");
        }
    }

    /** E a fronteira de cada ramal volta inteira pelo disco. */
    @Test
    void everyBranchFrontierSurvivesTheRoundTrip() {
        int[] cuts = {200, 180, 160, 0};

        Mine mine = Mine.restore(UUID.randomUUID(), MineShaft.from(ENTRY, Side.NORTH), cuts);

        assertEquals(cuts.length, mine.cuts().length);

        for (int index = 0; index < cuts.length; index++) {
            assertEquals(cuts[index], mine.cuts()[index],
                    "a fronteira do ramal " + index + " não voltou");
        }
    }

    /**
     * O poço é de um mineiro só até a galeria começar — 2026-09-04.
     *
     * <p><b>Foi um gametest que pegou isto</b>, e ele pegou com dois
     * mineiros recebendo o <b>mesmo bloco</b>. Abaixo de
     * {@link MineShaft#CARVED} os quatro ramais apontam para as mesmas
     * posições — é o que faz deles ramais da mesma escada —, e repartir
     * antes disso é o defeito de 2026-08-26 de volta: os dois andam para
     * o mesmo lugar, os dois escrevem {@code could not reach the stone}
     * no mesmo tique, e o recuo do cursor roda duas vezes por um bloco.
     */
    @Test
    void theSharedPitIsDugByOneMinerAtATime() {
        Mine mine = opened();

        assertEquals(1, mine.branchesOpenNow(),
                "a mina recém-aberta já repartiu o poço");

        while (mine.arm(0).cut() < MineShaft.CARVED - 1) {
            mine.arm(0).nextPosition();
        }

        assertEquals(1, mine.branchesOpenNow(),
                "repartiu com o último bloco do poço ainda por olhar");

        mine.arm(0).nextPosition();

        assertEquals(Mine.ARMS, mine.branchesOpenNow(),
                "aberto o poço, os quatro ramais deviam abrir");
    }
}
