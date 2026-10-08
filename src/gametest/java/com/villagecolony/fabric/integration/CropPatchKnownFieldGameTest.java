package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * E3, playtest de 2026-10-08: com a roça toda plantada e nada maduro, o fazendeiro
 * revarria o raio inteiro e terminava em "still sweeping — the budget ran out". A
 * roça conhecida responde sozinha; o raio só volta pela expansão periódica.
 *
 * <p>O centro vai quarenta acima do piso, como no {@code FarmerGameTest}: a bateria
 * planta lavoura nas arenas vizinhas, e lá em cima não há lavoura de ninguém.
 */
public class CropPatchKnownFieldGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_known_field", tickLimit = 200)
    public void aPlantedKnownFieldAnswersWithoutSweepingTheRadiusAgain(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(1, 2, 1)).up(40);
        // Raio 16: 33² = 1.089 colunas, mais que as 1.024 de uma passagem.
        int radius = 16;
        UUID colonyId = UUID.randomUUID();

        try {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.setBlockState(center.add(dx, -1, dz), Blocks.FARMLAND.getDefaultState());
                    world.setBlockState(center.add(dx, 0, dz), Blocks.WHEAT.getDefaultState());
                }
            }

            // A primeira volta pelo raio aprende a roça e fecha.
            CropPatch.Field field;
            int passes = 0;

            do {
                field = CropPatch.survey(world, colonyId, center, radius);
                passes++;
            } while (field.incomplete() && passes < 10);

            context.assertTrue(!field.incomplete() && field.ripe().isEmpty(),
                    "a primeira volta não fechou em " + passes + " passagens");

            CropPatch.Field again = CropPatch.survey(world, colonyId, center, radius);

            context.assertTrue(!again.incomplete(),
                    "com a roça conhecida toda plantada, o fazendeiro voltou a varrer o raio");
            context.assertTrue(RingSweep.pausedAt(colonyId, RingSweep.Scan.FARMING).isEmpty(),
                    "a varredura do raio recomeçou sem a expansão vencer");

            // Controle: maduro na roça conhecida é achado nela, numa passagem.
            world.setBlockState(center, ((CropBlock) Blocks.WHEAT).withAge(7));

            CropPatch.Field ripe = CropPatch.survey(world, colonyId, center, radius);

            context.assertTrue(ripe.ripe().map(center::equals).orElse(false),
                    "o trigo maduro da roça conhecida não foi achado: " + ripe);
        } finally {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.setBlockState(center.add(dx, 0, dz), Blocks.AIR.getDefaultState());
                    world.setBlockState(center.add(dx, -1, dz), Blocks.AIR.getDefaultState());
                }
            }

            CropPatch.forget(colonyId);
            RingSweep.forget(colonyId, RingSweep.Scan.FARMING);
        }

        context.complete();
    }
}
