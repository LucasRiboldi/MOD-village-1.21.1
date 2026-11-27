package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Regra 380 (notas do jogo da 0.4.145, J6 🅲: "a vila nasce com uma quantidade mínima de animais, o pastor traz eles para
 * o curral, reproduz eles até ter 6 de cada"): na fundação nasce um casal de vaca, porco, ovelha e galinha ao lado da
 * casa grande, contando o que já há por perto; uma segunda fundação no mesmo lugar não dobra.
 */
public class FoundationAnimalsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_animals", tickLimit = 20)
    public void theVillageIsBornWithAPairOfEachSpecies(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(4, 2, 4));
        Box around = new Box(center).expand(FoundationAnimals.RADIUS + 2);

        try {
            for (BlockPos at : BlockPos.iterate(center.add(-4, -1, -4), center.add(4, -1, 4))) {
                world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState());
            }

            FoundationAnimals.spawn(world, center);
            FoundationAnimals.spawn(world, center);

            for (EntityType<? extends AnimalEntity> species : FoundationAnimals.SPECIES) {
                int count = world.getEntitiesByType(species, around, AnimalEntity::isAlive).size();

                context.assertTrue(count == FoundationAnimals.PAIR,
                        "Regra 380: " + species.getUntranslatedName() + " nasceu " + count + " vezes, e não um casal");
            }
        } finally {
            for (EntityType<? extends AnimalEntity> species : FoundationAnimals.SPECIES) {
                world.getEntitiesByType(species, around, animal -> true).forEach(AnimalEntity::discard);
            }
        }

        context.complete();
    }
}
