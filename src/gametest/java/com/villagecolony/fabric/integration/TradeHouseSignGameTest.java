package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.UUID;

/**
 * Regra 392 (H5, item 6, autor 27-11: "só coloque um quadro na frente em cima da porta indicando a profissão como os baús já
 * fazem"): a casa de ofício pronta ganha o quadro com o ícone do ofício na parede de fora, acima da porta; outra casa não.
 */
public class TradeHouseSignGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "trade_house_sign", tickLimit = 20)
    public void theTradeHouseShowsItsProfessionOverTheDoor(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos min = context.getAbsolutePos(new BlockPos(2, 2, 2));
        BlockPos max = min.add(4, 4, 4);
        BlockPos door = min.add(2, 0, 0);

        for (BlockPos at : BlockPos.iterate(min, max)) {
            boolean wall = at.getX() == min.getX() || at.getX() == max.getX() || at.getZ() == min.getZ() || at.getZ() == max.getZ()
                    || at.getY() == max.getY();

            world.setBlockState(at, wall ? Blocks.COBBLESTONE.getDefaultState() : Blocks.AIR.getDefaultState());
        }

        world.setBlockState(door, Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.SOUTH)
                .with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        world.setBlockState(door.up(), Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.SOUTH)
                .with(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        Building trade = new Building(UUID.randomUUID(), UUID.randomUUID(),
                new ResourceId("villagecolony", "colony/plains/majest_lumberjack"),
                MinecraftTypeAdapter.toColonyPos(min), MinecraftTypeAdapter.toColonyPos(max), true);
        Building other = new Building(UUID.randomUUID(), UUID.randomUUID(), ResourceId.vanilla("village/plains/houses/plains_small_house_1"),
                MinecraftTypeAdapter.toColonyPos(min), MinecraftTypeAdapter.toColonyPos(max), true);
        BlockPos spot = door.up(2).north();

        context.assertFalse(TradeHouseSign.mark(world, other), "Regra 392: a casa comum ganhou quadro de ofício");
        context.assertTrue(TradeHouseSign.mark(world, trade), "Regra 392: a casa do lenhador não ganhou o quadro");

        List<ItemFrameEntity> frames = world.getEntitiesByClass(ItemFrameEntity.class, new Box(spot), frame -> true);

        context.assertTrue(frames.size() == 1 && frames.getFirst().getHeldItemStack().isOf(Items.IRON_AXE),
                "Regra 392: o quadro com o machado não ficou acima da porta, do lado de fora: " + frames);
        context.assertFalse(TradeHouseSign.mark(world, trade), "Regra 392: a casa ganhou um segundo quadro");

        frames.forEach(ItemFrameEntity::discard);
        context.complete();
    }
}
