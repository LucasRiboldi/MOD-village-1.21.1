package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Leitura dos animais vivos nos currais carregados da vila (Regras 167 e 238). */
record PenFlock(List<Stock> pens) {

    record Stock(ShepherdPens.Pen pen, List<AnimalEntity> animals) {
        List<AnimalEntity> adults(EntityType<?> species) {
            return animals.stream().filter(animal -> animal.getType() == species && !animal.isBaby()).toList();
        }

        long young(EntityType<?> species) {
            return animals.stream().filter(animal -> animal.getType() == species && animal.isBaby()).count();
        }
    }

    static PenFlock read(ServerWorld world, Colony colony) {
        return read(world, ShepherdPens.pensOf(world, colony));
    }

    static PenFlock read(ServerWorld world, List<ShepherdPens.Pen> pens) {
        Set<UUID> counted = new HashSet<>();
        return new PenFlock(pens.stream().map(pen -> new Stock(pen,
                world.getEntitiesByClass(AnimalEntity.class, ShepherdPens.areaOf(pen),
                        animal -> animal.isAlive() && pen.reach().contains(animal.getBlockPos()))
                        .stream().filter(animal -> counted.add(animal.getUuid())).toList())).toList());
    }

    long young(EntityType<?> species) {
        return pens.stream().mapToLong(stock -> stock.young(species)).sum();
    }

    List<AnimalEntity> adults(EntityType<?> species) {
        return pens.stream().flatMap(stock -> stock.adults(species).stream()).toList();
    }
}
