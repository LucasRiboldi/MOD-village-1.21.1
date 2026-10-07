package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quem vai dormir fecha a porta da casa — ADR-037 V2 e C3: o aldeão deitado, e
 * o que foi levado para a cama. Uma vez por noite, as portas abertas a até
 * {@link #RADIUS} blocos da cama.
 */
final class HouseDoors {

    static {
        ServerMemory.register(HouseDoors.class, HouseDoors::clearAll);
    }

    /** Até onde da cama uma porta é da casa. */
    static final int RADIUS = 6;

    private static final int HEIGHT = 2;

    private static final Map<UUID, Long> CLOSED_ON = new HashMap<>();

    private HouseDoors() {
    }

    static void clearAll() {
        CLOSED_ON.clear();
    }

    /** Fecha as portas da casa deste aldeão, se ainda não fechou nesta noite. */
    static int closeOnce(ServerWorld world, VillagerEntity villager, BlockPos bed) {
        long night = Math.floorDiv(world.getTimeOfDay(), (long) WorkClock.DAY);

        if (CLOSED_ON.getOrDefault(villager.getUuid(), Long.MIN_VALUE) == night) {
            return 0;
        }

        CLOSED_ON.put(villager.getUuid(), night);

        return close(world, villager, bed);
    }

    /** Fecha as portas abertas em volta da cama; devolve quantas fechou. */
    static int close(ServerWorld world, VillagerEntity villager, BlockPos bed) {
        int closed = 0;

        for (BlockPos at : BlockPos.iterate(bed.add(-RADIUS, -HEIGHT, -RADIUS), bed.add(RADIUS, HEIGHT, RADIUS))) {
            WorldChunk chunk = world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4);

            if (chunk == null) {
                continue;
            }

            BlockState state = chunk.getBlockState(at);

            if (state.getBlock() instanceof DoorBlock door && door.isOpen(state)
                    && state.get(DoorBlock.HALF) == net.minecraft.block.enums.DoubleBlockHalf.LOWER) {
                door.setOpen(villager, world, state, at.toImmutable(), false);
                closed++;
            }
        }

        return closed;
    }
}
