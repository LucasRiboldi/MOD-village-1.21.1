package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * O curral novo — autor, 2026-10-22: com pastor na vila e todos os currais cheios
 * ({@link ShepherdPens#allFull}, lotação pelo tamanho), o curral do catálogo do estilo
 * ({@code animal_pen}) é a próxima obra, como o armazém de {@link StoragePlans}. Com
 * {@link #CROWDED_POST} animais amarrados num mesmo poste ({@link #crowdedPost}) o curral também é a
 * próxima obra, com prioridade, e o pastor leva os do poste para ele um a um (Regra 130, autor 2026-10-27).
 */
final class PenPlans {

    /** O nome do curral no catálogo do jogo, em todos os estilos. */
    static final String TYPE = "animal_pen";

    /** Quantos animais num mesmo poste pedem um curral (autor, 2026-10-27). */
    static final int CROWDED_POST = 5;

    private PenPlans() {
    }

    /**
     * As plantas do curral, quando a vila precisa de um: tem pastor, todos os currais estão cheios e o
     * curral não foi a última obra tentada — largado ou recém-acabado, não volta na vez seguinte (E48), e
     * o recém-acabado ainda não entrou na releitura dos portões.
     */
    static List<Blueprint> forColony(ServerWorld world, Colony colony, List<Building> buildings) {
        if (!buildings.isEmpty() && isPen(buildings.get(buildings.size() - 1).blueprint())) {
            return List.of();
        }

        if (!ShepherdFlock.hasShepherd(colony)
                || !(ShepherdPens.allFull(world, colony) || crowdedPost(world, colony))) {
            return List.of();
        }

        String style = HousePlans.paletteOf(world, colony.center()).style();
        List<Blueprint> plans = new ArrayList<>();

        for (ResourceId id : VillageStructures.buildableFor(style)) {
            if (!isPen(id) || PlanRefusals.skip(world, colony.id(), colony.center(), id)) {
                continue;
            }

            HousePlans.READ.computeIfAbsent(id, missing -> StructureBlueprintReader.read(world, missing))
                    .ifPresent(plans::add);
        }

        return plans;
    }

    /** Se algum poste da vila segura {@link #CROWDED_POST} animais ou mais. */
    static boolean crowdedPost(ServerWorld world, Colony colony) {
        net.minecraft.util.math.Box area = colony.bounds()
                .map(box -> new net.minecraft.util.math.Box(box.minX() - 16, box.minY() - 8, box.minZ() - 16,
                        box.maxX() + 17, box.maxY() + 8, box.maxZ() + 17))
                .orElseGet(() -> new net.minecraft.util.math.Box(
                        com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toBlockPos(colony.center())).expand(48));

        return world.getEntitiesByClass(net.minecraft.entity.decoration.LeashKnotEntity.class, area, knot -> true)
                .stream()
                .anyMatch(knot -> ShepherdHerding.tiedAt(world, knot.getAttachedBlockPos()) >= CROWDED_POST);
    }

    static boolean isPen(ResourceId id) {
        return id.path().contains(TYPE);
    }
}
