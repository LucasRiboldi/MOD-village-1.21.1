package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** No fundo do mundo a mina vira rampa para longe do centro (ADR-036 17). */
public class MineRampGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_ramp", tickLimit = 40)
    public void anExhaustedMineClimbsOutAwayFromTheCentre(TestContext context) {
        BlockPos centre = context.getAbsolutePos(new BlockPos(1, 1, 1));
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        Colony colony = fixture.colony();

        // A boca a leste do centro e baixa o bastante para o nível seguinte
        // passar da rocha-mãe: a mina está no fundo.
        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(centre.add(6, 6, 0));
        Mine mine = VillageColonyMod.MINES.open(colony.id(), MineShaft.from(mouth, Side.NORTH));

        try {
            context.assertTrue(!mine.shaft().mayDeepen(), "o cenário precisa de uma mina no fundo");

            // O nível foi cavado até o salão, e os ramais acabaram.
            mine.arm(0).reopenFrom(MineShaft.SHARED_BLOCKS);

            for (MineArm arm : mine.arms()) {
                arm.finish();
            }

            MineDigging.nextTarget(context.getWorld(), UUID.randomUUID(), colony.id(), centre);

            Mine after = VillageColonyMod.MINES.of(colony.id()).orElseThrow();
            ColonyPos floor = after.shaft().entry();

            context.assertTrue(after.shaft().isRamp(), "a mina esgotada não virou rampa");
            context.assertTrue(after.shaft().descent() == Side.awayFrom(
                            MinecraftTypeAdapter.toColonyPos(centre), floor, Side.NORTH),
                    "a rampa vai para " + after.shaft().descent() + ", e não para longe do centro");
            context.assertTrue(after.shaft().ascent() == centre.getY() - floor.y(),
                    "a rampa tem " + after.shaft().ascent() + " degraus; até a altura do centro são "
                            + (centre.getY() - floor.y()));
            context.assertTrue(after.branchesOpenNow() == 1, "a rampa abriu mais de um ramal");
        } finally {
            VillageColonyMod.MINES.removeOfColony(colony.id());
            fixture.cleanUp();
        }

        context.complete();
    }
}
