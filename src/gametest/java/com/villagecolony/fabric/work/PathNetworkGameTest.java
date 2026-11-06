package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.integration.LotSpacing;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** A rede de caminhos no mundo — Regras 123 e 124. */
public class PathNetworkGameTest implements FabricGameTest {

    /** O diário mede blocos realmente assentados, sem contar de novo o piso pronto — ADR-043. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "path_paving_journal", tickLimit = 20)
    public void theJournalCountsOnlyTheSingleBlockActuallyChanged(TestContext context) throws IOException {
        ServerWorld world = context.getWorld();
        BlockPos at = context.getAbsolutePos(BlockPos.ORIGIN).add(3_200_200, 30, 3_200_200);
        UUID colonyId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        Path previousJournal = ActionJournal.file();
        Path journal = Files.createTempFile("village-path-paving-", ".jsonl");
        ActionJournal.flush();
        ActionJournal.writeTo(journal);
        try {
            world.getChunk(at);
            world.setBlockState(at.down(), Blocks.STONE.getDefaultState());
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState());
            PathStep step = new PathStep(at.getX(), at.getY(), at.getZ(), Side.EAST, 0, 0);
            context.assertTrue(PathPaving.layAndRecord(world, colonyId, workerId, step), "primeiro passo recusado");
            context.assertTrue(world.getBlockState(at).isOf(Blocks.DIRT_PATH), "nenhum bloco foi assentado");
            context.assertTrue(PathPaving.layAndRecord(world, colonyId, workerId, step), "piso pronto recusado");
            ActionJournal.flush();
            List<String> actions = Files.readAllLines(journal).stream()
                    .filter(line -> line.contains(workerId.toString().substring(0, 8))).toList();
            context.assertTrue(actions.size() == 1 && actions.getFirst().contains("\"n\":1"),
                    "um bloco alterado deve produzir uma ação de quantidade 1: " + actions);
        } finally {
            ActionJournal.flush();
            ActionJournal.writeTo(previousJournal);
            Files.deleteIfExists(journal);
            world.setBlockState(at, Blocks.AIR.getDefaultState());
            world.setBlockState(at.down(), Blocks.AIR.getDefaultState());
        }
        context.complete();
    }

    /** Índice e retorno de sucesso exigem apoio físico antes da pavimentação — ADR-043. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "path_entry_support", tickLimit = 20)
    public void anUnsupportedFloorIsRejectedWithoutChangingTheWorld(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(3_200_100, 30, 3_200_100);
        UUID colonyId = UUID.randomUUID();
        world.getChunk(base);
        try {
            world.setBlockState(base.down(), Blocks.AIR.getDefaultState());
            world.setBlockState(base, Blocks.GRASS_BLOCK.getDefaultState());
            PathStep step = new PathStep(base.getX(), base.getY(), base.getZ(), Side.EAST, 0, 0);
            context.assertFalse(PathPaving.layStep(world, colonyId, step),
                    "pavimentação sem apoio não pode retornar sucesso");
            context.assertTrue(world.getBlockState(base).isOf(Blocks.GRASS_BLOCK),
                    "pavimentação recusada alterou o piso");
        } finally {
            world.setBlockState(base, Blocks.AIR.getDefaultState());
            PathLinks.clearAll();
        }
        context.complete();
    }

    /** Construção nova nasce a no mínimo três blocos de outra; o caminho pode encostar. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lot_spacing", tickLimit = 20)
    public void aNewLotKeepsThreeBlocksFromAnyConstruction(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(3_100_000, 0, 3_100_000);
        Building house = new Building(UUID.randomUUID(), colonyId, ResourceId.vanilla("test_house"),
                new ColonyPos(base.getX(), 0, base.getZ()), new ColonyPos(base.getX() + 4, 5, base.getZ() + 4));
        ColonyPos lot = new ColonyPos(3, 4, 3);

        VillageColonyMod.BUILDINGS.register(house);

        try {
            context.assertTrue(LotSpacing.tooClose(colonyId, base.getX() + 6, base.getZ(), lot),
                    "um bloco de folga devia ser pouco");
            context.assertTrue(LotSpacing.tooClose(colonyId, base.getX() + 7, base.getZ(), lot),
                    "dois blocos de folga devia ser pouco");
            context.assertFalse(LotSpacing.tooClose(colonyId, base.getX() + 8, base.getZ(), lot),
                    "três blocos de folga deviam bastar");
        } finally {
            VillageColonyMod.BUILDINGS.remove(house.id());
        }

        context.complete();
    }
}
