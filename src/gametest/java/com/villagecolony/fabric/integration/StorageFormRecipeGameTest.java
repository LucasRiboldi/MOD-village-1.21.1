package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/**
 * E6, playtest de 2026-10-08: "could not make iron_ingot — needs iron_block". A
 * forma guardada de um item não é rota para fabricá-lo; a fornalha é.
 */
public class StorageFormRecipeGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "storage_form")
    public void theIngotIsNotMadeFromItsOwnBlock(TestContext context) {
        ServerWorld world = context.getWorld();

        context.assertTrue(CraftingLookup.isStorageForm(world, Items.IRON_INGOT, Items.IRON_BLOCK),
                "o bloco de ferro não foi reconhecido como forma guardada do lingote");
        context.assertTrue(CraftingLookup.isStorageForm(world, Items.WHEAT, Items.HAY_BLOCK),
                "o fardo não foi reconhecido como forma guardada do trigo");
        context.assertTrue(CraftingLookup.billFor(world, Items.IRON_INGOT,
                        CraftingLookup.producing(world, Items.IRON_INGOT)).isEmpty(),
                "o lingote ainda tem rota de bancada pela forma guardada");

        // Controle: o funil continua sendo feito de lingote, e a tábua de tronco.
        context.assertTrue(!CraftingLookup.isStorageForm(world, Items.HOPPER, Items.IRON_INGOT),
                "o lingote virou forma guardada do funil");
        context.assertTrue(CraftingLookup.billFor(world, Items.HOPPER,
                        CraftingLookup.producing(world, Items.HOPPER)).isPresent(),
                "o funil perdeu a receita");
        context.assertTrue(CraftingLookup.billFor(world, Items.OAK_PLANKS,
                        CraftingLookup.producing(world, Items.OAK_PLANKS)).isPresent(),
                "a tábua perdeu a receita");
        context.complete();
    }
}
