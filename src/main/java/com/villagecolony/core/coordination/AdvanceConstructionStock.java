package com.villagecolony.core.coordination;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Mantém a reserva antecipada dos três ofícios que produzem blocos de obra. */
final class AdvanceConstructionStock {

    private AdvanceConstructionStock() {
    }

    static void addTo(
            Map<ResourceType, Integer> goals,
            UUID colonyId,
            ResourceType stone,
            ResourceTally owned) {

        AdvanceStock.addTo(goals, colonyId,
                AdvanceStock.piecesFor(colonyId, "mason", MasonStock.piecesFor(stone)), owned::amountOf);

        for (String lane : List.of("smelter", "carpenter")) {
            AdvanceStock.addTo(goals, colonyId, lane, AdvanceStock.piecesFor(colonyId, lane,
                    lane.equals("smelter") ? AdvanceStock.SMELTER_PIECES : AdvanceStock.CARPENTER_PIECES),
                    owned::amountOf);
        }
    }
}
