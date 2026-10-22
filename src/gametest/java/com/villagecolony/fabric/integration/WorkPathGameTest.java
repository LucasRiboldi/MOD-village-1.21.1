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

/**
 * A obra longe da rua ganha o corredor que sai da porta — ADR-039 D2 e a regra do autor de 2026-10-22: em
 * linha reta na direção da porta, curva em ângulo reto, e parada no primeiro caminho que encostar. A regra
 * antiga ia da rua até a borda do lote, trocando de eixo a cada passo.
 */
public class WorkPathGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_hours", tickLimit = 20)
    public void theCorridorLeavesTheDoorAndTurnsTowardTheRoad(TestContext context) {
        // Tudo dentro da arena de 8x8: na borda dela há barreira, e a barreira recusa o calçamento.
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK.getDefaultState());
            }

            context.setBlockState(new BlockPos(x, 1, 0), Blocks.DIRT_PATH.getDefaultState());
        }

        BlockPos absoluteRoad = context.getAbsolutePos(new BlockPos(0, 1, 0));
        List<Long> columns = new ArrayList<>();

        for (int x = 0; x <= 7; x++) {
            BlockPos road = context.getAbsolutePos(new BlockPos(x, 1, 0));
            columns.add(ColonyRoads.column(road.getX(), road.getZ()));
        }

        // O lote: 3x3 de x 5 a 7 e z 3 a 5, com o piso no y 2 e a porta no meio da parede oeste.
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(5, 2, 3)));
        UUID colonyId = UUID.randomUUID();
        Colony colony = Colony.create(colonyId, origin);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture fixture = ColonyFixture.create().owning(colony);

        SweepPersistence.restore(new ColonyRoads(colonyId, MinecraftTypeAdapter.toColonyPos(absoluteRoad), columns));

        List<BlueprintBlock> blocks = new ArrayList<>();

        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                blocks.add(new BlueprintBlock(new ColonyPos(x, 1, z), MinecraftTypeAdapter.toResourceId(
                        x == 0 && z == 1 ? Blocks.OAK_DOOR : Blocks.OAK_PLANKS)));
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
            context.assertTrue(second == 1, "o corredor tem 5 blocos; o segundo ciclo calçou " + second);

            // Da porta (x 5, z 4) para oeste: x 4 e x 3; curva para o norte: z 3, 2 e 1, encostando na rua.
            for (BlockPos cell : List.of(new BlockPos(4, 1, 4), new BlockPos(3, 1, 4), new BlockPos(3, 1, 3),
                    new BlockPos(3, 1, 2), new BlockPos(3, 1, 1))) {
                context.expectBlock(Blocks.DIRT_PATH, cell);
            }

            for (BlockPos verge : List.of(new BlockPos(4, 1, 3), new BlockPos(2, 1, 4), new BlockPos(4, 1, 1),
                    new BlockPos(2, 1, 2))) {
                context.expectBlock(Blocks.GRASS_BLOCK, verge);
            }

            context.assertTrue(WorkPath.paveToward(context.getWorld(), colonyId) == 0,
                    "o corredor já encostava na rua e continuou calçando");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colonyId);
            fixture.cleanUp();
        }

        context.complete();
    }
}
