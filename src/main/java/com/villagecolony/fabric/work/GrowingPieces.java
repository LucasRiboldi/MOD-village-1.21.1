package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Relva alta, grama e flores da planta aparecem sozinhas — regra do autor, 2026-10-12
 * (Regra 55). O construtor não vai até elas: no playtest 0.3.37 ele deixou a grama de
 * lado por não alcançá-la e a casa terminou sem a relva alta.
 *
 * <p>A cada ciclo da obra, cada peça que cresce sozinha ({@link BlockShaping#growsByItself},
 * fora o bloco de grama, que é chão) surge no lugar dela quando o lugar está livre. Terra
 * embaixo vira grama, que é onde a planta nasce. A planta alta surge com as duas metades.
 */
final class GrowingPieces {

    private GrowingPieces() {
    }

    /**
     * Faz surgir as peças que crescem sozinhas.
     *
     * @return quantas surgiram
     */
    static int appear(ServerWorld world, ConstructionProject project) {
        int appeared = 0;

        for (BlueprintBlock block : project.remaining()) {
            Optional<Block> material = MinecraftTypeAdapter.toBlock(block.block());

            if (material.isEmpty() || material.get() == Blocks.GRASS_BLOCK
                    || !BlockShaping.growsByItself(material.get().getDefaultState())) {
                continue;
            }

            BlockPos target = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(block));

            if (!world.isChunkLoaded(target)) {
                continue;
            }

            if (world.getBlockState(target).isOf(material.get())) {
                project.markPlaced(block);
                continue;
            }

            BlockState state = BuilderPlacement.shaped(world, project, block, material.get(), target);

            if (state.contains(Properties.DOUBLE_BLOCK_HALF)
                    && state.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
                // A metade de cima surge junto com a de baixo.
                continue;
            }

            if (place(world, target, state)) {
                project.markPlaced(block);
                appeared++;
            }
        }

        if (appeared > 0) {
            VillageColonyMod.LOGGER.info("Project {} — {} grass and flower pieces grew by themselves",
                    project.id(), appeared);
        }

        return appeared;
    }

    private static boolean place(ServerWorld world, BlockPos target, BlockState state) {
        boolean tall = state.getBlock() instanceof TallPlantBlock;

        if (!world.getBlockState(target).isReplaceable()
                || (tall && !world.getBlockState(target.up()).isReplaceable())) {
            return false;
        }

        BlockPos below = target.down();

        if (!state.canPlaceAt(world, target) && world.getBlockState(below).isIn(BlockTags.DIRT)) {
            world.setBlockState(below, Blocks.GRASS_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
        }

        if (!state.canPlaceAt(world, target)) {
            return false;
        }

        if (tall) {
            TallPlantBlock.placeAt(world, state, target, Block.NOTIFY_ALL);
        } else {
            world.setBlockState(target, state, Block.NOTIFY_ALL);
        }

        return true;
    }
}
