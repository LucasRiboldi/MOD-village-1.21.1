package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.gametest.ColonyFixture;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/** O pastor traz o animal solto na corda e o amarra na cerca (ADR-038 P2b). */
public class ShepherdHerdingGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_herding", tickLimit = 60)
    public void theShepherdLeadsALooseCowToTheFence(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(4, 1, 4));
        BlockPos fence = new BlockPos(1, 1, 1);
        context.setBlockState(fence, Blocks.OAK_FENCE.getDefaultState());
        VillagerEntity shepherd = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        CowEntity cow = context.spawnEntity(EntityType.COW, new BlockPos(10, 1, 10));
        context.getWorld().setTimeOfDay(1_000);

        context.waitAndRun(5, () -> {
            try {
                fixture.owning(shepherd.getUuid());
                VillageColonyMod.WORKERS.register(shepherd.getUuid(), fixture.colony().id())
                        .assign(ProfessionType.SHEPHERD);

                Optional<ShepherdHerding.Herd> herd =
                        ShepherdHerding.start(context.getWorld(), fixture.colony(), shepherd);

                context.assertTrue(herd.isPresent() && herd.get().animal.equals(cow.getUuid()),
                        "o pastor não escolheu a vaca solta: kept=" + ShepherdFlock.isKept(context.getWorld(), cow)
                                + " leashed=" + cow.isLeashed() + " can=" + cow.canBeLeashed() + " herd=" + herd.isPresent());
                context.assertTrue(herd.get().fence.equals(context.getAbsolutePos(fence)),
                        "a cerca escolhida não é a mais perto");

                // Chega à vaca: põe a corda.
                shepherd.refreshPositionAndAngles(cow.getX() + 1, cow.getY(), cow.getZ(), 0, 0);
                ShepherdHerding.step(context.getWorld(), shepherd.getUuid(), herd.get());
                context.assertTrue(cow.getLeashHolder() == shepherd, "a vaca não está na corda do pastor");

                // Chega à cerca: amarra.
                BlockPos at = context.getAbsolutePos(new BlockPos(2, 1, 1));
                shepherd.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
                boolean goesOn = ShepherdHerding.step(context.getWorld(), shepherd.getUuid(), herd.get());

                context.assertTrue(!goesOn, "a coleta não terminou na cerca");
                context.assertTrue(cow.getLeashHolder() instanceof LeashKnotEntity knot
                                && knot.getAttachedBlockPos().equals(context.getAbsolutePos(fence)),
                        "a vaca não ficou amarrada na cerca");
                context.assertTrue(ShepherdFlock.isKept(context.getWorld(), cow), "a vaca amarrada não conta como guardada");
            } finally {
                fixture.cleanUp();
            }

            context.complete();
        });
    }
}
