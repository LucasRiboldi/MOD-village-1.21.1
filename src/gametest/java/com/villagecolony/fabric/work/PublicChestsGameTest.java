package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** Pedra crua além do teto do baú do mineiro vai para o baú público da vila (autor, 2026-10-12). */
public class PublicChestsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "public_chests", tickLimit = 20)
    public void stoneOverTheMinerCapGoesToThePublicChest(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos ownAt = context.getAbsolutePos(new BlockPos(1, 2, 1));
        BlockPos publicAt = context.getAbsolutePos(new BlockPos(3, 2, 1));
        ColonyPos own = MinecraftTypeAdapter.toColonyPos(ownAt);
        ColonyPos shared = MinecraftTypeAdapter.toColonyPos(publicAt);
        Colony colony = Colony.create(UUID.randomUUID(), own);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID miner = UUID.randomUUID();

        colony.measure(new VillageBounds(ownAt.getX() - 4, ownAt.getY() - 2, ownAt.getZ() - 4,
                ownAt.getX() + 4, ownAt.getY() + 4, ownAt.getZ() + 4));
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(miner, colony.id());
        owned.owning(miner);
        WorkerStorage storage = WorkerStorage.of(miner, own);
        VillageColonyMod.STORAGES.register(storage);
        world.setBlockState(ownAt, Blocks.CHEST.getDefaultState());
        world.setBlockState(publicAt, Blocks.CHEST.getDefaultState());
        ChestDepositor.deposit(world, own, Items.COBBLESTONE, MinerHaul.TYPE_CAP);

        try {
            context.assertTrue(PublicChests.of(world, colony.id(), ownAt).equals(List.of(shared)),
                    "o baú público não é só o baú sem ofício: " + PublicChests.of(world, colony.id(), ownAt));

            MinerHaul.deposit(world, colony.id(), storage, List.of(new ItemStack(Items.COBBLESTONE, 5)),
                    ownAt.up(), Items.COAL);

            context.assertTrue(ColonyChests.countIn(world, List.of(shared), Items.COBBLESTONE) == 5,
                    "a pedra além do teto não foi para o baú público");
            context.assertTrue(ColonyChests.countIn(world, List.of(own), Items.COBBLESTONE) == MinerHaul.TYPE_CAP,
                    "o baú do mineiro passou do teto por tipo");
        } finally {
            ((Inventory) world.getBlockEntity(ownAt)).clear();
            ((Inventory) world.getBlockEntity(publicAt)).clear();
            world.setBlockState(ownAt, Blocks.AIR.getDefaultState());
            world.setBlockState(publicAt, Blocks.AIR.getDefaultState());
            owned.cleanUp();
        }

        context.complete();
    }
}
