package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/** Armazém comunitário de emergência no salão já aberto de uma mina. */
public final class MineOverflowStorage {
    private static final int HALL_STORAGE_INDEX = MineShaft.CARVED
            + MineShaft.SEARCH_AREA_BLOCKS - MineShaft.HEADROOM;

    private MineOverflowStorage() {
    }

    /**
     * Cria, quando necessário, um único baú no último piso já aberto do salão.
     * A posição é determinística e o próprio bloco no mundo é a fonte da
     * verdade; nenhum registro extra é persistido.
     */
    public static Optional<ColonyPos> ensure(ServerWorld world, UUID colonyId) {
        Optional<ColonyPos> candidate = completedHallStorage(colonyId);
        if (candidate.isEmpty()) {
            return Optional.empty();
        }

        BlockPos at = MinecraftTypeAdapter.toBlockPos(candidate.get());
        if (!world.getChunkManager().isChunkLoaded(at.getX() >> 4, at.getZ() >> 4)) {
            return Optional.empty();
        }
        if (world.getBlockState(at).isOf(Blocks.CHEST)) {
            return candidate;
        }
        if (!world.getBlockState(at).isAir() || !world.getBlockState(at.down()).isSolidBlock(world, at.down())) {
            return Optional.empty();
        }

        world.setBlockState(at, Blocks.CHEST.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, net.minecraft.util.math.Direction.NORTH));
        VillageColonyMod.LOGGER.info(
                "Colony {} placed emergency community storage in its completed mine hall at {}",
                colonyId.toString().substring(0, 8), at.toShortString());
        return candidate;
    }

    /** Retorna o armazém do salão completo atual, se ele já existir. */
    public static Optional<ColonyPos> existing(ServerWorld world, UUID colonyId) {
        Optional<ColonyPos> candidate = completedHallStorage(colonyId);
        return candidate.filter(position -> {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(position);
            return world.getChunkManager().isChunkLoaded(at.getX() >> 4, at.getZ() >> 4)
                    && world.getBlockState(at).isOf(Blocks.CHEST);
        });
    }

    static Optional<ColonyPos> completedHallStorage(UUID colonyId) {
        return VillageColonyMod.MINES.of(colonyId)
                .flatMap(mine -> completedHallStorage(mine));
    }

    static Optional<ColonyPos> completedHallStorage(Mine mine) {
        // A rampa do fundo (ADR-036 17) não tem salão: o índice cairia num degrau.
        if (mine.shaft().isRamp()) {
            return Optional.empty();
        }

        for (MineArm arm : mine.arms()) {
            if (arm.cut() > HALL_STORAGE_INDEX) {
                return Optional.of(arm.shaft().positionAt(HALL_STORAGE_INDEX));
            }
        }
        return Optional.empty();
    }
}
