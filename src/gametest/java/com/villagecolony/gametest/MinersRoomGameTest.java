package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O espaço que mantém o mineiro cavando — 2026-09-25, decisão do autor:
 * "galerias novas sem parar".
 *
 * <p>A meta de pedra passou a ser o guardado mais o que ainda cabe nos baús
 * dos mineiros (ver {@code ColonyGoals.of}). Este teste afirma a medida do
 * lado do mundo: conta o baú do mineiro, e só o dele — o baú do lenhador ao
 * lado não é lugar de pedra.
 */
public class MinersRoomGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miners_room")
    public void onlyTheMinersChestCountsAsRoomForStone(TestContext context) {
        BlockPos minerChest = new BlockPos(1, 2, 1);
        BlockPos lumberjackChest = new BlockPos(3, 2, 1);

        context.setBlockState(minerChest, Blocks.CHEST.getDefaultState());
        context.setBlockState(lumberjackChest, Blocks.CHEST.getDefaultState());

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(minerChest)));
        UUID miner = UUID.randomUUID();
        UUID lumberjack = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony).owning(miner).owning(lumberjack);

        try {
            VillageColonyMod.WORKERS.register(miner, colony.id()).assign(ProfessionType.MINER);
            VillageColonyMod.WORKERS.register(lumberjack, colony.id()).assign(ProfessionType.LUMBERJACK);
            VillageColonyMod.STORAGES.register(WorkerStorage.of(miner, at(context, minerChest)));
            VillageColonyMod.STORAGES.register(WorkerStorage.of(lumberjack, at(context, lumberjackChest)));

            int room = ColonyChests.minersRoom(context.getWorld(), colony.id());

            context.assertTrue(room == 27 * 64,
                    "esperava o baú vazio do mineiro (27 x 64), e o espaço deu " + room);
        } finally {
            owned.cleanUp();
        }

        context.complete();
    }

    private static ColonyPos at(TestContext context, BlockPos relative) {
        return MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(relative));
    }
}
