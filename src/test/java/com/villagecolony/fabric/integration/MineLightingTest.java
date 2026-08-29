package com.villagecolony.fabric.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * De quantos em quantos blocos a mina ganha luz — 2026-08-28.
 *
 * <p><b>Só a boca tinha lanterna, e nem sempre.</b> A sessão de
 * 2026-08-26, 23:20:18 saiu com {@code lantern at nowhere it fits}, e a
 * galeria continua sem uma única luz depois dela. Vinte blocos abaixo do
 * chão, com luz zero, é criatura nascendo <b>dentro</b> da mina — e o
 * mineiro é um aldeão desarmado.
 *
 * <p>A conta que este teste trava é só a do <b>espaçamento</b>: qual
 * posição da ordem de cavar pede tocha, dado o que já foi aberto. O
 * resto — se cabe, se tem apoio, se já tem uma lá — é pergunta para o
 * mundo, e vive na bateria de jogo.
 */
class MineLightingTest {

    /** Mina que não abriu posição nenhuma não tem onde pôr tocha. */
    @Test
    void anUnopenedMineHasNowhereToLight() {
        assertEquals(-1, MineLighting.spotFor(0));
    }

    /** A primeira posição aberta é a primeira a receber luz. */
    @Test
    void theFirstOpenedPositionIsTheFirstLit() {
        assertEquals(0, MineLighting.spotFor(1));
    }

    /**
     * Dentro do primeiro trecho, a tocha continua sendo a da boca.
     *
     * <p>Oito posições abertas ainda são o primeiro espaçamento: a luz
     * de índice zero cobre todas elas.
     */
    @Test
    void oneSpacingStillBelongsToTheFirstTorch() {
        assertEquals(0, MineLighting.spotFor(MineLighting.SPACING));
    }

    /** Passado o trecho, nasce o ponto de luz seguinte. */
    @Test
    void theNextSpacingOpensTheNextTorch() {
        assertEquals(MineLighting.SPACING, MineLighting.spotFor(MineLighting.SPACING + 1));
    }

    /** É sempre o mais recente múltiplo do espaçamento já aberto. */
    @Test
    void theSpotIsTheNewestMultipleAlreadyOpen() {
        assertEquals(16, MineLighting.spotFor(18));
        assertEquals(16, MineLighting.spotFor(24));
        assertEquals(24, MineLighting.spotFor(25));
    }

    /** Espaçamento negativo ou cursor inválido não inventa posição. */
    @Test
    void anImpossibleCursorLightsNothing() {
        assertEquals(-1, MineLighting.spotFor(-3));
    }
}
