package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineFloor;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Conservação da passagem planejada da mina — Regra 119 e ADR-025. */
final class MineRoute {
    private record Progress(int arm, int index) { }
    private record Layout(Set<ColonyPos> cells, Map<ColonyPos, List<Progress>> floors) { }
    private static final Map<MineShaft, Layout> LAYOUTS = new WeakHashMap<>();
    static {
        ServerMemory.register(MineRoute.class, LAYOUTS::clear);
    }
    private MineRoute() { }

    private static Layout layout(Mine mine) {
        return LAYOUTS.computeIfAbsent(mine.shaft(), ignored -> {
            Set<ColonyPos> cells = mine.plannedCells();
            Map<ColonyPos, List<Progress>> floors = new HashMap<>();
            for (int arm = 0; arm < Mine.ARMS; arm++) {
                MineShaft shaft = mine.arm(arm).shaft();
                for (int index = 0; !shaft.beyondTheArm(index); index++) {
                    ColonyPos feet = shaft.positionAt(index);
                    ColonyPos floor = new ColonyPos(feet.x(), feet.y() - 1, feet.z());
                    if (!cells.contains(floor)) {
                        floors.computeIfAbsent(floor, at -> new ArrayList<>()).add(new Progress(arm, index));
                    }
                }
            }
            return new Layout(cells, floors);
        });
    }

    /** O piso é apoio mesmo quando contém minério; a geometria deriva do registro da mina. */
    static boolean protects(Mine mine, BlockPos at) {
        return layout(mine).floors().containsKey(MinecraftTypeAdapter.toColonyPos(at));
    }

    static boolean protects(BlockPos at) {
        return MineReturn.protectsFloor(at)
                || VillageColonyMod.MINES.all().stream().anyMatch(mine -> protects(mine, at));
    }

    /** Um apoio por passagem, próximo do trabalhador, somente em trecho já visitado pelo cursor. */
    static int repairNear(ServerWorld world, Mine mine, BlockPos miner) {
        Layout route = layout(mine);
        for (var entry : route.floors().entrySet()) {
            BlockPos floor = MinecraftTypeAdapter.toBlockPos(entry.getKey());
            if (Math.abs(floor.getX() - miner.getX()) > 4
                    || Math.abs(floor.getZ() - miner.getZ()) > 4
                    || Math.abs(floor.getY() + 1 - miner.getY()) > 2
                    || !world.getChunkManager().isChunkLoaded(floor.getX() >> 4, floor.getZ() >> 4)
                    || entry.getValue().stream().noneMatch(p -> mine.arm(p.arm()).cut() > p.index())
                    || !world.getBlockState(floor).isAir()
                    || !world.getBlockState(floor.up()).isAir()
                    || !world.getBlockState(floor.up(2)).isAir()) {
                continue;
            }
            if (MineFloor.patch(world, floor.up(), route.cells())) {
                return 1;
            }
        }
        return 0;
    }
}
