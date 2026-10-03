package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineFloor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

/**
 * O piso da passagem — ADR-025, fase 1.
 *
 * <p>A mina é cavada por geometria fixa; o mundo tem caverna. Quando a célula
 * cavada fica sobre um vão que a mina não planejou, o degrau vira queda. A
 * picareta tapa o vão com pedregulho, como o jogador faria antes de pisar.
 */
public class MineFloorGameTest implements FabricGameTest {

    private static final BlockPos DUG = new BlockPos(3, 4, 3);

    private static void rockWithTheCellDug(TestContext context) {
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 6; y++) {
                for (int z = 1; z <= 5; z++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        context.setBlockState(DUG, Blocks.AIR.getDefaultState());
    }

    private static ColonyPos colonyPos(TestContext context, BlockPos relative) {
        return MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(relative));
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_floor", tickLimit = 20)
    public void aCaveUnderThePassageGetsAFloor(TestContext context) {
        rockWithTheCellDug(context);
        context.setBlockState(DUG.down(), Blocks.AIR.getDefaultState());

        boolean patched = MineFloor.patch(
                context.getWorld(), context.getAbsolutePos(DUG), Set.of(colonyPos(context, DUG)));

        context.assertTrue(patched, "o vão sob a passagem ficou aberto");
        context.expectBlock(Blocks.COBBLESTONE, DUG.down());
        context.complete();
    }

    /** O vão que a própria mina vai cavar é o degrau de baixo, não queda. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_floor", tickLimit = 20)
    public void aPlannedCellBelowIsLeftOpen(TestContext context) {
        rockWithTheCellDug(context);
        context.setBlockState(DUG.down(), Blocks.AIR.getDefaultState());

        boolean patched = MineFloor.patch(
                context.getWorld(),
                context.getAbsolutePos(DUG),
                Set.of(colonyPos(context, DUG), colonyPos(context, DUG.down())));

        context.assertFalse(patched, "tapou a célula que a mina planejou abrir");
        context.expectBlock(Blocks.AIR, DUG.down());
        context.complete();
    }

    /** Fora da passagem — o veio que o mineiro segue — o chão não é dele. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_floor", tickLimit = 20)
    public void aCellOffThePassageIsNotFloored(TestContext context) {
        rockWithTheCellDug(context);
        context.setBlockState(DUG.down(), Blocks.AIR.getDefaultState());

        boolean patched = MineFloor.patch(context.getWorld(), context.getAbsolutePos(DUG), Set.of());

        context.assertFalse(patched, "pôs piso fora da passagem planejada");
        context.expectBlock(Blocks.AIR, DUG.down());
        context.complete();
    }

    /** Chão firme fica como está: o remendo não troca pedra por pedregulho. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_floor", tickLimit = 20)
    public void aSolidFloorIsLeftAlone(TestContext context) {
        rockWithTheCellDug(context);

        boolean patched = MineFloor.patch(
                context.getWorld(), context.getAbsolutePos(DUG), Set.of(colonyPos(context, DUG)));

        context.assertFalse(patched, "remendou um chão que já era firme");
        context.expectBlock(Blocks.STONE, DUG.down());
        context.complete();
    }
}
