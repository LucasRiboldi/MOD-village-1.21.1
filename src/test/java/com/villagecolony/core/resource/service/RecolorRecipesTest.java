package com.villagecolony.core.resource.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Receita de tingir peça pronta — decisão do autor, 2026-09-26.
 *
 * <p>Na vila de 26-09 a colônia tentou o tapete verde por "corante verde +
 * tapete preto", que pedia "corante preto + tapete azul", e assim por diante:
 * uma corrente de tingimentos que nunca fecha. A receita de base (lã verde →
 * tapete) nem aparecia.
 */
class RecolorRecipesTest {

    private static final List<String> OTHER_CARPETS = List.of(
            "black_carpet", "blue_carpet", "white_carpet", "light_blue_carpet");

    @Test
    void dyeingAFinishedCarpetIsARecolor() {
        assertTrue(RecolorRecipes.isRecolor("green_carpet",
                List.of(List.of("green_dye"), OTHER_CARPETS)));
    }

    @Test
    void theBaseCarpetRecipeIsNot() {
        assertFalse(RecolorRecipes.isRecolor("green_carpet",
                List.of(List.of("green_wool"), List.of("green_wool"))));
        assertFalse(RecolorRecipes.isRecolor("white_carpet",
                List.of(List.of("white_wool"), List.of("white_wool"))));
    }

    /** Duas palavras de cor: "light_blue" não pode ser lido como "blue". */
    @Test
    void twoWordColoursAreReadWhole() {
        assertTrue(RecolorRecipes.isRecolor("light_blue_bed",
                List.of(List.of("light_blue_dye"), List.of("white_bed", "blue_bed"))));
        assertTrue(RecolorRecipes.isRecolor("blue_bed",
                List.of(List.of("blue_dye"), List.of("white_bed", "light_blue_bed"))));
    }

    /** Lã é matéria-prima do pastor: tingir lã branca é produção, não retoque. */
    @Test
    void dyeingWoolIsProductionNotARecolor() {
        assertFalse(RecolorRecipes.isRecolor("green_wool",
                List.of(List.of("green_dye"), List.of("white_wool", "black_wool"))));
    }

    /** Vidro e terracota sem cor são a matéria-prima: tingir é o caminho normal. */
    @Test
    void dyeingTheUncolouredBaseIsNotARecolor() {
        assertFalse(RecolorRecipes.isRecolor("green_stained_glass",
                List.of(List.of("glass"), List.of("green_dye"))));
        assertFalse(RecolorRecipes.isRecolor("cyan_terracotta",
                List.of(List.of("terracotta"), List.of("cyan_dye"))));
    }

    @Test
    void anUncolouredResultIsNeverARecolor() {
        assertFalse(RecolorRecipes.isRecolor("oak_door",
                List.of(List.of("oak_planks"), List.of("oak_planks"))));
        assertFalse(RecolorRecipes.isRecolor("glass_pane", List.of(List.of("glass"))));
    }

    /** O mesmo produto na mesma cor não é recolorir (ex.: a receita que junta peças iguais). */
    @Test
    void theSameColourIsNotARecolor() {
        assertFalse(RecolorRecipes.isRecolor("green_carpet", List.of(List.of("green_carpet"))));
    }

    /** Misturar corante é produção — a bateria mostrou o roxo sem produtor na primeira versão. */
    @Test
    void mixingDyesIsProductionNotARecolor() {
        assertFalse(RecolorRecipes.isRecolor("purple_dye",
                List.of(List.of("red_dye"), List.of("blue_dye"))));
        assertFalse(RecolorRecipes.isRecolor("lime_dye",
                List.of(List.of("green_dye"), List.of("white_dye"))));
    }

    /** Nome que é só a cor ("green_") não é peça: nada a retocar (o PIT pediu). */
    @Test
    void aColourWithNothingAfterItIsNotAPiece() {
        assertFalse(RecolorRecipes.isRecolor("green_", List.of(List.of("red_"))));
    }
}
