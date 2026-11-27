package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra 392 (H5, item 6, autor 27-11: "faça todas as casas idênticas à do mineiro, só coloque um quadro na frente em cima da
 * porta indicando a profissão"): cada ofício do mod, fora o mineiro e o construtor, tem a casa {@code majest_<ofício>}, cópia
 * da casa do mineiro, que entra na vaga da casa de ofício (Regra 49).
 */
class TradeHouseModelsTest {

    private static final List<ProfessionType> WITH_HOUSE = List.of(ProfessionType.LUMBERJACK, ProfessionType.MASON,
            ProfessionType.SMELTER, ProfessionType.CARPENTER, ProfessionType.FARMER, ProfessionType.SHEPHERD,
            ProfessionType.GROUNDSKEEPER);

    @Test
    @DisplayName("Regra 392: toda casa majest_ entra na vaga da casa de ofício, em todo estilo")
    void everyTradeHouseIsBuildable() {
        for (String style : List.of("plains", "desert", "savanna", "snowy", "taiga")) {
            List<ResourceId> houses = ColonyModels.tradeHousesFor(style);

            for (ProfessionType profession : WITH_HOUSE) {
                String name = "majest_" + profession.name().toLowerCase(java.util.Locale.ROOT);

                assertTrue(houses.stream().anyMatch(id -> id.path().endsWith("/" + name)),
                        "Regra 392: a casa " + name + " não entrou no estilo " + style + ": " + houses);
            }
        }
    }

    @Test
    @DisplayName("Regra 392: a casa majest_ diz de que ofício é")
    void theTradeHouseKnowsItsProfession() {
        assertEquals(Optional.of(ProfessionType.CARPENTER),
                ColonyModels.professionOf(new ResourceId("villagecolony", "colony/plains/majest_carpenter")));
        assertEquals(Optional.of(ProfessionType.GROUNDSKEEPER),
                ColonyModels.professionOf(new ResourceId("villagecolony", "colony/majest_groundskeeper")));
    }
}
