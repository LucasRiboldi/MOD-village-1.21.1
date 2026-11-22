package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.construction.model.RoadRegions;
import com.villagecolony.core.construction.model.VillagePathPlanner;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O aviso de bloco mudado do modo 2 do grafo — ADR-057: o quadrado de {@value RoadRegions#SIZE} × {@value RoadRegions#SIZE}
 * em que um bloco mudou, dentro da caixa de uma colônia, fica sujo até a próxima montagem do grafo. Assim o chão nivelado,
 * a casa nova e a edição do jogador refazem a região, sem esperar a impressão digital. As mudanças de terreno também
 * liberam a busca nas colunas recusadas afetadas (Regras 177 e 295).
 */
public final class RoadRegionEvents {

    static {
        ServerMemory.register(RoadRegionEvents.class, RoadRegionEvents::clearAll);
    }

    /** Por colônia, os quadrados marcados desde a última montagem. */
    private static final Map<UUID, Set<Long>> MARKED = new HashMap<>();
    private static final Map<UUID, Map<Long, Integer>> REFUSED = new HashMap<>();

    private RoadRegionEvents() {
    }

    /** Exclui o passo da busca até mudar seu piso, apoio ou passagem (Regra 295). */
    public static void refused(UUID colonyId, PathStep step) {
        REFUSED.computeIfAbsent(colonyId, ignored -> new HashMap<>())
                .put(ColonyRoads.column(step.x(), step.z()), step.floorY());
    }

    public static Set<Long> refusedColumns(UUID colonyId) {
        return REFUSED.getOrDefault(colonyId, Map.of()).keySet();
    }

    /**
     * Um bloco mudou no mundo: o quadrado dele fica sujo para a colônia cuja caixa o contém, se a mudança importa ao grafo
     * ({@link #matters}). Planta que cresce e folha que cai não sujam nada.
     */
    public static void changed(World world, BlockPos pos, BlockState before, BlockState after) {
        if (!(world instanceof ServerWorld server) || !server.getServer().isOnThread()
                || !world.getRegistryKey().equals(World.OVERWORLD)) {
            return;
        }

        boolean roadChange = matters(world, pos, before, after);
        // Dois blocos sólidos podem ter apoio e proteção diferentes, sem alterar a presença de colisão.
        if (!roadChange && (REFUSED.isEmpty() || before.getBlock() == after.getBlock())) {
            return;
        }

        int x = pos.getX();
        int z = pos.getZ();
        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            UUID id = colony.id();
            reconsiderNearby(id, pos);

            Optional<VillageBounds> box = colony.bounds();
            if (roadChange && box.isPresent() && x >= box.get().minX() && x <= box.get().maxX()
                    && z >= box.get().minZ() && z <= box.get().maxZ()) {
                mark(id, x, z);
            }
        }
    }

    private static void reconsiderNearby(UUID id, BlockPos changed) {
        Map<Long, Integer> refused = REFUSED.get(id);
        if (refused == null) {
            return;
        }
        int x = changed.getX();
        int z = changed.getZ();
        reconsider(id, refused, changed, x, z);
        for (Direction direction : Direction.Type.HORIZONTAL) {
            reconsider(id, refused, changed, x + direction.getOffsetX(), z + direction.getOffsetZ());
        }
        if (refused.isEmpty()) {
            REFUSED.remove(id);
        }
    }

    /** Marca também a região do passo liberado, que pode ser a vizinha da região editada. */
    private static void reconsider(UUID id, Map<Long, Integer> refused, BlockPos changed, int x, int z) {
        long column = ColonyRoads.column(x, z);
        Integer floor = refused.get(column);
        if (floor != null && changed.getY() >= floor - VillagePathPlanner.MAX_FILL - 1
                && changed.getY() <= floor + TreeClearing.HEADROOM + 1) {
            refused.remove(column);
            mark(id, x, z);
        }
    }

    private static void mark(UUID id, int x, int z) {
        MARKED.computeIfAbsent(id, ignored -> new HashSet<>()).add(RoadRegions.regionOf(x, z));
    }

    /** Mudança em caminho, porta, fluido ou na presença de colisão. */
    static boolean matters(World world, BlockPos pos, BlockState before, BlockState after) {
        return VillageRoad.isPathBlock(before) || VillageRoad.isPathBlock(after)
                || before.getBlock() instanceof DoorBlock || after.getBlock() instanceof DoorBlock
                || before.getFluidState().getFluid() != after.getFluidState().getFluid()
                || before.getCollisionShape(world, pos).isEmpty() != after.getCollisionShape(world, pos).isEmpty();
    }

    /** Os quadrados marcados desta colônia; a marca sai com a leitura. */
    public static Set<Long> drain(UUID colonyId) {
        Set<Long> marked = MARKED.remove(colonyId);

        return marked == null ? Set.of() : marked;
    }

    public static void clearAll() {
        MARKED.clear();
        REFUSED.clear();
    }
}
