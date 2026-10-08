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
        // Tudo dentro da arena de 8x8: na borda dela há barreira, e a barreira recusa o calçamento.
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        BlockPos road = new BlockPos(0, 1, 4);
        context.setBlockState(road, Blocks.DIRT_PATH.getDefaultState());

        BlockPos absoluteRoad = context.getAbsolutePos(road);
        // O lote: 2x2 de x 6 a 7 e z 3 a 4, com o piso no y 2.
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(6, 2, 3)));
        UUID colonyId = UUID.randomUUID();
        Colony colony = Colony.create(colonyId, origin);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture fixture = ColonyFixture.create().owning(colony);

        SweepPersistence.restore(new ColonyRoads(colonyId, MinecraftTypeAdapter.toColonyPos(absoluteRoad),
                List.of(ColonyRoads.column(absoluteRoad.getX(), absoluteRoad.getZ()))));

        List<BlueprintBlock> blocks = new ArrayList<>();

        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                // Um acima do piso: a caixa da obra aberta (que a proteção recusa) não cobre a altura do
                // caminho, e só a parada na borda do lote segura o calçamento.
                blocks.add(new BlueprintBlock(new ColonyPos(x, 1, z), MinecraftTypeAdapter.toResourceId(Blocks.OAK_PLANKS)));
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
            context.assertTrue(second == 1, "do x 1 ao 5 são 5 blocos; o segundo ciclo calçou " + second);

            for (int x = 1; x <= 5; x++) {
                context.expectBlock(Blocks.DIRT_PATH, new BlockPos(x, 1, 4));
            }

            context.expectBlock(Blocks.GRASS_BLOCK, new BlockPos(6, 1, 4));
            context.assertTrue(WorkPath.paveToward(context.getWorld(), colonyId) == 0,
                    "o caminho encostou no lote e continuou calçando");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colonyId);
            fixture.cleanUp();
        }

        context.complete();
    }
}
