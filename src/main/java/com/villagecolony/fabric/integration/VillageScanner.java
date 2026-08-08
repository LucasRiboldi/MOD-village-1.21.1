package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lê o mundo e devolve vilas candidatas.
 *
 * <p>Detecta vilas — não confundir com detectar aldeões dentro de uma
 * colônia, que será {@code VillagerScanner}. Ver ADR-003 §7.
 *
 * <p>Nunca varre o mundo: coleta apenas em torno de um ponto de gatilho,
 * dentro de {@link VillageDetector#SEARCH_RADIUS}.
 */
public final class VillageScanner {

    private final VillageDetector detector = new VillageDetector();

    /**
     * Procura vilas em torno de um ponto.
     *
     * <p>Clusters fora de PLAINS são descartados em silêncio: o MVP só
     * suporta esse bioma, e isso não é erro. Ver ADR-003 §5.
     *
     * @param trigger centro da busca — o chunk que acabou de carregar ou
     *     o ponto avaliado no ciclo longo
     */
    public List<VillageCandidate> scan(ServerWorld world, BlockPos trigger) {
        List<ColonyPos> beds = collectBeds(world, trigger);

        if (beds.size() < VillageDetector.MIN_BEDS) {
            return List.of();
        }

        List<VillageCandidate> candidates = new ArrayList<>();

        Optional<ColonyPos> from = Optional.of(MinecraftTypeAdapter.toColonyPos(trigger));

        for (List<ColonyPos> cluster : detector.cluster(beds)) {
            detector.evaluate(
                            cluster,
                            countVillagers(world, cluster),
                            findMeetingPoint(world, cluster),
                            from)
                    .filter(candidate -> isPlains(world, candidate.center()))
                    .ifPresent(candidates::add);
        }

        return candidates;
    }

    /**
     * Camas registradas como POI no raio de busca.
     *
     * <p>{@code ANY} inclui camas livres: uma vila que perdeu aldeões
     * continua sendo uma vila, e é a validação que decide isso.
     */
    private static List<ColonyPos> collectBeds(ServerWorld world, BlockPos trigger) {
        return world.getPointOfInterestStorage()
                .getInCircle(
                        poi -> poi.matchesKey(PointOfInterestTypes.HOME),
                        trigger,
                        VillageDetector.SEARCH_RADIUS,
                        PointOfInterestStorage.OccupationStatus.ANY)
                .map(poi -> MinecraftTypeAdapter.toColonyPos(poi.getPos()))
                .toList();
    }

    /** Sino do cluster, que tem prioridade como centro. Ver ADR-003 §4. */
    private static Optional<ColonyPos> findMeetingPoint(ServerWorld world, List<ColonyPos> cluster) {
        if (cluster.isEmpty()) {
            return Optional.empty();
        }

        BlockPos anyBed = MinecraftTypeAdapter.toBlockPos(cluster.get(0));

        return world.getPointOfInterestStorage()
                .getNearestPosition(
                        poi -> poi.matchesKey(PointOfInterestTypes.MEETING),
                        anyBed,
                        VillageDetector.CLUSTER_DISTANCE,
                        PointOfInterestStorage.OccupationStatus.ANY)
                .map(MinecraftTypeAdapter::toColonyPos);
    }

    /**
     * Aldeões vivos na área do cluster.
     *
     * <p>A caixa é construída a partir das camas e não do mundo inteiro:
     * buscar todos os aldeões do mundo é proibido por
     * Performance-Rules.md §5.
     */
    private static int countVillagers(ServerWorld world, List<ColonyPos> cluster) {
        if (cluster.isEmpty()) {
            return 0;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (ColonyPos bed : cluster) {
            minX = Math.min(minX, bed.x());
            minY = Math.min(minY, bed.y());
            minZ = Math.min(minZ, bed.z());
            maxX = Math.max(maxX, bed.x());
            maxY = Math.max(maxY, bed.y());
            maxZ = Math.max(maxZ, bed.z());
        }

        Box area = new Box(minX, minY, minZ, maxX + 1.0, maxY + 1.0, maxZ + 1.0)
                .expand(VillageDetector.CLUSTER_DISTANCE);

        return world.getEntitiesByClass(VillagerEntity.class, area, VillagerEntity::isAlive).size();
    }

    private static boolean isPlains(ServerWorld world, ColonyPos center) {
        return world.getBiome(MinecraftTypeAdapter.toBlockPos(center))
                .matchesKey(BiomeKeys.PLAINS);
    }
}
