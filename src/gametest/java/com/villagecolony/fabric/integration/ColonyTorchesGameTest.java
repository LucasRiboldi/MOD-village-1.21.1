package com.villagecolony.fabric.integration;

import com.villagecolony.fabric.content.ModContent;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/** A tocha da colônia — Regra 127: anel de luz, bloqueio de monstro a 30 blocos, e o anel sai com ela. */
public class ColonyTorchesGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_torch", tickLimit = 40)
    public void theTorchLightsARingAndKeepsMonstersAway(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos torch = context.getAbsolutePos(BlockPos.ORIGIN).add(4_000_000, 20, 4_000_000);

        try {
            for (int x = -12; x <= 12; x++) {
                for (int z = -12; z <= 12; z++) {
                    BlockPos floor = torch.add(x, -1, z);
                    world.getChunk(floor);
                    world.setBlockState(floor, Blocks.STONE.getDefaultState());
                }
            }

            ColonyTorches.place(world, torch);

            context.assertTrue(world.getBlockState(torch).isOf(ModContent.COLONY_TORCH), "a tocha não foi posta");
            context.assertTrue(lightsAround(world, torch) == com.villagecolony.fabric.content.ColonyTorchBlock.RING.size(),
                    "o anel devia ter " + com.villagecolony.fabric.content.ColonyTorchBlock.RING.size() + " luzes e tem " + lightsAround(world, torch));

            context.assertTrue(ColonyTorches.isNear(world, torch.add(20, 0, 0)), "a 20 blocos devia barrar monstro");
            context.assertFalse(ColonyTorches.isNear(world, torch.add(40, 0, 0)), "a 40 blocos não barra mais");
            context.assertTrue(ColonyTorches.suppresses(world, SpawnReason.NATURAL, torch.add(5, 0, 5)),
                    "o nascimento natural perto devia ser barrado");
            context.assertFalse(ColonyTorches.suppresses(world, SpawnReason.SPAWNER, torch.add(5, 0, 5)),
                    "o criador de monstro não é barrado");

            world.breakBlock(torch, false);

            context.assertTrue(lightsAround(world, torch) == 0, "quebrada a tocha, o anel de luz devia sair");
        } finally {
            for (BlockPos at : BlockPos.iterate(torch.add(-12, -1, -12), torch.add(12, 5, 12))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }
        }

        context.complete();
    }

    private static int lightsAround(ServerWorld world, BlockPos torch) {
        int lights = 0;

        for (BlockPos at : BlockPos.iterate(torch.add(-11, -1, -11), torch.add(11, 5, 11))) {
            if (world.getBlockState(at).isOf(Blocks.LIGHT)) {
                lights++;
            }
        }

        return lights;
    }
}
