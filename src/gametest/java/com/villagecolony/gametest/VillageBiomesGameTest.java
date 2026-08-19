package com.villagecolony.gametest;

import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.VillageBiomes;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.world.biome.BiomeKeys;

import java.util.Optional;

/**
 * A tabela de biomas — a Regra 20.
 *
 * <p>Ela responde duas perguntas com uma lista só: onde o mod atende
 * vila, e de que madeira aquela vila é. Até 2026-08-19 a resposta era
 * "planície, em carvalho", e todo o resto era descartado em silêncio.
 *
 * <p>Roda como teste de jogo, e não de unidade, por uma razão só: as
 * chaves de bioma são do jogo, e o Core não as conhece. A função em si é
 * pura — não toca no mundo, e por isso não monta cenário nenhum.
 */
public class VillageBiomesGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_biomes")
    public void eachVillageBiomeBuildsInItsOwnWood(TestContext context) {
        assertWood(context, "planície", BiomeKeys.PLAINS, "oak_planks");
        assertWood(context, "taiga", BiomeKeys.TAIGA, "spruce_planks");
        assertWood(context, "planície nevada", BiomeKeys.SNOWY_PLAINS, "spruce_planks");
        assertWood(context, "savana", BiomeKeys.SAVANNA, "acacia_planks");

        context.complete();
    }

    /**
     * Bioma sem vila continua de fora, e isso é limite e não recusa.
     *
     * <p>A distinção é da ADR-003 §5 e importa: recusa marca colônia
     * abandonada, e uma vila que o mod não atende não pode ser marcada
     * assim — ela está lá, viva, e o mod é que não a alcança.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_biomes")
    public void aBiomeWithoutVillagesIsNotOnTheTable(TestContext context) {
        context.assertTrue(
                VillageBiomes.woodFor(BiomeKeys.OCEAN).isEmpty(),
                "o oceano entrou na tabela de vilas");

        context.assertTrue(
                VillageBiomes.woodFor(BiomeKeys.JUNGLE).isEmpty(),
                "a selva entrou na tabela de vilas, e o jogo não gera vila lá");

        context.complete();
    }

    /**
     * O deserto está na tabela, e é o caso incômodo.
     *
     * <p>O jogo gera vila lá e a colônia nasce; o que ela não faz é
     * construir, porque não há árvore. Deixá-lo de fora seria mais
     * cômodo e seria mentira sobre onde há vila.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_biomes")
    public void theDesertIsAVillageBiomeEvenWithoutTrees(TestContext context) {
        context.assertTrue(
                VillageBiomes.woodFor(BiomeKeys.DESERT).isPresent(),
                "o jogo gera vila no deserto, e a tabela diz que não");

        context.complete();
    }

    private static void assertWood(
            TestContext context, String name, net.minecraft.registry.RegistryKey<
                    net.minecraft.world.biome.Biome> biome, String expected) {

        Optional<ResourceId> wood = VillageBiomes.woodFor(biome);

        context.assertTrue(wood.isPresent(), "a vila de " + name + " ficou sem madeira");

        context.assertTrue(
                wood.get().path().equals(expected),
                "a vila de " + name + " devia construir em " + expected
                        + " e pediu " + wood.get().path());
    }
}
