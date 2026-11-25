package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * O chão do bioma para aterrar — Regra 363 (autor, 2026-11-25, G1 🅲: "tem terra embaixo dos blocos de areia no deserto,
 * e deveria ser arenito"). Debaixo de grama e terra, terra; debaixo de areia, o arenito (da areia vermelha, o arenito
 * vermelho), que segura a areia de cima; senão o bloco de baixo, se é chão natural que não cai; sem pista nenhuma, o do
 * bioma. Terra só fora do deserto.
 */
public final class BiomeSoil {

    private BiomeSoil() {
    }

    /** O bloco que aterra embaixo de {@code surface}, numa coluna cujo bloco de baixo é {@code below}. */
    public static BlockState fillUnder(@Nullable ServerWorld world, BlockPos at, BlockState surface, BlockState below) {
        // Debaixo de grama e terra, terra; debaixo de areia, arenito.
        if (surface.isIn(BlockTags.DIRT)) {
            return Blocks.DIRT.getDefaultState();
        }

        BlockState held = surface.getBlock() instanceof FallingBlock ? firmFor(surface) : null;

        if (held != null) {
            return held;
        }

        if (SiteSurroundings.isNaturalGround(below) && !below.isOf(Blocks.FARMLAND)
                && !(below.getBlock() instanceof FallingBlock)) {
            return below;
        }

        BlockState firm = firmFor(below);

        if (firm == null) {
            firm = firmFor(surface);
        }

        if (firm != null) {
            return firm;
        }

        return world != null && isDesert(world, at) ? Blocks.SANDSTONE.getDefaultState() : Blocks.DIRT.getDefaultState();
    }

    /** O bloco firme que segura este chão solto: arenito sob areia, arenito vermelho sob areia vermelha. */
    public static @Nullable BlockState firmFor(BlockState loose) {
        if (loose.isOf(Blocks.RED_SAND) || loose.isOf(Blocks.RED_SANDSTONE)) {
            return Blocks.RED_SANDSTONE.getDefaultState();
        }

        if (loose.isIn(BlockTags.SAND) || loose.isOf(Blocks.SANDSTONE) || loose.isOf(Blocks.SMOOTH_SANDSTONE)
                || loose.isOf(Blocks.CUT_SANDSTONE) || loose.isOf(Blocks.SUSPICIOUS_SAND)) {
            return Blocks.SANDSTONE.getDefaultState();
        }

        return null;
    }

    /** Se esta coluna é de deserto ou de terra vermelha, pelo bioma. */
    public static boolean isDesert(ServerWorld world, BlockPos at) {
        return world.getBiome(at).isIn(ConventionalBiomeTags.IS_DESERT)
                || world.getBiome(at).isIn(ConventionalBiomeTags.IS_BADLANDS);
    }
}
