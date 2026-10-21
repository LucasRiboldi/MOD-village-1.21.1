package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.world.biome.BiomeKeys;

/**
 * Rota própria para os itens do registro de faltas — pedido do autor, 2026-10-20. Cada caso tem o
 * controle: sem a prova do mundo, a rota continua fechada.
 */
public class OwnRoutesGameTest implements FabricGameTest {

    private static final WorldEvidence WOOD = new WorldEvidence(() -> false, () -> true, () -> false);

    private static final WorldEvidence CLAY = new WorldEvidence(() -> false, () -> false, () -> true);

    /** O tronco descascado sai do tronco no machado: não é receita do jogo, e não tinha rota. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "own_routes")
    public void aStrippedLogComesFromTheLog(TestContext context) {
        SupplyRoute route = SupplyRoutes.inBiome(context.getWorld(), BiomeKeys.PLAINS, Items.STRIPPED_OAK_LOG,
                WorldEvidence.NONE);

        context.assertTrue(route.available() && route.source() == SupplySource.STRIPPING,
                "o tronco de carvalho descascado não tem rota pelo machado: " + route);
        context.complete();
    }

    /** A tocha do deserto: com árvore ao alcance há graveto; sem árvore, não (controle). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "own_routes")
    public void aDesertTorchNeedsATreeNearby(TestContext context) {
        context.assertTrue(SupplyRoutes.inBiome(context.getWorld(), BiomeKeys.DESERT, Items.TORCH, WOOD).available(),
                "com árvore ao alcance, a tocha do deserto continua sem rota");
        context.assertFalse(SupplyRoutes.inBiome(context.getWorld(), BiomeKeys.DESERT, Items.TORCH,
                WorldEvidence.NONE).available(), "controle: sem árvore, a tocha do deserto ganhou rota");
        context.complete();
    }

    /** A terracota do deserto: com argila ao alcance há o que fundir; sem argila, não (controle). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "own_routes")
    public void desertTerracottaNeedsClayNearby(TestContext context) {
        context.assertTrue(SupplyRoutes.inBiome(context.getWorld(), BiomeKeys.DESERT, Items.TERRACOTTA, CLAY).available(),
                "com argila ao alcance, a terracota do deserto continua sem rota");
        context.assertFalse(SupplyRoutes.inBiome(context.getWorld(), BiomeKeys.DESERT, Items.TERRACOTTA,
                WorldEvidence.NONE).available(), "controle: sem argila, a terracota do deserto ganhou rota");
        context.complete();
    }
}
