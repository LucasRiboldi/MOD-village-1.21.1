package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ResourceType;

import java.util.Map;
import java.util.Objects;

/**
 * Quanto a colônia quer ter de cada recurso.
 *
 * <p>Provisório e assumido como tal. Resource-System.md
 * §"Necessidade de Recursos" fala em metas mínimas e dá um exemplo, mas
 * nada define de onde elas vêm. A resposta real é a Fase 9: a meta sai
 * do que a expansão pretende construir, e até existir expansão não há
 * de onde derivá-la.
 *
 * <p>Enquanto isso, um número fixo — o bastante para o ciclo da ADR-002
 * ter o que perguntar e para o primeiro trabalhador da Fase 8 ter o que
 * fazer. Trocar isto por metas de obra é uma mudança nesta classe e em
 * mais nada: {@code ColonyCycle} já recebe a meta pronta.
 *
 * <p>Recebe a colônia por parâmetro, e não porque a usa hoje: a meta
 * por colônia é o que virá, e a assinatura já é a definitiva.
 */
public final class ColonyGoals {

    /**
     * A meta do MVP, igual para toda colônia.
     *
     * <p>Uma pilha de madeira bruta e meia de pedra. Não tem tábua: ela
     * é {@code PROCESSED} e viraria tarefa de fabricação, que ninguém
     * executa antes da Fase 8.
     */
    private static final Map<ResourceType, Integer> MVP_GOAL = Map.of(
            ResourceType.OAK_LOG, 64,
            ResourceType.COBBLESTONE, 32);

    private ColonyGoals() {
    }

    public static Map<ResourceType, Integer> of(Colony colony) {
        Objects.requireNonNull(colony, "colony");

        return MVP_GOAL;
    }
}
