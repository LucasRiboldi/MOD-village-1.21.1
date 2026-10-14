package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * O construtor tira do lugar quem fica parado onde a peça vai — bug 0.3.53: a obra ficava
 * refém de um aldeão que não saía, 84 peças adiadas por "someone kept standing".
 */
public class BuilderShoveGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_shove", tickLimit = 10)
    public void aStandingOccupantIsShovedOutOfTheSpot(TestContext context) {
        BlockPos rel = new BlockPos(3, 2, 3);
        BlockPos target = context.getAbsolutePos(rel);
        // Chão sob o alvo e sob os vizinhos, e espaço livre ao lado para onde empurrar.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                context.getWorld().setBlockState(target.add(dx, -1, dz), Blocks.STONE.getDefaultState());
            }
        }

        VillagerEntity occupant = context.spawnEntity(EntityType.VILLAGER, rel);

        try {
            context.assertTrue(occupant != null, "o aldeão não nasceu no alvo");
            context.assertTrue(BuilderApproach.someoneStandsIn(context.getWorld(), target),
                    "o aldeão não está no alvo antes do empurrão");

            boolean shoved = BuilderApproach.shoveOutOf(context.getWorld(), target);

            context.assertTrue(shoved, "shoveOutOf não achou ninguém para empurrar");
            context.assertFalse(BuilderApproach.someoneStandsIn(context.getWorld(), target),
                    "o aldeão continuou no alvo depois do empurrão");
        } finally {
            if (occupant != null) {
                occupant.discard();
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    context.getWorld().setBlockState(target.add(dx, -1, dz), Blocks.AIR.getDefaultState());
                }
            }
        }

        context.complete();
    }
}
