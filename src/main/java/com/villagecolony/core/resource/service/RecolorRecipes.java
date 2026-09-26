package com.villagecolony.core.resource.service;

import java.util.List;
import java.util.Optional;

/**
 * Receita que tinge uma peça pronta — decisão do autor, 2026-09-26: "ignorar
 * as receitas de tingir peça pronta".
 *
 * <p><b>O defeito.</b> O livro de receitas de 1.21 tem, para cada peça colorida,
 * a receita de base (duas lãs verdes → tapete verde) e a de retoque (corante
 * verde + um tapete de <i>outra</i> cor). A colônia pegava a primeira que o
 * livro devolvia, e na vila de 26-09 foi a de retoque: tapete verde pedia
 * tapete preto, que pedia tapete azul, que pedia lã azul — uma corrente que
 * nunca fecha, enquanto a obra esperava.
 *
 * <p><b>A regra.</b> Uma receita é retoque quando alguma casa dela aceita a
 * mesma peça em outra cor. A cor é lida do nome ({@code light_blue_bed} é
 * "cama", cor "light_blue"), como o próprio jogo nomeia os dezesseis corantes.
 *
 * <p><b>A exceção é a matéria-prima.</b> Lã é do pastor, e ele tosquia lã
 * branca: tingir lã é produção, não retoque. Corante misturado também
 * (vermelho + azul = roxo). Vidro e terracota sem cor já
 * ficam de fora sozinhos, porque o ingrediente não tem cor no nome.
 *
 * <p>Função pura sobre nomes, no {@code core}: o livro de receitas é do jogo e
 * fica na camada {@code fabric}, que só traduz a receita para cá.
 */
public final class RecolorRecipes {

    /** As cores do jogo, as de duas palavras primeiro — "light_blue" antes de "blue". */
    private static final List<String> COLOURS = List.of(
            "light_blue", "light_gray",
            "white", "orange", "magenta", "yellow", "lime", "pink", "gray",
            "cyan", "purple", "blue", "brown", "green", "red", "black");

    /**
     * O que se tinge como produção, e não como retoque: a lã que o pastor
     * tosquia branca e o corante que se mistura (vermelho + azul = roxo). A
     * primeira versão só tinha a lã, e a bateria mostrou o roxo sem produtor.
     */
    private static final java.util.Set<String> RAW_MATERIALS = java.util.Set.of("wool", "dye");

    private RecolorRecipes() {
    }

    /**
     * Se esta receita tinge uma peça pronta.
     *
     * @param resultPath o nome do resultado, sem o domínio ({@code green_carpet})
     * @param slotOptions para cada casa da receita, os nomes que ela aceita
     */
    public static boolean isRecolor(String resultPath, List<List<String>> slotOptions) {
        Optional<Coloured> result = split(resultPath);

        if (result.isEmpty() || RAW_MATERIALS.contains(result.get().base())) {
            return false;
        }

        for (List<String> options : slotOptions) {
            for (String option : options) {
                Optional<Coloured> ingredient = split(option);

                if (ingredient.isPresent()
                        && ingredient.get().base().equals(result.get().base())
                        && !ingredient.get().colour().equals(result.get().colour())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static Optional<Coloured> split(String path) {
        for (String colour : COLOURS) {
            String prefix = colour + "_";

            if (path.startsWith(prefix) && path.length() > prefix.length()) {
                return Optional.of(new Coloured(colour, path.substring(prefix.length())));
            }
        }

        return Optional.empty();
    }

    private record Coloured(String colour, String base) {
    }
}
