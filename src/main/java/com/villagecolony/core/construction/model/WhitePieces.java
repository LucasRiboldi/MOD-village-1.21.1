package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;

import java.util.List;
import java.util.Set;

/**
 * Cama, lã, tapete e estandarte de qualquer cor saem brancos — decisão do autor, 2026-10-27 (item 5): a
 * obra aceita a peça da cor que a vila tem, a branca primeiro. A lã que o rebanho dá é branca, e a peça
 * colorida pedia corante que a vila não fabrica: no jogo da 0.3.93 houve 64 "cama sem lã" e lã preta
 * parada por corante.
 *
 * <p>Puro, e aplicado onde a planta é montada ({@code StructureBlueprintReader}), como a areia do deserto:
 * quem pergunta o que a obra precisa e quem assenta leem a mesma planta.
 */
public final class WhitePieces {

    /** As famílias que mudam de cor, pelo fim do nome. */
    static final List<String> FAMILIES = List.of("_bed", "_wool", "_carpet", "_banner", "_wall_banner");

    /** As dezesseis cores do jogo. */
    static final Set<String> COLORS = Set.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
            "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");

    private WhitePieces() {
    }

    /** A planta com as peças coloridas em branco. */
    public static List<BlueprintBlock> baked(List<BlueprintBlock> blocks) {
        return blocks.stream()
                .map(block -> {
                    ResourceId white = whiteOf(block.block());

                    return white.equals(block.block()) ? block
                            : new BlueprintBlock(block.offset(), white, block.furniture(), block.facing(), block.states());
                })
                .toList();
    }

    /** A versão branca desta peça, ou ela mesma quando não é de cor. */
    public static ResourceId whiteOf(ResourceId block) {
        if (!ResourceId.VANILLA.equals(block.namespace())) {
            return block;
        }

        String path = block.path();

        for (String family : FAMILIES) {
            if (!path.endsWith(family)) {
                continue;
            }

            String color = path.substring(0, path.length() - family.length());

            // "_wall_banner" termina também em "_banner": a cor é o que sobra antes da família inteira.
            if (COLORS.contains(color)) {
                return color.equals("white") ? block : ResourceId.vanilla("white" + family);
            }
        }

        return block;
    }
}
