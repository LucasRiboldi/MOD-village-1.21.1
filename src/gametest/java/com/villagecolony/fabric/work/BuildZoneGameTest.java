package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;

/** O construtor põe bloco de qualquer lugar da zona da obra (ADR-037 B1). */
public class BuildZoneGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_zone", tickLimit = 20)
    public void theZoneIsTheFootprintPlusFourBlocks(TestContext context) {
        ColonyPos origin = new ColonyPos(100, 64, 200);
        ConstructionProject project = ConstructionProject.plan(UUID.randomUUID(), Blueprint.of(
                ResourceId.vanilla("test_zone"), List.of(
                        new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("stone")),
                        new BlueprintBlock(new ColonyPos(9, 3, 9), ResourceId.vanilla("stone")))), origin);

        context.assertTrue(BuilderApproach.isInsideZone(project, new Vec3d(105.5, 70, 205.5)),
                "o meio da obra não é zona");
        context.assertTrue(BuilderApproach.isInsideZone(project, new Vec3d(96.5, 64, 213.5)),
                "quatro blocos fora da pegada ainda são zona");
        context.assertTrue(!BuilderApproach.isInsideZone(project, new Vec3d(95.5, 64, 205.5)),
                "cinco blocos fora da pegada já não são zona");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_zone", tickLimit = 40)
    public void nobodyIsBuriedByTheNextBlock(TestContext context) {
        BlockPos spot = new BlockPos(2, 1, 2);

        context.assertTrue(!BuilderApproach.someoneStandsIn(context.getWorld(), context.getAbsolutePos(spot)),
                "o lugar vazio foi dado como ocupado");
        context.spawnEntity(EntityType.VILLAGER, spot);
        context.waitAndRun(5, () -> {
            context.assertTrue(BuilderApproach.someoneStandsIn(context.getWorld(), context.getAbsolutePos(spot)),
                    "o aldeão no lugar do bloco não foi visto");
            context.complete();
        });
    }
}
