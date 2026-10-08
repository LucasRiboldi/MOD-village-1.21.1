package com.villagecolony.fabric.work;

import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.UUID;

/** O teto por tipo no baú do mineiro: três compartimentos de 64 (ADR-037 C2). */
public class MinerHaulCapGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 1, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_haul_cap")
    public void pastTheCapTheMinerStopsKeepingThatType(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ChestBlockEntity chest = (ChestBlockEntity) context.getBlockEntity(CHEST);

        for (int slot = 0; slot < 3; slot++) {
            chest.setStack(slot, new ItemStack(Items.GRANITE, 64));
        }

        WorkerStorage storage = WorkerStorage.of(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST)));

        MinerHaul.Haul haul = MinerHaul.deposit(
                context.getWorld(), storage,
                List.of(new ItemStack(Items.GRANITE, 1), new ItemStack(Items.COBBLESTONE, 1)),
                context.getAbsolutePos(new BlockPos(4, 1, 4)),
                Items.COBBLESTONE);

        int granite = 0;
        int cobblestone = 0;

        for (int slot = 0; slot < chest.size(); slot++) {
            if (chest.getStack(slot).isOf(Items.GRANITE)) {
                granite += chest.getStack(slot).getCount();
            } else if (chest.getStack(slot).isOf(Items.COBBLESTONE)) {
                cobblestone += chest.getStack(slot).getCount();
            }
        }

        context.assertTrue(granite == MinerHaul.TYPE_CAP, "o granito passou do teto: " + granite);
        context.assertTrue(cobblestone == 1 && haul.wanted() == 1,
                "o pedido nao tem teto e devia ter entrado");
        context.assertTrue(context.getWorld().getEntitiesByClass(
                        ItemEntity.class,
                        new Box(context.getAbsolutePos(BlockPos.ORIGIN)).expand(8),
                        entity -> entity.getStack().isOf(Items.GRANITE)).isEmpty(),
                "o granito excedente virou item no chao");
        context.complete();
    }

    /**
     * Pedra acima do teto não é baú cheio — playtest de 2026-10-08: o mineiro atrás de
     * minério pausava com "the chest that serves him is full" e o baú com espaço.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_haul_cap")
    public void stoneAboveTheCapDoesNotPauseTheMiner(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ChestBlockEntity chest = (ChestBlockEntity) context.getBlockEntity(CHEST);

        for (int slot = 0; slot < 3; slot++) {
            chest.setStack(slot, new ItemStack(Items.COBBLESTONE, 64));
        }

        WorkerStorage storage = WorkerStorage.of(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST)));

        MinerHaul.Haul capped = MinerHaul.deposit(
                context.getWorld(), storage, List.of(new ItemStack(Items.COBBLESTONE, 1)),
                context.getAbsolutePos(new BlockPos(4, 1, 4)), Items.RAW_IRON);

        context.assertTrue(capped.stored() == 0 && !MinerHaul.chestIsFull(capped),
                "pedra acima do teto pausou o mineiro: " + capped);

        // Controle: baú sem um slot livre, e o pedido não cabe — aí é cheio.
        for (int slot = 0; slot < chest.size(); slot++) {
            chest.setStack(slot, new ItemStack(Items.DIRT, 64));
        }

        MinerHaul.Haul full = MinerHaul.deposit(
                context.getWorld(), storage, List.of(new ItemStack(Items.RAW_IRON, 1)),
                context.getAbsolutePos(new BlockPos(4, 1, 4)), Items.RAW_IRON);

        context.assertTrue(MinerHaul.chestIsFull(full), "o baú cheio de verdade não pausou: " + full);

        context.getWorld().getEntitiesByClass(ItemEntity.class,
                new Box(context.getAbsolutePos(BlockPos.ORIGIN)).expand(8), entity -> true)
                .forEach(ItemEntity::discard);
        context.complete();
    }
}
