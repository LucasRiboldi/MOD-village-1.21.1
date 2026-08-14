package com.villagecolony.gametest;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.Map;
import java.util.Optional;

/**
 * A casa do jogo vira projeto da colônia — TASK-031.
 *
 * <p>Gametest e não teste de unidade porque é fronteira pura: só um
 * servidor de verdade tem o gerenciador de estruturas carregado com os
 * arquivos do Vanilla.
 *
 * <p>As afirmações são frouxas de propósito onde o Vanilla pode mudar.
 * Um teste que exigisse "esta casa tem 143 blocos" quebraria na próxima
 * versão do jogo sem que nada do mod estivesse errado. O que ele afirma
 * é o que precisa ser verdade para a obra existir: a casa foi lida, tem
 * tamanho de casa, é feita de coisas que a colônia produz, e não traz
 * andaime do gerador.
 */
public class BlueprintReaderGameTest implements FabricGameTest {

    /** Menos blocos que isto não é casa nenhuma. */
    private static final int PLAUSIBLE_HOUSE = 50;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void thePlainsHouseIsReadFromTheGame(TestContext context) {
        Blueprint house = read(context);

        context.assertTrue(
                house.blockCount() >= PLAUSIBLE_HOUSE,
                "a casa veio com " + house.blockCount() + " blocos — menos que uma casa");

        context.assertTrue(
                house.size().x() > 1 && house.size().y() > 1 && house.size().z() > 1,
                "a casa não tem três dimensões: " + house.size());

        context.complete();
    }

    /**
     * A casa é feita de tábua, que é o que o fabricante produz.
     *
     * <p>É a ligação que dá motivo à Fase 9: até aqui a tábua era feita
     * porque cabia no baú.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void theHouseIsMadeOfWhatTheColonyProduces(TestContext context) {
        Map<ResourceId, Integer> materials = read(context).materials();

        ResourceId planks = MinecraftTypeAdapter.toResourceId(Blocks.OAK_PLANKS);

        context.assertTrue(
                materials.containsKey(planks),
                "a casa de planície não pede tábua de carvalho — pede " + materials.keySet());

        context.assertTrue(
                materials.get(planks) > 0,
                "a conta de tábua veio zerada");

        context.complete();
    }

    /**
     * Nada de ar nem de bloco de encaixe do gerador.
     *
     * <p>A casa Vanilla é peça de jigsaw. Colocar o que ela traz de
     * andaime deixaria blocos de comando na vila do jogador — e o ar
     * ocuparia o projeto inteiro dizendo "aqui não vai nada".
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void theScaffoldingOfTheGeneratorIsLeftOut(TestContext context) {
        for (BlueprintBlock block : read(context).blocks()) {
            Optional<Block> real = MinecraftTypeAdapter.toBlock(block.block());

            context.assertTrue(
                    real.isPresent(),
                    "o projeto pede um bloco que o jogo não conhece: " + block.block());

            context.assertTrue(
                    real.get() != Blocks.AIR
                            && real.get() != Blocks.STRUCTURE_BLOCK
                            && real.get() != Blocks.JIGSAW,
                    "andaime do gerador entrou no projeto: " + block.block()
                            + " em " + block.offset());
        }

        context.complete();
    }

    /** Estrutura que não existe devolve vazio, e não derruba o tick. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_reader")
    public void anUnknownStructureIsEmptyAndNotAnError(TestContext context) {
        Optional<Blueprint> nothing = StructureBlueprintReader.read(
                context.getWorld(), ResourceId.vanilla("village/plains/houses/no_such_house"));

        context.assertTrue(nothing.isEmpty(), "uma estrutura inexistente devolveu projeto");

        context.complete();
    }

    private static Blueprint read(TestContext context) {
        return StructureBlueprintReader
                .read(context.getWorld(), StructureBlueprintReader.PLAINS_SMALL_HOUSE)
                .orElseThrow(() -> new AssertionError(
                        "o jogo não devolveu a casa "
                                + StructureBlueprintReader.PLAINS_SMALL_HOUSE));
    }
}
