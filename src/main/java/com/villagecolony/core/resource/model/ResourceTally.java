package com.villagecolony.core.resource.model;

import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Quanto de cada recurso foi contado.
 *
 * <p>Imutável. Uma contagem é a fotografia de um momento: o baú muda o
 * tempo todo, e um objeto que se atualizasse sozinho não teria como
 * dizer de quando é o número que carrega.
 *
 * <p>Somar duas contagens produz uma terceira — é assim que os baús de
 * uma colônia viram um total. Ver Resource-System.md §"Registro de
 * Recursos".
 */
public final class ResourceTally {

    private static final ResourceTally EMPTY = new ResourceTally(
            new EnumMap<>(ResourceType.class), new LinkedHashMap<>());

    private final Map<ResourceType, Integer> counts;
    private final Map<ResourceId, Integer> idCounts;

    private ResourceTally(
            Map<ResourceType, Integer> counts, Map<ResourceId, Integer> idCounts) {
        this.counts = Collections.unmodifiableMap(counts);
        this.idCounts = Collections.unmodifiableMap(idCounts);
    }

    /** Nada contado. Também é o resultado de um baú vazio. */
    public static ResourceTally empty() {
        return EMPTY;
    }

    /**
     * Uma contagem a partir de um mapa.
     *
     * <p>Zeros e ausências são a mesma coisa e não são guardados: um
     * mapa com {@code OAK_LOG=0} e outro sem a chave dizem o mesmo, e
     * mantê-los distintos faria duas contagens iguais não serem iguais.
     */
    public static ResourceTally of(Map<ResourceType, Integer> counts) {
        Objects.requireNonNull(counts, "counts");

        Map<ResourceType, Integer> copy = new EnumMap<>(ResourceType.class);

        for (Map.Entry<ResourceType, Integer> entry : counts.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "resource type");

            int amount = entry.getValue() == null ? 0 : entry.getValue();

            if (amount < 0) {
                throw new IllegalArgumentException(
                        "Negative amount for " + entry.getKey() + ": " + amount);
            }

            if (amount > 0) {
                copy.put(entry.getKey(), amount);
            }
        }

        Map<ResourceId, Integer> ids = new LinkedHashMap<>();
        copy.forEach((type, amount) -> ids.put(
                ResourceId.vanilla(type.name().toLowerCase(java.util.Locale.ROOT)), amount));
        return copy.isEmpty() ? EMPTY : new ResourceTally(copy, ids);
    }

    /** Contagem tipada acompanhada pelos IDs exatos observados no mundo. */
    public static ResourceTally of(
            Map<ResourceType, Integer> counts, Map<ResourceId, Integer> idCounts) {
        Objects.requireNonNull(counts, "counts");
        Objects.requireNonNull(idCounts, "idCounts");

        ResourceTally typed = of(counts);
        Map<ResourceId, Integer> ids = copyIdCounts(idCounts);
        Map<ResourceType, Integer> typedCounts = new EnumMap<>(ResourceType.class);
        typedCounts.putAll(typed.counts);
        return typed.isEmpty() && ids.isEmpty()
                ? EMPTY
                : new ResourceTally(typedCounts, ids);
    }

    /** Contagem aberta para recursos que ainda não têm um {@link ResourceType}. */
    public static ResourceTally ofIds(Map<ResourceId, Integer> idCounts) {
        Objects.requireNonNull(idCounts, "idCounts");
        Map<ResourceId, Integer> ids = copyIdCounts(idCounts);
        return ids.isEmpty() ? EMPTY : new ResourceTally(
                new EnumMap<>(ResourceType.class), ids);
    }

    private static Map<ResourceId, Integer> copyIdCounts(Map<ResourceId, Integer> source) {
        Map<ResourceId, Integer> copy = new LinkedHashMap<>();
        for (Map.Entry<ResourceId, Integer> entry : source.entrySet()) {
            ResourceId id = Objects.requireNonNull(entry.getKey(), "resource id");
            int amount = entry.getValue() == null ? 0 : entry.getValue();
            if (amount < 0) {
                throw new IllegalArgumentException("Negative amount for " + id + ": " + amount);
            }
            if (amount > 0) {
                copy.put(id, amount);
            }
        }
        return copy;
    }

    /** Zero para o que não foi contado. Ausência é zero, não erro. */
    public int amountOf(ResourceType type) {
        return counts.getOrDefault(type, 0);
    }

    public int amountOf(ResourceId id) {
        return idCounts.getOrDefault(Objects.requireNonNull(id, "id"), 0);
    }

    /**
     * Quanto a colônia tem somando o grupo inteiro.
     *
     * <p>Oito madeiras contam como madeira. Ver {@link ResourceGroup}.
     *
     * <p>{@link ResourceGroup#NONE} não soma nada: um recurso que só se
     * satisfaz com ele mesmo devolve a própria contagem, e é o chamador
     * que decide qual das duas perguntas está fazendo.
     */
    public int amountOfGroup(ResourceGroup group) {
        Objects.requireNonNull(group, "group");

        int total = 0;

        for (Map.Entry<ResourceType, Integer> entry : counts.entrySet()) {
            if (entry.getKey().group() == group) {
                total += entry.getValue();
            }
        }

        return total;
    }

    public boolean has(ResourceType type) {
        return amountOf(type) > 0;
    }

    public boolean isEmpty() {
        return counts.isEmpty() && idCounts.isEmpty();
    }

    /** Somente leitura, e sem os zeros. */
    public Map<ResourceType, Integer> counts() {
        return counts;
    }

    /** Quantidades pela identidade exata, sem agrupar variantes. */
    public Map<ResourceId, Integer> idCounts() {
        return idCounts;
    }

    /**
     * A soma desta contagem com outra.
     *
     * <p>É o que transforma os baús de uma colônia num total.
     */
    public ResourceTally plus(ResourceTally other) {
        Objects.requireNonNull(other, "other");

        if (other.isEmpty()) {
            return this;
        }

        if (isEmpty()) {
            return other;
        }

        Map<ResourceType, Integer> sum = new EnumMap<>(ResourceType.class);
        sum.putAll(counts);
        Map<ResourceId, Integer> idSum = new LinkedHashMap<>(idCounts);

        for (Map.Entry<ResourceType, Integer> entry : other.counts.entrySet()) {
            sum.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        other.idCounts.forEach((id, amount) -> idSum.merge(id, amount, Integer::sum));

        return new ResourceTally(sum, idSum);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ResourceTally tally
                && counts.equals(tally.counts) && idCounts.equals(tally.idCounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(counts, idCounts);
    }

    @Override
    public String toString() {
        return isEmpty() ? "ResourceTally[empty]"
                : "ResourceTally[counts=" + counts + ", ids=" + idCounts + "]";
    }
}
