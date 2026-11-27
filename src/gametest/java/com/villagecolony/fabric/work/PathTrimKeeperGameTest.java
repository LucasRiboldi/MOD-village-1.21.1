package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.VillageRoad;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Regra 390 (ADR-065 etapa 2, L1 🅲 do jogo da 0.4.150: o caminho virava grama sem ninguém trocar, o do jogador também):
 * só o caminho que a colônia calçou volta ao solo, depois de 2 dias fora de toda linha, e quem troca é o Zelador, ao lado
 * da coluna, um bloco por vez.
 */
public class PathTrimKeeperGameTest implements FabricGameTest {

    private static final int SIZE = 24;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "path_trim_keeper", tickLimit = 20)
    public void onlyTheColonyPathLeavesAndTheKeeperWalksToIt(TestContext context) {
        ServerWorld world = context.getWorld();
        // Longe da bateria: a plataforma não pode invadir a arena vizinha.
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 100, 1)).add(2_850_000, 0, 2_850_000);

        chunks(world, origin, true);

        for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 0, SIZE))) {
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
        }

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(12, 1, 12));
        Colony colony = fixture.colony();
        BlockPos colonyPath = origin.add(20, 0, 20);
        BlockPos playerPath = origin.add(21, 0, 20);
        List<ColonyPos> surplus = List.of(MinecraftTypeAdapter.toColonyPos(colonyPath),
                MinecraftTypeAdapter.toColonyPos(playerPath));
        Worker worker = fixture.emptyHandedWorker(context, ProfessionType.GROUNDSKEEPER, new BlockPos(1, 2, 1));
        int wait = PathTrim.waitDays;

        try {
            world.setBlockState(colonyPath, Blocks.DIRT_PATH.getDefaultState(), 18);
            world.setBlockState(playerPath, Blocks.DIRT_PATH.getDefaultState(), 18);
            ColonySavedData.get(world.getServer()).roadGraphs().markLaid(colony.id(), colonyPath.getX(), colonyPath.getZ());

            PathTrim.offer(colony.id(), surplus);
            context.assertTrue(PathTrim.pass(world, colony) == 0, "Regra 390: o caminho saiu antes dos 2 dias fora da linha");
            context.assertTrue(VillageRoad.isPathBlock(world.getBlockState(colonyPath)), "Regra 390: o caminho da colônia sumiu cedo");

            PathTrim.waitDays = 0;
            PathTrim.offer(colony.id(), surplus);

            VillagerEntity keeper = (VillagerEntity) world.getEntity(worker.villagerId());

            if (keeper == null) {
                throw new AssertionError("o Zelador do teste não existe");
            }

            keeper.setAiDisabled(true);
            keeper.refreshPositionAndAngles(origin.add(3, 1, 3), 0, 0);

            context.assertTrue(PathTrim.work(world, colony, keeper) == -1, "Regra 390: o Zelador longe não foi andar até a coluna");
            context.assertTrue(VillageRoad.isPathBlock(world.getBlockState(colonyPath)),
                    "Regra 390: o caminho mudou com o Zelador longe dele");
            context.assertTrue(WorkTargets.of(keeper.getUuid()).isPresent(), "Regra 390: o Zelador não ganhou para onde andar");

            keeper.refreshPositionAndAngles(colonyPath.add(-1, 1, 0), 0, 0);

            context.assertTrue(PathTrim.work(world, colony, keeper) == 1, "Regra 390: o Zelador ao lado não trocou a coluna");
            context.assertFalse(VillageRoad.isPathBlock(world.getBlockState(colonyPath)),
                    "Regra 390: o caminho da colônia não voltou ao solo");
            context.assertTrue(PathTrim.work(world, colony, keeper) == 0, "Regra 390: o Zelador trocou mais de um bloco");
            context.assertTrue(VillageRoad.isPathBlock(world.getBlockState(playerPath)),
                    "Regra 390: o caminho do jogador voltou ao solo");
        } finally {
            PathTrim.waitDays = wait;
            WorkTargets.clear(worker.villagerId());
            fixture.cleanUp();

            for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 2, SIZE))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }

            chunks(world, origin, false);
        }

        context.complete();
    }

    private static void chunks(ServerWorld world, BlockPos origin, boolean forced) {
        for (int x = origin.getX() >> 4; x <= (origin.getX() + SIZE) >> 4; x++) {
            for (int z = origin.getZ() >> 4; z <= (origin.getZ() + SIZE) >> 4; z++) {
                world.setChunkForced(x, z, forced);
            }
        }
    }
}
