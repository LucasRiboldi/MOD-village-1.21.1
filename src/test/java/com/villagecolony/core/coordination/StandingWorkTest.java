package com.villagecolony.core.coordination;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Toda profissão trabalha sem depender da obra — decisão do autor,
 * 2026-09-26: "todas profissões têm que trabalhar independente do pedido da
 * obra; eles só dão prioridade ao bloco solicitado pela obra".
 */
class StandingWorkTest {

    private static final ResourceTally OWNED = ResourceTally.of(Map.of(
            ResourceType.WHITE_WOOL, 58, ResourceType.WHEAT, 9));

    /** O pastor tosquia enquanto couber lã no baú dele, com ou sem cama pedida. */
    @Test
    void theShepherdKeepsShearingWhileItsChestHasRoom() {
        Map<ResourceType, Integer> goals = StandingWork.widen(
                Map.of(ResourceType.OAK_LOG, 100), OWNED, new StandingWork.Rooms(64, 0));

        assertEquals(58 + 64, goals.get(ResourceType.WHITE_WOOL));
        assertEquals(100, goals.get(ResourceType.OAK_LOG), "as outras metas ficam como estavam");
    }

    /** O pedido da obra continua valendo quando é maior. */
    @Test
    void aBiggerWorkRequestStillWins() {
        Map<ResourceType, Integer> goals = StandingWork.widen(
                Map.of(ResourceType.WHITE_WOOL, 500, ResourceType.WHEAT, 64), OWNED,
                new StandingWork.Rooms(10, 10));

        assertEquals(500, goals.get(ResourceType.WHITE_WOOL));
        assertEquals(64, goals.get(ResourceType.WHEAT), "piso da despensa maior que guardado + espaço");
    }

    /** O fazendeiro colhe enquanto couber no baú dele, além do piso da despensa. */
    @Test
    void theFarmerKeepsHarvestingWhileItsChestHasRoom() {
        Map<ResourceType, Integer> goals = StandingWork.widen(
                Map.of(ResourceType.WHEAT, 64), OWNED, new StandingWork.Rooms(0, 200));

        assertEquals(9 + 200, goals.get(ResourceType.WHEAT));
    }

    /** Sem baú (espaço zero), nada muda: a meta não cresce para quem não tem onde guardar. */
    @Test
    void noRoomChangesNothing() {
        Map<ResourceType, Integer> before = Map.of(ResourceType.WHEAT, 64);

        Map<ResourceType, Integer> goals = StandingWork.widen(before, OWNED, new StandingWork.Rooms(0, 0));

        assertEquals(before, goals);
        assertFalse(goals.containsKey(ResourceType.WHITE_WOOL));
    }
}
