package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/** Quem está sem tarefa aparece fazendo alguma coisa, e isso não vira trabalho na conta — Regra 219. */
public class IdleRoutineGameTest implements FabricGameTest {

    private static BlockPos target(VillagerEntity villager) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET)
                .map(WalkTarget::getLookTarget)
                .map(look -> look.getBlockPos())
                .orElse(null);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_routine", tickLimit = 20)
    public void anIdleWorkerWalksToHisChestThenToANeighbour(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        Colony colony = fixture.colony();
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);

            Worker worker = fixture.equippedWorker(context, ProfessionType.CARPENTER, new BlockPos(3, 1, 3));
            Worker other = fixture.equippedWorker(context, ProfessionType.MASON, new BlockPos(6, 1, 3));
            VillagerEntity villager = (VillagerEntity) world.getEntity(worker.villagerId());
            VillagerEntity neighbour = (VillagerEntity) world.getEntity(other.villagerId());
            BlockPos chest = context.getAbsolutePos(new BlockPos(5, 1, 7));

            VillageColonyMod.STORAGES.register(WorkerStorage.of(worker.villagerId(), MinecraftTypeAdapter.toColonyPos(chest)));
            villager.getBrain().forget(MemoryModuleType.WALK_TARGET);

            IdleRoutine.pass(world, colony);

            context.assertTrue(chest.equals(target(villager)), "sem bancada, o ocioso devia ir ao baú, e foi a " + target(villager));

            villager.getBrain().forget(MemoryModuleType.WALK_TARGET);
            IdleRoutine.pass(world, colony);

            context.assertTrue(target(villager) == null, "ele ainda estava no lugar e já saiu de novo");

            IdleRoutine.forgetWaits();
            IdleRoutine.pass(world, colony);

            context.assertTrue(neighbour.getBlockPos().equals(target(villager)), "depois do baú vem o vizinho, e foi a " + target(villager));
            context.assertTrue(
                    WorkTime.classify(false, false, false, true, true, 0) == WorkTime.State.IDLE,
                    "o passeio de quem está sem tarefa continua sendo ócio");
        } finally {
            world.setTimeOfDay(before);
            fixture.cleanUp();
        }

        context.complete();
    }

    /** Dormir é dormir: fora do expediente a rotina não manda ninguém. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_routine_night", tickLimit = 20)
    public void nobodyIsSentOutAfterHours(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(13_000);

            Worker worker = fixture.equippedWorker(context, ProfessionType.CARPENTER, new BlockPos(3, 1, 3));
            VillagerEntity villager = (VillagerEntity) world.getEntity(worker.villagerId());
            BlockPos chest = context.getAbsolutePos(new BlockPos(5, 1, 7));

            VillageColonyMod.STORAGES.register(WorkerStorage.of(worker.villagerId(), MinecraftTypeAdapter.toColonyPos(chest)));
            villager.getBrain().forget(MemoryModuleType.WALK_TARGET);

            IdleRoutine.pass(world, fixture.colony());

            context.assertTrue(target(villager) == null, "à noite o ocioso foi mandado a " + target(villager));
        } finally {
            world.setTimeOfDay(before);
            fixture.cleanUp();
        }

        context.complete();
    }
}
