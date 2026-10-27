package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.PathTerrain;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * O terreno do mundo como o traçado do caminho o enxerga — Regra 123.
 *
 * <p><b>O caminho não destrói construção</b> (autor, 27-10): coluna com casa ou obra da colônia, peça
 * da vila que não seja chão do bioma, bloco do jogador ou água não tem chão para caminho. O que se corta
 * ({@code VillagePathPlanner.MAX_CUT}) e o que se aterra é sempre chão natural. Chunk fora de memória
 * não é lido (ADR-002): a coluna fica proibida e o traçado passa por outro lado ou espera.
 */
public final class WorldPathTerrain implements PathTerrain {

    /** Quanto acima e abaixo da altura de referência se procura o chão. */
    static final int ABOVE = 3;
    static final int BELOW = 6;

    /** Quantos blocos acima do chão não podem ser construção: o corte e a altura livre do caminho. */
    static final int HEADROOM = 3;

    private final ServerWorld world;
    private final List<Building> buildings;
    private final Optional<ConstructionProject> open;

    public WorldPathTerrain(ServerWorld world, UUID colonyId) {
        this.world = world;
        this.buildings = List.copyOf(VillageColonyMod.BUILDINGS.ofColony(colonyId));
        this.open = VillageColonyMod.CONSTRUCTIONS.openOf(colonyId);
    }

    @Override
    public OptionalInt groundY(int x, int z, int nearY) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return OptionalInt.empty();
        }

        for (int y = nearY + ABOVE; y >= nearY - BELOW; y--) {
            BlockPos at = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(at);

            if (state.isAir() || (LotGround.isNothing(state) && state.getFluidState().isEmpty())) {
                continue;
            }

            if (!state.getFluidState().isEmpty()) {
                return OptionalInt.empty();
            }

            if (!state.isOf(Blocks.DIRT_PATH) && RoadPaving.refusalOfTheGround(world, at, state).isPresent()) {
                return OptionalInt.empty();
            }

            return builtAbove(at) ? OptionalInt.empty() : OptionalInt.of(y);
        }

        return OptionalInt.empty();
    }

    @Override
    public boolean blocked(int x, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return true;
        }

        for (Building building : buildings) {
            if (inside(x, z, building.min(), building.max())) {
                return true;
            }
        }

        return open.filter(project -> {
            ColonyPos origin = project.origin();
            ColonyPos size = project.blueprint().size();

            return inside(x, z, origin, new ColonyPos(origin.x() + size.x() - 1, origin.y(), origin.z() + size.z() - 1));
        }).isPresent();
    }

    /** Construção em cima do chão, na altura do corte e do caminho: a coluna é de alguém. */
    private boolean builtAbove(BlockPos ground) {
        for (int up = 1; up <= HEADROOM; up++) {
            BlockPos at = ground.up(up);
            BlockState state = world.getBlockState(at);

            if (state.isAir() || LotGround.isNothing(state)) {
                continue;
            }

            if (!BlockProtection.mayDigOut(world, at, state)) {
                return true;
            }
        }

        return false;
    }

    private static boolean inside(int x, int z, ColonyPos min, ColonyPos max) {
        return x >= min.x() && x <= max.x() && z >= min.z() && z <= max.z();
    }
}
