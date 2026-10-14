package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
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

import java.util.UUID;

/** A cada ciclo, o baú de profissão quase cheio espalha o excedente para os baús sem profissão (autor, 2026-10-13). */
public final class ProfessionChestSpreadGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "profession_chest_spread", tickLimit = 20)
    public void aTightProfessionChestSpreadsToThePublicChestEveryCycle(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos ownAt = context.getAbsolutePos(new BlockPos(2, 2, 2));
        BlockPos publicAt = context.getAbsolutePos(new BlockPos(4, 2, 2));
        ColonyPos own = MinecraftTypeAdapter.toColonyPos(ownAt);
        Colony colony = Colony.create(UUID.randomUUID(), own);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID miner = UUID.randomUUID();

        colony.measure(new VillageBounds(ownAt.getX() - 4, ownAt.getY() - 2, ownAt.getZ() - 4,
                ownAt.getX() + 4, ownAt.getY() + 4, ownAt.getZ() + 4));
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(miner, colony.id());
        owned.owning(miner);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(miner, own));
        world.setBlockState(ownAt, Blocks.CHEST.getDefaultState());
        world.setBlockState(publicAt, Blocks.CHEST.getDefaultState());

        ChestBlockEntity source = (ChestBlockEntity) world.getBlockEntity(ownAt);
        ChestBlockEntity community = (ChestBlockEntity) world.getBlockEntity(publicAt);

        // Quase cheio: só os últimos slots livres ainda não sobram — abaixo do limiar TIGHT.
        int filled = source.size() - 2;

        for (int slot = 0; slot < filled; slot++) {
            source.setStack(slot, new ItemStack(Items.COBBLESTONE));
        }

        try {
            int before = count(community, Items.COBBLESTONE);

            int moved = ProfessionChestOverflow.spread(world, colony);

            context.assertTrue(moved > 0, "o baú de profissão quase cheio não espalhou nada");
            context.assertTrue(count(community, Items.COBBLESTONE) == before + moved,
                    "o excedente não chegou ao baú sem profissão");

            // Um baú folgado (bem mais que o limiar de slots livres) não espalha.
            source.clear();
            source.setStack(0, new ItemStack(Items.COBBLESTONE));

            context.assertTrue(ProfessionChestOverflow.spread(world, colony) == 0,
                    "um baú de profissão folgado não devia espalhar");
        } finally {
            source.clear();
            community.clear();
            world.setBlockState(ownAt, Blocks.AIR.getDefaultState());
            world.setBlockState(publicAt, Blocks.AIR.getDefaultState());
            owned.cleanUp();
        }

        context.complete();
    }

    private static int count(ChestBlockEntity chest, net.minecraft.item.Item item) {
        int amount = 0;

        for (int slot = 0; slot < chest.size(); slot++) {
            if (chest.getStack(slot).isOf(item)) {
                amount += chest.getStack(slot).getCount();
            }
        }

        return amount;
    }
}
