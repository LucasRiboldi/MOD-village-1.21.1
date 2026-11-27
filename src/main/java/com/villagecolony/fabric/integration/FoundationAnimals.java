package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * Os animais com que a vila nasce — Regra 380 (autor 26-11, notas do jogo da 0.4.145, J6 🅲: "a vila nasce com uma
 * quantidade mínima de animais, o pastor traz eles para o curral, reproduz eles até ter 6 de cada, só depois ... que
 * qualquer abate daquela raça pode acontecer"; no deserto só havia coelho). Ao assentar a casa grande, nasce ao lado
 * dela um casal adulto de vaca, porco, ovelha e galinha, contando os que já estão a até {@value #RADIUS} blocos; o
 * pastor os leva para o curral pela Regra 196.
 */
public final class FoundationAnimals {

    /** Até onde os animais que já estão contam para o casal. */
    static final int RADIUS = 8;

    /** Quantos de cada espécie. */
    static final int PAIR = 2;

    static final List<EntityType<? extends AnimalEntity>> SPECIES =
            List.of(EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN);

    private FoundationAnimals() {
    }

    /** Faz nascer o que falta para um casal de cada espécie em volta de {@code center}; diz quantos nasceram. */
    public static int spawn(ServerWorld world, BlockPos center) {
        Box around = new Box(center).expand(RADIUS);
        int born = 0;

        for (int index = 0; index < SPECIES.size(); index++) {
            EntityType<? extends AnimalEntity> species = SPECIES.get(index);
            int have = world.getEntitiesByType(species, around, AnimalEntity::isAlive).size();

            for (int missing = PAIR - have; missing > 0; missing--) {
                BlockPos at = center.add(index % 2 == 0 ? 2 : -2, 0, index < 2 ? 2 : -2);
                AnimalEntity animal = species.create(world, null, at, SpawnReason.STRUCTURE, false, false);

                if (animal != null && world.spawnEntity(animal)) {
                    animal.setBreedingAge(0);
                    born++;
                }
            }
        }

        if (born > 0) {
            VillageColonyMod.LOGGER.info("VC_FOUNDATION_ANIMALS born={} at={} rule=380", born, center.toShortString());
        }

        return born;
    }
}
