package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A árvore que já recusou duas vezes fica de fora por mais tempo —
 * 2026-09-02.
 *
 * <p><b>O prazo era um só, e curto demais para o que ele custa.</b> A
 * sessão de 2026-09-02 mediu o ciclo inteiro, duas vezes:
 *
 * <pre>
 * 19:18:36  made no progress for 2400 work ticks on the tree at 749, 63, 905
 * 19:18:36  Tree at 749, 63, 905 is out of reach — skipping it for 6000 ticks
 * 19:28:10  made no progress for 2400 work ticks on the tree at 749, 63, 905
 * </pre>
 *
 * <p>Cinco minutos de castigo, e nove minutos e trinta e quatro segundos
 * depois ela era de novo a árvore mais próxima. Cada volta custa
 * <b>dois minutos de expediente</b> — o {@code STALL_LIMIT} inteiro — e
 * nada na árvore mudou entre uma e outra. Aconteceu com duas árvores na
 * mesma sessão, quatro tentativas ao todo: perto de metade do tempo dos
 * dois lenhadores.
 *
 * <p>O prazo de dez ciclos continua certo <b>na primeira recusa</b>, e é
 * decisão do autor: o jogador constrói a ponte e vê o mod mudar de ideia
 * na mesma sessão. A segunda recusa é outra coisa — é prova de que a
 * primeira não foi azar.
 */
class TreeMarksTest {

    @Test
    void aTreeRefusedOnceSitsOutTheUsualTenCycles() {
        assertEquals(6000, TreeMarks.memoryFor(1));
    }

    @Test
    void aTreeRefusedAgainSitsOutLonger() {
        assertTrue(TreeMarks.memoryFor(2) > TreeMarks.memoryFor(1));
    }

    /**
     * E a terceira mais ainda — o castigo cresce enquanto a recusa se
     * repete, porque cada repetição é uma medida nova da mesma parede.
     */
    @Test
    void eachRefusalAfterThatCostsMore() {
        assertTrue(TreeMarks.memoryFor(3) > TreeMarks.memoryFor(2));
    }

    /**
     * Mas ele para de crescer.
     *
     * <p>Sem teto, uma árvore recusada vinte vezes ficaria de fora por
     * mais tempo que o servidor vive — e a Regra 23 vale aqui também: o
     * jogador aplaina o barranco, e a floresta volta a ser floresta.
     */
    @Test
    void theSitOutStopsGrowing() {
        assertEquals(TreeMarks.memoryFor(9), TreeMarks.memoryFor(20));
    }

    /** Recusa nenhuma não é castigo nenhum. */
    @Test
    void aTreeThatNeverRefusedIsNotHeldOut() {
        assertEquals(0, TreeMarks.memoryFor(0));
    }

    /**
     * E a mesma escada vale para a parede — 2026-09-09.
     *
     * <p>Estes cinco travam o E1 da sessão de 09-06, que é a marca
     * {@code REJECTED} pagando dez ciclos <b>para sempre</b>: 925 linhas
     * de {@code Not a tree} sobre 140 coordenadas, sete voltas por
     * parede, e 48 árvores derrubadas na sessão inteira.
     *
     * <p>Rodam sem mundo de propósito — ver
     * {@code TreeMarks.forgetStaleMarksAt}. A bateria de gametest não
     * avança o relógio, e o castigo mais curto é de 6.000 tiques: sem
     * entregar a hora não há como provar que a segunda recusa dura mais
     * que a primeira, que é a coisa toda que mudou.
     */
    @BeforeEach
    void forgetWhatOtherTestsRefused() {
        TreeMarks.forgetRejected();
    }

    /** A primeira recusa não mudou, e é decisão do autor: dez ciclos. */
    @Test
    void theFirstRefusalOfAWallStillLastsTenCycles() {
        List<BlockPos> wall = wallAt(0);

        TreeMarks.rejectAt(0, wall);

        assertTrue(TreeMarks.isRejectedAt(5999, wall.get(0)));
        assertFalse(TreeMarks.isRejectedAt(6000, wall.get(0)));
    }

