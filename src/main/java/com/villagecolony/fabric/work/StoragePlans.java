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
 * O armazém da colônia, {@code storage_majest} — ADR-036 item 9: sem baú livre
 * na vila para aliviar o baú de profissão lotado, ele é a próxima obra.
 */
final class StoragePlans {

    /** O modelo do armazém, em {@code colony/}. */
    static final String MODEL = "storage_majest";

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

        for (ResourceId id : VillageStructures.buildableFor(style)) {
            if (!isStorage(id) || PlanRefusals.skip(world, colony.id(), colony.center(), id)) {
                continue;
            }

            HousePlans.READ.computeIfAbsent(id, missing -> StructureBlueprintReader.read(world, missing))
                    .ifPresent(plans::add);
        }

        return plans;
    }

    static boolean isStorage(ResourceId id) {
        return ColonyModels.isColonyModel(id) && id.path().endsWith("/" + MODEL);
    }
}
