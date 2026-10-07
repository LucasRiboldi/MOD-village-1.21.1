package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** O ramal com o minério que falta vem primeiro; empate mantém a ordem (ADR-039 D3). */
public class MineOreHeadingGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_vein", tickLimit = 20)
    public void theArmWithTheWantedOreComesFirst(TestContext context) {
        ServerWorld world = context.getWorld();
        // Alto: a arena fica perto do fundo do mundo, e o poço desce vários níveis antes dos ramais.
        BlockPos entry = context.getAbsolutePos(new BlockPos(4, 1, 4)).up(80);
        Mine mine = Mine.open(UUID.randomUUID(),
                MineShaft.from(new ColonyPos(entry.getX(), entry.getY(), entry.getZ()), Side.NORTH));

        int[] before = MineOreHeading.order(world, mine, true, true);

        context.assertTrue(before[0] == 0 && before[1] == 1 && before[2] == 2 && before[3] == 3,
                "sem minério nenhum, a ordem dos ramais mudou");

        BlockPos ore = MinecraftTypeAdapter.toBlockPos(mine.arm(2).shaft().positionAt(MineShaft.SHARED_BLOCKS + 3));
        BlockPos old = ore.toImmutable();
        var previous = world.getBlockState(old);

        world.setBlockState(old, Blocks.IRON_ORE.getDefaultState());
        context.assertTrue(world.getBlockState(old).isOf(Blocks.IRON_ORE), "o cenário não pôs o ferro em " + old.toShortString());

        try {
            context.assertTrue(MineOreHeading.oreAhead(world, mine.arm(2), false, true) == 1,
                    "o ferro no corredor do ramal 2 não foi contado");
            context.assertTrue(MineOreHeading.order(world, mine, false, true)[0] == 2,
                    "faltando ferro, o ramal com ferro não veio primeiro");
            context.assertTrue(MineOreHeading.order(world, mine, true, false)[0] == 0,
                    "faltando só carvão, o ferro passou à frente");
        } finally {
            world.setBlockState(old, previous);
        }

        context.complete();
    }
}
