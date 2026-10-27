package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** O pedreiro recolhe neve com a pá — item 5 de 27-10: a neve natural vira bola de neve no baú. */
public class SnowShovelGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "snow_shovel", tickLimit = 20)
    public void naturalSnowIsShoveledIntoTheColonyChest(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos ground = context.getAbsolutePos(new BlockPos(2, 1, 2));
        BlockPos snow = ground.up();
        BlockPos chestAt = context.getAbsolutePos(new BlockPos(5, 2, 5));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(chestAt));
        UUID keeper = UUID.randomUUID();

        world.setBlockState(ground, Blocks.DIRT.getDefaultState());
        world.setBlockState(snow, Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, 3));
        world.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(keeper, colony.id());
        VillageColonyMod.STORAGES.register(WorkerStorage.of(keeper, MinecraftTypeAdapter.toColonyPos(chestAt)));

        try {
            context.assertTrue(SnowShovel.isNaturalSnow(world, snow), "a camada de neve do chão devia contar");
            context.assertFalse(SnowShovel.buildWantsSnow(colony.id()), "sem obra aberta, ninguém pede neve");

            int stored = SnowShovel.shovelSnowAt(world, colony.id(), snow);

            context.assertTrue(stored > 0, "nenhuma bola de neve foi guardada");
            context.assertTrue(world.getBlockState(snow).isAir(), "a neve devia sair do chão");
            context.assertTrue(ColonyChests.countIn(world, List.of(MinecraftTypeAdapter.toColonyPos(chestAt)),
                    Items.SNOWBALL) == stored, "as bolas de neve não estão no baú da colônia");
        } finally {
            VillageColonyMod.STORAGES.remove(keeper);
            VillageColonyMod.WORKERS.remove(keeper);
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
