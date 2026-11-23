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
 * O chão de dentro dos currais fica fora do caminho de quem não é pastor — Regra 122.
 *
 * <p>Para o aldeão que está fora do curral, o chão de dentro é bloqueado: o portão aberto
 * na caminhada do pastor não vira atalho. Quem já está dentro anda nele com custo, para poder
 * sair. O pastor, que trabalha ali, anda como sempre. Os currais são os que o pastor já mede
 * ({@code ShepherdPens.pensOf}).
 */
public final class PenFloors {

    static {
        ServerMemory.register(PenFloors.class, PenFloors::clearAll);
    }

    /** Como o chão do curral conta para quem está dentro: o perigo leve, que o Vanilla cobra 8 por bloco. */
    public static final PathNodeType COSTLY = PathNodeType.DANGER_OTHER;

    /** Como o chão do curral conta para quem está fora: não se anda nele. */
    public static final PathNodeType CLOSED = PathNodeType.BLOCKED;

    private static final Map<UUID, Set<Long>> BY_COLONY = new HashMap<>();

    private static final Set<Long> FLOORS = new HashSet<>();

    private PenFloors() {
    }

    /** Se este lugar é chão de dentro de um curral: a lavoura dali é pasto do rebanho, não roça (Regra 332). */
    public static boolean isPenFloor(BlockPos at) {
        return FLOORS.contains(at.asLong());
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
     * O tipo do nó para este bicho: o mesmo, ou, quando é chão de curral, o nó seria andável e quem
     * anda é aldeão que não é pastor, {@link #CLOSED} (ele está fora) ou {@link #COSTLY}
     * (ele está dentro e precisa sair).
     */
    public static PathNodeType forWalker(MobEntity mob, int x, int y, int z, PathNodeType type) {
        if (FLOORS.isEmpty() || !(mob instanceof VillagerEntity)
                || !mob.getWorld().getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)) {
            return type;
        }

        boolean targetInside = FLOORS.contains(BlockPos.asLong(x, y, z));
        boolean inside = FLOORS.contains(mob.getBlockPos().asLong());
        if (!targetInside && !(inside && mob.getWorld().getBlockState(new BlockPos(x, y, z)).getBlock()
                instanceof net.minecraft.block.FenceGateBlock)) return type;

        boolean shepherd = VillageColonyMod.WORKERS.find(mob.getUuid())
                .flatMap(Worker::profession)
                .filter(ProfessionType.SHEPHERD::equals)
                .isPresent();

        if (shepherd) {
            return inside && !targetInside ? CLOSED : type;
        }

        if (!targetInside) return type;
        if (!inside) return CLOSED;
        return type == PathNodeType.WALKABLE ? COSTLY : type;
    }

    static void clearAll() {
        BY_COLONY.clear();
        FLOORS.clear();
    }
}
