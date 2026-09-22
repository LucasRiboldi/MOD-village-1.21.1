package com.villagecolony.gametest;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.work.MaterialChoice;
import com.villagecolony.fabric.work.PottedPlant;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Inventario regeneravel da regra de suprimento por bioma.
 *
 * <p>Le os NBTs Vanilla que {@link VillageStructures} realmente autoriza
 * e pergunta a {@link BiomeConstructionSupply} pelo mesmo caminho usado
 * pelo construtor. Cada requisito termina em rota local, fornecimento
 * automatico ou formacao no terreno; um quarto estado seria uma obra que
 * pode esperar para sempre e, por isso, reprova o teste.
 */
public class ConstructionSupplyAuditGameTest implements FabricGameTest {

    private static final Map<String, RegistryKey<Biome>> BIOMES = Map.of(
            "plains", BiomeKeys.PLAINS,
            "desert", BiomeKeys.DESERT,
            "savanna", BiomeKeys.SAVANNA,
            "taiga", BiomeKeys.TAIGA,
            "snowy", BiomeKeys.SNOWY_PLAINS);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "construction_supply",
            tickLimit = 800)
    public void everyBuildableVillageBlockHasABiomeSupplyDecision(TestContext context) {
        ServerWorld world = context.getWorld();
        StringBuilder report = new StringBuilder("# Auditoria de suprimento das estruturas Vanilla\n\n");
        report.append("Gerado pelo GameTest contra os NBTs e receitas carregados pelo Minecraft 1.21.1. ")
                .append("Escopo: as estruturas que VillageStructures permite construir, nao todas as ")
                .append("estruturas decorativas ou de ruina do jogo.\n\n");

        int templates = 0;
        int placedBlocks = 0;
        int automaticKinds = 0;

        for (Map.Entry<String, RegistryKey<Biome>> entry : BIOMES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList()) {

            StyleAudit audit = audit(world, entry.getKey(), entry.getValue());
            templates += audit.templates().size();
            placedBlocks += audit.placedBlocks();
            automaticKinds += audit.automatic().size();
            append(report, audit);

            context.assertTrue(
                    audit.unclassified().isEmpty(),
                    entry.getKey() + " possui requisito sem decisao de suprimento: "
                            + audit.unclassified());
        }

        report.append("## Totais\n\n")
                .append("- ").append(templates).append(" plantas autorizadas\n")
                .append("- ").append(placedBlocks).append(" colocacoes de bloco somando uma execucao de cada planta\n")
                .append("- ").append(automaticKinds).append(" entradas de fornecimento automatico por estilo\n");

        writeReport(report);
        context.complete();
    }

    private static StyleAudit audit(
            ServerWorld world, String style, RegistryKey<Biome> biome) {

        Map<String, Integer> local = new TreeMap<>();
        Map<String, Integer> automatic = new TreeMap<>();
        Map<String, Integer> formed = new TreeMap<>();
        List<String> unclassified = new ArrayList<>();
        List<ResourceId> templates = VillageStructures.buildableFor(style).stream()
                .sorted(Comparator.comparing(ResourceId::path))
                .toList();
        int placedBlocks = 0;

        for (ResourceId template : templates) {
            Optional<Blueprint> blueprint = StructureBlueprintReader.read(world, template);

            if (blueprint.isEmpty()) {
                unclassified.add(template.path() + " (NBT ausente)");
                continue;
            }

            for (BlueprintBlock entry : blueprint.get().blocks()) {
                placedBlocks++;
                classify(world, biome, entry.block(), local, automatic, formed, unclassified);
            }
        }

        return new StyleAudit(
                style, templates, placedBlocks, local, automatic, formed, List.copyOf(unclassified));
    }

    private static void classify(
            ServerWorld world,
            RegistryKey<Biome> biome,
            ResourceId id,
            Map<String, Integer> local,
            Map<String, Integer> automatic,
            Map<String, Integer> formed,
            List<String> unclassified) {

        Optional<Block> block = MinecraftTypeAdapter.toBlock(id);

        if (block.isEmpty()) {
            unclassified.add(id.path() + " (bloco nao registrado)");
            return;
        }

        if (PottedPlant.isPotted(block.get())) {
            classifyItem(world, biome, PottedPlant.pot(), local, automatic);
            PottedPlant.plantOf(block.get()).ifPresent(item ->
                    classifyItem(world, biome, item, local, automatic));
            return;
        }

        if (BuilderWork.isShapedFromTheGround(block.get().getDefaultState())) {
            formed.merge(id.path(), 1, Integer::sum);
            return;
        }

        List<Item> choices = MaterialChoice.forBlock(block.get());
        Optional<Item> localChoice = choices.stream()
                .filter(item -> BiomeConstructionSupply.hasRouteInBiome(world, biome, item))
                .findFirst();

        if (localChoice.isPresent()) {
            local.merge(describe(id.path(), localChoice.get()), 1, Integer::sum);
            return;
        }

        if (choices.isEmpty()) {
            unclassified.add(id.path() + " (sem item de construcao)");
            return;
        }

        automatic.merge(itemId(choices.getFirst()), 1, Integer::sum);
    }

    private static void classifyItem(
            ServerWorld world,
            RegistryKey<Biome> biome,
            Item item,
            Map<String, Integer> local,
            Map<String, Integer> automatic) {

        if (BiomeConstructionSupply.hasRouteInBiome(world, biome, item)) {
            local.merge(itemId(item), 1, Integer::sum);
        } else {
            automatic.merge(itemId(item), 1, Integer::sum);
        }
    }

    private static String describe(String requestedBlock, Item supplied) {
        String suppliedId = itemId(supplied);

        return requestedBlock.equals(suppliedId)
                ? requestedBlock
                : requestedBlock + " -> " + suppliedId;
    }

    private static String itemId(Item item) {
        return Registries.ITEM.getId(item).getPath();
    }

    private static void append(StringBuilder report, StyleAudit audit) {
        report.append("## ").append(audit.style()).append("\n\n")
                .append("Plantas autorizadas: ").append(audit.templates().size()).append(". ")
                .append("Colocacoes: ").append(audit.placedBlocks()).append(".\n\n")
                .append("### Estruturas\n\n");

        for (ResourceId template : audit.templates()) {
            report.append("- `").append(template.path()).append("`\n");
        }

        appendRequirements(report, "Rota local", audit.local());
        appendRequirements(report, "Fornecimento automatico", audit.automatic());
        appendRequirements(report, "Formado no local", audit.formed());
        report.append('\n');
    }

    private static void appendRequirements(
            StringBuilder report, String title, Map<String, Integer> requirements) {

        report.append("\n### ").append(title).append("\n\n");

        if (requirements.isEmpty()) {
            report.append("Nenhum.\n");
            return;
        }

        for (Map.Entry<String, Integer> requirement : requirements.entrySet()) {
            report.append("- `").append(requirement.getKey()).append("`: ")
                    .append(requirement.getValue()).append("\n");
        }
    }

    private static void writeReport(StringBuilder report) {
        Path where = Path.of("construction-supply-audit.md").toAbsolutePath();

        try {
            Files.writeString(where, report.toString());
            com.villagecolony.VillageColonyMod.LOGGER.info(
                    "[construction-supply] audit written to {}", where);
        } catch (Exception unwritable) {
            throw new IllegalStateException("could not write construction supply audit", unwritable);
        }
    }

    private record StyleAudit(
            String style,
            List<ResourceId> templates,
            int placedBlocks,
            Map<String, Integer> local,
            Map<String, Integer> automatic,
            Map<String, Integer> formed,
            List<String> unclassified) {
    }
}
