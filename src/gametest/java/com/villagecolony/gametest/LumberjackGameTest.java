package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.TreeHarvester;
import com.villagecolony.fabric.integration.TreeScanner;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * O lenhador derrubando — Fase 8.
 *
 * <p>Primeiro código do mod que escreve no mundo, e por isso o primeiro
 * em que um defeito estraga o save de quem joga. Estes testes correm num
 * mundo descartável: é exatamente o que o item A do §8 comprou.
 *
 * <p>Cada regra decidida pelo autor em 2026-08-08 tem um teste, e os
 * negativos importam mais que os positivos: o que o lenhador **não**
 * pode quebrar é o que protege a construção do jogador.
 */
public class LumberjackGameTest implements FabricGameTest {

    /** Uma árvore de carvalho simples: quatro troncos sobre terra. */
    private static BlockPos plantTree(TestContext context, BlockPos base) {
        context.setBlockState(base.down(), Blocks.DIRT.getDefaultState());

        for (int y = 0; y < 4; y++) {
            context.setBlockState(base.up(y), Blocks.OAK_LOG.getDefaultState());
        }

        return base;
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_fell")
    public void fellingTakesTheWholeTrunk(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);

        plantTree(context, base);

        int felled = TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.assertTrue(felled == 4, "esperava 4 troncos derrubados, foram " + felled);

        for (int y = 1; y < 4; y++) {
            context.expectBlock(Blocks.AIR, base.up(y));
        }

        context.complete();
    }

    /**
     * A muda entra no lugar da base.
     *
     * <p>É o que impede a floresta ao redor da vila de sumir com o tempo,
     * e a única parte da regra que se autocorrige sem o jogador notar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_replant")
    public void fellingReplantsASapling(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);

        plantTree(context, base);

        TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.expectBlock(Blocks.OAK_SAPLING, base);

        context.complete();
    }

    /**
     * Folha não é alvo.
     *
     * <p>O autor escolheu "só tronco" justamente para não encostar em
     * construção feita de folha.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_leaves")
    public void fellingLeavesTheLeavesAlone(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);
        BlockPos leaf = base.up(4);

        plantTree(context, base);
        context.setBlockState(leaf, Blocks.OAK_LEAVES.getDefaultState());

        TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.expectBlock(Blocks.OAK_LEAVES, leaf);

        context.complete();
    }

    /**
     * Madeira que não é carvalho fica de pé.
     *
     * <p>A regra é {@code oak_log} e nada mais. Uma casa de bétula
     * encostada numa árvore não pode virar estoque da colônia.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_birch")
    public void fellingIgnoresOtherWoods(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);
        BlockPos birch = base.up(1).east();

        plantTree(context, base);
        context.setBlockState(birch, Blocks.BIRCH_LOG.getDefaultState());

        TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.expectBlock(Blocks.BIRCH_LOG, birch);

        context.complete();
    }

    /** A madeira derrubada entra no baú, e a colônia a conta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_deposit")
    public void theWoodGoesIntoTheChest(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);
        BlockPos chest = new BlockPos(5, 2, 2);

        plantTree(context, base);
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();

        int felled = TreeHarvester.fell(world, context.getAbsolutePos(base));

        ColonyPos chestPos =
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest));

        int leftOver = ChestDepositor.deposit(world, chestPos, Items.OAK_LOG, felled);

        context.assertTrue(leftOver == 0, "sobrou madeira sem lugar: " + leftOver);

        int stored = ChestInventoryReader
                .read(world, context.getAbsolutePos(chest))
                .amountOf(ResourceType.OAK_LOG);

        context.assertTrue(
                stored == felled,
                "derrubou " + felled + " e o baú guardou " + stored);

        context.complete();
    }

    /**
     * A busca acha a árvore que está ao alcance.
     *
     * <p>O raio de 64 é o do jogo; aqui a árvore está a poucos blocos, e
     * o que se verifica é que a varredura por colunas a encontra.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_search")
    public void theSearchFindsATreeNearby(TestContext context) {
        BlockPos base = new BlockPos(4, 2, 4);
        BlockPos center = new BlockPos(1, 2, 1);

        plantTree(context, base);

        boolean found = TreeScanner.findNearestLog(
                        context.getWorld(), context.getAbsolutePos(center), 16)
                .isPresent();

        context.assertTrue(found, "a árvore ao lado não foi encontrada");

        context.complete();
    }
}
