package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/** O mesmo curral amplo em todos os biomas, com piso e iluminação próprios (Regra 72). */
public final class PenModelGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_model", tickLimit = 20)
    public void theLargePenHasWaterCropsGrassGateAndPostTorches(TestContext context) {
        var plan = StructureBlueprintReader.read(context.getWorld(), PenPlans.MODEL).orElseThrow();
        context.assertTrue(plan.size().x() == 15 && plan.size().z() == 15,
                "o curral comum deveria ter piso de 15 por 15");
        context.assertTrue(plan.hasStreetLayer() && plan.streetLayer() == 0,
                "o piso do curral deve estar no nível da rua");
        long grass = plan.blocks().stream().filter(block -> block.block().path().equals("grass_block")).count();
        long water = plan.blocks().stream().filter(block -> block.block().path().equals("water")).count();
        long farmland = plan.blocks().stream().filter(block -> block.block().path().equals("farmland")).count();
        long crops = plan.blocks().stream().filter(block -> block.block().path().equals("wheat")).count();
        long gates = plan.blocks().stream().filter(block -> block.block().path().equals("oak_fence_gate")).count();
        long torches = plan.blocks().stream().filter(block -> block.block().path().equals("torch")).count();
        boolean rubble = plan.blocks().stream().anyMatch(block -> block.offset().y() > 0
                && (block.block().path().equals("dirt") || block.block().path().equals("stone")));
        context.assertTrue(grass == 212 && water == 1 && farmland == 12 && crops == 12
                        && gates == 4 && torches == 4 && !rubble,
                "água, cultivos, grama, portão, tochas ou interior do curral divergiram");
        for (int turn = 0; turn < 4; turn++) {
            var rotated = plan.rotated(turn);
            var positions = rotated.blocks().stream().filter(block -> block.block().path().equals("oak_fence_gate"))
                    .map(com.villagecolony.core.construction.model.BlueprintBlock::offset).collect(java.util.stream.Collectors.toSet());
            context.assertTrue(positions.equals(java.util.Set.of(
                    new com.villagecolony.core.type.ColonyPos(7, 1, 0), new com.villagecolony.core.type.ColonyPos(14, 1, 7),
                    new com.villagecolony.core.type.ColonyPos(7, 1, 14), new com.villagecolony.core.type.ColonyPos(0, 1, 7))),
                    "rotação perdeu acesso central de um dos quatro lados");
        }
        context.complete();
    }
}
