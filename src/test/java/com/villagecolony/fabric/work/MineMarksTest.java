package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A pedra que o mineiro não alcança fica de fora por um prazo — E44,
 * 2026-09-10.
 *
 * <p><b>O cursor segurava a posição para sempre, e ela prendia os dois
 * mineiros.</b> A sessão de 2026-09-10 às 08:33 mediu o laço inteiro, e a
 * queixa do autor foi a forma dele: <i>"mineiros não estão trabalhando e
 * estão posicionados no mesmo túnel, um atrás do outro"</i>.
 *
 * <pre>
 * 08:33     4c4171a4 mira 2442,44,-1424 — out of reach, 9,9 blocos
 * 08:34:13  desiste: "walked for 2400 ticks ... without arriving"
 * 08:34:43  d5f6de43 assume o ramal e recebe A MESMA pedra
 * 08:36:17  desiste com a mesma frase
 * </pre>
 *
 * <p>Cada volta custa os 2.400 tiques do guarda de travamento, e não há
 * saída: o {@code couldNotReach} devolve a posição ao cursor — o que está
 * certo, pular por uma desistência deixou três sessões com a galeria
 * intacta em 2026-08-27 — e nada nunca a soltava.
 *
 * <p>O relógio é entregue em vez de lido do mundo pelo mesmo motivo do
 * {@code TreeMarksTest}: o castigo mais curto é de 6.000 tiques, e nenhum
 * {@code tickLimit} de bateria chega perto.
 */
class MineMarksTest {

    private static final BlockPos THE_STONE = new BlockPos(2442, 44, -1424);

    private static final BlockPos ANOTHER_STONE = new BlockPos(2443, 44, -1424);

    @BeforeEach
    void forgetEverything() {
        MineMarks.clearAll();
    }

    @Test
    void aStoneRefusedOnceSitsOutTenCycles() {
        assertEquals(6000, MineMarks.memoryFor(1));
    }

    @Test
    void aStoneRefusedAgainSitsOutLonger() {
        assertTrue(MineMarks.memoryFor(2) > MineMarks.memoryFor(1));
    }

    @Test
    void eachRefusalAfterThatCostsMore() {
        assertTrue(MineMarks.memoryFor(3) > MineMarks.memoryFor(2));
    }

    /** E a escada tem teto: senão a pedra sairia da mina para sempre. */
    @Test
    void theLadderStopsClimbing() {
        assertEquals(MineMarks.memoryFor(20), MineMarks.memoryFor(30));
    }

    @Test
    void aStoneNeverRefusedIsFairGame() {
        assertFalse(MineMarks.isOutOfReachAt(0, THE_STONE));
    }

    /**
     * O caso do E44, e é o que a sessão viu: o segundo mineiro assume o
     * ramal trinta segundos depois e <b>não</b> pode receber a mesma
     * pedra.
     */
    @Test
    void theSecondMinerDoesNotGetTheStoneTheFirstOneGaveUp() {
        MineMarks.refuseAt(1000, THE_STONE);

        assertTrue(
                MineMarks.isOutOfReachAt(1600, THE_STONE),
                "a pedra que acabou de custar 2.400 tiques continua de castigo");
    }

    /** E o castigo é da pedra, não da mina inteira. */
    @Test
    void theStoneBesideItIsStillFairGame() {
        MineMarks.refuseAt(1000, THE_STONE);

        assertFalse(MineMarks.isOutOfReachAt(1600, ANOTHER_STONE));
    }

    /**
     * O prazo vence, e a pedra volta — é o que separa castigo de exílio.
     * O jogador cava a rampa até ela e o mod muda de ideia.
     */
    @Test
    void thePunishmentEndsAndTheStoneComesBack() {
        MineMarks.refuseAt(1000, THE_STONE);

        assertFalse(
                MineMarks.isOutOfReachAt(1000 + MineMarks.memoryFor(1), THE_STONE),
                "vencido o prazo, a picareta pode tentar de novo");
    }

    /**
     * <b>E a contagem sobrevive ao castigo</b> — é o que faz a escada
     * existir. Se ela morresse junto, toda recusa seria a primeira e o
     * prazo nunca passaria de dez ciclos, que é o laço com outro nome.
     */
    @Test
    void theSecondRefusalCostsMoreThanTheFirst() {
        MineMarks.refuseAt(1000, THE_STONE);

        long afterTheFirst = 1000 + MineMarks.memoryFor(1);

        MineMarks.refuseAt(afterTheFirst, THE_STONE);

        assertTrue(
                MineMarks.isOutOfReachAt(afterTheFirst + MineMarks.memoryFor(1), THE_STONE),
                "a segunda recusa dura mais que a primeira");
    }

    /** A picareta pegou: a pedra deixa de ser suspeita. */
    @Test
    void diggingItClearsTheMark() {
        MineMarks.refuseAt(1000, THE_STONE);

        MineMarks.dug(THE_STONE);

        assertFalse(MineMarks.isOutOfReachAt(1600, THE_STONE));
    }

    /**
     * E a contagem também vence, um dia. Sem isto o mapa cresceria
     * enquanto o servidor vivesse, e a pedra de ontem puniria a mina de
     * amanhã.
     */
    @Test
    void theTallyItselfIsForgottenEventually() {
        MineMarks.refuseAt(1000, THE_STONE);

        MineMarks.forgetStaleMarksAt(1000 + 2L * 6000 * 8);

        assertEquals(0, MineMarks.size(), "a marca vencida saiu do mapa");
    }
}
