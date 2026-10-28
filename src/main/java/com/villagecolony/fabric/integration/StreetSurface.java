package com.villagecolony.fabric.integration;

import com.villagecolony.fabric.content.ModContent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

/**
 * O chão das ruas e caminhos da colônia — mistura gasta natural, escolha do autor (2026-10-28; estudo em
 * {@code docs/research/2026-10-28-ruas-naturais.md}). O meio é o caminho do bioma ({@link VillageRoad#pavingOn});
 * a borda sorteia entre o caminho e o desgaste — cascalho e terra grossa, ou arenito na areia —, e a rua deixa
 * de parecer um tapete reto.
 *
 * <p>Todo bloco desta lista, numa coluna que a colônia assentou, é da vila: nenhuma profissão o minera nem
 * recolhe ({@link BlockProtection#mayBreak}).
 */
public final class StreetSurface {

    /** Os blocos que uma rua ou caminho da colônia pode ter na superfície. */
    static final Set<Block> MATERIALS = Set.of(Blocks.DIRT_PATH, ModContent.SAND_PATH, Blocks.GRAVEL,
            Blocks.COARSE_DIRT, Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE,
            Blocks.PACKED_MUD);

    /** De cada tantas colunas de borda, quantas ficam com o desgaste em vez do caminho. */
    static final int WORN_IN = 2;

    private StreetSurface() {
    }

    /** O chão do meio da rua sobre este chão. */
    public static BlockState center(ServerWorld world, BlockPos at, BlockState ground) {
        return VillageRoad.pavingOn(world, at, ground).getDefaultState();
    }

    /** O chão da borda: o caminho, ou o desgaste (cascalho e terra grossa; arenito na areia). */
    public static BlockState edge(ServerWorld world, BlockPos at, BlockState ground) {
        Block path = VillageRoad.pavingOn(world, at, ground);

        if (world.getRandom().nextInt(WORN_IN) != 0) {
            return path.getDefaultState();
        }

        boolean sandy = path == ModContent.SAND_PATH || ground.isIn(BlockTags.SAND);

        if (sandy) {
            return Blocks.SANDSTONE.getDefaultState();
        }

        // Cascalho cai: sobre o vazio a borda fica com a terra grossa.
        boolean falls = world.getBlockState(at.down()).isReplaceable();

        return (!falls && world.getRandom().nextBoolean() ? Blocks.GRAVEL : Blocks.COARSE_DIRT).getDefaultState();
    }

    /** Se este bloco é material de rua. */
    public static boolean isStreetMaterial(BlockState state) {
        return MATERIALS.contains(state.getBlock());
    }

    /** Se este bloco é chão de rua ou caminho que uma colônia assentou: é da vila, ninguém o tira. */
    public static boolean isColonyStreet(BlockPos pos, BlockState state) {
        return isStreetMaterial(state) && RoadIndex.isLaidByAColony(pos);
    }
}
