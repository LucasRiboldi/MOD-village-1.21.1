package com.villagecolony.fabric.work;

import com.villagecolony.core.movement.Cell;
import com.villagecolony.core.movement.Terrain;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlockProtection;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

import java.util.HashMap;
import java.util.Map;

/**
 * O mundo lido como o {@code DetourPlanner} o vê — ADR-025, fase 2.
 *
 * <p>Uma leitura por posição, guardada: o A* pergunta pela mesma célula
 * várias vezes (o piso de um passo é o vizinho de outro), e a instância vive só
 * o tempo de um planejamento ou de uma checagem de passo. Mundo que muda depois
 * é lido de novo pela instância seguinte.
 *
 * <p><b>A Regra 3 mora aqui.</b> Só é {@link Cell#ROCK} o terreno natural que
 * o {@link BlockProtection} deixa quebrar e que não guarda nada (sem bloco com
 * inventário); só é {@link Cell#OPEN} o vazio onde pôr bloco não cobre
 * construção de ninguém. O resto se pisa ou se contorna.
 */
public final class WorldTerrain implements Terrain {

    private final ServerWorld world;

    private final Map<ColonyPos, Cell> seen = new HashMap<>();

    public WorldTerrain(ServerWorld world) {
        this.world = world;
    }

    @Override
    public Cell at(ColonyPos pos) {
        return seen.computeIfAbsent(pos, at -> classify(MinecraftTypeAdapter.toBlockPos(at)));
    }

    private Cell classify(BlockPos pos) {
        if (!world.isInBuildLimit(pos)
                || !world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
            return Cell.BARRIER;
        }

        BlockState state = world.getBlockState(pos);

        if (!state.getFluidState().isEmpty()) {
            return Cell.FLUID;
        }

        VoxelShape shape = state.getCollisionShape(world, pos);

        if (shape.isEmpty()) {
            if (isHazard(state)) {
                return Cell.BARRIER;
            }

            return state.isReplaceable()
                    && !BlockProtection.isColonyBuilt(pos)
                    && !BlockProtection.isVillageOriginal(world, pos)
                    ? Cell.OPEN
                    : Cell.PASSAGE;
        }

        if (!Block.isShapeFullCube(shape) || isHazard(state)) {
            return Cell.BARRIER;
        }

        if (state.getBlock() instanceof FallingBlock) {
            return Cell.LOOSE;
        }

        return mayDig(pos, state) ? Cell.ROCK : Cell.FIRM;
    }

    private boolean mayDig(BlockPos pos, BlockState state) {
        return state.getHardness(world, pos) >= 0
                && world.getBlockEntity(pos) == null
                && isNaturalGround(state)
                && BlockProtection.mayBreak(world, pos, state);
    }

    /** O que queima, fura ou prende quem passa. */
    private static boolean isHazard(BlockState state) {
        return state.isIn(BlockTags.FIRE)
                || state.isOf(Blocks.MAGMA_BLOCK)
                || state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isOf(Blocks.COBWEB)
                || state.isOf(Blocks.POWDER_SNOW)
                || state.isOf(Blocks.WITHER_ROSE);
    }

    /**
     * Terreno natural — pedra, terra, areia, argila, minério. Mesma lista que
     * o {@code StrandedEscape} usa desde o E47: quem cava para sair e quem
     * cava para passar respondem igual.
     */
    public static boolean isNaturalGround(BlockState state) {
        return state.isIn(BlockTags.BASE_STONE_OVERWORLD)
                || state.isIn(BlockTags.DIRT)
                || state.isIn(BlockTags.SAND)
                || state.isIn(BlockTags.TERRACOTTA)
                || state.isIn(BlockTags.COAL_ORES)
                || state.isIn(BlockTags.IRON_ORES)
                || state.isIn(BlockTags.COPPER_ORES)
                || state.isIn(BlockTags.GOLD_ORES)
                || state.isIn(BlockTags.REDSTONE_ORES)
                || state.isIn(BlockTags.LAPIS_ORES)
                || state.isIn(BlockTags.DIAMOND_ORES)
                || state.isIn(BlockTags.EMERALD_ORES)
                || state.isOf(Blocks.GRAVEL)
                || state.isOf(Blocks.CLAY)
                || state.isOf(Blocks.SANDSTONE)
                || state.isOf(Blocks.RED_SANDSTONE);
    }
}
