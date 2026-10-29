package com.villagecolony.fabric.integration;

import com.villagecolony.fabric.content.ModContent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

/**
 * O chão das ruas e caminhos da colônia — só o caminho do chão (autor, 2026-10-28, depois do jogo da 0.3.104:
 * "voltar para utilizar apenas blocos de caminho"; a mistura gasta da Regra 138 deixava a rua com cara de quebrada).
 * Meio e borda são o mesmo bloco, {@link VillageRoad#pavingOn}; o desgaste que a colônia já assentou volta a ser
 * caminho ({@link #unworn}).
 *
 * <p>Todo bloco desta lista, numa coluna que a colônia assentou, é da vila: nenhuma profissão o minera nem
 * recolhe ({@link BlockProtection#mayBreak}).
 */
public final class StreetSurface {

    /** Os blocos que uma rua ou caminho da colônia pode ter na superfície. */
    static final Set<Block> MATERIALS = Set.of(Blocks.DIRT_PATH, ModContent.SAND_PATH, Blocks.GRAVEL,
            Blocks.COARSE_DIRT, Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE,
            Blocks.PACKED_MUD);

    /** O desgaste que a mistura antiga assentava na borda. */
    private static final Set<Block> WORN = Set.of(Blocks.GRAVEL, Blocks.COARSE_DIRT, Blocks.SANDSTONE);

    private StreetSurface() {
    }

    /** O chão do meio da rua sobre este chão. */
    public static BlockState center(ServerWorld world, BlockPos at, BlockState ground) {
        return VillageRoad.pavingOn(world, at, ground).getDefaultState();
    }

    /** O chão da borda: o mesmo do meio. */
    public static BlockState edge(ServerWorld world, BlockPos at, BlockState ground) {
        return center(world, at, ground);
    }

    /**
     * Troca pelo caminho o desgaste que a colônia assentou nesta coluna — o caminho do chão de baixo, como a rua
     * nova. Cascalho sobre pedra continua cascalho, que é o caminho da pedra.
     *
     * @return se trocou
     */
    public static boolean unworn(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        if (!WORN.contains(state.getBlock()) || !isColonyStreet(at, state)) {
            return false;
        }

        BlockState path = center(world, at, world.getBlockState(at.down()));

        if (path.isOf(state.getBlock())) {
            return false;
        }

        world.setBlockState(at, path);

        return true;
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
