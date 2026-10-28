package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O chão de dentro dos currais é caro no caminho de quem não é pastor — Regra 122 (jogo da 0.3.93:
 * 217 entradas em curral só na planície, e o mineiro da taiga 3,5 h cercado).
 *
 * <p>O caminho do aldeão de ofício passa por fora quando há volta; atravessar o curral continua
 * possível quando é o único jeito, só não é mais o atalho. O pastor, que trabalha ali, anda como
 * sempre. Os currais são os que o pastor já mede ({@code ShepherdPens.pensOf}).
 */
public final class PenFloors {

    static {
        ServerMemory.register(PenFloors.class, PenFloors::clearAll);
    }

    /** Como o chão do curral conta no caminho: o perigo leve, que o Vanilla cobra 8 por bloco. */
    public static final PathNodeType COSTLY = PathNodeType.DANGER_OTHER;

    private static final Map<UUID, Set<Long>> BY_COLONY = new HashMap<>();

    private static final Set<Long> FLOORS = new HashSet<>();

    private PenFloors() {
    }

    /** O chão de dentro de cada curral desta colônia, relido. */
    public static void update(UUID colonyId, List<? extends Set<BlockPos>> reaches) {
        Set<Long> floors = new HashSet<>();

        for (Set<BlockPos> reach : reaches) {
            for (BlockPos at : reach) {
                floors.add(at.asLong());
            }
        }

        BY_COLONY.put(colonyId, floors);
        FLOORS.clear();
        BY_COLONY.values().forEach(FLOORS::addAll);
    }

    /**
     * O tipo do nó para este bicho: o mesmo, ou {@link #COSTLY} quando é chão de curral, o nó seria
     * andável e quem anda é aldeão de ofício que não é pastor.
     */
    public static PathNodeType forWalker(MobEntity mob, int x, int y, int z, PathNodeType type) {
        if (FLOORS.isEmpty() || type != PathNodeType.WALKABLE || !(mob instanceof VillagerEntity)
                || !FLOORS.contains(BlockPos.asLong(x, y, z))) {
            return type;
        }

        boolean shepherd = VillageColonyMod.WORKERS.find(mob.getUuid())
                .flatMap(Worker::profession)
                .filter(ProfessionType.SHEPHERD::equals)
                .isPresent();

        return shepherd || VillageColonyMod.WORKERS.find(mob.getUuid()).isEmpty() ? type : COSTLY;
    }

    static void clearAll() {
        BY_COLONY.clear();
        FLOORS.clear();
    }
}
