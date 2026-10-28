package com.villagecolony.fabric.work;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;

/**
 * As peças que aparecem quando a obra pede — decisão do autor, 2026-10-21: terracota (todas as cores
 * e a vitrificada), fardo de feno e argila. No deserto jogado elas não tinham rota (terracota
 * verde-limão 4×, feno, argila, terracota branca vitrificada), a obra as punha de lado e o carpinteiro
 * e o construtor ficavam esperando.
 *
 * <p>E o gelo — gelo, gelo compactado e gelo azul (autor, 2026-10-28, T1 do diagnóstico da 0.3.103: 162 gelos
 * compactados postos de lado na vila da neve): o jogo só o dá com toque suave, e nenhuma profissão o obtém.
 *
 * <p>O baú vem primeiro: a que a vila já tem é usada. Sem ela, a peça surge no lugar, como a horta
 * (BuilderPlacement), e não vira pedido de material nem falta no registro.
 */
final class AppearingPieces {

    private AppearingPieces() {
    }

    /** Se esta peça aparece quando a obra a pede, sem esperar material. */
    static boolean appears(Block block) {
        return block.getDefaultState().isIn(BlockTags.TERRACOTTA)
                || Registries.BLOCK.getId(block).getPath().endsWith("_glazed_terracotta")
                || block == Blocks.HAY_BLOCK
                || block == Blocks.CLAY
                || block == Blocks.ICE
                || block == Blocks.PACKED_ICE
                || block == Blocks.BLUE_ICE;
    }

    /**
     * Se a obra põe esta peça sem estoque: moldada do chão (a horta: terra arada, plantação, água),
     * nunca posta, ou que aparece. Ninguém a pede ao fabricante nem vai buscá-la no chão — no playtest
     * 0.3.97 a colônia pediu trigo ao carpinteiro 19 vezes e mandou recolher trigo do chão para a horta.
     */
    static boolean needsNoStock(com.villagecolony.core.type.ResourceId id) {
        return com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toBlock(id)
                .map(block -> BlockShaping.isShapedFromTheGround(block.getDefaultState())
                        || BlockShaping.isNeverPlaced(block.getDefaultState())
                        || appears(block))
                .orElse(false);
    }
}
