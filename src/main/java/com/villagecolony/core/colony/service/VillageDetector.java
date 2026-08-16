package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.ClusterRejection;
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

    /**
     * Dois centros mais próximos que isto se sobrepõem.
     *
     * <p>ADR-003 §5: o MVP não funde as duas colônias, apenas registra o
     * aviso. Fundir exige nova ADR — e a decisão de 2026-08-12 já disse
     * qual será o critério dela: duas vilas viram uma quando um bloco de
     * uma encostar no bloco da outra, o que depende da construção existir.
     *
     * <p>É o mesmo número de {@link #CLUSTER_DISTANCE}, e não por acaso:
     * abaixo dele as camas de uma seriam agrupadas com as da outra.
     */
    public static final int OVERLAP_DISTANCE = CLUSTER_DISTANCE;

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

        // O aglomerado vazio é barrado aqui e não por rejectionOf, e a
        // diferença é de propósito: "não vi nada" não é o mesmo que "vi e
        // recusei". Recusa marca colônia abandonada; raio sem cama
        // nenhuma não pode marcar nada. Ver ClusterRejection.
        if (cluster.isEmpty() || rejectionOf(cluster, villagerCount).isPresent()) {
            return Optional.empty();
        }

        ColonyPos center = meetingPoint.orElseGet(() -> anchoredCenterOf(cluster));

        boolean complete = trigger
                .map(from -> coversWholeCluster(cluster, from))
                .orElse(false);

        return Optional.of(
                new VillageCandidate(center, cluster.size(), complete, trigger.orElse(null)));
    }

    /**
     * Por que este aglomerado não é uma vila, quando não é.
     *
     * <p>É a mesma validação do Passo 3 da ADR-003 que {@link #evaluate}
     * aplica — e é aqui que ela mora, para que aprovar e recusar não
     * possam divergir. Um dia com duas cópias da regra é o dia em que o
     * mod aceita uma vila e diz que a recusou.
     *
     * <p>A ordem importa no que ela conta: camas de menos vem primeiro
     * porque é a condição mais forte. Um aglomerado de duas camas sem
     * ninguém em volta é acampamento, e chamá-lo de "vila sem população"
     * daria a entender que houve vila ali.
     *
     * <p>Existe porque {@code ColonyState.ABANDONED} não tinha quem o
     * atribuísse: a detecção só sabia dizer o que aprovava, e "a vila
     * acabou" chegava indistinguível de "ninguém olhou". Ver ADR-003 §6 e
     * {@link ClusterRejection}.
     *
     * @return vazio quando o aglomerado qualifica como vila
     */
    public Optional<ClusterRejection> rejectionOf(List<ColonyPos> cluster, int villagerCount) {
        Objects.requireNonNull(cluster, "cluster");

        if (cluster.isEmpty()) {
            // Sem cama não há lugar de que falar, e um centro médio de
            // lista vazia seria uma divisão por zero.
            return Optional.empty();
        }

        ClusterRejection.Reason reason;

        if (cluster.size() < MIN_BEDS) {
            reason = ClusterRejection.Reason.TOO_FEW_BEDS;
        } else if (villagerCount < MIN_VILLAGERS) {
            reason = ClusterRejection.Reason.TOO_FEW_VILLAGERS;
        } else {
            return Optional.empty();
        }

        return Optional.of(new ClusterRejection(
                meanOf(cluster), cluster.size(), villagerCount, reason));
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
     * O centro do aglomerado, sempre numa cama que existe.
     *
     * <p><b>Regra do autor, 2026-08-15:</b> a posição do centro é marcada
     * na horizontal e na vertical dos blocos existentes. Antes disto o
     * centro era a média pura das camas — um ponto calculado que não
     * precisa coincidir com coisa alguma: ele cai no ar entre duas casas,
     * dentro do morro atrás da vila, ou no meio do lago. Nos três casos a
     * colônia passa a medir distância a partir de um lugar onde não há
     * nada, e a âncora que deveria ser estável é a que menos existe.
     *
     * <p>A média continua sendo o alvo — ela é o que descreve onde a vila
     * está. O que muda é que o centro passa a ser a <b>cama mais próxima
     * dela</b>, e cama é bloco: tem horizontal e tem vertical, as duas do
     * mundo real.
     *
     * <p>Isto não substitui o ponto de encontro. Quando há sino, ele
     * continua mandando — e pelo mesmo motivo, aliás: sino também é bloco
     * que existe.
     */
    private static ColonyPos anchoredCenterOf(List<ColonyPos> cluster) {
        return nearestTo(meanOf(cluster), cluster);
    }

    /**
     * A cama do aglomerado mais perto deste ponto.
     *
     * <p>Empate resolvido pela ordem do aglomerado, que é a da varredura
     * de POI e é estável entre ciclos — e estabilidade é o ponto: um
     * centro que trocasse de cama a cada ciclo faria a colônia se ver
     * andando sem que nada tivesse mudado.
     */
    private static ColonyPos nearestTo(ColonyPos target, List<ColonyPos> cluster) {
        ColonyPos nearest = cluster.get(0);
        long best = squaredDistance(nearest, target);

        for (ColonyPos bed : cluster) {
            long distance = squaredDistance(bed, target);

            if (distance < best) {
                nearest = bed;
                best = distance;
            }
        }

        return nearest;
    }

    private static long squaredDistance(ColonyPos from, ColonyPos to) {
        long dx = (long) from.x() - to.x();
        long dy = (long) from.y() - to.y();
        long dz = (long) from.z() - to.z();

        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Média das posições das camas.
     *
     * <p>Soma em long: 64 camas em coordenada extrema estouram int.
     */
    private static ColonyPos meanOf(List<ColonyPos> cluster) {
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
