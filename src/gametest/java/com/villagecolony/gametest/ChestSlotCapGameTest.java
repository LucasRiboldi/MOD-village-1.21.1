package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** No máximo três compartimentos do mesmo item por baú da colônia (ADR-036 9). */
public class ChestSlotCapGameTest {

    private static ColonyPos chestAt(TestContext context, BlockPos relative) {
        BlockPos absolute = context.getAbsolutePos(relative);

        context.getWorld().setBlockState(absolute, Blocks.CHEST.getDefaultState());

        return MinecraftTypeAdapter.toColonyPos(absolute);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_slot_cap",
            tickLimit = 100)
    public void aColonyChestStopsAtThreeSlotsOfOneItem(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyPos chest = chestAt(context, new BlockPos(1, 1, 1));

        int room = ChestDepositor.freeSpaceFor(world, chest, Items.COBBLESTONE);
        int left = ChestDepositor.deposit(world, chest, Items.COBBLESTONE, 300);

        if (room != 192) {
            throw new AssertionError("espaco medido " + room + ", esperado 3 pilhas (192)");
        }

        if (left != 300 - 192) {
            throw new AssertionError("sobraram " + left + ", esperado " + (300 - 192)
                    + " — o bau abriu mais de 3 compartimentos de pedregulho");
        }

        // O teto é por item: outro item ainda entra.
        if (ChestDepositor.deposit(world, chest, Items.DIRT, 64) != 0) {
            throw new AssertionError("terra nao entrou num bau com 24 compartimentos vazios");
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_slot_cap",
            tickLimit = 100)
    public void aNamedChestHasNoCap(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyPos chest = chestAt(context, new BlockPos(3, 1, 3));

        if (!(world.getBlockEntity(MinecraftTypeAdapter.toBlockPos(chest)) instanceof ChestBlockEntity block)) {
            throw new AssertionError("o bau do cenario nao existe");
        }

        NbtCompound nbt = block.createNbt(world.getRegistryManager());
        nbt.putString("CustomName", Text.Serialization.toJsonString(Text.literal("Meu bau"), world.getRegistryManager()));
        block.read(nbt, world.getRegistryManager());

        if (block.getCustomName() == null) {
            throw new AssertionError("o cenario nao conseguiu nomear o bau");
        }

        int left = ChestDepositor.deposit(world, chest, Items.COBBLESTONE, 300);

        if (left != 0) {
            throw new AssertionError("o bau nomeado recusou " + left + " — nomeado nao tem teto");
        }

        context.complete();
    }
}
