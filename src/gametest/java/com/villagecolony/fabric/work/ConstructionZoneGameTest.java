package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.PlayerClearedPlants;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * A faixa de construção e a planta do jogador — regras do autor, 2026-10-11:
 * muda perto da rua tira o lote da escolha, então o construtor a limpa e ninguém
 * planta ali; e o que o jogador tirou não volta.
 */
public class ConstructionZoneGameTest implements FabricGameTest {

    private static final BlockPos PATH = new BlockPos(0, 1, 0);

    private static final BlockPos NEAR = new BlockPos(2, 1, 2);

    private static final BlockPos FAR = new BlockPos(7, 1, 7);

    private static final BlockPos CHEST = new BlockPos(0, 2, 7);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_grove", tickLimit = 20)
    public void aSaplingBesideTheRoadIsClearedAndNothingIsPlantedThere(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        context.setBlockState(PATH, Blocks.DIRT_PATH.getDefaultState());
        context.setBlockState(NEAR.up(), Blocks.OAK_SAPLING.getDefaultState());
        context.setBlockState(FAR.up(), Blocks.OAK_SAPLING.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        UUID builder = UUID.randomUUID();
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        try {
            VillageColonyMod.STORAGES.register(WorkerStorage.of(builder, chest));

            BlockPos near = context.getAbsolutePos(NEAR);
            BlockPos far = context.getAbsolutePos(FAR);

            context.assertTrue(BuilderPaving.isZoneWeed(context.getWorld(), near),
                    "a muda a dois blocos da rua devia ser limpa");
            context.assertFalse(BuilderPaving.isZoneWeed(context.getWorld(), far),
                    "a muda a sete blocos da rua está fora da faixa");

            BuilderPaving.clear(context.getWorld(), builder, near.up());

            context.assertTrue(context.getBlockState(NEAR.up()).isAir(), "a muda continuou no lugar");
            context.assertTrue(ColonyChests.countIn(context.getWorld(), List.of(chest), Items.OAK_SAPLING) == 1,
                    "a muda tirada não foi para o baú do construtor");

            context.assertFalse(TreeNursery.isSpotForANursery(context.getWorld(), near),
                    "o viveiro plantaria na faixa de construção");

            context.setBlockState(FAR.up(), Blocks.AIR.getDefaultState());
            boolean farWasFine = TreeNursery.isSpotForANursery(context.getWorld(), far);

            PlayerClearedPlants.observe(far.up(), Blocks.OAK_SAPLING.getDefaultState());

            context.assertTrue(farWasFine && !TreeNursery.isSpotForANursery(context.getWorld(), far),
                    "o lugar de onde o jogador tirou a muda voltaria a receber muda (antes: " + farWasFine + ")");
        } finally {
            VillageColonyMod.STORAGES.remove(builder);
            PlayerClearedPlants.clearAll();

            for (int x = 0; x <= 7; x++) {
                for (int z = 0; z <= 7; z++) {
                    for (int y = 1; y <= 3; y++) {
                        context.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        context.complete();
    }
}
