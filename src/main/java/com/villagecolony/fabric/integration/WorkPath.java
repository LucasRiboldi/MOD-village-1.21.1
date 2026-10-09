package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;
import java.util.UUID;

/**
 * O caminho até a obra — ADR-039 D2. A obra que não encosta em rua (lote da
 * busca larga, Emenda 7) ganha um calçamento em linha reta do caminho mais
 * perto até a borda do lote, até {@link #STEPS_PER_CYCLE} blocos por ciclo.
 * Calçar não custa material (Regra 15), e a Regra 3 vale como na rua: o
 * trecho para no primeiro bloco recusado.
 */
public final class WorkPath {

    /** Blocos calçados por ciclo, no máximo. */
    static final int STEPS_PER_CYCLE = 4;

    /** A obra mais longe que isto da rua não ganha caminho: não é vizinha dela. */
    static final int MAX_GAP = 24;

    private WorkPath() {
    }

    /** Calça um trecho rumo à obra aberta desta colônia; devolve quantos blocos entraram. */
    public static int paveToward(ServerWorld world, UUID colonyId) {
        Optional<ConstructionProject> open = VillageColonyMod.CONSTRUCTIONS.openOf(colonyId)
                .filter(project -> project.state() == ConstructionState.BUILDING);

        if (open.isEmpty() || open.get().blueprint().isPlantation()) {
            return 0;
        }

        ColonyPos origin = open.get().origin();
        ColonyPos size = open.get().blueprint().size();

        if (RoadIndex.roadSideOf(world, colonyId, origin, size).isPresent()) {
            return 0;
        }

        return RoadIndex.roadsOf(colonyId)
                .flatMap(roads -> roads.nearestRoadColumnTo(origin, size, MAX_GAP))
                .map(column -> pave(world, colonyId, column, origin, size))
                .orElse(0);
    }

    private static int pave(ServerWorld world, UUID colonyId, long column, ColonyPos origin, ColonyPos size) {
        int x = ColonyRoads.xOf(column);
        int z = ColonyRoads.zOf(column);
        Optional<BlockPos> start = RoadPaving.groundNear(world, new BlockPos(x, origin.y() - 1, z), origin.y() - 1);

        if (start.isEmpty()) {
            return 0;
        }

        BlockPos previous = start.get();
        int laid = 0;

        while (laid < STEPS_PER_CYCLE) {
            Optional<Direction> towards = towards(previous, origin, size);

            if (towards.isEmpty()) {
                break;
            }

            BlockPos ahead = previous.offset(towards.get());

            if (gapX(ahead.getX(), origin, size) + gapZ(ahead.getZ(), origin, size) == 0) {
                // A próxima coluna já é o lote: o caminho chegou à borda.
                break;
            }

            Optional<BlockPos> ground = RoadPaving.groundNear(world, ahead, previous.getY());

            if (ground.isEmpty()) {
                break;
            }

            BlockPos at = ground.get();
            BlockState state = world.getBlockState(at);

            if (!VillageRoad.isPaving(world, state)) {
                if (RoadPaving.refusalAt(world, at, state).isPresent()) {
                    break;
                }

                if (!world.getBlockState(at.up()).isAir()) {
                    world.setBlockState(at.up(), Blocks.AIR.getDefaultState());
                }

                world.setBlockState(at, Blocks.DIRT_PATH.getDefaultState());
                laid++;
            }

            RoadIndex.remember(colonyId, at);
            previous = at;
        }

        if (laid > 0) {
            VillageColonyMod.LOGGER.info("Colony {} paved {} path blocks toward the work at {}",
                    colonyId.toString().substring(0, 8), laid, MinecraftTypeAdapter.toBlockPos(origin).toShortString());
        }

        return laid;
    }

    /** O lado que mais aproxima esta coluna do lote: o eixo de maior folga primeiro. */
    static Optional<Direction> towards(BlockPos from, ColonyPos origin, ColonyPos size) {
        int dx = gapX(from.getX(), origin, size);
        int dz = gapZ(from.getZ(), origin, size);

        if (dx == 0 && dz == 0) {
            return Optional.empty();
        }

        if (dx >= dz) {
            return Optional.of(from.getX() < origin.x() ? Direction.EAST : Direction.WEST);
        }

        return Optional.of(from.getZ() < origin.z() ? Direction.SOUTH : Direction.NORTH);
    }

    private static int gapX(int x, ColonyPos origin, ColonyPos size) {
        return Math.max(0, Math.max(origin.x() - x, x - (origin.x() + size.x() - 1)));
    }

    private static int gapZ(int z, ColonyPos origin, ColonyPos size) {
        return Math.max(0, Math.max(origin.z() - z, z - (origin.z() + size.z() - 1)));
    }
}
