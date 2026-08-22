package com.villagecolony.core.construction.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * O que o bioma de uma vila dá — a Regra 20, encolhida em 2026-08-21.
 *
 * <p>A paleta chegou a dizer também de que é a parede e a porta de cada
 * estilo. Havia um só leitor para os dois — a cabana escrita em código —
 * e ela saiu com a Regra 27. O que se afirma aqui é o que sobrou, e o
 * que sobrou tem quem pergunte.
 */
class VillagePaletteTest {

    /**
     * O estilo é a pasta do catálogo, e é o elo com a Regra 27.
     *
     * <p>Ele não sai da madeira: bioma nevado usa pinheiro como a taiga,
     * e as casas dele são outras.
     */
    @Test
    void theStyleIsTheCatalogFolder() {
        assertEquals("taiga", VillagePalette.ofWood("taiga").style());
        assertEquals("snowy", VillagePalette.ofWood("snowy").style());
        assertEquals("desert", VillagePalette.ofSandstone().style());
    }

    /**
     * No deserto a pedra é arenito, e é ela que faz a vila construir.
     *
     * <p>É o que fecha o limite conhecido do deserto: até 2026-08-20 a
     * vila de deserto nascia, contratava, contava recurso e não
     * construía nunca, porque procurava pedregulho onde só há areia.
     */
    @Test
    void theDesertMinesTheStoneItHas() {
        assertEquals(VillagePalette.SANDSTONE, VillagePalette.ofSandstone().stone());
        assertEquals(VillagePalette.COBBLESTONE, VillagePalette.ofWood("plains").stone());
    }

    /** O vidro é o mesmo em toda parte: o fundidor faz um só. */
    @Test
    void everyVillageSmeltsTheSameGlass() {
        for (VillagePalette palette : new VillagePalette[] {
                VillagePalette.ofWood("plains"), VillagePalette.ofSandstone()}) {

            assertEquals(VillagePalette.GLASS, palette.glass());
        }
    }
}
