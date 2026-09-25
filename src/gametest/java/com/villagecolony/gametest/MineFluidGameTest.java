package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineFlooding;
import com.villagecolony.fabric.work.MineDigging;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * A pedra que segura líquido fica — pedido do autor, 2026-09-25: "não deixar
 * o mineiro quebrar o bloco que tem líquido atrás".
 *
 * <p>Antes, a picareta quebrava e o {@link MineFlooding} vedava a nascente no
 * mesmo tique. Vedação protegida (água da vila) ficava aberta e inundava a
 * galeria. Agora a pedra nem vira alvo.
 */
public class MineFluidGameTest implements FabricGameTest {

    private static final BlockPos ENTRY = new BlockPos(3, 6, 4);

    private static void solidRock(TestContext context) {
        for (int x = 0; x <= 9; x++) {
            for (int y = 1; y <= 9; y++) {
                for (int z = 0; z <= 8; z++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }
    }

    private static MineShaft mineOf(TestContext context, Colony colony) {
        MineShaft shaft = MineShaft.from(
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ENTRY)), Side.EAST);

        VillageColonyMod.MINES.restore(Mine.restore(colony.id(), shaft, 0));

        return shaft;
    }

    private static Colony colony(TestContext context) {
        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(6, 2, 6))));

        VillageColonyMod.COLONIES.register(colony);

        return colony;
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_fluid", tickLimit = 20)
    public void theFirstCutWithWaterBehindItIsNotServed(TestContext context) {
        solidRock(context);

        Colony colony = colony(context);
        BlockPos first = MinecraftTypeAdapter.toBlockPos(mineOf(context, colony).positionAt(0));
        BlockPos spring = first.down();

        context.getWorld().setBlockState(spring, Blocks.WATER.getDefaultState());

        // O controle: a pedra está de fato encostada na água.
        context.assertTrue(MineFlooding.holdsBackFluid(context.getWorld(), first),
                "o cenário não pôs água encostada no primeiro corte");

        Optional<BlockPos> target = MineDigging.nextTarget(
                context.getWorld(), UUID.randomUUID(), colony.id(), context.getAbsolutePos(ENTRY));

        context.assertFalse(target.isPresent() && target.get().equals(first),
                "o mineiro recebeu a pedra que segura a água em " + first.toShortString());
        context.assertTrue(target.isEmpty() || !MineFlooding.holdsBackFluid(context.getWorld(), target.get()),
                "o alvo servido no lugar dela também segura líquido");
        context.complete();
    }

    /** Sem líquido em volta, o mesmo corte é servido — prova que o teste acima mede a água. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_fluid", tickLimit = 20)
    public void theSameCutWithoutWaterIsServed(TestContext context) {
        solidRock(context);

        Colony colony = colony(context);
        BlockPos first = MinecraftTypeAdapter.toBlockPos(mineOf(context, colony).positionAt(0));

        Optional<BlockPos> target = MineDigging.nextTarget(
                context.getWorld(), UUID.randomUUID(), colony.id(), context.getAbsolutePos(ENTRY));

        context.assertTrue(target.isPresent() && target.get().equals(first),
                "sem água, o primeiro corte devia ser servido: " + target.map(BlockPos::toShortString));
        context.complete();
    }

    /** Lava conta igual, e líquido a dois blocos não conta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_fluid", tickLimit = 20)
    public void onlyATouchingFluidHoldsTheStone(TestContext context) {
        solidRock(context);

        BlockPos stone = context.getAbsolutePos(new BlockPos(4, 4, 4));

        context.getWorld().setBlockState(stone.east(2), Blocks.WATER.getDefaultState());
        context.assertFalse(MineFlooding.holdsBackFluid(context.getWorld(), stone),
                "água a dois blocos segurou a pedra");

        context.getWorld().setBlockState(stone.up(), Blocks.LAVA.getDefaultState());
        context.assertTrue(MineFlooding.holdsBackFluid(context.getWorld(), stone),
                "lava encostada não segurou a pedra");
        context.complete();
    }
}
