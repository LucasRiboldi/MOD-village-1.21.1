package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.coordination.PlanningBudget;
import com.villagecolony.core.coordination.VillageExpansion;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner.Footprint;
import com.villagecolony.fabric.integration.BuildSiteScanner.Site;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O lote dentro da faixa anexada, com o caminho até a rua — decisão do autor de 2026-10-22 (A5), que
 * emenda a Regra 56 D ("o lote só nasce encostado em rua").
 *
 * <p>No deserto (0.3.86) a vila anexou uma faixa ao sul com 423 de 440 colunas planas, e a rua que
 * devia entrar nela não andou um bloco: a duna subia dois na entrada. Sem rua, a faixa não dava lote,
 * e a vila ficou sem obra. Agora, com a faixa pendente, a vila procura o lote dentro dela e um caminho
 * a pé até a rua que já existe:
 *
 * <ul>
 *   <li>o caminho <b>segue o terreno</b>, com degrau de no máximo um ({@link RoadPaving#groundNear}),
 *       sem cortar nem aterrar, e cada coluna passa pelas mesmas recusas da rua
 *       ({@link RoadPaving#refusalAt}); contorna a duna quando há volta;</li>
 *   <li>tem até {@link #MAX_PATH} blocos, e é calçado na abertura da obra (o {@code path} do
 *       {@link Site}, como o recuo da Regra 62);</li>
 *   <li>o lote nasce <b>o mais fundo na faixa</b> que couber nela.</li>
 * </ul>
 *
 * <p>A busca é fatiada como a varredura de lote: {@link PlanningBudget#DEADLINE_MS} por chamada, com
 * cursor por colônia.
 */
public final class StripLot {

    static {
        ServerMemory.register(StripLot.class, StripLot::clearAll);
    }

    /** O caminho mais longo até a rua: o alcance da rede de caminhos além da caixa (Regra 64). */
    public static final int MAX_PATH = 32;

    /** De quantos em quantos blocos a porta é testada, na frente e ao longo da faixa. */
    static final int STEP = 2;

    /** Onde a busca parou, por colônia: a faixa medida e a próxima porta. */
    private record Cursor(VillageBounds strip, Side side, int next) {
    }

    private static final Map<UUID, Cursor> CURSORS = new HashMap<>();

    private StripLot() {
    }

    /**
     * O lote mais fundo da faixa anexada que tem caminho até a rua.
     *
     * @param box a caixa da vila de agora
     * @return o lote, com o caminho a calçar; vazio quando nenhum serviu nesta passada (a seguinte continua)
     */
    public static Optional<Site> find(
            ServerWorld world, UUID colonyId, VillageBounds box, Side side, List<Footprint> footprints) {

        VillageBounds strip = VillageExpansion.stripOf(box, side);
        Direction out = Direction.fromVector(side.offsetX(), 0, side.offsetZ());

        if (out == null) {
            return Optional.empty();
        }

        List<BlockPos> doors = doorColumns(strip, side);
        Cursor cursor = CURSORS.get(colonyId);
        int next = cursor != null && cursor.strip().equals(strip) && cursor.side() == side ? cursor.next() : 0;
        long deadline = System.nanoTime() + PlanningBudget.DEADLINE_MS * 1_000_000L;

        for (; next < doors.size(); next++) {
            if (System.nanoTime() > deadline) {
                CURSORS.put(colonyId, new Cursor(strip, side, next));

                return Optional.empty();
            }

            Optional<Site> site = siteAt(world, colonyId, doors.get(next), out, strip, footprints);

            if (site.isPresent()) {
                CURSORS.remove(colonyId);

                return site;
            }
        }

        // Volta inteira sem lote: a próxima passada recomeça do fundo.
        CURSORS.remove(colonyId);

        return Optional.empty();
    }

    /** As colunas da porta, do fundo da faixa para a frente, e ao longo dela. */
    static List<BlockPos> doorColumns(VillageBounds strip, Side side) {
        boolean alongX = side.offsetX() == 0;
        int depth = alongX ? strip.maxZ() - strip.minZ() + 1 : strip.maxX() - strip.minX() + 1;
        List<BlockPos> doors = new ArrayList<>();

        for (int k = depth - 1; k >= 0; k -= STEP) {
            int lateralFrom = alongX ? strip.minX() : strip.minZ();
            int lateralTo = alongX ? strip.maxX() : strip.maxZ();

            for (int lateral = lateralFrom; lateral <= lateralTo; lateral += STEP) {
                // k blocos para dentro da faixa, a partir da borda da caixa.
                int edge = switch (side) {
                    case EAST -> strip.minX() + k;
                    case WEST -> strip.maxX() - k;
                    case SOUTH -> strip.minZ() + k;
                    case NORTH -> strip.maxZ() - k;
                };

                doors.add(alongX ? new BlockPos(lateral, 0, edge) : new BlockPos(edge, 0, lateral));
            }
        }

        return doors;
    }

    /** O lote com a porta nesta coluna, voltada para a vila, dentro da faixa e com caminho até a rua. */
    private static Optional<Site> siteAt(
            ServerWorld world, UUID colonyId, BlockPos column, Direction out, VillageBounds strip,
            List<Footprint> footprints) {

        Optional<BlockPos> door = groundAt(world, column.getX(), column.getZ());

        if (door.isEmpty() || RoadPaving.refusalAt(world, door.get(), world.getBlockState(door.get())).isPresent()) {
            return Optional.empty();
        }

        int floorY = door.get().getY();

        for (Footprint footprint : footprints) {
            if (footprint.doorSide() != out.getOpposite()) {
                continue;
            }

            ColonyPos size = footprint.size();
            int lotX = column.getX() + out.getOffsetX();
            int lotZ = column.getZ() + out.getOffsetZ();
            int originX = out.getOffsetX() < 0 ? lotX - size.x() + 1 : lotX;
            int originZ = out.getOffsetZ() < 0 ? lotZ - size.z() + 1 : lotZ;
            boolean alongX = out.getOffsetX() == 0;
            // A porta no meio da frente da casa.
            originX -= alongX ? size.x() / 2 : 0;
            originZ -= alongX ? 0 : size.z() / 2;

            if (!inside(strip, originX, originZ, size)) {
                continue;
            }

            Optional<Integer> floor = LotLevel.flatGroundAt(world, colonyId, originX, originZ, floorY, floorY, size);

            if (floor.isEmpty()) {
                continue;
            }

            Optional<List<ColonyPos>> path = pathToTheRoad(world, colonyId, door.get(), originX, originZ, size);

            if (path.isPresent()) {
                VillageColonyMod.LOGGER.info(
                        "Colony {} finds a lot inside the annexed strip at {}, {} — {} blocks of path to the road",
                        colonyId.toString().substring(0, 8), originX, originZ, path.get().size());

                return Optional.of(new Site(
                        new ColonyPos(originX, floor.get(), originZ), out.getOpposite(), size, path.get()));
            }
        }

        return Optional.empty();
    }

    /**
     * O caminho a pé da porta até a rua: busca em largura pelo terreno, degrau de no máximo um, cada
     * coluna calçável e com espaço para passar, fora do lote, até {@link #MAX_PATH} blocos.
     *
     * @return as colunas a calçar, da porta até a última antes da rua; vazio sem caminho
     */
    static Optional<List<ColonyPos>> pathToTheRoad(
            ServerWorld world, UUID colonyId, BlockPos door, int originX, int originZ, ColonyPos size) {

        if (isRoad(world, colonyId, door)) {
            return Optional.of(List.of());
        }

        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        Map<BlockPos, Integer> depth = new HashMap<>();

        frontier.add(door);
        parent.put(door, door);
        depth.put(door, 1);

        while (!frontier.isEmpty()) {
            BlockPos at = frontier.poll();

            if (depth.get(at) >= MAX_PATH) {
                continue;
            }

            for (Direction way : Direction.Type.HORIZONTAL) {
                BlockPos column = at.offset(way);

                if (onTheLot(column, originX, originZ, size)) {
                    continue;
                }

                Optional<BlockPos> ground = RoadPaving.groundNear(world, column, at.getY());

                if (ground.isEmpty() || parent.containsKey(ground.get()) || !mayWalk(world, at, ground.get())) {
                    continue;
                }

                if (isRoad(world, colonyId, ground.get())) {
                    return Optional.of(trace(parent, at));
                }

                BlockState state = world.getBlockState(ground.get());

                if (RoadPaving.refusalAt(world, ground.get(), state).isPresent()) {
                    continue;
                }

                parent.put(ground.get(), at);
                depth.put(ground.get(), depth.get(at) + 1);
                frontier.add(ground.get());
            }
        }

        return Optional.empty();
    }

    /** Cabe o corpo: dois de ar em cima, e um terceiro sobre quem sobe o degrau. */
    private static boolean mayWalk(ServerWorld world, BlockPos from, BlockPos to) {
        return world.getBlockState(to.up(2)).getCollisionShape(world, to.up(2)).isEmpty()
                && (to.getY() <= from.getY()
                        || world.getBlockState(from.up(2)).getCollisionShape(world, from.up(2)).isEmpty());
    }

    private static boolean isRoad(ServerWorld world, UUID colonyId, BlockPos ground) {
        return VillageRoad.isPaving(world, ground) || RoadIndex.isRoadArea(world, colonyId, ground);
    }

    private static List<ColonyPos> trace(Map<BlockPos, BlockPos> parent, BlockPos last) {
        List<ColonyPos> path = new ArrayList<>();
        BlockPos at = last;

        while (true) {
            path.add(MinecraftTypeAdapter.toColonyPos(at));
            BlockPos before = parent.get(at);

            if (before == null || before.equals(at)) {
                break;
            }

            at = before;
        }

        return path;
    }

    /** O chão desta coluna: o topo do terreno, pela mesma leitura do lote. */
    private static Optional<BlockPos> groundAt(ServerWorld world, int x, int z) {
        if (world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) {
            return Optional.empty();
        }

        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

        return LotGround.groundInColumn(world, x, z, top);
    }

    private static boolean inside(VillageBounds strip, int originX, int originZ, ColonyPos size) {
        return strip.containsColumn(originX, originZ)
                && strip.containsColumn(originX + size.x() - 1, originZ + size.z() - 1);
    }

    private static boolean onTheLot(BlockPos column, int originX, int originZ, ColonyPos size) {
        return column.getX() >= originX && column.getX() < originX + size.x()
                && column.getZ() >= originZ && column.getZ() < originZ + size.z();
    }

    public static void clearAll() {
        CURSORS.clear();
    }
}
