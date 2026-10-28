package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.LotScore;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * As falhas no meio da rua viram rua — Regra 149 (autor, 2026-10-28: no mapa das ruas da taiga simulada a rua
 * do jogo era pontilhada, com grama e terra no meio do percurso). Uma falha é de {@link #MAX_GAP} blocos ou
 * menos numa linha reta entre duas colunas de rua, com no máximo um de desnível. Só o chão natural é trocado
 * (a mesma recusa do calçamento, {@link RoadPaving#refusalOfTheGround}); o desgaste da borda que a colônia
 * assentou já é rua ({@link StreetSurface#isColonyStreet}) e fica.
 */
public final class StreetGaps {

    /** A falha mais longa que se tapa. */
    public static final int MAX_GAP = 2;

    private StreetGaps() {
    }

    /**
     * Tapa até {@code max} colunas de falha, a partir das colunas de rua mais perto de {@code near}.
     *
     * @return quantas colunas viraram rua
     */
    public static int mend(ServerWorld world, UUID colonyId, BlockPos near, int max) {
        Optional<ColonyRoads> roads = RoadIndex.roadsOf(colonyId);

        if (roads.isEmpty() || max <= 0) {
            return 0;
        }

        List<Long> columns = roads.get().columns();
        int mended = 0;

        for (int index : LotScore.nearestFirst(columns, near.getX(), near.getZ())) {
            long column = columns.get(index);
            Optional<BlockPos> street = surface(world, ColonyRoads.xOf(column), ColonyRoads.zOf(column));

            if (street.isEmpty() || !isStreet(world, street.get())) {
                continue;
            }

            for (Direction side : Direction.Type.HORIZONTAL) {
                mended += fill(world, colonyId, street.get(), side);

                if (mended >= max) {
                    return mended;
                }
            }
        }

        return mended;
    }

    /** A falha que sai desta coluna de rua para este lado, se a linha volta a ser rua logo depois. */
    static int fill(ServerWorld world, UUID colonyId, BlockPos street, Direction side) {
        for (int gap = 1; gap <= MAX_GAP; gap++) {
            Optional<BlockPos> beyond = surface(world, street.getX() + side.getOffsetX() * (gap + 1),
                    street.getZ() + side.getOffsetZ() * (gap + 1));

            if (beyond.isEmpty() || Math.abs(beyond.get().getY() - street.getY()) > 1) {
                return 0;
            }

            if (!isStreet(world, beyond.get())) {
                continue;
            }

            int laid = 0;

            for (int step = 1; step <= gap; step++) {
                Optional<BlockPos> hole = surface(world, street.getX() + side.getOffsetX() * step,
                        street.getZ() + side.getOffsetZ() * step);

                if (hole.isEmpty() || isStreet(world, hole.get()) || !pave(world, colonyId, street, hole.get())) {
                    return laid;
                }

                laid++;
            }

            return laid;
        }

        return 0;
    }

    private static boolean pave(ServerWorld world, UUID colonyId, BlockPos street, BlockPos hole) {
        BlockState ground = world.getBlockState(hole);

        if (Math.abs(hole.getY() - street.getY()) > 1
                || RoadPaving.refusalOfTheGround(world, hole, ground).isPresent()) {
            return false;
        }

        BlockState above = world.getBlockState(hole.up());

        if (!above.isAir()) {
            if (!above.isReplaceable()) {
                return false;
            }

            world.setBlockState(hole.up(), Blocks.AIR.getDefaultState());
        }

        world.setBlockState(hole, VillageRoad.pavingOn(world, hole, ground).getDefaultState());
        RoadIndex.remember(colonyId, hole);

        return true;
    }

    /** Se este bloco é rua: caminho, ou o desgaste que a colônia assentou. */
    static boolean isStreet(ServerWorld world, BlockPos at) {
        return VillageRoad.isPaving(world, at) || StreetSurface.isColonyStreet(at, world.getBlockState(at));
    }

    /** O chão da coluna, por baixo da copa e da planta rasteira; vazio se o chunk não está carregado. */
    static Optional<BlockPos> surface(ServerWorld world, int x, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return Optional.empty();
        }

        BlockPos top = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);

        return Optional.of(world.getBlockState(top).isReplaceable() ? top.down() : top);
    }
}
