package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.service.ProfessionDemand;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Os animais da vila: toda espécie que o pastor cria (Regra 239), dentro da caixa. É o rebanho que sustenta os pastores — Regra 235
 * (autor, 2026-11-11): uma vaga de pastor a cada {@value ProfessionDemand#ANIMALS_PER_SHEPHERD} animais, e sempre 1.
 */
final class VillageAnimals {

    private VillageAnimals() {
    }

    /** A caixa da vila com folga de 8 em altura; sem caixa, 48 em volta do centro. */
    static Box areaOf(Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());

        return colony.bounds()
                .map(box -> new Box(box.minX(), box.minY() - 8, box.minZ(), box.maxX() + 1, box.maxY() + 8, box.maxZ() + 1))
                .orElseGet(() -> new Box(center).expand(48));
    }

    /** Quantos de cada espécie há na área, na ordem de {@link ShepherdFlock#SPECIES}; a espécie que falta conta 0. */
    static Map<String, Integer> count(ServerWorld world, Box area) {
        Map<String, Integer> animals = new LinkedHashMap<>();

        for (EntityType<?> species : ShepherdFlock.SPECIES) {
            animals.put(EntityType.getId(species).getPath(), world.getEntitiesByType(species, area, any -> true).size());
        }

        return animals;
    }

    /** Grava o teto de pastores pelo rebanho de agora (Regra 235). */
    static void capShepherds(ServerWorld world, Colony colony) {
        int animals = count(world, areaOf(colony)).values().stream().mapToInt(Integer::intValue).sum();

        ProfessionDemand.setCap(colony.id(), ProfessionType.SHEPHERD, ProfessionDemand.shepherdsFor(animals));
    }
}
