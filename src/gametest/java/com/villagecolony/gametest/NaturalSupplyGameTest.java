package com.villagecolony.gametest;

import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.List;

/**
 * A peça pronta só nasce para bloco de manufatura — decisão do autor,
 * 2026-09-26. Na sessão longa a regra fabricou 22 toras, 22 grama e 15 terra.
 * Aqui e não em unitário: a regra lê tags do jogo, que só existem com mundo.
 */
public class NaturalSupplyGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "natural_supply", tickLimit = 20)
    public void onlyManufacturedPiecesMayBeConjured(TestContext context) {
        List<Item> natural = List.of(Items.OAK_LOG, Items.SPRUCE_LOG, Items.DIRT, Items.GRASS_BLOCK,
                Items.SAND, Items.COBBLESTONE, Items.STONE, Items.GRAVEL, Items.WHITE_WOOL, Items.WHEAT,
                Items.OAK_LEAVES, Items.OAK_SAPLING, Items.DANDELION);
        List<Item> made = List.of(Items.OAK_PLANKS, Items.OAK_STAIRS, Items.OAK_FENCE, Items.OAK_DOOR,
                Items.TORCH, Items.GLASS_PANE, Items.YELLOW_WOOL, Items.BREWING_STAND, Items.STRIPPED_OAK_LOG);

        for (Item item : natural) {
            context.assertTrue(BiomeConstructionSupply.isNatural(item), item + " devia ser da natureza");
        }

        for (Item item : made) {
            context.assertFalse(BiomeConstructionSupply.isNatural(item), item + " é de manufatura");
        }

        context.complete();
    }
}
