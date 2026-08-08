package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Decide o que é uma vila, a partir de posições de cama.
 *
 * <p>Puro: não conhece Minecraft. Quem lê os POIs do mundo é
 * {@code fabric.integration.VillageScanner}, que converte antes de
 * chamar aqui.
 *
 * <p>Algoritmo e valores vêm da ADR-003.
 */
public final class VillageDetector {

    /** Raio de coleta de camas em torno do gatilho. Nunca infinito. */
    public static final int SEARCH_RADIUS = 64;

    /** Duas camas pertencem ao mesmo cluster até esta distância. */
    public static final int CLUSTER_DISTANCE = 32;

    /** Menos que isto é acampamento, não vila. */
    public static final int MIN_BEDS = 3;

    /** Vila sem população não é vila. */
    public static final int MIN_VILLAGERS = 2;

    /** Colônia existente a esta distância é a mesma vila, não outra. */
    public static final int DUPLICATE_DISTANCE = 64;

    /** Intervalo do ciclo longo de detecção. Ver Performance-Rules.md §4. */
    public static final int CYCLE_TICKS = 600;

    private static final long CLUSTER_DISTANCE_SQUARED =
            (long) CLUSTER_DISTANCE * CLUSTER_DISTANCE;

    /**
     * Agrupa camas por proximidade transitiva.
     *
     * <p>A distância é horizontal: uma cama no sótão não pertence a outra
     * vila só por estar mais alta.
     *
     * <p>Transitivo significa que A-B e B-C põem as três no mesmo cluster,
     * mesmo que A e C estejam além do limite — é assim que uma rua comprida
     * continua sendo uma vila só.
     *
     * @return clusters; a ordem acompanha a das camas recebidas
     */
    public List<List<ColonyPos>> cluster(Collection<ColonyPos> beds) {
        Objects.requireNonNull(beds, "beds");

        List<ColonyPos> remaining = new ArrayList<>(beds);
        Set<ColonyPos> visited = new HashSet<>();
        List<List<ColonyPos>> clusters = new ArrayList<>();

        for (ColonyPos bed : remaining) {
            if (!visited.add(bed)) {
                continue;
            }

            List<ColonyPos> cluster = new ArrayList<>();
            Deque<ColonyPos> queue = new ArrayDeque<>();

            queue.add(bed);
            cluster.add(bed);

            while (!queue.isEmpty()) {
                ColonyPos current = queue.removeFirst();

                for (ColonyPos other : remaining) {
                    if (visited.contains(other)) {
                        continue;
                    }

                    if (current.horizontalDistanceSquared(other) <= CLUSTER_DISTANCE_SQUARED) {
                        visited.add(other);
                        cluster.add(other);
                        queue.add(other);
                    }
                }
            }

            clusters.add(cluster);
        }

        return clusters;
    }

    /**
     * Decide se um cluster é uma vila e onde fica seu centro.
     *
     * @param cluster camas agrupadas
     * @param villagerCount aldeões vivos no raio
     * @param meetingPoint sino do cluster, se houver
     * @param trigger de onde a busca partiu, quando se sabe; sem ele
     *     nenhuma observação se prova completa
     * @return vazio quando o cluster não qualifica — não é erro, apenas
     *     não é vila
     */
    public Optional<VillageCandidate> evaluate(
            List<ColonyPos> cluster,
            int villagerCount,
            Optional<ColonyPos> meetingPoint,
            Optional<ColonyPos> trigger) {

        Objects.requireNonNull(cluster, "cluster");
        Objects.requireNonNull(meetingPoint, "meetingPoint");
        Objects.requireNonNull(trigger, "trigger");

        if (cluster.size() < MIN_BEDS || villagerCount < MIN_VILLAGERS) {
            return Optional.empty();
        }

        ColonyPos center = meetingPoint.orElseGet(() -> averageOf(cluster));

        boolean complete = trigger
                .map(from -> coversWholeCluster(cluster, from))
                .orElse(false);

        return Optional.of(new VillageCandidate(center, cluster.size(), complete));
    }

    /**
     * Se a busca feita a partir de {@code trigger} não pode ter cortado
     * cama alguma deste cluster.
     *
     * <p>A prova é geométrica. Toda cama de um cluster está a no máximo
     * {@link #CLUSTER_DISTANCE} de outra cama dele — é a definição de
     * cluster. Logo, se toda cama vista está a até
     * {@code SEARCH_RADIUS - CLUSTER_DISTANCE} do gatilho, qualquer cama
     * ligada a elas ainda cairia dentro de {@link #SEARCH_RADIUS} e
     * teria sido coletada. Nada ficou de fora.
     *
     * <p>Fora dessa margem a resposta é "não sei", e o seguro é dizer
     * que não. Uma vila grande observada da beirada é o caso comum, e é
     * exatamente ela que não pode encolher a colônia.
     *
     * <p>Mede na horizontal, como a clusterização. Uma cama muito acima
     * ou abaixo das outras cabe no cluster e poderia cair fora da esfera
     * de busca — é o limite conhecido desta prova, e vale o risco: vila
     * Vanilla é de superfície, e o erro possível é a colônia deixar de
     * encolher, não encolher errado.
     */
    private static boolean coversWholeCluster(List<ColonyPos> cluster, ColonyPos trigger) {
        int margin = SEARCH_RADIUS - CLUSTER_DISTANCE;
        long marginSquared = (long) margin * margin;

        for (ColonyPos bed : cluster) {
            if (bed.horizontalDistanceSquared(trigger) > marginSquared) {
                return false;
            }
        }

        return true;
    }

    /**
     * Média das posições das camas.
     *
     * <p>Soma em long: 64 camas em coordenada extrema estouram int.
     */
    private static ColonyPos averageOf(List<ColonyPos> cluster) {
        long x = 0;
        long y = 0;
        long z = 0;

        for (ColonyPos bed : cluster) {
            x += bed.x();
            y += bed.y();
            z += bed.z();
        }

        int size = cluster.size();

        return new ColonyPos(
                (int) (x / size),
                (int) (y / size),
                (int) (z / size));
    }
}
