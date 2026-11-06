package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.LotGround;
import com.villagecolony.fabric.integration.RoadIndex;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A ponte da vila — autor, 2026-11-04: sem caminho a seco na zona da vila, o construtor atravessa a água.
 *
 * <p>Redimensionável: o comprimento vem do traçado ({@code PathLinks}), até {@value #MAX_SPAN} colunas de água, e a
 * largura é de 3 tábuas onde o lado também é água, com cerca nas duas beiras e o eixo livre. Tábua e cerca saem dos
 * baús, da mesma madeira; sem tábua a ponte espera, e sem cerca ela segue sem guarda.
 */
final class BridgeDeck {

    /** A maior travessia, em colunas de água no eixo. */
    static final int MAX_SPAN = 64;

    private record Wood(Block planks, Block fence) {
    }

    private static final List<Wood> WOODS = List.of(
            new Wood(Blocks.OAK_PLANKS, Blocks.OAK_FENCE),
            new Wood(Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_FENCE),
            new Wood(Blocks.BIRCH_PLANKS, Blocks.BIRCH_FENCE),
            new Wood(Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_FENCE),
            new Wood(Blocks.ACACIA_PLANKS, Blocks.ACACIA_FENCE),
            new Wood(Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_FENCE),
            new Wood(Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_FENCE),
            new Wood(Blocks.CHERRY_PLANKS, Blocks.CHERRY_FENCE));

    private BridgeDeck() {
    }

    /** O piso deste passo fica na água ou logo acima dela. */
    static boolean isOverWater(ServerWorld world, BlockPos floor) {
        return isWater(world, floor) || (world.getBlockState(floor).isAir() && isWater(world, floor.down()));
    }

    /** Quantos passos do traçado atravessam água. */
    static int waterColumns(ServerWorld world, List<PathStep> steps) {
        int water = 0;

        for (PathStep step : steps) {
            if (isOverWater(world, new BlockPos(step.x(), step.floorY(), step.z()))) {
                water++;
            }
        }

        return water;
    }

    /**
     * Assenta o passo da ponte.
     *
     * @return falso quando não há tábua nos baús ou a seção é reservada: nada foi escrito
     */
    static boolean lay(ServerWorld world, UUID colonyId, UUID workerId, PathStep step, List<PathStep> steps) {
        BlockPos floor = new BlockPos(step.x(), step.floorY(), step.z());
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId, new ColonyPos(step.x(), step.floorY(), step.z()));
        Optional<Wood> wood = WOODS.stream()
                .filter(candidate -> ColonyChests.countIn(world, chests, candidate.planks().asItem()) > 0)
                .findFirst();

        if (wood.isEmpty()) {
            return false;
        }

        take(world, chests, wood.get().planks());
        world.setBlockState(floor, wood.get().planks().getDefaultState());
        clearAbove(world, floor);
        RoadIndex.remember(colonyId, floor);
        ActionJournal.action(workerId, "PAVED", "bridge", 1, floor, "bridge");

        Set<Long> axis = new HashSet<>();

        for (PathStep other : steps) {
            axis.add(ColonyRoads.column(other.x(), other.z()));
        }

        for (int offset : new int[] {-1, 1}) {
            BlockPos side = new BlockPos(step.sideX(offset), step.floorY(), step.sideZ(offset));

            if (!axis.contains(ColonyRoads.column(side.getX(), side.getZ())) && isOverWater(world, side)
                    && take(world, chests, wood.get().planks())) {
                world.setBlockState(side, wood.get().planks().getDefaultState());
                clearAbove(world, side);

                if (take(world, chests, wood.get().fence())) {
                    world.setBlockState(side.up(), wood.get().fence().getDefaultState());
                }
            }
        }

        return true;
    }

    private static boolean take(ServerWorld world, List<ColonyPos> chests, Block block) {
        return ColonyChests.withdraw(world, chests, block.asItem(), 1) > 0;
    }

    private static void clearAbove(ServerWorld world, BlockPos floor) {
        BlockState above = world.getBlockState(floor.up());

        if (!above.isAir() && above.getFluidState().isEmpty() && LotGround.isNothing(above)) {
            world.breakBlock(floor.up(), false);
        }
    }

    private static boolean isWater(ServerWorld world, BlockPos at) {
        return world.getFluidState(at).isIn(FluidTags.WATER);
    }
}
