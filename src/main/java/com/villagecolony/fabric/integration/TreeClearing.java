package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tudo o que cresce na zona da obra sai, em todas as alturas, com o que encosta nele — Regra 22, emenda do autor
 * de 2026-11-04.
 *
 * <p>Parte de cada coluna da zona e segue por tronco e folha encostados (também na diagonal), para fora da zona.
 * Planta e flor encostadas saem, mas não continuam a busca. O que sai vai para os baús; o que não couber fica no
 * chão. A busca para a {@value #REACH} blocos da zona e em {@value #MAX_BLOCKS} blocos: o limite da floresta inteira.
 * Só tira o que a proteção deixa ({@link LotGrowth#isClearableOnSite}).
 */
final class TreeClearing {

    /** Até onde, além da zona, a busca segue a árvore encostada. */
    static final int REACH = 16;

    /** Quantos blocos uma obra tira de uma vez. */
    static final int MAX_BLOCKS = 2_000;

    private TreeClearing() {
    }

    /**
     * Limpa a zona das colunas {@code minX..maxX} e {@code minZ..maxZ}, de {@code fromY} para cima.
     *
     * @return quantos blocos saíram
     */
    static int sweep(ServerWorld world, int minX, int minZ, int maxX, int maxZ, int fromY, List<ColonyPos> chests) {
        Deque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> toClear = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }

                int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);

                for (int y = fromY; y < top; y++) {
                    BlockPos at = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(at);

                    if (grows(state) && (LotGrowth.isClearableOnSite(world, at, state) || isBareTrunk(world, at, state))
                            && seen.add(at)) {
                        toClear.add(at);

                        if (spreads(state)) {
                            open.add(at);
                        }
                    }
                }
            }
        }

        while (!open.isEmpty() && toClear.size() < MAX_BLOCKS) {
            BlockPos from = open.poll();

            for (BlockPos near : BlockPos.iterate(from.add(-1, -1, -1), from.add(1, 1, 1))) {
                if (near.getX() < minX - REACH || near.getX() > maxX + REACH
                        || near.getZ() < minZ - REACH || near.getZ() > maxZ + REACH
                        || near.getY() < fromY || !world.isChunkLoaded(near.getX() >> 4, near.getZ() >> 4)
                        || seen.contains(near)) {
                    continue;
                }

                BlockState state = world.getBlockState(near);

                if (grows(state) && LotGrowth.isClearableOnSite(world, near, state)) {
                    BlockPos kept = near.toImmutable();

                    seen.add(kept);
                    toClear.add(kept);

                    if (spreads(state)) {
                        open.add(kept);
                    }
                }
            }
        }

        // Do alto para baixo: o que está em cima de quê sai primeiro, e nada cai ou vira item sozinho.
        toClear.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

        for (BlockPos at : toClear) {
            collect(world, at, chests);
        }

        return toClear.size();
    }

    /** Tronco sem copa dentro da zona: a árvore ficou pela metade, e dentro da zona ela sai também. */
    private static boolean isBareTrunk(ServerWorld world, BlockPos at, BlockState state) {
        return LotGrowth.isSolidGrowth(state) && BlockProtection.mayBreak(world, at, state);
    }

    /** Tronco, folha, cogumelo, bambu e planta do chão; sem água, que também é "substituível". */
    private static boolean grows(BlockState state) {
        return spreads(state)
                || state.isIn(BlockTags.FLOWERS)
                || state.isOf(Blocks.VINE)
                || (state.isIn(BlockTags.REPLACEABLE) && state.getFluidState().isEmpty() && !state.isAir());
    }

    /** O que continua a busca: a árvore e o que vive como ela. */
    private static boolean spreads(BlockState state) {
        return LotGrowth.isLooseGrowth(state);
    }

    private static void collect(ServerWorld world, BlockPos at, List<ColonyPos> chests) {
        BlockState state = world.getBlockState(at);

        for (ItemStack drop : Block.getDroppedStacks(state, world, at, world.getBlockEntity(at))) {
            int left = drop.getCount();

            for (ColonyPos chest : chests) {
                left = ChestDepositor.deposit(world, chest, drop.getItem(), left);

                if (left == 0) {
                    break;
                }
            }

            if (left > 0) {
                world.spawnEntity(new ItemEntity(world, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5,
                        drop.copyWithCount(left)));
            }
        }

        world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
    }
}
