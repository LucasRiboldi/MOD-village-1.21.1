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

    /**
     * O pastor anda até a vaca, volta andando com ela na corda e a amarra (B5): sem teletransporte,
     * pela navegação do jogo, com o tique do servidor rodando a coleta.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_herding", tickLimit = 600)
    public void theShepherdWalksTheCowToTheFence(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.STONE.getDefaultState());
            }
        }

        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(4, 1, 4));
        // Cantos opostos: a 7 da cerca a vaca não conta como "em curral" (a arena é fechada).
        BlockPos fence = new BlockPos(0, 1, 0);
        context.setBlockState(fence, Blocks.OAK_FENCE.getDefaultState());
        VillagerEntity shepherd = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 1, 2));
        CowEntity cow = context.spawnEntity(EntityType.COW, new BlockPos(7, 1, 7));
        cow.setAiDisabled(true);
        context.getWorld().setTimeOfDay(1_000);

        context.waitAndRun(5, () -> {
            fixture.owning(shepherd.getUuid());
            VillageColonyMod.WORKERS.register(shepherd.getUuid(), fixture.colony().id())
                    .assign(ProfessionType.SHEPHERD);
            ShepherdHerding.plan(context.getWorld(), fixture.colony(), id -> false);
            context.assertTrue(ShepherdHerding.isHerding(shepherd.getUuid()), "o pastor não começou a coleta");
        });

        StringBuilder trace = new StringBuilder();

        for (int tick = 50; tick <= 550; tick += 50) {
            int at = tick;
            context.runAtTick(at, () -> trace.append(' ').append(at).append(':')
                    .append(ShepherdHerding.phaseOf(shepherd.getUuid()).map(Enum::name).orElse("-"))
                    .append('@').append(context.getRelativePos(shepherd.getBlockPos()).toShortString())
                    .append(cow.isLeashed() ? "+corda" : ""));
        }

        context.runAtTick(560, () -> {
            try {
                context.assertTrue(cow.getLeashHolder() instanceof LeashKnotEntity knot
                                && knot.getAttachedBlockPos().equals(context.getAbsolutePos(fence)),
                        "andando, o pastor não amarrou a vaca na cerca: corda=" + cow.getLeashHolder()
                                + ", fase=" + ShepherdHerding.phaseOf(shepherd.getUuid())
                                + ", trajeto" + trace);
            } finally {
                fixture.cleanUp();
            }

            context.complete();
        });
    }
}
