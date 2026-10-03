package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A atividade da colônia acompanha jogadores presentes agora, sem memória. */
class VillageFocusTest {

    @Test
    void everyVillageWithAPlayerPresentMayPlanNow() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        List<Colony> active = List.of(
                Colony.create(first, new ColonyPos(0, 64, 0)),
                Colony.create(second, new ColonyPos(128, 64, 0)));

        VillageColonyMod.COLONIES.register(active.get(0));
        VillageColonyMod.COLONIES.register(active.get(1));
        try {
            assertEquals(
                    Set.of(first, second),
                    Set.copyOf(VillageFocus.planners(active, Set.of(first, second))),
                    "uma vila com jogador presente ficou parada por causa da memória de foco");
        } finally {
            VillageColonyMod.COLONIES.remove(first);
            VillageColonyMod.COLONIES.remove(second);
        }
    }
}
