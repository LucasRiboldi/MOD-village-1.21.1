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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O caminho até a obra — ADR-039 D2, e o corredor da regra do autor de 2026-10-22. A obra que não encosta
 * em rua (lote da busca larga, Emenda 7) ganha um {@link Corridor}: sai da porta em linha reta, vira só em
 * ângulo reto e para no primeiro caminho que encostar. Era uma linha que trocava de eixo a cada passo e
 * fazia escada em diagonal até a borda do lote. Calça até {@link #STEPS_PER_CYCLE} blocos por ciclo, da
 * ponta da rua para a porta: o trecho já calçado é o encontro do corredor do ciclo seguinte, na mesma reta.
 * Calçar não custa material (Regra 15), e a Regra 3 vale como na rua.
 */
public final class WorkPath {

    /** Blocos calçados por ciclo, no máximo. */
    static final int STEPS_PER_CYCLE = 4;

    /** A obra mais longe que isto da rua não ganha caminho: não é vizinha dela. */
    static final int MAX_GAP = 24;

    /** O corredor mais longo. */
    static final int MAX_LENGTH = 32;

    private WorkPath() {
    }

    public static int paveToward(ServerWorld world, UUID colonyId) {
        Optional<ConstructionProject> open = VillageColonyMod.CONSTRUCTIONS.openOf(colonyId)
                .filter(project -> project.state() == ConstructionState.BUILDING);

        if (open.isEmpty() || open.get().blueprint().isPlantation()) {
            return 0;
        }

        ConstructionProject project = open.get();
        ColonyPos origin = project.origin();
        ColonyPos size = project.blueprint().size();

        if (RoadIndex.roadSideOf(world, colonyId, origin, size).isPresent()) {
            return 0;
        }

        Optional<Long> road = RoadIndex.roadsOf(colonyId).flatMap(roads -> roads.nearestRoadColumnTo(origin, size, MAX_GAP));

        if (road.isEmpty()) {
            return 0;
        }

        Corridor.Door door = Corridor.doorOf(project).orElseGet(() -> sideFacing(origin, size, road.get()));
        Optional<BlockPos> start = RoadPaving.groundNear(world, door.front().down(), origin.y() - 1);

        if (start.isEmpty()) {
            return 0;
        }

        return Corridor.plan(world, colonyId, start.get(), door.out(), MAX_LENGTH, at -> onTheLot(at, origin, size))
                .map(cells -> pave(world, colonyId, cells, origin))
                .orElse(0);
    }

    /** Calça até {@link #STEPS_PER_CYCLE} blocos do corredor, da ponta da rua para a porta. */
    private static int pave(ServerWorld world, UUID colonyId, List<BlockPos> cells, ColonyPos origin) {
        int laid = 0;

        for (int index = cells.size() - 1; index >= 0 && laid < STEPS_PER_CYCLE; index--) {
            BlockPos at = cells.get(index);

            if (VillageRoad.isPaving(world, at)) {
                continue;
            }

            BlockState state = world.getBlockState(at);

            if (!world.getBlockState(at.up()).isAir()) {
                world.setBlockState(at.up(), Blocks.AIR.getDefaultState());
            }

            world.setBlockState(at, VillageRoad.pavingOn(state).getDefaultState());
            RoadIndex.remember(colonyId, at);
            laid++;
        }

        if (laid > 0) {
            VillageColonyMod.LOGGER.info("Colony {} paved {} path blocks of the corridor toward the work at {}",
                    colonyId.toString().substring(0, 8), laid, MinecraftTypeAdapter.toBlockPos(origin).toShortString());
        }

        return laid;
    }

    /** A obra sem porta: o meio do lado do lote virado para a rua mais perto. */
    static Corridor.Door sideFacing(ColonyPos origin, ColonyPos size, long column) {
        int dx = ColonyRoads.xOf(column) - (origin.x() + size.x() / 2);
        int dz = ColonyRoads.zOf(column) - (origin.z() + size.z() / 2);
        Direction out = Math.abs(dx) >= Math.abs(dz)
                ? (dx < 0 ? Direction.WEST : Direction.EAST)
                : (dz < 0 ? Direction.NORTH : Direction.SOUTH);
        int x = switch (out) {
            case WEST -> origin.x() - 1;
            case EAST -> origin.x() + size.x();
            default -> origin.x() + size.x() / 2;
        };
        int z = switch (out) {
            case NORTH -> origin.z() - 1;
            case SOUTH -> origin.z() + size.z();
            default -> origin.z() + size.z() / 2;
        };

        return new Corridor.Door(new BlockPos(x, origin.y(), z), out);
    }

    private static boolean onTheLot(BlockPos at, ColonyPos origin, ColonyPos size) {
        return at.getX() >= origin.x() && at.getX() < origin.x() + size.x()
                && at.getZ() >= origin.z() && at.getZ() < origin.z() + size.z();
    }
}
