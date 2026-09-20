package com.villagecolony.gametest;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.Set;

/** Contrato do blueprint modificado da casa grande da fundacao. */
public class BigHouseModBlueprintGameTest implements FabricGameTest {

    private static final ResourceId BIG_HOUSE_MOD =
            new ResourceId("villagecolony", "houses/big_house_mod");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "bighousemod_blueprint")
    public void theModBlueprintContainsEightBedsAndEightChests(TestContext context) {
        Blueprint blueprint = StructureBlueprintReader.read(
                        context.getWorld(), BIG_HOUSE_MOD)
                .orElseThrow(() -> new AssertionError("BigHouseMOD nao foi encontrada"));

        long beds = blueprint.blocks().stream()
                .filter(block -> block.block().equals(ResourceId.vanilla("white_bed")))
                .count();
        long chests = blueprint.blocks().stream()
                .filter(block -> block.block().equals(ResourceId.vanilla("chest")))
                .count();

        context.assertTrue(blueprint.size().equals(new ColonyPos(7, 11, 11)),
                "dimensao inesperada: " + blueprint.size());
        context.assertTrue(beds == 8,
                "BigHouseMOD tem " + beds + " pes de cama, esperado 8");
        context.assertTrue(chests == 8,
                "BigHouseMOD tem " + chests + " baus, esperado 8");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "bighousemod_blueprint")
    public void theModBlueprintHasNoVanillaFurnitureOrGeneratorScaffolding(
            TestContext context) {
        Blueprint blueprint = StructureBlueprintReader.read(
                        context.getWorld(), BIG_HOUSE_MOD)
                .orElseThrow(() -> new AssertionError("BigHouseMOD nao foi encontrada"));

        Set<ResourceId> forbidden = Set.of(
                ResourceId.vanilla("wall_torch"),
                ResourceId.vanilla("torch"),
                ResourceId.vanilla("lantern"),
                ResourceId.vanilla("jigsaw"),
                ResourceId.vanilla("structure_block"),
                ResourceId.vanilla("structure_void"));

        for (BlueprintBlock block : blueprint.blocks()) {
            context.assertFalse(forbidden.contains(block.block()),
                    "moveis/andaimes vanilla entraram no blueprint: " + block.block());
        }

        context.assertTrue(blueprint.blocks().stream().noneMatch(
                        block -> block.offset().equals(new ColonyPos(3, 1, 5))),
                "o corredor central nao ficou livre");
        context.assertFalse(blueprint.materials().containsKey(
                        ResourceId.vanilla("air")),
                "ar nao pode virar material da casa");
        context.complete();
    }
}
