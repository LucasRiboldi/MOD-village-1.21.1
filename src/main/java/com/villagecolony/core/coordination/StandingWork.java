package com.villagecolony.core.coordination;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Trabalho contínuo das profissões que produzem — decisão do autor,
 * 2026-09-26: <i>"todas profissões têm que trabalhar independente do pedido
 * da obra; eles só dão prioridade ao bloco solicitado pela obra"</i>.
 *
 * <p>É a Regra 1 da madeira (o guardado mais o que ainda cabe) estendida a
 * quem ainda parava: o pastor só tosquiava quando a obra pedia cama, e o
 * fazendeiro parava no piso da despensa. O lenhador e o mineiro já seguiam
 * esta regra ({@code ColonyGoals}); o que os parou na sessão longa de 26-09
 * foi a falta de baú.
 *
 * <p>A prioridade da obra não muda: o {@code ColonyCycle} marca como
 * {@code CONSTRUCTION_MATERIAL} os pedidos que a obra faz, e eles vêm antes.
 * Aqui só se garante que, sem obra pedindo, ainda há trabalho.
 */
public final class StandingWork {

    /**
     * Quanto ainda cabe no baú de cada profissão contínua.
     *
     * @param wool lã que cabe nos baús dos pastores
     * @param food colheita que cabe nos baús dos fazendeiros
     */
    public record Rooms(int wool, int food) {

        public Rooms {
            if (wool < 0 || food < 0) {
                throw new IllegalArgumentException("negative room: " + wool + ", " + food);
            }
        }
    }

    private StandingWork() {
    }

    /** As metas com o trabalho contínuo de pastor e fazendeiro por cima. */
    public static Map<ResourceType, Integer> widen(
            Map<ResourceType, Integer> goals, ResourceTally owned, Rooms rooms) {

        Objects.requireNonNull(goals, "goals");
        Objects.requireNonNull(owned, "owned");
        Objects.requireNonNull(rooms, "rooms");

        Map<ResourceType, Integer> widened = new LinkedHashMap<>(goals);

        if (rooms.wool() > 0) {
            widened.merge(ResourceType.WHITE_WOOL,
                    owned.amountOf(ResourceType.WHITE_WOOL) + rooms.wool(), Math::max);
        }

        if (rooms.food() > 0) {
            widened.merge(ResourceType.WHEAT,
                    owned.amountOfGroup(ResourceGroup.CROPS) + rooms.food(), Math::max);
        }

        return Map.copyOf(widened);
    }
}
