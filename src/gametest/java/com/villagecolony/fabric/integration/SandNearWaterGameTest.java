package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Areia perto da água em qualquer bioma — decisão do autor, 2026-09-30.
 *
 * <p>O mundo da bateria é planície, onde a areia não tinha rota: vidraça sem
 * cadeia, e ninguém ia buscar areia para o vidro.
 */
public final class SandNearWaterGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "sand_near_water")
    public void sandBesideWaterIsFoundAndDrySandIsNot(TestContext context) {
        BlockPos beach = new BlockPos(2, 1, 2);
        context.setBlockState(beach, Blocks.SAND);
        context.setBlockState(beach.east(), Blocks.WATER);
        BlockPos dune = new BlockPos(6, 1, 6);
        context.setBlockState(dune, Blocks.SAND);

        context.assertTrue(SandNearWater.within(context.getWorld(), context.getAbsolutePos(beach), 1),
                "a areia com água ao lado não foi achada");
        context.assertTrue(!SandNearWater.within(context.getWorld(), context.getAbsolutePos(dune), 1),
                "areia seca, longe da água, foi contada como beira d'água");
        context.complete();
    }

    /** Com uma praia ao alcance, a planície passa a ter rota para o vidro. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "sand_near_water")
    public void aBeachGivesThePlainsARouteToGlass(TestContext context) {
        BlockPos beach = new BlockPos(3, 1, 3);
        context.setBlockState(beach, Blocks.SAND);
        context.setBlockState(beach.north(), Blocks.WATER);
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(beach.up())));
        VillageColonyMod.COLONIES.register(colony);

        try {
            context.assertTrue(
                    BiomeConstructionSupply.hasRouteInBiome(context.getWorld(), colony.id(), Items.GLASS),
                    "com areia de beira d'água ao alcance, o vidro continuou sem rota na planície");
        } finally {
            SandNearWater.clearAll();
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
