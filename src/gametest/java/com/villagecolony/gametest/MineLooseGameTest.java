package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.MineDigging;
import com.villagecolony.fabric.work.MineSettling;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * Regra 348: a pedra com solto pendurado em cima fica fora da escolha do mineiro até o solto sair. No jogo da 0.4.111
 * o mineiro recusou as mesmas 12 posições do salão cerca de 180 vezes cada, por uma hora: a marca de recusa era apagada
 * cada vez que ele cavava uma pedra vizinha, e o cursor servia a mesma pedra de novo.
 */
public class MineLooseGameTest implements FabricGameTest {

    private static final BlockPos ENTRY = new BlockPos(3, 6, 4);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_loose", tickLimit = 20)
    public void aStoneUnderSomethingLooseIsNotServed(TestContext context) {
        for (BlockPos at : BlockPos.iterate(0, 1, 0, 9, 9, 8)) {
            context.setBlockState(at.toImmutable(), Blocks.STONE.getDefaultState());
        }

        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(6, 2, 6))));
        VillageColonyMod.COLONIES.register(colony);

        try {
            MineShaft shaft = MineShaft.from(MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ENTRY)), Side.EAST);
            VillageColonyMod.MINES.restore(Mine.restore(colony.id(), shaft, 0));
            BlockPos first = MinecraftTypeAdapter.toBlockPos(shaft.positionAt(0));

            // Cascalho a dois blocos, com vão no meio: o solto pendurado de MineSettling.looseAbove. Lido no mesmo
            // tique, antes de o cascalho cair.
            context.getWorld().setBlockState(first.up(), Blocks.AIR.getDefaultState());
            context.getWorld().setBlockState(first.up(2), Blocks.GRAVEL.getDefaultState());
            context.assertTrue(MineSettling.looseAbove(context.getWorld(), first),
                    "o cenário não pôs solto pendurado sobre o primeiro corte");

            Optional<BlockPos> target = MineDigging.nextTarget(
                    context.getWorld(), UUID.randomUUID(), colony.id(), context.getAbsolutePos(ENTRY));

            context.assertFalse(target.isPresent() && target.get().equals(first),
                    "Regra 348: o mineiro recebeu a pedra com solto em cima em " + first.toShortString());
        } finally {
            VillageColonyMod.MINES.removeOfColony(colony.id());
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
