package com.villagecolony.core.coordination;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceType;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * As metas da fornalha — o que se funde e o ferro —, a última parte de
 * {@link ColonyGoals#of}. Separada em 2026-10-02 pelo teto de 500 linhas; o
 * texto e a ordem das entradas vieram sem mudança, e a ordem importa: as
 * metas são um {@code LinkedHashMap}, e quem já entrou antes (a areia do
 * vidro, a pedra da obra) decide se o produto tem cadeia.
 */
final class FurnaceGoals {

    private FurnaceGoals() {
    }

    static void addTo(Map<ResourceType, Integer> goals, ResourceTally owned, WorkDemand work) {

        for (ResourceType made : StockRules.everythingTheFurnaceMakes()) {
            int asked = work.smelted().getOrDefault(made, 0);
            boolean chained = StockRules.rawOf(made)
                    .map(raw -> goals.containsKey(raw) || owned.amountOf(raw) > 0)
                    .orElse(true);

            if (chained) {
                goals.put(made, Math.max(ColonyGoals.SMELTED_FLOOR, asked));
            } else if (asked > 0) {
                goals.put(made, asked);
            }
        }

        // E o que a obra pede e a fornalha NÃO faz — um material cuja
        // produção declarada mudou, ou que chegou por datapack — continua
        // entrando pela lista da obra. O laço acima cobre o catálogo do
        // jogo; esta linha cobre o que a obra sabe e ele não.
        work.smelted().forEach((made, amount) -> {
            if (amount > 0 && !goals.containsKey(made)) {
                goals.put(made, amount);
            }
        });

        // O ferro do lampião — 2026-08-21, e com o passo do meio de volta:
        // o lingote é da fornalha, e o cru é da mina. Sem as duas metas o
        // fundidor sabia fundir ferro e nenhuma tarefa lhe chegava.
        if (work.iron() > 0) {
            goals.put(ResourceType.IRON_INGOT, work.iron());

            int ingotsMissing = work.iron() - owned.amountOf(ResourceType.IRON_INGOT);

            goals.put(ResourceType.RAW_IRON, ColonyGoals.MINERAL_FLOOR + Math.max(0, ingotsMissing));
        } else {
            goals.put(ResourceType.RAW_IRON, ColonyGoals.MINERAL_FLOOR);
        }
    }
}
