package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.BlockProtection;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * O lugar encostado no tronco do carvalho de copa baixa — Regra 324: quando nenhuma das oito colunas em volta da base tem
 * lugar de ficar, a escolhida é a que só tem folha natural na altura dos pés ou da cabeça, e o lenhador tira essa folha
 * ao chegar perto. Sem isso o lugar de ficar caía a dois blocos do tronco, onde a Regra 277 não deixa cortar.
 */
final class TrunkStand {

    /** As oito colunas em volta da base: primeiro as dos lados, depois as das quinas. */
    private static final int[][] AROUND = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    /** A altura dos pés em relação à base: no nível dela, depois um abaixo, depois um acima (Regra 277). */
    private static final int[] HEIGHTS = {0, -1, 1};

    private TrunkStand() {
    }

    /** A coluna encostada no tronco que fica de pé depois de tirar a folha natural dos pés e da cabeça. */
    static Optional<BlockPos> leafy(ServerWorld world, BlockPos base) {
        for (int dy : HEIGHTS) {
            for (int[] side : AROUND) {
                BlockPos at = base.add(side[0], dy, side[1]);
                BlockPos floor = at.down();

                if (!world.getBlockState(floor).getCollisionShape(world, floor).isEmpty()
                        && BuilderApproach.isDry(world, at)
                        && (isNaturalLeaf(world, at) || isNaturalLeaf(world, at.up()))
                        && (BuilderApproach.passable(world, at) || isNaturalLeaf(world, at))
                        && (BuilderApproach.passable(world, at.up()) || isNaturalLeaf(world, at.up()))) {
                    return Optional.of(at);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Perto do lugar de ficar, o lenhador tira a folha natural dos pés e da cabeça dele.
     *
     * @return quantas folhas tirou
     */
    static int clear(ServerWorld world, VillagerEntity villager, BlockPos base) {
        if (!base.isWithinDistance(villager.getBlockPos(), LumberjackWork.REACH + 2)) {
            return 0;
        }

        BlockPos stand = TreeChoice.approachTo(world, base);

        if (!stand.isWithinDistance(villager.getBlockPos(), LumberjackWork.REACH)) {
            return 0;
        }

        int cleared = 0;

        for (BlockPos at : new BlockPos[] {stand, stand.up()}) {
            BlockState state = world.getBlockState(at);

            if (isNaturalLeaf(world, at)) {
                world.breakBlock(at, true);
                villager.swingHand(Hand.MAIN_HAND);
                ActionJournal.action(villager.getUuid(), "CLEARED_LEAVES", ActionJournal.idOf(state.getBlock().asItem()),
                        1, at, "leaves beside the trunk");
                cleared++;
            }
        }

        return cleared;
    }

    private static boolean isNaturalLeaf(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        return state.isIn(BlockTags.LEAVES) && !BlockProtection.isPlayerPlaced(state)
                && !BlockProtection.isColonyBuilt(at) && !BlockProtection.isVillageOriginal(world, at);
    }
}
