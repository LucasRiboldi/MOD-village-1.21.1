package com.villagecolony.fabric.integration;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Quantos ticks um trabalhador leva para quebrar um bloco.
 *
 * <p>Decisão do autor em 2026-08-08, Regra 2: o trabalhador leva para
 * quebrar um bloco o mesmo tempo que um jogador levaria com ferramenta
 * de ferro. Até então a árvore inteira caía dentro de um tick — o que é
 * visível e errado, e é também um pico de custo dentro de um tick, que é
 * o tipo de coisa que já travou este projeto duas vezes.
 *
 * <p>A conta é a do Vanilla, e é a do Vanilla de propósito. O jogo
 * calcula por tick uma fração do bloco:
 *
 * <pre>{@code
 * fração por tick = velocidade da ferramenta ÷ dureza ÷ divisor
 *
 * divisor 30  quando a ferramenta colhe o bloco
 * divisor 100 quando não colhe
 * }</pre>
 *
 * <p>e o bloco cai quando a soma chega a 1. Daí o número de ticks ser o
 * inverso da fração.
 *
 * <p>Nada aqui é número escrito à mão. A dureza vem do bloco, a
 * velocidade vem da ferramenta contra aquele bloco, e se a ferramenta
 * colhe ou não vem do próprio estado. Um machado de ferro contra tronco
 * dá 6 de velocidade e 2 de dureza — dez ticks, meio segundo, que é o
 * que um jogador leva. Contra folha o machado não é a ferramenta certa,
 * e o valor sai da mesma fórmula em vez de sair de uma tabela paralela
 * que envelheceria no primeiro bloco novo.
 */
public final class BlockBreakTime {

    /** Divisor do Vanilla quando a ferramenta colhe o bloco. */
    private static final float HARVESTABLE = 30.0f;

    /** Divisor do Vanilla quando ela não colhe — o bloco cai sem dropar. */
    private static final float UNHARVESTABLE = 100.0f;

    /**
     * Teto para bloco que não se quebra.
     *
     * <p>Bedrock tem dureza negativa, que no Vanilla significa
     * indestrutível. Nada da colheita de árvore chega aqui, mas devolver
     * um número em vez de estourar mantém quem chama simples: um bloco
     * que pede este tanto de ticks nunca termina, e o trabalho segue
     * para o próximo.
     */
    public static final int NEVER = Integer.MAX_VALUE;

    private BlockBreakTime() {
    }

    /**
     * Quantos ticks este bloco pede desta ferramenta.
     *
     * <p>Nunca menos de um: um bloco de dureza zero ainda custa o tick
     * em que é quebrado, e devolver zero poria quem chama num laço que
     * quebra tudo dentro do mesmo tick — exatamente o que a Regra 2
     * veio desfazer.
     */
    public static int ticksFor(ServerWorld world, BlockPos pos, BlockState state, Item tool) {
        float hardness = state.getHardness(world, pos);

        if (hardness < 0.0f) {
            return NEVER;
        }

        if (hardness == 0.0f) {
            return 1;
        }

        ItemStack held = new ItemStack(tool);

        float speed = held.getMiningSpeedMultiplier(state);
        float divisor = harvests(held, state) ? HARVESTABLE : UNHARVESTABLE;

        float perTick = speed / hardness / divisor;

        if (perTick <= 0.0f) {
            return NEVER;
        }

        return Math.max(1, (int) Math.ceil(1.0f / perTick));
    }

    /**
     * Se esta ferramenta colhe este bloco.
     *
     * <p>Bloco que não exige ferramenta é colhido por qualquer coisa,
     * inclusive pela mão — é o caso de tronco e de folha. A pergunta
     * existe porque o divisor do Vanilla muda com ela, e um bloco
     * quebrado sem a ferramenta certa leva mais de três vezes o tempo.
     */
    private static boolean harvests(ItemStack tool, BlockState state) {
        return !state.isToolRequired() || tool.isSuitableFor(state);
    }
}
