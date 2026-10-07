package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A obra longe da rua ganha caminho até a borda do lote, e não dentro dele (ADR-039 D2). */
public class WorkPathGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_hours", tickLimit = 20)
    public void aPathIsPavedFromTheRoadToTheEdgeOfTheLot(TestContext context) {
        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 8; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        BlockPos road = new BlockPos(1, 1, 4);
        context.setBlockState(road, Blocks.DIRT_PATH.getDefaultState());

        BlockPos absoluteRoad = context.getAbsolutePos(road);
        // O lote: 4x4 de x 8 a 11 e z 3 a 6, com o piso no y 2.
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(8, 2, 3)));
        UUID colonyId = UUID.randomUUID();
        Colony colony = Colony.create(colonyId, origin);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture fixture = ColonyFixture.create().owning(colony);

        SweepPersistence.restore(new ColonyRoads(colonyId, MinecraftTypeAdapter.toColonyPos(absoluteRoad),
                List.of(ColonyRoads.column(absoluteRoad.getX(), absoluteRoad.getZ()))));

        List<BlueprintBlock> blocks = new ArrayList<>();

        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                blocks.add(new BlueprintBlock(new ColonyPos(x, 0, z), MinecraftTypeAdapter.toResourceId(Blocks.OAK_PLANKS)));
            }
        }

        ConstructionProject project = ConstructionProject.plan(
                colonyId, Blueprint.of(ResourceId.vanilla("test_work_path"), blocks), origin);
        VillageColonyMod.CONSTRUCTIONS.register(project);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        try {
            int first = WorkPath.paveToward(context.getWorld(), colonyId);
            int second = WorkPath.paveToward(context.getWorld(), colonyId);

            context.assertTrue(first == WorkPath.STEPS_PER_CYCLE, "o primeiro ciclo calçou " + first);
            context.assertTrue(second == 2, "do x 2 ao 7 são 6 blocos; o segundo ciclo calçou " + second);

            for (int x = 2; x <= 7; x++) {
                context.expectBlock(Blocks.DIRT_PATH, new BlockPos(x, 1, 4));
            }

            context.expectBlock(Blocks.GRASS_BLOCK, new BlockPos(8, 1, 4));
            context.assertTrue(WorkPath.paveToward(context.getWorld(), colonyId) == 0,
                    "o caminho encostou no lote e continuou calçando");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colonyId);
            fixture.cleanUp();
        }

        context.complete();
    }
}
