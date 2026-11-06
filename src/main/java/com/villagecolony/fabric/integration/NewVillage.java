package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

/**
 * Só região nova — plano mestre do MAE §0.2 e escolha do autor de 2026-11-05: as capacidades físicas novas (terra sob
 * caminho, ponte, ligação da mina, limpeza ampliada de árvores) só valem em vila gerada com a política atual
 * ({@link VillageOrigin#isCurrent}); vila antiga mantém o comportamento anterior.
 */
public final class NewVillage {

    private NewVillage() {
    }

    /** Se a colônia nasceu de uma vila gerada com a política atual. */
    public static boolean is(ServerWorld world, UUID colonyId) {
        return VillageColonyMod.COLONIES.find(colonyId).map(colony -> is(world, colony)).orElse(false);
    }

    public static boolean is(ServerWorld world, Colony colony) {
        return GeneratedVillages.villagesReaching(world, colony.center().x() >> 4, colony.center().z() >> 4).stream()
                .anyMatch(VillageOrigin::isCurrent);
    }
}
