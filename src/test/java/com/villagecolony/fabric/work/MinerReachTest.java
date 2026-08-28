package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O alcance do mineiro, e a mentira que o relatório contava —
 * 2026-08-27.
 *
 * <p><b>A sessão das 22:19.</b> Os dois mineiros travaram, e o relatório
 * dizia a mesma coisa a cada trinta segundos:
 *
 * <pre>
 * digging Pedra at 728, 44, 878, 4 blocks away, 5/6 ticks, stall 2219/2400
 * </pre>
 *
 * <p>"Quatro blocos" e o alcance é quatro: parecia que ele estava no
 * lugar certo e não batia. Mas as duas frases mediam coisas diferentes —
 * o relatório usava {@code getBlockPos()}, que é inteiro, e ainda
 * truncava a raiz; o alcance usa a posição real do aldeão. **Qualquer
 * distância entre 4,0 e 4,99 aparecia como "4 blocks away" e estava
 * fora de alcance.**
 *
 * <p>Instrumento que mente é pior que instrumento nenhum: ele mandou
 * procurar o defeito onde ele não estava. A conta passou a ser uma só, e
 * é esta.
 */
class MinerReachTest {

    private static final BlockPos STONE = new BlockPos(728, 44, 878);

    /** Encostado na pedra: dentro de alcance, sem discussão. */
    @Test
    void besideTheStoneIsWithinReach() {
        assertTrue(MinerReach.isWithinReach(727.5, 44.0, 878.5, STONE));
    }

    /**
     * Quatro e meio está fora, e o relatório antigo dizia "quatro".
     *
     * <p>É o caso exato da sessão: longe o bastante para o alcance
     * recusar, perto o bastante para o log dizer que ele chegou.
     */
    @Test
    void fourAndAHalfIsOutOfReachEvenThoughItRoundsToFour() {
        double x = STONE.getX() + 0.5 - 4.5;

        assertFalse(MinerReach.isWithinReach(x, STONE.getY() + 0.5, STONE.getZ() + 0.5, STONE));

        assertEquals(
                4,
                (int) MinerReach.distanceTo(x, STONE.getY() + 0.5, STONE.getZ() + 0.5, STONE),
                "a distância truncada é a que o relatório mostrava");
    }

    /** A borda pertence a quem alcança: exatamente quatro ainda vale. */
    @Test
    void exactlyFourStillReaches() {
        double x = STONE.getX() + 0.5 - MinerReach.REACH;

        assertTrue(MinerReach.isWithinReach(x, STONE.getY() + 0.5, STONE.getZ() + 0.5, STONE));
    }

    /**
     * A altura conta, e é o E30.
     *
     * <p>Quatro blocos no plano mais quatro de altura são cinco e meia de
     * distância real. Enquanto o alcance ignorava o {@code dy}, o mineiro
     * batia na pedra de cima do buraco e ela caía.
     */
    @Test
    void heightCountsTowardTheDistance() {
        assertFalse(MinerReach.isWithinReach(
                STONE.getX() + 0.5 - 4, STONE.getY() + 0.5 + 4, STONE.getZ() + 0.5, STONE));
    }

    /**
     * A distância que o relatório mostra é a que o alcance usa.
     *
     * <p>A afirmação inteira deste ciclo: uma conta só. Enquanto forem
     * duas, o log continua podendo dizer "chegou" sobre quem não chegou.
     */
    @Test
    void theReportedDistanceIsTheOneTheReachUses() {
        double x = 724.2;
        double y = 44.0;
        double z = 878.7;

        double reported = MinerReach.distanceTo(x, y, z, STONE);

        assertEquals(
                reported <= MinerReach.REACH,
                MinerReach.isWithinReach(x, y, z, STONE),
                "o número mostrado e a decisão de alcance discordaram");
    }
}
