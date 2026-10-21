package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.Side;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;
import java.util.UUID;

/**
 * A rua que entra na faixa anexada — Regra 56 D. O lote só nasce encostado em rua, e no
 * playtest da 0.3.40 a vila anexou 16 blocos a leste, não havia ponta de rua virada
 * para lá, a rua cresceu para oeste e a faixa ficou vazia.
 *
 * <p>Na faixa de {@link #BAND} blocos por dentro do lado anexado, a vila acha a rua mais
 * avançada para aquele lado (a ponta virada para ele primeiro) e começa ali um trecho
 * na direção da faixa ({@link RoadExtension#startGrowing}); o planejador o calça a
 * cada passagem, como qualquer trecho, e o lote nasce na beira dele.
 */
public final class StripRoad {

    /** Até quantos blocos por dentro do lado anexado se procura a rua. */
    static final int BAND = 24;

    private StripRoad() {
    }

    /**
     * Começa a rua para dentro da faixa.
     *
     * @param box a caixa de antes do anexo
     * @return de onde a rua parte
     */
    public static Optional<BlockPos> lead(ServerWorld world, UUID colonyId, VillageBounds box, Side side) {
        Direction towards = Direction.fromVector(side.offsetX(), 0, side.offsetZ());
        Optional<BlockPos> start = find(world, colonyId, box, side);

        if (towards == null || start.isEmpty()) {
            VillageColonyMod.LOGGER.info("Colony {} found no road within {} blocks of its {} side to lead into the strip",
                    colonyId.toString().substring(0, 8), BAND, side);

            return Optional.empty();
        }

        RoadExtension.startGrowing(colonyId, start.get(), towards);
        VillageColonyMod.LOGGER.info("Colony {} leads a road {} into the annexed strip from {}",
                colonyId.toString().substring(0, 8), side, start.get().toShortString());

        return start;
    }

    /** A rua de onde a faixa deste lado seria alcançada, sem começar nada. */
    public static Optional<BlockPos> find(ServerWorld world, UUID colonyId, VillageBounds box, Side side) {
        Direction towards = Direction.fromVector(side.offsetX(), 0, side.offsetZ());

        if (towards == null) {
            return Optional.empty();
        }

        BlockPos best = null;
        int bestReach = Integer.MIN_VALUE;
        boolean bestIsEnd = false;
        int y = (box.minY() + box.maxY()) / 2;

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int reach = x * side.offsetX() + z * side.offsetZ();
                int edge = side.offsetX() > 0 ? box.maxX() : side.offsetX() < 0 ? -box.minX()
                        : side.offsetZ() > 0 ? box.maxZ() : -box.minZ();

                if (edge - reach >= BAND || !world.isChunkLoaded(new BlockPos(x, y, z))) {
                    continue;
                }

                // Pela superfície na altura da caixa, como a busca de lote (RoadsideSites.roadGroundAt):
                // a janela do meio da caixa não via as ruas da beira d'água da vila na duna.
                Optional<BlockPos> ground = RoadsideSites.roadGroundAt(world, colonyId, x, z, y);

                if (ground.isEmpty() || !isRoad(world, colonyId, ground.get())) {
                    continue;
                }

                boolean isEnd = RoadPaving.openSideOf(world, colonyId, ground.get()).filter(towards::equals).isPresent();

                if ((isEnd && !bestIsEnd) || (isEnd == bestIsEnd && reach > bestReach)) {
                    best = ground.get();
                    bestReach = reach;
                    bestIsEnd = isEnd;
                }
            }
        }

        return Optional.ofNullable(best);
    }

    private static boolean isRoad(ServerWorld world, UUID colonyId, BlockPos ground) {
        return RoadIndex.isRoadArea(world, colonyId, ground) || VillageRoad.isPaving(world, ground);
    }
}
