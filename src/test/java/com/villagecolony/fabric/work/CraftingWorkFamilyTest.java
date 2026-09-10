package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceId;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cada peça da obra vai para a oficina certa.
 *
 * <p>Com a divisão do fabricante em carpinteiro e pedreiro — 2026-09-10,
 * a pedido do autor —, o {@code produceForWork} percorre a lista da obra
 * e pula o que não é da família de quem está trabalhando. Sem esse filtro
 * a divisão seria só de nome: os dois percorreriam a mesma lista, fariam
 * a mesma peça, e o segundo chegaria sempre para achar o trabalho feito.
 *
 * <p><b>O que este arquivo cobre, e o que não cobre.</b> Ele afirma a
 * <b>classificação</b> — que escada de pedra é do pedreiro e porta de
 * carvalho é do carpinteiro. Que o filtro de fato <b>reparta o
 * trabalho</b> numa colônia rodando não está coberto aqui, e a lacuna foi
 * medida: removido o {@code continue} que usa este predicado, 701
 * unitários e 275 testes de jogo continuam verdes. Isso é gametest, e
 * está na lista de pendências.
 */
class CraftingWorkFamilyTest {

    private static ResourceId block(String path) {
        return new ResourceId("minecraft", path);
    }

    /** A alvenaria das cinco vilas do jogo é do pedreiro. */
    @Test
    void theStoneOfEveryVillageStyleBelongsToTheMason() {
        for (String path : new String[] {
                "cobblestone",
                "mossy_cobblestone",
                "stone",
                "stone_bricks",
                "stone_brick_stairs",
                "stone_slab",
                "sandstone",
                "cut_sandstone",
                "smooth_sandstone_stairs",
                "chiseled_sandstone",
                "bricks",
                "polished_andesite",
                "polished_diorite",
                "polished_granite",
                "terracotta",
                "quartz_block"}) {

            assertTrue(
                    CraftingWork.isMasonry(block(path)),
                    path + " não foi para o pedreiro, e é alvenaria");
        }
    }

    /** A madeira e o acabamento continuam do carpinteiro. */
    @Test
    void theWoodAndTheTrimBelongToTheCarpenter() {
        for (String path : new String[] {
                "oak_log",
                "stripped_oak_log",
                "oak_planks",
                "oak_stairs",
                "oak_slab",
                "oak_fence",
                "oak_door",
                "oak_trapdoor",
                "spruce_planks",
                "acacia_fence_gate",
                "glass_pane",
                "torch",
                "lantern",
                "white_bed",
                "hay_block",
                "composter"}) {

            assertFalse(
                    CraftingWork.isMasonry(block(path)),
                    path + " foi para o pedreiro, e não é alvenaria");
        }
    }

    /**
     * <b>A tocha de redstone é a armadilha do nome.</b>
     *
     * <p>O caminho dela contém {@code stone} e ela não é alvenaria
     * nenhuma. Classificá-la mal mandaria o carpinteiro ignorá-la e o
     * pedreiro tentar uma receita que não é dele — e a obra esperaria
     * pelos dois, que é o pior dos dois erros possíveis aqui.
     *
     * <p>É o caso que justifica a exceção escrita no predicado, e sem
     * este teste ela pareceria zelo excessivo e sairia na primeira
     * limpeza.
     */
    @Test
    void redstoneIsNotMasonryEvenThoughTheNameSaysStone() {
        assertTrue(
                block("redstone_torch").path().contains("stone"),
                "o cenário desta armadilha mudou: o nome já não contém stone");

        assertFalse(
                CraftingWork.isMasonry(block("redstone_torch")),
                "a tocha de redstone virou alvenaria pelo nome, e a obra esperaria"
                        + " o pedreiro fazer o que é do carpinteiro");

        assertFalse(
                CraftingWork.isMasonry(block("redstone_lamp")),
                "a lâmpada de redstone virou alvenaria pelo nome");

        assertFalse(
                CraftingWork.isMasonry(block("redstone_block")),
                "o bloco de redstone virou alvenaria pelo nome");
    }

}
