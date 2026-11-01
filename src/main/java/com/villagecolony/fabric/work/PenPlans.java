package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.math.Box;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * O curral novo — Regra 72: com pastor na vila, um cercado por espécie com par adulto;
 * cercados prontos contam para a meta. O curral único do mod
 * ({@code animal_pen}) é a próxima obra, como o armazém de {@link StoragePlans}. Com
 * {@link #CROWDED_POST} animais amarrados num mesmo poste ({@link #crowdedPost}) o curral também é a
 * próxima obra, com prioridade, e o pastor leva os do poste para ele um a um (Regra 130, autor 2026-10-27).
 */
final class PenPlans {

    /** O nome do curral no catálogo do jogo, em todos os estilos. */
    static final String TYPE = "animal_pen";
    static final ResourceId MODEL = new ResourceId("villagecolony", "colony/animal_pen");

    /** Quantos animais num mesmo poste pedem um curral (autor, 2026-10-27). */
    static final int CROWDED_POST = 5;

    private PenPlans() {
    }

    /**
     * As plantas do curral, quando a vila precisa de um: tem pastor, falta cercado por espécie e o
     * curral não foi a última obra tentada — largado ou recém-acabado, não volta na vez seguinte (E48), e
     * o recém-acabado ainda não entrou na releitura dos portões.
     */
    static List<Blueprint> forColony(ServerWorld world, Colony colony, List<Building> buildings) {
        if (!buildings.isEmpty() && isPen(buildings.get(buildings.size() - 1).blueprint())) {
            return List.of();
        }

        if (!ShepherdFlock.hasShepherd(colony)
                || ShepherdPens.pensOf(world, colony).size() >= neededPens(world, colony)) {
            return List.of();
        }

        if (PlanRefusals.skip(world, colony.id(), colony.center(), MODEL)) {
            return List.of();
        }
        return HousePlans.READ.computeIfAbsent(MODEL, missing -> StructureBlueprintReader.read(world, missing))
                .map(List::of).orElseGet(List::of);
    }

    /** Um cercado para cada espécie com ao menos um par adulto na vila (Regra 72). */
    static int neededPens(ServerWorld world, Colony colony) {
        Box area = colony.bounds()
                .map(box -> new Box(box.minX() - 16, box.minY() - 8, box.minZ() - 16,
                        box.maxX() + 17, box.maxY() + 8, box.maxZ() + 17))
                .orElseGet(() -> new Box(com.villagecolony.fabric.adapter.MinecraftTypeAdapter
                        .toBlockPos(colony.center())).expand(48));
        int species = 0;
        for (var type : ShepherdFlock.SPECIES) {
            List<? extends AnimalEntity> adults = world.getEntitiesByType(type, area,
                    animal -> animal.isAlive() && !animal.isBaby());
            if (adults.size() >= 2) {
                species++;
            }
        }
        return species;
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
