package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.RoadIndex;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * O bosque do lenhador perto da vila — pedido do autor, 2026-10-08: <i>"plantar
 * três mudas perto da vila em espaços que não atrapalhariam"</i>, longe de onde a
 * vila vai construir.
 *
 * <p>No playtest de 08-10 nenhuma árvore ficava no miolo da vila; as 32 cortadas
 * estavam a 48–107 blocos do centro, e o lenhador passou 26% do tempo parado nas
 * caminhadas longas. O viveiro da borda ({@link LumberjackNursery}) planta num anel
 * fixo a partir do centro, que numa vila grande cai dentro dela.
 *
 * <p><b>Onde.</b> Fora da caixa da vila, de {@link #NEAR} a {@link #FAR} blocos da
 * borda, do lado do baú do lenhador primeiro. <b>O que não atrapalha:</b> a caixa
 * da vila (as casas e os lotes de dentro), a casa ou obra da colônia com
 * {@link #CLEARANCE} de folga, e a rua — o lote novo nasce ao lado da rua, então a
 * muda fica a {@link #ROAD_CLEARANCE} dela —, e as outras mudas do bosque a
 * {@link #SPACING}. Mantém {@link #SIZE} vivas: a cortada é reposta.
 */
public final class VillageGrove {

    static {
        ServerMemory.register(VillageGrove.class, VillageGrove::clearAll);
    }

    /** Quantas mudas o bosque tem. */
    public static final int SIZE = 3;

    /** A distância mínima e máxima da borda da vila. */
    static final int NEAR = 4;
    static final int FAR = 10;

    /** Folga até casa ou obra da colônia. */
    static final int CLEARANCE = 4;

    /** Folga até a rua, onde nascem os lotes novos. */
    static final int ROAD_CLEARANCE = 6;

    /** Entre duas mudas do bosque. */
    static final int SPACING = 5;

    /** De quanto em quanto tempo o bosque é conferido: 1 min. */
    static final long CHECK_EVERY = 1_200;

    private static final Map<UUID, List<BlockPos>> GROVES = new HashMap<>();

    private static final Map<UUID, Long> CHECKED = new HashMap<>();

    private VillageGrove() {
    }

    /** Confere o bosque desta colônia, no máximo uma vez por {@link #CHECK_EVERY}. */
    static void tend(ServerWorld world, UUID colonyId) {
        long now = world.getTime();
        Long last = CHECKED.get(colonyId);

        if (last != null && now - last < CHECK_EVERY) {
            return;
        }

        CHECKED.put(colonyId, now);
        VillageColonyMod.COLONIES.find(colonyId).ifPresent(colony -> tendNow(world, colony));
    }

    /**
     * Repõe o bosque até {@link #SIZE}, sem o relógio — para o teste.
     *
     * @return quantas mudas foram plantadas agora
     */
    static int tendNow(ServerWorld world, Colony colony) {
        Optional<VillageBounds> box = colony.bounds();
        Optional<Block> sapling = TreeNursery.saplingFor(world, colony.center());

        if (box.isEmpty() || sapling.isEmpty()) {
            return 0;
        }

        List<BlockPos> grove = GROVES.computeIfAbsent(colony.id(), id -> new ArrayList<>());

        grove.removeIf(ground -> !alive(world, ground));

        if (grove.isEmpty()) {
            grove.addAll(found(world, box.get(), colony.center().y()));
        }

        int planted = 0;

        while (grove.size() < SIZE) {
            Optional<BlockPos> spot = spotFor(world, colony, box.get(), grove);

            if (spot.isEmpty() || !TreeNursery.plant(world, spot.get(), sapling.get())) {
                break;
            }

            grove.add(spot.get());
            planted++;
            com.villagecolony.fabric.integration.VillageTrees.rememberSapling(colony.id(), spot.get().up());

            VillageColonyMod.LOGGER.info(
                    "Colony {} — the lumberjack planted {} near the village at {} (grove {} of {})",
                    colony.id().toString().substring(0, 8), TreeNursery.idOf(sapling.get()),
                    spot.get().toShortString(), grove.size(), SIZE);
        }

        return planted;
    }

    /** As mudas de agora, para o teste. */
    static List<BlockPos> of(UUID colonyId) {
        return List.copyOf(GROVES.getOrDefault(colonyId, List.of()));
    }

    /** O primeiro lugar que serve, do lado do baú do lenhador para os outros. */
    private static Optional<BlockPos> spotFor(ServerWorld world, Colony colony, VillageBounds box, List<BlockPos> grove) {
        double towards = towardsTheLumberjack(colony.id(), box);
        List<int[]> ring = new ArrayList<>();

        for (int away = NEAR; away <= FAR; away++) {
            for (int x = box.minX() - away; x <= box.maxX() + away; x += 2) {
                ring.add(new int[] {x, box.minZ() - away, away});
                ring.add(new int[] {x, box.maxZ() + away, away});
            }

            for (int z = box.minZ() - away; z <= box.maxZ() + away; z += 2) {
                ring.add(new int[] {box.minX() - away, z, away});
                ring.add(new int[] {box.maxX() + away, z, away});
            }
        }

        ring.sort(Comparator.<int[]>comparingDouble(cell -> turn(towards,
                        Math.atan2(cell[1] - box.centerZ(), cell[0] - box.centerX())))
                .thenComparingInt(cell -> cell[2]));

        for (int[] cell : ring) {
            Optional<BlockPos> ground = groundAt(world, cell[0], cell[1], colony.center().y());

            if (ground.isPresent() && fits(world, colony, ground.get(), grove)) {
                return ground;
            }
        }

        return Optional.empty();
    }

    /** Não atrapalha: lugar de viveiro, longe de casa, obra, rua e das outras mudas. */
    private static boolean fits(ServerWorld world, Colony colony, BlockPos ground, List<BlockPos> grove) {
        if (!TreeNursery.isSpotForANursery(world, ground)) {
            return false;
        }

        for (BlockPos other : grove) {
            if (other.getSquaredDistance(ground) < SPACING * SPACING) {
                return false;
            }
        }

        ColonyPos at = MinecraftTypeAdapter.toColonyPos(ground);

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (near(building.min(), building.max(), at, CLEARANCE)) {
                return false;
            }
        }

        if (VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .map(project -> Building.of(project))
                .filter(site -> near(site.min(), site.max(), at, CLEARANCE))
                .isPresent()) {
            return false;
        }

        OptionalInt road = RoadIndex.roadsOf(colony.id())
                .map(roads -> roads.blocksToTheNearestRoad(at))
                .orElseGet(OptionalInt::empty);

        return road.isPresent() ? road.getAsInt() >= ROAD_CLEARANCE : !pathNear(world, ground);
    }

    private static boolean near(ColonyPos min, ColonyPos max, ColonyPos at, int margin) {
        return at.x() >= min.x() - margin && at.x() <= max.x() + margin
                && at.z() >= min.z() - margin && at.z() <= max.z() + margin;
    }

    /** Sem índice de ruas: chão de caminho a menos de {@link #ROAD_CLEARANCE}. */
    private static boolean pathNear(ServerWorld world, BlockPos ground) {
        for (int dx = -ROAD_CLEARANCE + 1; dx < ROAD_CLEARANCE; dx++) {
            for (int dz = -ROAD_CLEARANCE + 1; dz < ROAD_CLEARANCE; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (world.getBlockState(ground.add(dx, dy, dz)).isOf(Blocks.DIRT_PATH)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** As mudas do bosque que já estão de pé no anel (depois de reiniciar o servidor). */
    private static List<BlockPos> found(ServerWorld world, VillageBounds box, int centreY) {
        List<BlockPos> found = new ArrayList<>();

        for (int x = box.minX() - FAR; x <= box.maxX() + FAR && found.size() < SIZE; x++) {
            for (int z = box.minZ() - FAR; z <= box.maxZ() + FAR && found.size() < SIZE; z++) {
                if (box.containsColumn(x, z, NEAR - 1)) {
                    continue;
                }

                groundAt(world, x, z, centreY)
                        .filter(ground -> alive(world, ground))
                        .ifPresent(found::add);
            }
        }

        return found;
    }

    /** Terra de viveiro com muda ou tronco em cima. */
    private static boolean alive(ServerWorld world, BlockPos ground) {
        return world.getChunkManager().getWorldChunk(ground.getX() >> 4, ground.getZ() >> 4) != null
                && world.getBlockState(ground).isOf(TreeNursery.BED)
                && (world.getBlockState(ground.up()).isIn(BlockTags.SAPLINGS)
                        || world.getBlockState(ground.up()).isIn(BlockTags.LOGS));
    }

    /** O chão nesta coluna, perto da altura da vila; chunk só se carregado. */
    private static Optional<BlockPos> groundAt(ServerWorld world, int x, int z, int centreY) {
        if (world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) {
            return Optional.empty();
        }

        for (int y = centreY + 4; y >= centreY - 8; y--) {
            BlockPos at = new BlockPos(x, y, z);

            if (!world.getBlockState(at).isAir() && world.getBlockState(at.up()).isAir()) {
                return Optional.of(at);
            }
        }

        return Optional.empty();
    }

    /** O ângulo do baú do lenhador a partir do centro da vila; zero sem ele. */
    private static double towardsTheLumberjack(UUID colonyId, VillageBounds box) {
        for (var worker : VillageColonyMod.WORKERS.ofColony(colonyId)) {
            if (worker.profession()
                    .filter(com.villagecolony.core.worker.model.ProfessionType.LUMBERJACK::equals).isEmpty()) {
                continue;
            }

            var chest = VillageColonyMod.STORAGES.of(worker.villagerId());

            if (chest.isPresent()) {
                return Math.atan2(chest.get().chestPosition().z() - box.centerZ(),
                        chest.get().chestPosition().x() - box.centerX());
            }
        }

        return 0.0;
    }

    /** Quanto girar de um ângulo ao outro, de 0 a π. */
    private static double turn(double from, double to) {
        double difference = Math.abs(from - to) % (2 * Math.PI);

        return difference > Math.PI ? 2 * Math.PI - difference : difference;
    }

    public static void clearAll() {
        GROVES.clear();
        CHECKED.clear();
    }
}
