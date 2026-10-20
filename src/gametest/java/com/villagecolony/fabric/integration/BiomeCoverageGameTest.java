package com.villagecolony.fabric.integration;

import com.villagecolony.fabric.world.MineFill;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.FallingBlock;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A cobertura do mod em todos os biomas do Overworld — pedido do autor, 2026-10-20: garantir o
 * funcionamento em todos os biomas do Minecraft.
 *
 * <p>Percorre o registro de biomas do jogo (não uma lista escrita aqui: bioma novo entra sozinho) e
 * grava em {@code biome-coverage.csv}, na pasta do servidor de teste, o que o mod faria em cada um:
 * vila Vanilla que o jogo gera, estilo da colônia, madeira, espécies do viveiro, pedra de enchimento
 * da mina, limpeza de lote na mata. Reprova se algum bioma ficar sem resposta — estilo sem paleta ou
 * sem espécie de árvore, enchimento que cai.
 *
 * <p>O CSV é o mapa de prioridades do roteiro por bioma ({@code docs/testing/roteiro-por-bioma.md}).
 */
public class BiomeCoverageGameTest implements FabricGameTest {

    private static final Map<String, TagKey<Biome>> VANILLA_VILLAGES = Map.of(
            "plains", BiomeTags.VILLAGE_PLAINS_HAS_STRUCTURE,
            "desert", BiomeTags.VILLAGE_DESERT_HAS_STRUCTURE,
            "savanna", BiomeTags.VILLAGE_SAVANNA_HAS_STRUCTURE,
            "snowy", BiomeTags.VILLAGE_SNOWY_HAS_STRUCTURE,
            "taiga", BiomeTags.VILLAGE_TAIGA_HAS_STRUCTURE);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "biome_coverage")
    public void everyOverworldBiomeHasAnAnswer(TestContext context) {
        var registry = context.getWorld().getRegistryManager().get(RegistryKeys.BIOME);
        List<String> rows = new ArrayList<>();
        List<String> gaps = new ArrayList<>();

        rows.add("biome,vanilla_village,style,own_style,wood,nursery,mine_fill,clears_trees");

        for (var key : registry.getKeys().stream().sorted((a, b) -> a.getValue().compareTo(b.getValue())).toList()) {
            RegistryEntry<Biome> biome = registry.entryOf(key);

            if (!biome.isIn(BiomeTags.IS_OVERWORLD)) {
                continue;
            }

            String name = key.getValue().getPath();
            String village = VANILLA_VILLAGES.entrySet().stream()
                    .filter(entry -> biome.isIn(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse("-");

            // Num mundo de um bioma só, sem vila gerada, o estilo é o do bioma se ele tem estilo, e a
            // planície se não tem (VillageStyle).
            boolean own = VillageBiomes.woodFor(key).isPresent();
            RegistryKey<Biome> style = own ? key : BiomeKeys.PLAINS;
            String wood = VillageBiomes.woodFor(style).map(Object::toString).orElse("");
            String nursery = VillageBiomes.forestSpeciesFor(style).map(Object::toString).orElse("").replace(",", "/");
            var fill = MineFill.fillFor(biome);

            if (VillageBiomes.paletteFor(style).isEmpty() || nursery.isEmpty() || wood.isEmpty()) {
                gaps.add(name + " (estilo " + style.getValue().getPath() + " sem paleta, madeira ou viveiro)");
            }

            if (fill.getBlock() instanceof FallingBlock) {
                gaps.add(name + " (enchimento da mina cai: " + fill.getBlock() + ")");
            }

            rows.add(String.join(",", name, village, style.getValue().getPath(), String.valueOf(own),
                    wood, nursery, Registries.BLOCK.getId(fill.getBlock()).getPath(),
                    String.valueOf(LotGrowth.clearsTrees(biome))));
        }

        Path csv = context.getWorld().getServer().getRunDirectory().resolve("biome-coverage.csv");

        try {
            Files.write(csv, rows, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new AssertionError("não deu para gravar " + csv, failure);
        }

        context.assertTrue(rows.size() > 50, "o registro devolveu só " + (rows.size() - 1) + " biomas do Overworld");
        context.assertTrue(gaps.isEmpty(), "biomas sem resposta: " + gaps);
        context.complete();
    }
}
