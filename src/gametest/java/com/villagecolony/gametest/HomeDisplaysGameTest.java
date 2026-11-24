package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestMarker;
import com.villagecolony.fabric.integration.HomeDisplays;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;

import java.util.List;
import java.util.UUID;

public class HomeDisplaysGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "home_displays")
    public void oneExteriorSignFollowsBothAssignedBeds(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos min = context.getAbsolutePos(new BlockPos(1, 1, 2));
        BlockPos max = min.add(4, 3, 4);
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            if (pos.getY() == min.getY() || pos.getZ() == min.getZ()) {
                world.setBlockState(pos, Blocks.STONE.getDefaultState());
            }
        }
        BlockPos door = min.add(2, 1, 0);
        var lower = Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.SOUTH);
        world.setBlockState(door, lower);
        world.setBlockState(door.up(), lower.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        BlockPos firstBed = min.add(1, 1, 3);
        BlockPos secondBed = min.add(3, 1, 3);
        UUID colony = UUID.randomUUID();
        Building house = new Building(UUID.randomUUID(), colony, ResourceId.vanilla("test_home"),
                MinecraftTypeAdapter.toColonyPos(min), MinecraftTypeAdapter.toColonyPos(max));
        VillageColonyMod.BUILDINGS.register(house);

        var first = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 2, 4));
        var second = context.spawnEntity(EntityType.VILLAGER, new BlockPos(4, 2, 4));
        var visitors = List.of(Worker.register(first.getUuid(), colony), Worker.register(second.getUuid(), colony));
        first.setCustomName(Text.literal("Ana"));
        second.setCustomName(Text.literal("Beto"));
        for (BlockPos bed : List.of(firstBed, secondBed)) {
            var foot = Blocks.RED_BED.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH);
            world.setBlockState(bed, foot.with(Properties.BED_PART, BedPart.HEAD));
            world.setBlockState(bed.north(), foot.with(Properties.BED_PART, BedPart.FOOT));
        }
        first.getBrain().remember(MemoryModuleType.HOME, GlobalPos.create(world.getRegistryKey(), firstBed));
        second.getBrain().remember(MemoryModuleType.HOME, GlobalPos.create(world.getRegistryKey(), secondBed));

        Box facade = new Box(door.north()).expand(2);
        try {
            HomeDisplays.reconcile(world, colony, door, visitors);
            HomeDisplays.reconcile(world, colony, door, visitors);
            var signs = world.getEntitiesByClass(TextDisplayEntity.class, facade, sign -> true);
            context.assertTrue(signs.size() == 1, "a casa deve ter uma única placa");
            var sign = signs.getFirst();
            context.assertTrue(sign.getBlockPos().getZ() < door.getZ(), "a placa precisa ficar do lado de fora");
            context.assertTrue(sign.getText().getString().contains("Ana") && sign.getText().getString().contains("Beto"),
                    "a placa precisa listar os dois moradores");

            BlockPos chest = min.add(4, 1, 1);
            world.setBlockState(chest, Blocks.CHEST.getDefaultState());
            world.setBlockState(chest.west(), Blocks.STONE.getDefaultState());
            context.assertTrue(ChestMarker.markAt(world, chest, ProfessionType.MINER),
                    "a casa de teste precisa de uma placa antiga na face lateral");
            world.setBlockState(chest.west(), Blocks.AIR.getDefaultState());
            context.assertTrue(ChestMarker.markHomeAt(world, chest, ProfessionType.MINER, second, colony),
                    "a placa antiga deveria girar para o centro quando o lado livre aparece");
            var frames = world.getEntitiesByClass(ItemFrameEntity.class, new Box(chest).expand(1), frame -> true);
            context.assertTrue(frames.size() == 1 && frames.getFirst().getHorizontalFacing() == Direction.WEST,
                    "o quadro interno precisa olhar para o centro da casa pequena");

            first.getBrain().forget(MemoryModuleType.HOME);
            HomeDisplays.reconcile(world, colony, door, visitors);
            context.assertTrue(!sign.getText().getString().contains("Ana") && sign.getText().getString().contains("Beto"),
                    "a troca de cama precisa atualizar a placa existente");
            second.getBrain().forget(MemoryModuleType.HOME);
            HomeDisplays.reconcile(world, colony, door, visitors);
            context.assertTrue(sign.isRemoved(), "uma casa sem moradores não deve exibir a lista antiga");
        } finally {
            VillageColonyMod.BUILDINGS.removeOfColony(colony);
        }
        context.complete();
    }
}
