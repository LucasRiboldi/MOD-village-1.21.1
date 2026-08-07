package com.villagecolony.core.resource.service;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.resource.model.ResourceType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * O que falta para a colônia atingir suas metas de estoque.
 *
 * <p>Resource-System.md §"Déficit": há déficit quando a quantidade atual
 * é menor que a necessária, e ele é a diferença.
 *
 * <p>Só compara. Não cria tarefa, não escolhe quem vai buscar e não
 * decide prioridade — Resource-System.md §"Geração de Tarefas" é da
 * Fase 7, e Profession-System.md §"Regras de Arquitetura" mantém a
 * decisão de prioridade na colônia.
 *
 * <p>Lógica pura, sem estado. A meta e o estoque chegam por parâmetro
 * porque nenhum dos dois pertence a esta classe: a meta é da colônia e
 * muda com o que ela pretende construir; o estoque é uma leitura dos
 * baús, que envelhece.
 */
public final class ResourceDemand {

    private ResourceDemand() {
    }

    /**
     * Quanto falta de cada recurso.
     *
     * <p>Só entram os que faltam. Recurso em dia não aparece com zero:
     * um mapa de déficits que lista o que não falta obriga todo chamador
     * a filtrar, e o primeiro que esquecer vai gerar trabalho para
     * buscar nada.
     *
     * <p>Sobra não vira déficit negativo. Ter 100 tábuas com meta de 64
     * significa déficit zero, não "-36": o excedente é outra pergunta, e
     * misturá-lo aqui faria uma soma de déficits cancelar falta com
     * sobra de recursos diferentes.
     *
     * @param goal quanto a colônia quer ter de cada recurso
     * @param owned quanto ela tem, tipicamente o total de
     *     {@code ColonyResources}
     */
    public static Map<ResourceType, Integer> deficit(
            Map<ResourceType, Integer> goal, ResourceTally owned) {

        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(owned, "owned");

        Map<ResourceType, Integer> missing = new EnumMap<>(ResourceType.class);

        for (Map.Entry<ResourceType, Integer> entry : goal.entrySet()) {
            ResourceType type = Objects.requireNonNull(entry.getKey(), "resource type");

            int wanted = entry.getValue() == null ? 0 : entry.getValue();

            if (wanted < 0) {
                throw new IllegalArgumentException(
                        "Negative goal for " + type + ": " + wanted);
            }

            int lacking = wanted - owned.amountOf(type);

            if (lacking > 0) {
                missing.put(type, lacking);
            }
        }

        return Collections.unmodifiableMap(missing);
    }

    /** Quanto falta de um recurso só. Zero quando a meta está atingida. */
    public static int deficitOf(ResourceType type, int goal, ResourceTally owned) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(owned, "owned");

        if (goal < 0) {
            throw new IllegalArgumentException("Negative goal for " + type + ": " + goal);
        }

        return Math.max(0, goal - owned.amountOf(type));
    }

    /** Se a colônia já tem tudo o que queria. */
    public static boolean isSatisfied(Map<ResourceType, Integer> goal, ResourceTally owned) {
        return deficit(goal, owned).isEmpty();
    }
}
