package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * O curral grande novo — um por vila, sem alterar os cercados legados. Com
 * {@link #CROWDED_POST} animais amarrados num mesmo poste ({@link #crowdedPost}) o curral também é a
 * próxima obra, com prioridade, e o pastor leva os do poste para ele um a um (Regra 130, autor 2026-10-27).
 */
final class PenPlans {

    /** O nome do curral no catálogo do jogo, em todos os estilos. */
    static final String TYPE = "animal_pen";
    static final ResourceId MODEL = new ResourceId("villagecolony", "colony/large_animal_pen");

    /** Quantos animais num mesmo poste pedem um curral (autor, 2026-10-27). */
    static final int CROWDED_POST = 5;

    private PenPlans() {
    }

    /**
     * A planta do curral grande, somente uma vez por vila. Cercados legados continuam
     * reconhecidos por {@link ShepherdPens}, mas não contam como este modelo novo.
     */
    static List<Blueprint> forColony(ServerWorld world, Colony colony, List<Building> buildings) {
        if (!ShepherdFlock.hasShepherd(colony) || hasLargePen(buildings)) {
            return List.of();
        }

        if (PlanRefusals.skip(world, colony.id(), colony.center(), MODEL)) {
            return List.of();
        }
        return HousePlans.READ.computeIfAbsent(MODEL, missing -> StructureBlueprintReader.read(world, missing))
                .map(List::of).orElseGet(List::of);
    }

    /** Uma obra terminada ou abandonada deste modelo ocupa a única vaga de curral grande. */
    static boolean hasLargePen(List<Building> buildings) {
        return buildings.stream().anyMatch(building -> isLargePen(building.blueprint()));
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

    static boolean isLargePen(ResourceId id) {
        return MODEL.equals(id);
    }
}
