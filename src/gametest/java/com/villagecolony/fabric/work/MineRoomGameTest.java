package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/** Pedra sem lugar de pé: o mineiro abre o lugar ao lado em vez de encerrar o ramal (ADR-038 P6). */
public class MineRoomGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_vein", tickLimit = 20)
    public void aWalledStoneGetsRoomDugBesideIt(TestContext context) {
        ServerWorld world = context.getWorld();

        // Um bloco maciço de pedra de 13x8x9, com um corredor aberto a leste,
        // a 4 blocos da emparedada (4,03 do pé: fora do alcance) e a 3 da vizinha.
        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 8; z++) {
                for (int y = 1; y <= 8; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        for (int x = 8; x <= 12; x++) {
            context.setBlockState(new BlockPos(x, 3, 4), Blocks.AIR.getDefaultState());
            context.setBlockState(new BlockPos(x, 4, 4), Blocks.AIR.getDefaultState());
        }

        BlockPos walled = context.getAbsolutePos(new BlockPos(4, 3, 4));
        BlockPos beside = context.getAbsolutePos(new BlockPos(5, 3, 4));
        Mine mine = Mine.open(UUID.randomUUID(),
                MineShaft.from(new ColonyPos(walled.getX() + 40, walled.getY(), walled.getZ()), Side.NORTH));

        context.assertTrue(MineVein.nowhereToStand(world, walled), "o cenário não emparedou a pedra");

        Optional<BlockPos> room = MineVein.roomBeside(world, mine, walled);

        context.assertTrue(room.equals(Optional.of(beside)),
                "a pedra ao lado alcançável era " + beside.toShortString() + ", veio " + room);

        world.setBlockState(beside, Blocks.AIR.getDefaultState());

        context.assertTrue(MineVein.roomBeside(world, mine, walled).equals(Optional.of(beside.up())),
                "aberto o pé, a próxima devia ser a cabeça " + beside.up().toShortString());

        world.setBlockState(beside.up(), Blocks.AIR.getDefaultState());

        context.assertTrue(!MineVein.nowhereToStand(world, walled), "aberto o lado, a pedra continuou sem lugar de pé");
        context.complete();
    }
}
