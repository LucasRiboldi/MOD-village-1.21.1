package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * O que o contrato 1 do structure_v3 aceita nos modelos reais representativos — ADR-046.
 *
 * <p>{@code ok}: leitura MAE e todas as peças suportadas. {@code payload}: a leitura recusa o NBT.
 * Outro valor: as peças recusadas pelo contrato 1. Mudou o catálogo ou o contrato, mude a tabela.
 */
public final class MaeStructureCatalogGameTest implements FabricGameTest {
    static final Map<String, String> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("minecraft:village/plains/houses/plains_small_house_1", "ok");
        EXPECTED.put("minecraft:village/desert/houses/desert_small_house_1", "ok");
        EXPECTED.put("minecraft:village/savanna/houses/savanna_small_house_1", "ok");
        EXPECTED.put("minecraft:village/taiga/houses/taiga_small_house_1", "ok");
        EXPECTED.put("minecraft:village/snowy/houses/snowy_small_house_1", "ok");
        EXPECTED.put("minecraft:village/plains/houses/plains_masons_house_1", "ok");
        EXPECTED.put("minecraft:village/desert/houses/desert_mason_1", "ok");
        EXPECTED.put("minecraft:village/savanna/houses/savanna_mason_1", "ok");
        EXPECTED.put("minecraft:village/taiga/houses/taiga_masons_house_1", "ok");
        EXPECTED.put("minecraft:village/snowy/houses/snowy_masons_house_1", "ok");
        EXPECTED.put("minecraft:village/plains/houses/plains_tool_smith_1", "ok");
        EXPECTED.put("minecraft:village/plains/houses/plains_animal_pen_1", "ok");
        EXPECTED.put("villagecolony:colony/large_animal_pen", "ok");
        EXPECTED.put("villagecolony:colony/storage_small", "ok");
        for (String style : java.util.List.of("", "plains/", "savanna/", "taiga/", "snowy/", "desert/")) {
            EXPECTED.put("villagecolony:colony/" + style + "storage_majest", "ok");
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_structure_catalog", tickLimit = 20)
    public void representativeModelsMatchTheContractTable(TestContext context) {
        var divergent = new ArrayList<String>();
        EXPECTED.forEach((name, expected) -> {
            String actual = outcome(context, ResourceId.parse(name));
            if (!expected.equals(actual)) divergent.add(name + ": esperado " + expected + ", obtido " + actual);
        });
        context.assertTrue(divergent.isEmpty(), "catálogo divergiu do contrato: " + divergent);
        context.complete();
    }

    private static String outcome(TestContext context, ResourceId id) {
        try {
            var read = StructureBlueprintReader.readForMae(context.getWorld(), id);
            if (read.isEmpty()) return "missing";
            var project = ConstructionProject.plan(UUID.randomUUID(), read.get(), new ColonyPos(0, 100, 0));
            var refused = new TreeSet<String>();
            for (var piece : MaeStructurePieces.ordered(project)) {
                if (!MaeStructureExecution.supported(piece)) {
                    refused.add(Registries.BLOCK.getId(piece.main().getBlock()).getPath());
                }
            }
            return refused.isEmpty() ? "ok" : String.join(",", refused);
        } catch (IllegalArgumentException | IllegalStateException refusedPayload) {
            return "unsafe_structure_payload".equals(refusedPayload.getMessage()) ? "payload" : refusedPayload.getMessage();
        }
    }
}
