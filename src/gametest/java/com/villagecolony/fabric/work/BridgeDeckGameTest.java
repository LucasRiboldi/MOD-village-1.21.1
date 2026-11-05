package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** A ponte do construtor — Regra 174: tábua e cerca dos baús, e sem tábua nada é escrito. */
public class BridgeDeckGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bridge_deck", tickLimit = 20)
    public void theDeckTakesPlanksAndFencesFromTheChests(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos floor = context.getAbsolutePos(new BlockPos(3, 126, 3));
        BlockPos chestAt = floor.add(0, 4, 0);
        ColonyFixture fixture = ColonyFixture.colonyAtWorld(floor);
        var worker = fixture.emptyHandedWorker(context, ProfessionType.BUILDER, new BlockPos(2, 2, 2));

        try {
            for (BlockPos at : BlockPos.iterate(floor.add(-1, 0, -1), floor.add(1, 0, 1))) {
                world.setBlockState(at, Blocks.WATER.getDefaultState(), 18);
            }

            world.setBlockState(chestAt, Blocks.CHEST.getDefaultState(), 18);
            VillageColonyMod.STORAGES.register(WorkerStorage.of(worker.villagerId(), MinecraftTypeAdapter.toColonyPos(chestAt)));

            PathStep step = new PathStep(floor.getX(), floor.getY(), floor.getZ(), Side.EAST, 0, 0);
            List<PathStep> steps = List.of(step);

            context.assertFalse(BridgeDeck.lay(world, fixture.colony().id(), worker.villagerId(), step, steps), "Regra 174: sem tábua a ponte não espera");
            context.assertTrue(world.getFluidState(floor).isEmpty() == false, "Regra 174: sem tábua, escreveu no mundo");

            if (world.getBlockEntity(chestAt) instanceof ChestBlockEntity chest) {
                chest.setStack(0, new ItemStack(Items.OAK_PLANKS, 16));
                chest.setStack(1, new ItemStack(Items.OAK_FENCE, 16));
            }

            context.assertTrue(BridgeDeck.isOverWater(world, floor), "Regra 174: a água não contou como travessia");
            context.assertTrue(BridgeDeck.waterColumns(world, steps) == 1, "Regra 174: a conta de colunas de água errou");
            context.assertTrue(BridgeDeck.lay(world, fixture.colony().id(), worker.villagerId(), step, steps), "Regra 174: com tábua a ponte não saiu");
            context.assertTrue(world.getBlockState(floor).isOf(Blocks.OAK_PLANKS), "Regra 174: o eixo não virou tábua");

            BlockPos side = new BlockPos(step.sideX(1), step.floorY(), step.sideZ(1));

            context.assertTrue(world.getBlockState(side).isOf(Blocks.OAK_PLANKS), "Regra 174: o lado sobre a água não virou tábua");
            context.assertTrue(world.getBlockState(side.up()).isOf(Blocks.OAK_FENCE), "Regra 174: a beira ficou sem cerca");
            context.assertTrue(world.getBlockState(floor.up()).isAir(), "Regra 174: o eixo ficou fechado");
            context.assertTrue(BridgeDeck.MAX_SPAN == 64, "Regra 174: a travessia vai até 64 colunas");
        } finally {
            VillageColonyMod.STORAGES.remove(worker.villagerId());
            fixture.cleanUp();

            for (BlockPos at : BlockPos.iterate(floor.add(-1, 0, -1), floor.add(1, 4, 1))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bridge_deck_land", tickLimit = 20)
    public void dryGroundIsNotACrossing(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos floor = context.getAbsolutePos(new BlockPos(3, 134, 3));

        world.setBlockState(floor, Blocks.GRASS_BLOCK.getDefaultState(), 18);

        try {
            context.assertFalse(BridgeDeck.isOverWater(world, floor), "Regra 174: chão seco contou como travessia");
        } finally {
            world.setBlockState(floor, Blocks.AIR.getDefaultState(), 18);
        }

        context.complete();
    }
}
