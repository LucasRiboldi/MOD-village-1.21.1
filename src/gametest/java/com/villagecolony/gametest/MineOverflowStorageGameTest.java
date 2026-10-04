package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineOverflowStorage;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/** Garante que o transbordo de emergência só ocupa um salão já aberto. */
public final class MineOverflowStorageGameTest implements FabricGameTest {
    private static final BlockPos ENTRY = new BlockPos(5, 18, 5);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_overflow_storage")
    public void completedMineHallReceivesOneEmergencyCommunityChest(TestContext context) {
        Colony colony = Colony.create(UUID.randomUUID(), position(context, new BlockPos(6, 2, 6)));
        VillageColonyMod.COLONIES.register(colony);

        MineShaft shaft = MineShaft.from(position(context, ENTRY), Side.EAST);
        Mine mine = Mine.restore(colony.id(), shaft, MineShaft.SHARED_BLOCKS);
        VillageColonyMod.MINES.restore(mine);

        ColonyPos expected = shaft.positionAt(MineShaft.SHARED_BLOCKS - MineShaft.HEADROOM);
        BlockPos expectedBlock = MinecraftTypeAdapter.toBlockPos(expected);
        context.getWorld().setBlockState(expectedBlock, Blocks.AIR.getDefaultState());
        context.getWorld().setBlockState(expectedBlock.down(), Blocks.STONE.getDefaultState());

        Optional<ColonyPos> storage = MineOverflowStorage.ensure(context.getWorld(), colony.id());

        context.assertTrue(storage.isPresent() && storage.get().equals(expected),
                "o salão completo não escolheu a posição determinística do baú");
        context.assertTrue(context.getWorld().getBlockState(expectedBlock).isOf(Blocks.CHEST),
                "o baú comunitário de emergência não foi colocado");
        context.complete();
    }

    private static ColonyPos position(TestContext context, BlockPos relative) {
        return MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(relative));
    }
}
