package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.UUID;

/**
 * Toda vila que nasce ganha 2 ovelhas, 2 galinhas, 2 vacas e 2 porcos dentro da caixa dela — regra do
 * autor, 2026-10-20. Longe da arena, para os bichos não entrarem nos testes do pastor.
 */
public class VillageFarmAnimalsGameTest implements FabricGameTest {

    private static final int HALF = 12;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_animals")
    public void aNewVillageGetsTwoOfEachFarmAnimalInsideItsBox(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(1, 0, 1)).add(1_000_000, 0, 1_800_000);

        for (int dx = -HALF - 16; dx <= HALF + 16; dx += 16) {
            for (int dz = -HALF - 16; dz <= HALF + 16; dz += 16) {
                world.getChunk(center.add(dx, 0, dz));
            }
        }

        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(center));
        colony.measure(new VillageBounds(center.getX() - HALF, center.getY() - 8, center.getZ() - HALF,
                center.getX() + HALF, center.getY() + 8, center.getZ() + HALF));
        Box box = new Box(center.getX() - HALF, world.getBottomY(), center.getZ() - HALF,
                center.getX() + HALF + 1, world.getTopY(), center.getZ() + HALF + 1);

        int born = VillageFarmAnimals.seedInitial(world, colony);
        List<AnimalEntity> animals = world.getEntitiesByClass(AnimalEntity.class, box, Entity::isAlive);

        try {
            context.assertTrue(born == VillageFarmAnimals.EACH * VillageFarmAnimals.SPECIES.size(),
                    "deviam nascer 8 bichos e nasceram " + born);

            for (EntityType<? extends AnimalEntity> species : VillageFarmAnimals.SPECIES) {
                long count = animals.stream().filter(animal -> animal.getType() == species).count();
                context.assertTrue(count == VillageFarmAnimals.EACH,
                        "a caixa da vila tem " + count + " de " + species.getUntranslatedName() + ", e não 2");
            }
        } finally {
            animals.forEach(Entity::discard);
        }

        context.complete();
    }
}
