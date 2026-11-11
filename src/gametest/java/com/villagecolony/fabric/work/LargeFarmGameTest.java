package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** A plantação grande do mod — Regra 241: 16x16, toda terra arada a até 4 blocos de água, pedida com o fazendeiro parado. */
public class LargeFarmGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "large_farm", tickLimit = 20)
    public void everyFarmlandOfTheLargeFarmIsWet(TestContext context) {
        Blueprint farm = StructureBlueprintReader.read(context.getWorld(), LargeFarmPlans.MODEL).orElse(null);

        context.assertTrue(farm != null, "Regra 241: a planta da plantação grande não foi lida");
        context.assertTrue(farm.size().x() == 16 && farm.size().z() == 16, "Regra 241: a plantação não é 16x16: " + farm.size());

        List<BlueprintBlock> water = farm.blocks().stream().filter(block -> block.block().equals(ResourceId.vanilla("water"))).toList();
        List<BlueprintBlock> farmland = farm.blocks().stream().filter(block -> block.block().equals(ResourceId.vanilla("farmland"))).toList();

        context.assertTrue(farmland.size() == 192 && water.size() == 4,
                "Regra 241: o miolo devia ter 192 terras aradas e 4 buracos de água: " + farmland.size() + " e " + water.size());

        for (BlueprintBlock soil : farmland) {
            boolean wet = water.stream().anyMatch(hole -> Math.abs(hole.offset().x() - soil.offset().x()) <= 4
                    && Math.abs(hole.offset().z() - soil.offset().z()) <= 4 && hole.offset().y() == soil.offset().y());

            context.assertTrue(wet, "Regra 241: terra arada seca em " + soil.offset());
        }

        Map<WorkTime.State, Integer> idle = new EnumMap<>(WorkTime.State.class);
        idle.put(WorkTime.State.WORK, 29);
        idle.put(WorkTime.State.WAIT, 71);
        Map<WorkTime.State, Integer> busy = new EnumMap<>(WorkTime.State.class);
        busy.put(WorkTime.State.WORK, 30);
        busy.put(WorkTime.State.WAIT, 70);

        context.assertTrue(LargeFarmPlans.idleEnough(idle), "Regra 241: 71% sem trabalhar não pediu a plantação");
        context.assertFalse(LargeFarmPlans.idleEnough(busy), "Regra 241: 70% sem trabalhar já pediu a plantação");
        context.complete();
    }
}
