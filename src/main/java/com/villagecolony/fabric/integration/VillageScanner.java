package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.ClusterRejection;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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
     * <p>Clusters em bioma onde o jogo não gera vila são descartados em
     * silêncio: o mod não os atende, e isso não é erro. A lista está em
     * {@link VillageBiomes}. Ver ADR-003 §5.
     *
     * @param trigger centro da busca — o chunk que acabou de carregar ou
     *     o ponto avaliado no ciclo longo
     */
    public List<VillageCandidate> scan(ServerWorld world, BlockPos trigger) {
        return scan(world, trigger, false);
    }

    /**
     * @param isProbe se esta varredura é a sonda ancorada no centro de
     *     uma colônia. Só ela marca a âncora dos candidatos, porque só
     *     ela parte do mesmo ponto a cada ciclo e produz leituras
     *     comparáveis entre si. Ver {@code Colony#observe}
     */
    public List<VillageCandidate> scan(ServerWorld world, BlockPos trigger, boolean isProbe) {
        return survey(world, trigger, isProbe).candidates();
    }

    /**
     * Tudo o que a varredura viu — o que aprovou e o que recusou.
     *
     * <p>{@link #scan} devolve só a metade aprovada, que é o que a
     * detecção precisa. Quem precisa da outra metade é a marcação de
     * colônia abandonada: sem ela, "a vila deixou de ser viável" e "a
     * vila não foi observada" chegam iguais. Ver ADR-003 §6 e
     * {@code ColonyAbandonment}.
     */
    public ScanResult survey(ServerWorld world, BlockPos trigger, boolean isProbe) {
        List<ColonyPos> beds = collectBeds(world, trigger);

        if (beds.size() < VillageDetector.MIN_BEDS) {
            // Nem a soma de todas as camas do raio chega ao mínimo: não
            // há aglomerado que possa passar, e contar aldeões seria
            // pagar uma busca por entidades para confirmar o óbvio. A
            // recusa sai daqui mesmo, com o centro das camas que houver
            // — e sem nenhuma cama, sem recusa alguma: um raio vazio não
            // afirma nada sobre vila nenhuma.
            return new ScanResult(
                    List.of(),
                    detector.rejectionOf(beds, ClusterRejection.VILLAGERS_NOT_COUNTED)
                            .map(List::of)
                            .orElseGet(List::of),
                    false);
        }

        List<VillageCandidate> candidates = new ArrayList<>();
        List<ClusterRejection> rejections = new ArrayList<>();
        boolean ignoredByBiome = false;

        Optional<ColonyPos> from = Optional.of(MinecraftTypeAdapter.toColonyPos(trigger));

        for (List<ColonyPos> cluster : detector.cluster(beds)) {
            int villagers = countVillagers(world, cluster);

            Optional<ClusterRejection> rejection = detector.rejectionOf(cluster, villagers);

            if (rejection.isPresent()) {
                rejections.add(rejection.get());

                continue;
            }

            Optional<VillageCandidate> candidate = detector.evaluate(
                    cluster, villagers, findMeetingPoint(world, cluster), from);

            if (candidate.isEmpty()) {
                continue;
            }

            if (!VillageBiomes.hasVillages(world, candidate.get().center())) {
                // Bioma em que o jogo não gera vila é limite do mod, não
                // recusa: a vila está lá, viva, e o mod é que não a
                // atende (ADR-003 §5). A diferença importa porque recusa
                // marca colônia abandonada e isto não pode marcar.
                //
                // A lista deixou de ser só PLAINS em 2026-08-19, com a
                // Regra 20 — ver VillageBiomes.
                ignoredByBiome = true;

                continue;
            }

            candidates.add(isProbe ? candidate.get() : withoutAnchor(candidate.get()));
        }

        return new ScanResult(List.copyOf(candidates), List.copyOf(rejections), ignoredByBiome);
    }

    /**
     * O que uma varredura viu.
     *
     * @param candidates aglomerados aprovados como vila
     * @param rejected aglomerados que não passaram na validação da
     *     ADR-003 §3, com o motivo de cada um
     * @param ignoredByBiome se algum aglomerado passou na validação e foi
     *     descartado por estar em bioma sem vila. Não é recusa, e quem
     *     decide abandono precisa saber a diferença
     */
    public record ScanResult(
            List<VillageCandidate> candidates,
            List<ClusterRejection> rejected,
            boolean ignoredByBiome) {
    }

    /**
     * A mesma observação, sem âncora.
     *
     * <p>A prova de completude continua valendo — ela não depende de
     * repetição. O que se retira é o direito de confirmar uma leitura
     * pela seguinte, que só a sonda tem: o gatilho de chunk e a posição
     * do jogador não voltam ao mesmo ponto de propósito, e um jogador
     * parado na borda da vila repetiria a mesma visão pobre ciclo após
     * ciclo até ela se confirmar.
     */
    private static VillageCandidate withoutAnchor(VillageCandidate candidate) {
        return new VillageCandidate(
                candidate.center(), candidate.bedCount(), candidate.complete());
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


}
