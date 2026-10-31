package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.ColonyModels;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * O armazém da colônia — ADR-036 item 9: sem baú livre na vila para aliviar o baú de
 * profissão lotado, ele é a próxima obra. Regra 165: o galpão {@code storage_small} primeiro;
 * o {@code storage_majest} só depois de um galpão terminado.
 */
final class StoragePlans {

    /** O modelo do armazém, em {@code colony/}. */
    static final String MODEL = "storage_majest";

    /** O galpão de 7×7, gerado por {@code scripts/gen_storage_small.py} — Regra 165. */
    static final String SMALL_MODEL = "storage_small";

    private StoragePlans() {
    }

    /**
     * As plantas do armazém, quando o catálogo deste estilo o tem e ele não foi
     * a última obra tentada — largado, não volta na vez seguinte (E48).
     */
    static List<Blueprint> forColony(ServerWorld world, Colony colony, List<Building> buildings) {
        if (!buildings.isEmpty() && isStorage(buildings.get(buildings.size() - 1).blueprint())) {
            return List.of();
        }

        String style = HousePlans.paletteOf(world, colony.center()).style();
        List<Blueprint> plans = new ArrayList<>();
        // Regra 165: o pequeno primeiro; o grande só com um pequeno terminado de pé.
        boolean smallStands = buildings.stream()
                .anyMatch(building -> building.finished() && isSmallStorage(building.blueprint()));

        for (ResourceId id : VillageStructures.buildableFor(style)) {
            if (!isStorage(id) || (!smallStands && !isSmallStorage(id))
                    || PlanRefusals.skip(world, colony.id(), colony.center(), id)) {
                continue;
            }

            HousePlans.READ.computeIfAbsent(id, missing -> StructureBlueprintReader.read(world, missing))
                    .ifPresent(plans::add);
        }

        // Com o pequeno de pé, o grande vem à frente; o pequeno fica para onde o grande não cabe.
        plans.sort(java.util.Comparator.comparing(plan -> isSmallStorage(plan.id())));
        return plans;
    }

    static boolean isStorage(ResourceId id) {
        return ColonyModels.isColonyModel(id)
                && (id.path().endsWith("/" + MODEL) || id.path().endsWith("/" + SMALL_MODEL));
    }

    /** Se é o armazém pequeno, {@code storage_small} — Regra 165. */
    static boolean isSmallStorage(ResourceId id) {
        return ColonyModels.isColonyModel(id) && id.path().endsWith("/" + SMALL_MODEL);
    }
}
