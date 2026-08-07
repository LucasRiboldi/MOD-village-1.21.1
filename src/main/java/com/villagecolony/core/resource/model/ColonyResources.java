package com.villagecolony.core.resource.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * O que uma colônia tem, e em qual baú.
 *
 * <p>É a "visão agregada" de Resource-System.md §"Registro de Recursos":
 * o total de cada recurso e a repartição por baú.
 *
 * <p>Imutável, e por isso não se chama {@code ResourceRegistry} como em
 * MVP-Tasks.md. Registro sugere algo que se mantém e se atualiza; isto é
 * uma leitura datada. O baú muda a cada tampa que o jogador levanta, e
 * um objeto que se dissesse registro estaria prometendo uma atualidade
 * que não tem. Mesmo motivo pelo qual {@code ResourceTally} não guarda
 * quando foi contado: quem lê decide se a leitura ainda serve.
 *
 * <p>A localização importa porque o trabalhador vai ao baú, não ao
 * total. Saber que a colônia tem 64 troncos não diz a ninguém para onde
 * andar.
 */
public final class ColonyResources {

    private static final ColonyResources EMPTY =
            new ColonyResources(new LinkedHashMap<>(), ResourceTally.empty());

    private final Map<ColonyPos, ResourceTally> byChest;

    private final ResourceTally total;

    private ColonyResources(Map<ColonyPos, ResourceTally> byChest, ResourceTally total) {
        this.byChest = Collections.unmodifiableMap(byChest);
        this.total = total;
    }

    /** Colônia sem baú nenhum, ou com todos vazios. */
    public static ColonyResources empty() {
        return EMPTY;
    }

    /**
     * Agrega as leituras dos baús.
     *
     * <p>Baú vazio não entra: ele não ajuda ninguém a decidir para onde
     * ir, e mantê-lo faria "a colônia tem três baús com madeira" contar
     * baús sem madeira.
     *
     * <p>A ordem de iteração do mapa recebido é preservada, para que
     * dois relatórios da mesma colônia saiam iguais.
     */
    public static ColonyResources of(Map<ColonyPos, ResourceTally> byChest) {
        Objects.requireNonNull(byChest, "byChest");

        Map<ColonyPos, ResourceTally> copy = new LinkedHashMap<>();
        ResourceTally total = ResourceTally.empty();

        for (Map.Entry<ColonyPos, ResourceTally> entry : byChest.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "chest position");
            Objects.requireNonNull(entry.getValue(), "tally");

            if (entry.getValue().isEmpty()) {
                continue;
            }

            copy.put(entry.getKey(), entry.getValue());
            total = total.plus(entry.getValue());
        }

        return copy.isEmpty() ? EMPTY : new ColonyResources(copy, total);
    }

    /** Quanto a colônia tem ao todo. */
    public ResourceTally total() {
        return total;
    }

    public int amountOf(ResourceType type) {
        return total.amountOf(type);
    }

    /** A repartição por baú, sem os vazios. Somente leitura. */
    public Map<ColonyPos, ResourceTally> byChest() {
        return byChest;
    }

    /** Onde há um recurso, para quem precisa ir buscá-lo. */
    public Map<ColonyPos, Integer> locationsOf(ResourceType type) {
        Map<ColonyPos, Integer> locations = new LinkedHashMap<>();

        for (Map.Entry<ColonyPos, ResourceTally> entry : byChest.entrySet()) {
            int amount = entry.getValue().amountOf(type);

            if (amount > 0) {
                locations.put(entry.getKey(), amount);
            }
        }

        return Collections.unmodifiableMap(locations);
    }

    public boolean isEmpty() {
        return byChest.isEmpty();
    }

    @Override
    public String toString() {
        return "ColonyResources[total=" + total + ", chests=" + byChest.size() + "]";
    }
}
