package com.villagecolony.core.construction.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Até onde o construtor sobe sozinho — 2026-09-16.
 *
 * <p><b>Pedido do autor:</b> <i>"pode melhorar para facilitar que o
 * trabalhador consiga subir nos blocos conforme a construção suba, não
 * fazer uma diferença maior de 2 blocos, o que impede o aldeão de subir e
 * continuar construindo"</i>.
 *
 * <p><b>O que o log de 02:58 mostrou.</b> A obra progredia — de 61 para 37
 * blocos restantes — e mesmo assim nenhuma casa terminou. O construtor
 * travava assim:
 *
 * <pre>
 * the builder has not moved a block in 300 ticks on the way to 2515, 67, -3054
 * the worker is at 2506, 68, -3051, 9 blocks away
 * the lot floor at 2515, 63, -3054 is Pedregulho
 * </pre>
 *
 * <p>Ele estava em <b>y=68</b>, em cima da própria obra, e o destino
 * calculado era o <b>piso do lote, y=63</b> — cinco blocos abaixo. Um
 * aldeão desce degrau de um; cinco é queda, e a navegação Vanilla
 * simplesmente não anda para lá.
 *
 * <p>A régua do autor é <b>dois</b>: é o que um aldeão vence subindo com
 * um degrau intermediário, e o que ele desce sem dano.
 */
class ClimbLimitTest {

    /** O degrau que um aldeão vence sozinho, subindo. */
    @Test
    void oneBlockIsAlwaysClimbable() {
        assertTrue(ClimbLimit.reachableFrom(64, 65), "um bloco acima é degrau comum");
    }

    /** Dois é o limite que o autor pediu. */
    @Test
    void twoBlocksIsTheLimitTheAuthorAsked() {
        assertTrue(ClimbLimit.reachableFrom(64, 66), "dois blocos acima é o limite pedido");
    }

    /**
     * Três já não: é onde o construtor precisa de ajuda.
     *
     * <p>É a fronteira que decide se a obra continua sozinha ou se o
     * andaime entra.
     */
    @Test
    void threeBlocksNeedsHelp() {
        assertFalse(
                ClimbLimit.reachableFrom(64, 67),
                "três blocos acima passaram por alcançáveis, e é onde o aldeão trava");
    }

    /**
     * Descer é mais generoso que subir, mas não é livre.
     *
     * <p>O caso do log: o construtor em y=68 mandado para y=63. Cinco
     * blocos de queda não é caminho.
     */
    @Test
    void theFallOfTheLogIsNotAPath() {
        assertFalse(
                ClimbLimit.reachableFrom(68, 63),
                "cinco blocos de queda passaram por caminho — é o travamento de 02:58");
    }

    /** Descer dois continua sendo caminho. */
    @Test
    void steppingDownTwoIsStillAPath() {
        assertTrue(ClimbLimit.reachableFrom(66, 64));
    }

    /** Mesmo nível é trivialmente alcançável. */
    @Test
    void theSameLevelIsReachable() {
        assertTrue(ClimbLimit.reachableFrom(64, 64));
    }

    /**
     * O degrau seguinte a partir de onde ele está.
     *
     * <p>É o que a construção em camadas usa: em vez de mandar o
     * construtor ao piso, manda-o ao patamar que ele alcança.
     */
    @Test
    void theNextLandingIsWithinTheLimit() {
        assertEquals(
                66,
                ClimbLimit.landingBetween(64, 70),
                "o patamar seguinte tinha de estar a dois do pé dele");

        assertEquals(
                70,
                ClimbLimit.landingBetween(69, 70),
                "com o alvo ao alcance, o patamar é o próprio alvo");
    }
}
