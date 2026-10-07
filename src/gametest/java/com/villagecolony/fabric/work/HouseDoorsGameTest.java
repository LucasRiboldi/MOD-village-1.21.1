package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/** Quem dorme fecha as portas a até {@link HouseDoors#RADIUS} da cama, e só as dela (ADR-037 V2). */
public class HouseDoorsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_hours", tickLimit = 20)
    public void theSleeperClosesOnlyTheDoorsOfTheHouse(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos bed = new BlockPos(1, 1, 1);
        BlockPos near = bed.east(HouseDoors.RADIUS);
        BlockPos far = bed.south(HouseDoors.RADIUS + 1);

        openDoorAt(context, near);
        openDoorAt(context, far);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        int closed = HouseDoors.closeOnce(world, villager, context.getAbsolutePos(bed));

        context.assertTrue(closed == 1, "esperava fechar 1 porta, fechou " + closed);
        context.assertTrue(!isOpen(context, near), "a porta a " + HouseDoors.RADIUS + " da cama ficou aberta");
        context.assertTrue(isOpen(context, far), "fechou a porta fora da casa, a " + (HouseDoors.RADIUS + 1));

        openDoorAt(context, near);
        context.assertTrue(HouseDoors.closeOnce(world, villager, context.getAbsolutePos(bed)) == 0,
                "na mesma noite, fechou a porta de novo");
        context.complete();
    }

    private static void openDoorAt(TestContext context, BlockPos lower) {
        BlockState door = Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.OPEN, true);

        context.setBlockState(lower, door.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        context.setBlockState(lower.up(), door.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static boolean isOpen(TestContext context, BlockPos lower) {
        return context.getBlockState(lower).get(DoorBlock.OPEN);
    }
}
