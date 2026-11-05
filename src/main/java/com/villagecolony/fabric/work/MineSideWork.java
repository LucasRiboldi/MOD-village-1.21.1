package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Quem espera ramal cava no próprio nível — Regra 193 (autor, 2026-11-05): cerca de um terço das amostras dos mineiros era
 * "waiting for a branch". Ele alarga o salão em volta, de dentro para fora, até {@value #RINGS} anéis, em duas alturas.
 *
 * <p>Não corta degrau nem o chão de degrau, e não encosta em fluido. O salão de referência é o do nível, quando já foi
 * cavado; antes dele, o salão de cima, onde a entrada do nível fica. No primeiro nível, sem salão, ele espera.
 */
final class MineSideWork {

    private static final int RINGS = 6;

    private static final int FIRST_RING = MineShaft.HALL_SIDE / 2 + 1;

    /** Sem nada a cavar, olha de novo depois disto: a varredura lê centenas de blocos. */
    private static final long LOOK_AGAIN = 600;

    /** A pedra que voltou esta vez e não saiu é inalcançável. */
    private static final int MAX_OFFERS = 3;

    private record Stairs(MineShaft shaft, Set<ColonyPos> cells) {
    }

    private static final Map<UUID, Stairs> STAIRS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_LOOK = new HashMap<>();
    private static final Map<UUID, BlockPos> TAKEN = new HashMap<>();
    private static final Map<BlockPos, Integer> OFFERS = new HashMap<>();

    static {
        ServerMemory.register(MineSideWork.class, MineSideWork::clearAll);
    }

    private MineSideWork() {
    }

    static Optional<BlockPos> nextCut(ServerWorld world, UUID workerId, UUID colonyId, Mine mine) {
        MineShaft shaft = mine.shaft();
        long now = world.getTime();

        TAKEN.remove(workerId);

        if (shaft.isRamp() || now < NEXT_LOOK.getOrDefault(colonyId, Long.MIN_VALUE)) {
            return Optional.empty();
        }

        List<MineShaft> levels = MineEntrance.levels(world, colonyId, mine);
        boolean hallDug = mine.arm(0).cut() >= MineShaft.SHARED_BLOCKS;

        if (!hallDug && levels.size() < 2) {
            return Optional.empty();
        }

        ColonyPos floor = hallDug ? shaft.hallFloor() : shaft.entry();
        Set<ColonyPos> stairs = stairsOf(colonyId, shaft, levels);

        for (int ring = FIRST_RING; ring < FIRST_RING + RINGS; ring++) {
            Optional<BlockPos> found = onRing(world, mine, floor, ring, stairs);

            if (found.isPresent()) {
                TAKEN.put(workerId, found.get());
                OFFERS.merge(found.get(), 1, Integer::sum);

                return found;
            }
        }

        NEXT_LOOK.put(colonyId, now + LOOK_AGAIN);

        return Optional.empty();
    }

    private static Optional<BlockPos> onRing(ServerWorld world, Mine mine, ColonyPos floor, int ring, Set<ColonyPos> stairs) {
        for (int dx = -ring; dx <= ring; dx++) {
            for (int dz = -ring; dz <= ring; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                    continue;
                }

                for (int height = 1; height <= 2; height++) {
                    BlockPos at = new BlockPos(floor.x() + dx, floor.y() + height, floor.z() + dz);

                    if (mayCut(world, mine, at, stairs)) {
                        return Optional.of(at);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static boolean mayCut(ServerWorld world, Mine mine, BlockPos at, Set<ColonyPos> stairs) {
        ColonyPos cell = MinecraftTypeAdapter.toColonyPos(at);

        if (!world.isChunkLoaded(at.getX() >> 4, at.getZ() >> 4) || stairs.contains(cell)
                || stairs.contains(new ColonyPos(cell.x(), cell.y() + 1, cell.z()))
                || TAKEN.containsValue(at) || OFFERS.getOrDefault(at, 0) >= MAX_OFFERS
                || !MineRock.isDiggableRock(world, mine, at)) {
            return false;
        }

        boolean touchesOpen = false;

        for (Direction side : Direction.values()) {
            BlockPos next = at.offset(side);

            if (!world.isChunkLoaded(next.getX() >> 4, next.getZ() >> 4) || !world.getFluidState(next).isEmpty()) {
                return false;
            }

            touchesOpen |= side != Direction.UP && MineRock.isOpenSpace(world, next, world.getBlockState(next));
        }

        return touchesOpen;
    }

    /** Os degraus do nível e dos três de cima: caracol do nível e os caracóis dos quatro ramais. */
    private static Set<ColonyPos> stairsOf(UUID colonyId, MineShaft shaft, List<MineShaft> levels) {
        Stairs known = STAIRS.get(colonyId);

        if (known != null && known.shaft().equals(shaft)) {
            return known.cells();
        }

        Set<ColonyPos> cells = new HashSet<>();
        int armStairCells = MineShaft.ARM_STAIRS * MineShaft.STAIR_STEP_BLOCKS;

        for (MineShaft level : levels.subList(Math.max(0, levels.size() - 4), levels.size())) {
            for (int index = 0; index < MineShaft.CARVED; index++) {
                cells.add(level.positionAt(index));
            }

            MineShaft heading = level;

            for (int arm = 0; arm < Mine.ARMS; arm++, heading = heading.turned()) {
                for (int index = 0; index < armStairCells; index++) {
                    cells.add(heading.positionAt(MineShaft.SHARED_BLOCKS + index));
                }
            }
        }

        STAIRS.put(colonyId, new Stairs(shaft, cells));

        return cells;
    }

    private static void clearAll() {
        STAIRS.clear();
        NEXT_LOOK.clear();
        TAKEN.clear();
        OFFERS.clear();
    }
}
