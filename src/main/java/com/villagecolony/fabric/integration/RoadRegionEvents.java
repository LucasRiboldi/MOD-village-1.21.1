package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.RoadRegions;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
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
 * a casa nova e a edição do jogador refazem a região, sem esperar a impressão digital. Só anota: não muda nem cancela nada.
 */
public final class RoadRegionEvents {

    static {
        ServerMemory.register(RoadRegionEvents.class, RoadRegionEvents::clearAll);
    }

    /** Por colônia, os quadrados marcados desde a última montagem. */
    private static final Map<UUID, Set<Long>> MARKED = new HashMap<>();

    private RoadRegionEvents() {
    }

    /**
     * Um bloco mudou no mundo: o quadrado dele fica sujo para a colônia cuja caixa o contém, se a mudança importa ao grafo
     * ({@link #matters}). Planta que cresce e folha que cai não sujam nada.
     */
    public static void changed(World world, BlockPos pos, BlockState before, BlockState after) {
        if (!matters(world, pos, before, after) || !(world instanceof ServerWorld server) || !server.getServer().isOnThread()
                || !world.getRegistryKey().equals(World.OVERWORLD)) {
            return;
        }

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            Optional<VillageBounds> box = colony.bounds();

            if (box.isPresent() && pos.getX() >= box.get().minX() && pos.getX() <= box.get().maxX()
                    && pos.getZ() >= box.get().minZ() && pos.getZ() <= box.get().maxZ()) {
                MARKED.computeIfAbsent(colony.id(), ignored -> new HashSet<>())
                        .add(RoadRegions.regionOf(pos.getX(), pos.getZ()));
            }
        }
    }

    /** A mudança importa ao grafo: bloco de caminho, porta, ou bloco que passa de sólido a vazio ou o contrário. */
    static boolean matters(World world, BlockPos pos, BlockState before, BlockState after) {
        return VillageRoad.isPathBlock(before) || VillageRoad.isPathBlock(after)
                || before.getBlock() instanceof DoorBlock || after.getBlock() instanceof DoorBlock
                || before.getCollisionShape(world, pos).isEmpty() != after.getCollisionShape(world, pos).isEmpty();
    }

    /** Os quadrados marcados desta colônia; a marca sai com a leitura. */
    public static Set<Long> drain(UUID colonyId) {
        Set<Long> marked = MARKED.remove(colonyId);

        return marked == null ? Set.of() : marked;
    }

    public static void clearAll() {
        MARKED.clear();
    }
}
