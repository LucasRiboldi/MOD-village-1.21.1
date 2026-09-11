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

    // ------------------------------------------------------------------
    // O beco — 2026-09-10, sessão das 22:57.
    //
    // Pergunta dos TRÊS leitores de proximidade (veia, pedra de
    // superfície, areia), e não do cursor do túnel: lá a defesa é a
    // curva do BLOCKED_BEFORE_TURNING, e o beco a destruiria. Ver o
    // javadoc de MineMarks.isInADeadEndAt.
    //
    // A escada de cima funcionou em jogo e não bastou: os alvos andavam
    // 2423 → 2422 → 2421 sem repetir um único, e os oito mineiros
    // paravam nos mesmos dois lugares, dois blocos abaixo do ponto de
    // apoio com "unable to climb". A pedra nunca se repetia porque o
    // cursor tinha pedra de sobra ATRÁS DO MESMO VÃO.
    // ------------------------------------------------------------------

    /** As três pedras do beco de -1424, na ordem em que a sessão as viu. */
    private static final BlockPos FIRST_OF_THE_DEAD_END = new BlockPos(2423, 45, -1424);

    private static final BlockPos SECOND_OF_THE_DEAD_END = new BlockPos(2422, 45, -1424);

    private static final BlockPos THIRD_OF_THE_DEAD_END = new BlockPos(2421, 44, -1424);

    /** E uma pedra do outro ramal, a vinte blocos. */
    private static final BlockPos FAR_AWAY = new BlockPos(2442, 44, -1424);

    private void refuseTheDeadEnd(long now) {
        MineMarks.refuseAt(now, FIRST_OF_THE_DEAD_END);
        MineMarks.refuseAt(now, SECOND_OF_THE_DEAD_END);
        MineMarks.refuseAt(now, THIRD_OF_THE_DEAD_END);
    }

    /**
     * <b>O caso da sessão.</b> Uma pedra nunca recusada, cercada de
     * recusas, não deve ser servida: é o vão que é intransponível, e não
     * cada bloco.
     */
    @Test
    void aStoneInsideADeadEndIsSkippedEvenIfItNeverRefused() {
        refuseTheDeadEnd(1000);

        BlockPos neverRefused = new BlockPos(2420, 45, -1424);

        assertFalse(
                MineMarks.isInADeadEndAt(1000, FAR_AWAY),
                "o outro ramal continua valendo");

        assertTrue(
                MineMarks.isInADeadEndAt(1000, neverRefused),
                "a pedra atrás do mesmo vão não é servida");
    }

    /** Duas recusas vizinhas ainda são azar; três são padrão. */
    @Test
    void twoRefusalsNearbyAreNotYetADeadEnd() {
        MineMarks.refuseAt(1000, FIRST_OF_THE_DEAD_END);
        MineMarks.refuseAt(1000, SECOND_OF_THE_DEAD_END);

        assertFalse(
                MineMarks.isInADeadEndAt(1000, new BlockPos(2420, 45, -1424)),
                "duas ainda podem ser dois alvos ruins");

        MineMarks.refuseAt(1000, THIRD_OF_THE_DEAD_END);

        assertTrue(
                MineMarks.isInADeadEndAt(1000, new BlockPos(2420, 45, -1424)),
                "a terceira faz padrão");
    }

    /** O beco é local: um ramal distante não paga pelo vão daqui. */
    @Test
    void aDeadEndDoesNotCloseTheWholeMine() {
        refuseTheDeadEnd(1000);

        assertFalse(
                MineMarks.isInADeadEndAt(1000, FAR_AWAY),
                "vinte blocos adiante é outro lugar");
    }

    /**
     * <b>O beco não é eterno.</b> A entrada sobrevive ao castigo para
     * carregar a contagem — ver isOutOfReachAt —, então contar entradas
     * em vez de recusas vivas faria a região ressuscitar sozinha.
     */
    @Test
    void aDeadEndOpensWhenItsRefusalsExpire() {
        refuseTheDeadEnd(1000);

        long afterTheFirstPunishment = 1000 + MineMarks.memoryFor(1);

        assertFalse(
                MineMarks.isInADeadEndAt(afterTheFirstPunishment, FIRST_OF_THE_DEAD_END),
                "vencido o castigo das três, o beco abre");
    }

    /**
     * <b>E a picareta desfaz o beco na hora.</b> Cavar aqui prova por
     * execução que o trecho é alcançável, e isso vale mais que qualquer
     * previsão: é assim que "o jogador constrói a rampa e o mod muda de
     * ideia" se cumpre para a região.
     */
    @Test
    void diggingInsideTheDeadEndOpensTheWholeNeighbourhood() {
        refuseTheDeadEnd(1000);

        MineMarks.dug(SECOND_OF_THE_DEAD_END);

        assertFalse(
                MineMarks.isOutOfReachAt(1000, FIRST_OF_THE_DEAD_END),
                "a vizinha sai do castigo junto");

        assertFalse(
                MineMarks.isOutOfReachAt(1000, THIRD_OF_THE_DEAD_END),
                "e a outra também");

        assertFalse(
                MineMarks.isInADeadEndAt(1000, FIRST_OF_THE_DEAD_END),
                "e o beco deixa de existir");
    }

    /** Cavar longe não abre o beco: a prova é local, como o beco. */
    @Test
    void diggingFarAwayLeavesTheDeadEndClosed() {
        refuseTheDeadEnd(1000);

        MineMarks.dug(FAR_AWAY);

        assertTrue(
                MineMarks.isInADeadEndAt(1000, FIRST_OF_THE_DEAD_END),
                "o beco continua fechado");
    }
}
