package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.TreeClearing;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O acesso livre entre as construções — Regras 178 e 242 (autor, 2026-11-05 e 2026-11-11): quem arruma o chão tira o
 * que passa do nível da casa no anel de {@value #RING} blocos em volta dela (calombo de até {@value #MAX_CUT}) e nivela o
 * vão entre duas construções a até {@value #MAX_GAP} blocos uma da outra: o chão do meio do vão, fora dos anéis, vai em
 * rampa da base de uma até a base da outra, cortando ou aterrando até {@value #LEVEL} blocos por coluna. Morro maior fica com o alisamento
 * ({@link GroundSmoothing}), que faz o degrau de um bloco.
 */
final class BuildingGaps {

    static final int MAX_GAP = 8;

    /** Quanto o vão entre as bases corta ou aterra por coluna (Regra 242). */
    static final int LEVEL = 4;

    static final int RING = 2;

    static final int MAX_CUT = 3;

    /** Colunas mexidas por passada. */
    private static final int BUDGET = 32;

    /** A lista de construções é relida a cada 5 minutos: ler as portas custa. */
    private static final long READ_EVERY = 6_000;

    private record Box(int minX, int minZ, int maxX, int maxZ, int floor) {
        boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    private record Read(List<Box> boxes, long at) {
    }

    private static final Map<UUID, Read> READS = new HashMap<>();

    static {
        ServerMemory.register(BuildingGaps.class, READS::clear);
    }

    private BuildingGaps() {
    }

    /** Uma passada do pedreiro; devolve quantas colunas mudaram. */
    static int pass(ServerWorld world, Colony colony) {
        List<Box> boxes = boxesOf(world, colony);
        Optional<BlockPos> mine = VillageColonyMod.MINES.of(colony.id())
                .map(found -> MinecraftTypeAdapter.toBlockPos(found.shaft().entry()));
        int[] left = {BUDGET};

        for (Box box : boxes) {
            ring(world, colony, box, boxes, mine, left);

            if (left[0] <= 0) {
                return BUDGET;
            }
        }

        for (int i = 0; i < boxes.size(); i++) {
            for (int j = i + 1; j < boxes.size(); j++) {
                gap(world, colony, boxes.get(i), boxes.get(j), boxes, mine, left);

                if (left[0] <= 0) {
                    return BUDGET;
                }
            }
        }

        return BUDGET - left[0];
    }

    /** Se esta coluna fica dentro de uma construção da vila. */
    static boolean insideABuilding(ServerWorld world, Colony colony, int x, int z) {
        return boxesOf(world, colony).stream().anyMatch(box -> box.contains(x, z));
    }

    private static List<Box> boxesOf(ServerWorld world, Colony colony) {
        long now = world.getTime();
        Read read = READS.get(colony.id());

        if (read != null && now - read.at() < READ_EVERY) {
            return read.boxes();
        }

        List<Box> boxes = new ArrayList<>();

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (building.finished()) {
                boxes.add(boxOf(world, MinecraftTypeAdapter.toBlockPos(building.min()),
                        MinecraftTypeAdapter.toBlockPos(building.max())));
            }
        }

        BlockPos hub = MinecraftTypeAdapter.toBlockPos(colony.center());

        for (BlockBox house : GeneratedVillages.housesAt(world, hub.getX(), hub.getZ())) {
            boxes.add(boxOf(world, new BlockPos(house.getMinX(), house.getMinY(), house.getMinZ()),
                    new BlockPos(house.getMaxX(), house.getMaxY(), house.getMaxZ())));
        }

        READS.put(colony.id(), new Read(List.copyOf(boxes), now));

        return boxes;
    }

    /** O piso é o chão em frente à porta; sem porta, a base da caixa. */
    private static Box boxOf(ServerWorld world, BlockPos min, BlockPos max) {
        int floor = DoorFronts.outside(world, min, max).stream().findFirst().map(BlockPos::getY).orElse(min.getY());

        return new Box(min.getX(), min.getZ(), max.getX(), max.getZ(), floor);
    }

    private static void ring(ServerWorld world, Colony colony, Box box, List<Box> boxes, Optional<BlockPos> mine, int[] left) {
        for (int x = box.minX() - RING; x <= box.maxX() + RING && left[0] > 0; x++) {
            for (int z = box.minZ() - RING; z <= box.maxZ() + RING && left[0] > 0; z++) {
                if (!box.contains(x, z)) {
                    cut(world, colony, x, z, box.floor(), MAX_CUT, boxes, mine, left);
                }
            }
        }
    }

    private static void gap(ServerWorld world, Colony colony, Box a, Box b, List<Box> boxes, Optional<BlockPos> mine,
                            int[] left) {
        int fromX = Math.max(a.minX(), b.minX());
        int toX = Math.min(a.maxX(), b.maxX());
        int fromZ = Math.max(a.minZ(), b.minZ());
        int toZ = Math.min(a.maxZ(), b.maxZ());

        if (fromX <= toX) {
            Box near = a.maxZ() < b.minZ() ? a : b;
            Box far = near == a ? b : a;

            if (far.minZ() - near.maxZ() - 1 >= 1 && far.minZ() - near.maxZ() - 1 <= MAX_GAP) {
                for (int z = near.maxZ() + 1; z < far.minZ() && left[0] > 0; z++) {
                    int floor = rampAt(near.floor(), far.floor(), z - near.maxZ(), far.minZ() - near.maxZ());

                    for (int x = fromX; x <= toX && left[0] > 0; x++) {
                        level(world, colony, x, z, floor, boxes, mine, left);
                    }
                }
            }
        } else if (fromZ <= toZ) {
            Box near = a.maxX() < b.minX() ? a : b;
            Box far = near == a ? b : a;

            if (far.minX() - near.maxX() - 1 >= 1 && far.minX() - near.maxX() - 1 <= MAX_GAP) {
                for (int x = near.maxX() + 1; x < far.minX() && left[0] > 0; x++) {
                    int floor = rampAt(near.floor(), far.floor(), x - near.maxX(), far.minX() - near.maxX());

                    for (int z = fromZ; z <= toZ && left[0] > 0; z++) {
                        level(world, colony, x, z, floor, boxes, mine, left);
                    }
                }
            }
        }
    }

    /** Se a coluna fica na construção ou no anel de {@value #RING} em volta dela. */
    private static boolean inRing(Box box, int x, int z) {
        return x >= box.minX() - RING && x <= box.maxX() + RING && z >= box.minZ() - RING && z <= box.maxZ() + RING;
    }

    /** A altura da rampa na coluna {@code step} de {@code span - 1}, da base {@code from} à base {@code to} (Regra 242). */
    static int rampAt(int from, int to, int step, int span) {
        return from + Math.round((float) (to - from) * step / span);
    }

    /**
     * Corta ou aterra esta coluna do vão até a rampa, até {@value #LEVEL} blocos. O anel de {@value #RING} em volta de cada
     * construção fica com a base dela: a rampa que entrasse nele seria cortada pelo anel na passada seguinte, sem fim.
     */
    private static void level(ServerWorld world, Colony colony, int x, int z, int floor, List<Box> boxes,
                              Optional<BlockPos> mine, int[] left) {
        if (!world.isChunkLoaded(x >> 4, z >> 4) || boxes.stream().anyMatch(box -> inRing(box, x, z))
                || mine.filter(at -> Math.abs(at.getX() - x) <= GroundSmoothing.MINE_GUARD
                        && Math.abs(at.getZ() - z) <= GroundSmoothing.MINE_GUARD).isPresent()) {
            return;
        }

        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

        if (top > floor) {
            cut(world, colony, x, z, floor, LEVEL, boxes, mine, left);

            return;
        }

        BlockPos surface = new BlockPos(x, top, z);

        if (top < floor && floor - top <= LEVEL && GroundSmoothing.isGround(world, surface, world.getBlockState(surface))
                && GroundSmoothing.column(world, x, z, top, floor)) {
            left[0]--;
        }
    }

    /** Tira a árvore e corta o calombo desta coluna até o piso, se ele não passa de {@code most} blocos. */
    private static void cut(ServerWorld world, Colony colony, int x, int z, int floor, int most, List<Box> boxes,
                            Optional<BlockPos> mine, int[] left) {
        if (!world.isChunkLoaded(x >> 4, z >> 4) || boxes.stream().anyMatch(box -> box.contains(x, z))
                || mine.filter(at -> Math.abs(at.getX() - x) <= GroundSmoothing.MINE_GUARD
                        && Math.abs(at.getZ() - z) <= GroundSmoothing.MINE_GUARD).isPresent()) {
            return;
        }

        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

        if (top <= floor || top - floor > TreeClearing.TALL) {
            return;
        }

        int cleared = 0;

        if (TreeClearing.isTree(world.getBlockState(new BlockPos(x, top, z)))) {
            cleared = TreeClearing.growthAbove(world, x, z, floor,
                    ColonyChests.nearestFirst(world, colony.id(), new ColonyPos(x, floor, z)));
            top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        }

        BlockPos surface = new BlockPos(x, top, z);
        BlockState state = world.getBlockState(surface);
        boolean bump = top > floor && top - floor <= most && GroundSmoothing.isGround(world, surface, state)
                && GroundSmoothing.column(world, x, z, top, floor);

        if (cleared > 0 || bump) {
            left[0]--;
        }
    }
}
