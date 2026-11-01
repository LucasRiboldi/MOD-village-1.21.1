package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/** O mesmo curral amplo em todos os biomas, com piso e iluminação próprios (Regra 72). */
public final class PenModelGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_model", tickLimit = 20)
    public void theSharedPenHasCleanFloorGateAndPostTorches(TestContext context) {
        var plan = StructureBlueprintReader.read(context.getWorld(), PenPlans.MODEL).orElseThrow();
        context.assertTrue(plan.size().x() == 15 && plan.size().z() == 15,
                "o curral comum deveria ter piso de 15 por 15");
        context.assertTrue(plan.hasStreetLayer() && plan.streetLayer() == 0,
                "o piso do curral deve estar no nível da rua");
        long grass = plan.blocks().stream().filter(block -> block.block().path().equals("grass_block")).count();
        long gates = plan.blocks().stream().filter(block -> block.block().path().equals("oak_fence_gate")).count();
        long torches = plan.blocks().stream().filter(block -> block.block().path().equals("torch")).count();
        boolean rubble = plan.blocks().stream().anyMatch(block -> block.offset().y() > 0
                && (block.block().path().equals("dirt") || block.block().path().equals("stone")));
        context.assertTrue(grass == 225 && gates == 1 && torches == 4 && !rubble,
                "piso, portão, tochas e interior do curral divergiram");
        context.complete();
    }
}
