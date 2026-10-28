package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O pedreiro sem tarefa nivela em volta da casa pronta — Regra 149: o monte encostado na parede baixa para a
 * altura do chão em volta, com no máximo um bloco de diferença.
 */
public class HouseSurroundingsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "house_surroundings", tickLimit = 20)
    public void aMoundAgainstTheWallComesDownToTheGroundAround(TestContext context) {
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.STONE);
                context.setBlockState(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        // A casa: um cubo de tábua de 3 × 3 com piso em y = 2.
        for (int x = 4; x <= 6; x++) {
            for (int z = 4; z <= 6; z++) {
                for (int y = 2; y <= 4; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.OAK_PLANKS);
                }
            }
        }

        // O monte encostado na parede oeste: terra até y = 4, três acima do chão.
        context.setBlockState(new BlockPos(3, 2, 5), Blocks.DIRT);
        context.setBlockState(new BlockPos(3, 3, 5), Blocks.DIRT);
        context.setBlockState(new BlockPos(3, 4, 5), Blocks.GRASS_BLOCK);

        Building house = new Building(UUID.randomUUID(), UUID.randomUUID(), ResourceId.vanilla("test_house"),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(4, 2, 4))),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(6, 4, 6))));

        int changed = SiteSurroundings.level(context.getWorld(), house);

        context.assertTrue(changed > 0, "o monte devia ser cortado");
        context.assertTrue(context.getBlockState(new BlockPos(3, 4, 5)).isAir()
                        && context.getBlockState(new BlockPos(3, 3, 5)).isAir(),
                "o monte devia baixar para no máximo um bloco acima do chão em volta");
        context.assertTrue(context.getBlockState(new BlockPos(5, 3, 5)).isOf(Blocks.OAK_PLANKS), "a casa não se mexe");
        context.complete();
    }
}
