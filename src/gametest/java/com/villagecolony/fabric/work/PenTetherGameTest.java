package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.gametest.ColonyFixture;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

/**
 * Regra 284: enquanto atravessa o portão, o animal fica preso a 2 blocos do lugar de dentro (o que a corda do jogo usa,
 * {@code setPositionTarget}), e o passeio e o pânico dele não o levam para longe; a amarra solta quando ele entra.
 */
public class PenTetherGameTest implements FabricGameTest {

    /** O portão fica em (3, 1, 2); fora, (3, 1, 1); dentro, (3, 1, 3) e (3, 1, 4). */
    private static ShepherdPens.Pen pen(TestContext context) {
        BlockPos gate = context.getAbsolutePos(new BlockPos(3, 1, 2));
        BlockPos inside = context.getAbsolutePos(new BlockPos(3, 1, 3));
        BlockPos outside = context.getAbsolutePos(new BlockPos(3, 1, 1));
        context.setBlockState(new BlockPos(3, 1, 2), Blocks.OAK_FENCE_GATE.getDefaultState().with(FenceGateBlock.OPEN, true));
        return new ShepherdPens.Pen(gate, inside, outside,
                Set.of(inside, context.getAbsolutePos(new BlockPos(3, 1, 4))), false);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_tether_walk", tickLimit = 20)
    public void theAnimalIsTetheredInsideWhileWalkingThrough(TestContext context) {
        ShepherdPens.Pen pen = pen(context);
        CowEntity cow = context.spawnEntity(EntityType.COW, new BlockPos(3, 1, 1));

        context.waitAndRun(2, () -> {
            ShepherdPens.startIn(context.getWorld(), pen, cow);
            boolean tethered = cow.hasPositionTarget();
            BlockPos at = cow.getPositionTarget();
            float range = cow.getPositionTargetRange();
            cow.discard();

            context.assertTrue(tethered && at.equals(ShepherdPens.deeper(pen)) && range == PenTether.RANGE,
                    "Regra 284: o animal atravessa o portão sem amarra: preso=" + tethered + " em=" + at + " raio=" + range);
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_tether_release", tickLimit = 20)
    public void theTetherLetsGoOnceTheAnimalIsInside(TestContext context) {
        ShepherdPens.Pen pen = pen(context);
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        VillagerEntity shepherd = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 1, 0));
        CowEntity cow = context.spawnEntity(EntityType.COW, new BlockPos(3, 1, 1));

        context.waitAndRun(2, () -> {
            boolean tethered;
            boolean stillTethered;
            try {
                if (!WorkClock.isWorkTime(context.getWorld().getTimeOfDay())) {
                    context.getWorld().setTimeOfDay(1_000);
                }
                fixture.owning(shepherd.getUuid());
                VillageColonyMod.WORKERS.register(shepherd.getUuid(), fixture.colony().id()).assign(ProfessionType.SHEPHERD);

                ShepherdPens.startIn(context.getWorld(), pen, cow);
                tethered = cow.hasPositionTarget();

                // Entrou: está no lugar de dentro e não encosta mais no portão.
                BlockPos deeper = ShepherdPens.deeper(pen);
                cow.refreshPositionAndAngles(deeper.getX() + 0.5, deeper.getY(), deeper.getZ() + 0.5, 0, 0);
                ShepherdHerding.Herd herd = new ShepherdHerding.Herd(cow.getUuid(), pen.gate(), context.getWorld().getTime());
                herd.pen = pen;
                herd.phase = ShepherdHerding.Phase.ENTER;
                herd.walking = context.getWorld().getTime();
                ShepherdHerding.step(context.getWorld(), shepherd.getUuid(), herd);
                stillTethered = cow.hasPositionTarget();
            } finally {
                fixture.cleanUp();
                cow.discard();
            }

            context.assertTrue(tethered, "Regra 284: o animal atravessa o portão sem amarra");
            context.assertFalse(stillTethered, "Regra 284: a amarra do portão ficou depois que o animal entrou");
            context.complete();
        });
    }
}
