package com.villagecolony.gametest;

import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * Quão cheio o baú está, medido no mundo — 2026-09-19.
 *
 * <p><b>Conta vaga ocupada, e não item guardado.</b> É a distinção que o
 * defeito de 12:09 cobrou: o baú do mineiro jogou 660 itens no chão, e um
 * baú com vinte e sete pilhas de <b>uma unidade</b> está cheio para todo
 * efeito. Medir por item diria que ele estava quase vazio, e a colônia
 * continuaria olhando o material ser destruído.
 */
public class ChestReliefGameTest {

    private static ColonyPos chestAt(TestContext context, BlockPos relative) {
        BlockPos absolute = context.getAbsolutePos(relative);

        context.getWorld().setBlockState(absolute, Blocks.CHEST.getDefaultState());

        return MinecraftTypeAdapter.toColonyPos(absolute);
    }

    /** Baú vazio mede zero. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_relief",
            tickLimit = 100)
    public void theEmptyChestMeasuresZero(TestContext context) {
        ServerWorld world = context.getWorld();

        ColonyPos chest = chestAt(context, new BlockPos(1, 1, 1));

        int full = ChestDepositor.howFull(world, chest);

        if (full != 0) {
            throw new AssertionError("bau vazio mediu " + full + "%, esperado 0");
        }

        context.complete();
    }

    /**
     * Pilhas de uma unidade enchem o baú, e a medida diz isso.
     *
     * <p>É o cenário exato do mineiro: cada bloco quebrado entra como uma
     * unidade, e as vagas acabam muito antes de o baú estar "cheio" em
     * número de itens.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_relief",
            tickLimit = 100)
    public void theSingleItemStacksStillFillTheChest(TestContext context) {
        ServerWorld world = context.getWorld();

        ColonyPos chest = chestAt(context, new BlockPos(2, 1, 2));

        BlockPos where = MinecraftTypeAdapter.toBlockPos(chest);

        if (!(world.getBlockEntity(where) instanceof ChestBlockEntity inventory)) {
            throw new AssertionError("o bau do cenario nao existe");
        }

        // Vinte vagas de UMA unidade cada: pouquissimos itens, muita vaga
        // gasta. Sao 20 de 27 vagas, que passa da metade.
        for (int slot = 0; slot < 20; slot++) {
            inventory.setStack(slot, new ItemStack(Items.SANDSTONE, 1));
        }

        inventory.markDirty();

        int full = ChestDepositor.howFull(world, chest);

        if (full <= 50) {
            throw new AssertionError(
                    "20 vagas de 27 mediram " + full + "% — a conta esta medindo item"
                            + " guardado, e nao vaga ocupada: o bau do mineiro parecia"
                            + " vazio enquanto jogava 660 itens no chao");
        }

        context.complete();
    }

    /**
     * Baú inexistente não declara aperto.
     *
     * <p>Sem leitura não se decide: um pedaço de mundo descarregado faria
     * a colônia fabricar sem motivo a cada ciclo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_relief",
            tickLimit = 100)
    public void theMissingChestDoesNotClaimToBeTight(TestContext context) {
        ServerWorld world = context.getWorld();

        ColonyPos nothing =
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, 1, 3)));

        int full = ChestDepositor.howFull(world, nothing);

        if (full != 0) {
            throw new AssertionError("posicao sem bau mediu " + full + "%, esperado 0");
        }

        context.complete();
    }
}
