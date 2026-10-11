package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** A leitura dos baús da colônia — F13, 2026-09-30. */
public final class ColonyChestSurveyGameTest implements FabricGameTest {

    /**
     * Dentro do mesmo minuto, a fotografia completa dos baús é reusada.
     *
     * <p>Pedido do autor: ler os baús no máximo uma vez por minuto e guardar
     * a lista em memória. A fase que o log chamava de "chests" custou 85 ms
     * de mediana por ciclo no playtest das 02:45.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_survey")
    public void aCompleteSurveyIsReusedWithinTheMinute(TestContext context) {
        BlockPos chestAt = new BlockPos(2, 2, 2);
        context.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chestAt));
        UUID colony = UUID.randomUUID();

        ChestDepositor.deposit(context.getWorld(), chest, Items.OAK_PLANKS, 4);

        try {
            ChestInventoryReader.ChestSurvey first = ColonyChestSurvey.advance(
                    context.getWorld(), colony, List.of(chest), ResourceGroup.PLANKS);

            ChestDepositor.deposit(context.getWorld(), chest, Items.OAK_PLANKS, 60);

            ChestInventoryReader.ChestSurvey second = ColonyChestSurvey.advance(
                    context.getWorld(), colony, List.of(chest), ResourceGroup.PLANKS);

            context.assertTrue(first.resources().amountOf(ResourceType.OAK_PLANKS) == 4,
                    "a primeira leitura não viu as quatro tábuas");
            context.assertTrue(second.resources().amountOf(ResourceType.OAK_PLANKS) == 4,
                    "a segunda leitura, no mesmo minuto, releu o baú em vez de reusar a fotografia: "
                            + second.resources().amountOf(ResourceType.OAK_PLANKS));
        } finally {
            ColonyChestSurvey.forget(colony);
        }

        context.complete();
    }

    /**
     * Rodada aberta decide com a última fotografia completa — escolha do autor,
     * 2026-10-11. Nove baús com uma tábua cada; um décimo baú entra e abre uma
     * rodada nova. A parcial dela via 8 tábuas (8 baús relidos) e a meta enxergava
     * falta; a decisão tem de ver as 9 da última leitura completa.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_survey")
    public void anOpenRoundDecidesWithTheLastCompleteSurvey(TestContext context) {
        UUID colony = UUID.randomUUID();
        java.util.List<ColonyPos> chests = new java.util.ArrayList<>();

        for (int i = 0; i < 10; i++) {
            BlockPos at = new BlockPos(i % 5, 2, i / 5 * 2);
            context.setBlockState(at, Blocks.CHEST.getDefaultState());
            ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(at));
            ChestDepositor.deposit(context.getWorld(), chest, Items.OAK_PLANKS, 1);
            chests.add(chest);
        }

        try {
            List<ColonyPos> nine = List.copyOf(chests.subList(0, 9));

            ColonyChestSurvey.advance(context.getWorld(), colony, nine, ResourceGroup.PLANKS);
            ChestInventoryReader.ChestSurvey complete =
                    ColonyChestSurvey.advance(context.getWorld(), colony, nine, ResourceGroup.PLANKS);

            ChestInventoryReader.ChestSurvey whileOpen =
                    ColonyChestSurvey.advance(context.getWorld(), colony, List.copyOf(chests), ResourceGroup.PLANKS);

            context.assertTrue(complete.resources().amountOf(ResourceType.OAK_PLANKS) == 9,
                    "a primeira rodada não fechou com as nove tábuas");
            context.assertTrue(whileOpen.resources().amountOf(ResourceType.OAK_PLANKS) == 9,
                    "a rodada aberta decidiu com a parcial: "
                            + whileOpen.resources().amountOf(ResourceType.OAK_PLANKS) + " tábuas");
        } finally {
            ColonyChestSurvey.forget(colony);
        }

        context.complete();
    }
}
