package com.villagecolony.core.storage.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quanto de uma varredura em círculo cai dentro de um cone — 2026-09-16.
 *
 * <p><b>O defeito que esta conta mede.</b> A coleta de terra e grama
 * procura <b>fora</b> da vila, num cone de 90° a partir do centro
 * deslocado — é a Regra 3 protegendo a vila de ser escavada. A varredura,
 * porém, é um <b>círculo</b> de raio 48: 9.409 colunas, contra um
 * orçamento de 1.024 por passagem.
 *
 * <p>O log de 01:19 mostrou o preço: <b>24 de 26</b> ciclos sem coleta
 * diziam <i>"still sweeping — the budget ran out before an answer —
 * dirt"</i>, e as obras esperaram {@code dirt} 93 vezes e
 * {@code grass_block} 42 vezes na semana — 78% de todas as esperas.
 *
 * <p>Este caso guarda a aritmética que justifica pular a coluna fora do
 * setor <b>antes</b> de gastar orçamento com ela: sem isso, três quartos
 * de cada passagem são pagos para descartar.
 */
class SectorBudgetTest {

    /** Quantas colunas tem um círculo de varredura deste raio. */
    private static int circle(int radius) {
        return (2 * radius + 1) * (2 * radius + 1);
    }

    /**
     * O cone de 90° é um quarto do círculo, e é o que sobra de útil.
     *
     * <p>{@code isInSector} exige {@code forward > 0} e
     * {@code |lateral| <= forward} — um quadrante, a menos da metade dele
     * que o {@code PROTECTED_RADIUS} ainda corta.
     */
    @Test
    void theSectorIsAboutAQuarterOfTheCircle() {
        int radius = 48;
        int total = circle(radius);

        assertTrue(
                total > 9000,
                "o círculo de raio 48 tem " + total + " colunas — a conta do orçamento"
                        + " parte daqui");

        assertTrue(
                total / 4 < 2500,
                "o cone útil é cerca de um quarto; o resto é orçamento gasto para"
                        + " descartar");
    }

    /**
     * Com orçamento de 1.024, uma volta inteira leva dez passagens.
     *
     * <p>Dez passagens são dez ciclos de trinta segundos: cinco minutos
     * até a primeira resposta, e o log mostrou a colônia não chegando lá.
     */
    @Test
    void aFullSweepCostsTenPassesAtTheCurrentBudget() {
        int passes = (circle(48) + 1023) / 1024;

        assertTrue(
                passes >= 9,
                "uma volta custa " + passes + " passagens — é por isso que a coleta de"
                        + " terra nunca respondia");
    }
}
