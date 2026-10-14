package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.integration.PlantPatch;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;

/**
 * O fazendeiro colhe flor, grama e cana; o fundidor, gelo e neve — regra do autor,
 * 2026-10-11 ({@link PlantPatch}).
 */
public class PlantPatchGameTest implements FabricGameTest {

    private static final int Y = 70;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_sowing", tickLimit = 20)
    public void plantsIceAndSnowAreFoundAndYieldTheirItem(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos poppy = context.getAbsolutePos(new BlockPos(1, Y, 1));
        BlockPos grass = context.getAbsolutePos(new BlockPos(3, Y, 1));
        BlockPos cane = context.getAbsolutePos(new BlockPos(5, Y, 1));
        BlockPos ice = context.getAbsolutePos(new BlockPos(7, Y, 1));

        try {
            for (BlockPos at : List.of(poppy, grass, cane, ice)) {
                world.setBlockState(at.down(), Blocks.GRASS_BLOCK.getDefaultState());
            }

            world.setBlockState(poppy, Blocks.POPPY.getDefaultState());
            world.setBlockState(grass, Blocks.SHORT_GRASS.getDefaultState());
            world.setBlockState(cane.down(), Blocks.SAND.getDefaultState());
            world.setBlockState(cane, Blocks.SUGAR_CANE.getDefaultState());
            world.setBlockState(ice, Blocks.ICE.getDefaultState());

            context.assertTrue(PlantPatch.in(world, poppy, poppy.getY(), ResourceType.POPPY).equals(Optional.of(poppy)),
                    "a papoula não foi achada");
            context.assertTrue(PlantPatch.in(world, cane, cane.getY(), ResourceType.SUGAR_CANE).isEmpty(),
                    "cana de um bloco só é a base: fica para crescer");

            world.setBlockState(cane.up(), Blocks.SUGAR_CANE.getDefaultState());
            context.assertTrue(PlantPatch.in(world, cane, cane.getY(), ResourceType.SUGAR_CANE).equals(Optional.of(cane.up())),
                    "a cana devia sair pelo topo, deixando a base");

            context.assertTrue(yields(world, grass, ResourceType.SHORT_GRASS, Items.SHORT_GRASS),
                    "a grama com tesoura devia render grama");
            context.assertTrue(yields(world, ice, ResourceType.ICE, Items.ICE),
                    "o gelo com toque suave devia render gelo");
        } finally {
            for (int x = 0; x <= 8; x++) {
                for (int dy = -1; dy <= 2; dy++) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y + dy, 1)), Blocks.AIR.getDefaultState());
                }
            }
        }

        context.complete();
    }

    private static boolean yields(ServerWorld world, BlockPos at, ResourceType resource, net.minecraft.item.Item item) {
        ItemStack tool = PlantPatch.toolFor(world, resource).orElse(ItemStack.EMPTY);

        return Block.getDroppedStacks(world.getBlockState(at), world, at, null, null, tool).stream()
                .anyMatch(stack -> stack.isOf(item));
    }
}
