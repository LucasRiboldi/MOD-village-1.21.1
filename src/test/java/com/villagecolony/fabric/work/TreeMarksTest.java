package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
