package com.villagecolony.fabric.integration;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

/**
 * A ferramenta de ferro certa para a ação que o trabalhador vai fazer —
 * decisão do autor, 2026-09-30.
 *
 * <p><i>"Cada aldeão usa a ferramenta apropriada de ferro para a devida
 * ação que vai executar."</i> Até aqui a conta usava sempre a ferramenta
 * da profissão: o fazendeiro cavava terra de enxada, o lenhador tirava
 * folha de machado, e o desvio de qualquer um furava pedra com o que
 * tivesse na mão.
 *
 * <p><b>Quem sabe qual é a certa é o jogo</b> (ADR-009): entre as quatro
 * de ferro, vence a que o bloco quebra mais depressa e que o colhe. A
 * ferramenta da mão só fica quando é pelo menos tão boa — é o que mantém a
 * pá de Toque Suave do fundidor na grama e a picareta melhor que o
 * {@link ToolUpgrade} tirou do baú.
 */
public final class ActionTool {

    private static final List<Item> IRON_TOOLS = List.of(
            Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE);

    private ActionTool() {
    }

    /** A ferramenta com que este bloco é quebrado, dada a que está na mão. */
    public static ItemStack forBlock(BlockState state, ItemStack held) {
        ItemStack best = held;
        float bestScore = score(held, state);

        for (Item candidate : IRON_TOOLS) {
            ItemStack tool = new ItemStack(candidate);
            float score = score(tool, state);

            if (score > bestScore) {
                best = tool;
                bestScore = score;
            }
        }

        return best;
    }

    /**
     * Velocidade, com a colheita pesando antes dela: ferramenta que não
     * colhe o bloco perde para qualquer uma que colha.
     */
    private static float score(ItemStack tool, BlockState state) {
        boolean harvests = !state.isToolRequired() || tool.isSuitableFor(state);
        float speed = tool.getMiningSpeedMultiplier(state);

        return harvests ? 1_000.0f + speed : speed;
    }
}
