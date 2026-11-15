package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

/**
 * O pátio da boca da mina — Regra 293 (autor 15-11, G4 da ADR-056): um disco de pedra de raio {@link #RADIUS} na base
 * do arco, com o centro no meio do vão; o chão acima do disco é cortado (árvore inteira junto) e o vão abaixo, aterrado;
 * nada fica acima do arco além das peças dele. O buraco da escada fica aberto, e o que a Regra 3 protege fica como está.
 * O baú que nasce com o arco fica na base dele, sobre o disco.
 */
public final class MineYard {

    /** O raio do pátio, em blocos. */
    public static final int RADIUS = 6;

    /** Até quantos blocos abaixo do disco um vão é aterrado; mais fundo é a escada, e fica. */
    static final int FILL = 3;

    private MineYard() {
    }

    /** O meio do vão do arco, onde fica o centro do pátio. */
    static BlockPos centerOf(BlockPos mouth, Direction descent) {
        return mouth.offset(descent.rotateYCounterclockwise());
    }

    /** Põe o pátio desta boca uma vez: quando o centro já é pedra, não faz nada. */
    public static void layOnce(ServerWorld world, BlockPos mouth, Direction descent) {
        if (!world.getBlockState(centerOf(mouth, descent).down()).isOf(Blocks.STONE)) {
            lay(world, mouth, descent, RADIUS);
        }
    }

    /**
     * Põe o pátio de raio {@code radius} nesta boca.
     *
     * @return quantos blocos mudaram
     */
    public static int lay(ServerWorld world, BlockPos mouth, Direction descent, int radius) {
        BlockPos center = centerOf(mouth, descent);
        int discY = mouth.getY() - 1;
        int changed = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;

                if (dx * dx + dz * dz > radius * radius || !world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }

                BlockPos disc = new BlockPos(x, discY, z);

                // O buraco da escada: ar no disco e logo abaixo dele — fica aberto.
                if (isOpen(world, disc) && isOpen(world, disc.down())) {
                    continue;
                }

                changed += clearAbove(world, mouth, descent, disc);
                changed += fillBelow(world, disc);

                BlockState state = world.getBlockState(disc);

                if (!state.isOf(Blocks.STONE) && !MineGate.isPart(mouth, descent, disc)
                        && (isOpen(world, disc) || SiteSurroundings.isNaturalOrOre(state))
                        && BlockProtection.mayBreak(world, disc, state)) {
                    world.setBlockState(disc, Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
                    changed++;
                }
            }
        }

        return changed;
    }

    /** Tira chão, árvore e planta acima do disco até o topo da coluna; as peças do arco ficam. */
    private static int clearAbove(ServerWorld world, BlockPos mouth, Direction descent, BlockPos disc) {
        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, disc.getX(), disc.getZ());
        int changed = 0;

        for (int y = top; y > disc.getY(); y--) {
            BlockPos at = new BlockPos(disc.getX(), y, disc.getZ());
            BlockState state = world.getBlockState(at);

            if (state.isAir() || !state.getFluidState().isEmpty() || MineGate.isArchPiece(mouth, descent, at)) {
                continue;
            }

            boolean growth = state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES) || state.isReplaceable()
                    || state.isIn(BlockTags.FLOWERS) || state.isIn(BlockTags.SAPLINGS);

            if ((growth || SiteSurroundings.isNaturalOrOre(state)) && BlockProtection.mayBreak(world, at, state)) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                changed++;
            }
        }

        return changed;
    }

    /** Aterra com terra o vão raso sob o disco, até {@value #FILL} blocos. */
    private static int fillBelow(ServerWorld world, BlockPos disc) {
        int changed = 0;

        for (int depth = 1; depth <= FILL && isOpen(world, disc.down(depth)); depth++) {
            if (depth == FILL && isOpen(world, disc.down(depth + 1))) {
                break;
            }

            world.setBlockState(disc.down(depth), Blocks.DIRT.getDefaultState(), Block.NOTIFY_LISTENERS);
            changed++;
        }

        return changed;
    }

    private static boolean isOpen(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        return (state.isAir() || state.isReplaceable()) && state.getFluidState().isEmpty();
    }

    /** Se esta pedra é do pátio desta boca: na altura do disco e no raio dele. */
    public static boolean isYardAt(BlockPos pos, BlockState state, BlockPos mouth, Direction descent) {
        BlockPos center = centerOf(mouth, descent);
        long dx = (long) pos.getX() - center.getX();
        long dz = (long) pos.getZ() - center.getZ();

        return state.isOf(Blocks.STONE) && pos.getY() == mouth.getY() - 1 && dx * dx + dz * dz <= (long) RADIUS * RADIUS;
    }

    /** Se esta pedra é do pátio de alguma mina de colônia. */
    public static boolean isYard(BlockPos pos, BlockState state) {
        if (!state.isOf(Blocks.STONE)) {
            return false;
        }

        for (Mine mine : VillageColonyMod.MINES.all()) {
            if (isYardAt(pos, state, MinecraftTypeAdapter.toBlockPos(mine.shaft().entry()),
                    MinecraftTypeAdapter.toDirection(mine.shaft().descent()))) {
                return true;
            }
        }

        return false;
    }

    /** Se a coluna é pátio de alguma mina: a folga da boca não vale sobre ele (Regra 293). */
    public static boolean isYardColumn(ServerWorld world, int x, int z) {
        for (Mine mine : VillageColonyMod.MINES.all()) {
            BlockPos disc = new BlockPos(x, mine.shaft().entry().y() - 1, z);

            if (isYardAt(disc, world.getBlockState(disc), MinecraftTypeAdapter.toBlockPos(mine.shaft().entry()),
                    MinecraftTypeAdapter.toDirection(mine.shaft().descent()))) {
                return true;
            }
        }

        return false;
    }
}