    /**
     * A segunda dura mais, e é o E1 inteiro.
     *
     * <p>A parede é recusada, o prazo vence, ela é recusada de novo — e
     * agora o castigo é outro. Sem isto a busca reencontra o mesmo pilar
     * de cinco em cinco minutos, que é o que a sessão mediu.
     */
    @Test
    void aWallRefusedTwiceSitsOutLongerThanTheFirstTime() {
        List<BlockPos> wall = wallAt(0);

        TreeMarks.rejectAt(0, wall);
        TreeMarks.rejectAt(6000, wall);

        assertTrue(
                TreeMarks.isRejectedAt(6000 + 6000, wall.get(0)),
                "a segunda recusa venceu no mesmo prazo da primeira, e a parede volta à busca");
    }

    /**
     * E a contagem sobrevive ao castigo vencido.
     *
     * <p>Era o defeito: {@code isRejected} apagava a marca ao vê-la
     * vencida, então toda recusa era a primeira e o prazo nunca passava
     * de dez ciclos. Perguntar não pode custar a memória de ter
     * perguntado.
     */
    @Test
    void askingAfterTheDeadlineDoesNotEraseTheTally() {
        List<BlockPos> wall = wallAt(0);

        TreeMarks.rejectAt(0, wall);

        assertFalse(TreeMarks.isRejectedAt(6000, wall.get(0)), "o castigo devia ter vencido");

        TreeMarks.rejectAt(6000, wall);

        assertTrue(
                TreeMarks.isRejectedAt(11999, wall.get(0)),
                "a pergunta apagou a contagem, e a segunda recusa voltou a valer dez ciclos");
    }

    /**
     * A contagem é do grupo, e não do bloco por onde a busca entrou.
     *
     * <p>{@code logInColumn} devolve o primeiro tronco de <b>cada</b>
     * coluna, então uma parede é reencontrada por qualquer um dos seus
     * blocos. Ler a contagem só da primeira posição faria a parede voltar
     * a ser ré primária sempre que a busca entrasse por outro canto.
     */
    @Test
    void theTallyBelongsToTheWallAndNotToTheBlockTheSearchFound() {
        List<BlockPos> wall = wallAt(0);

        TreeMarks.rejectAt(0, wall);

        // A mesma parede, reencontrada de trás para frente.
        List<BlockPos> fromTheOtherEnd = new ArrayList<>(wall);
        Collections.reverse(fromTheOtherEnd);

        TreeMarks.rejectAt(6000, fromTheOtherEnd);

        assertTrue(
                TreeMarks.isRejectedAt(6000 + 6000, wall.get(0)),
                "entrar pelo outro canto zerou a contagem da mesma parede");
    }

    /**
     * Mas a parede do jogador volta a valer, e é a Regra 23.
     *
     * <p>O teto de {@link TreeMarks#memoryFor} é o que garante isto: por
     * mais que ela recuse, o castigo para de crescer e o mundo tem a
     * chance de ter mudado.
     */
    @Test
    void evenTheMostRefusedWallComesBackEventually() {
        List<BlockPos> wall = wallAt(0);

        for (int refusal = 0; refusal < 20; refusal++) {
            TreeMarks.rejectAt(refusal * 100_000L, wall);
        }

        long last = 19 * 100_000L;

        assertFalse(
                TreeMarks.isRejectedAt(last + TreeMarks.memoryFor(9), wall.get(0)),
                "o castigo passou do teto, e a floresta que voltou a crescer ficou de fora");
    }

    /** Um pilar de troncos, que é a forma que a marca guarda. */
    private static List<BlockPos> wallAt(int x) {
        return List.of(
                new BlockPos(x, 64, 0), new BlockPos(x, 65, 0), new BlockPos(x, 66, 0));
    }
}
