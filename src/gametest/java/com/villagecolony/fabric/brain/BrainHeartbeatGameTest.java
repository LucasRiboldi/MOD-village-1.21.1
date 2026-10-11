package com.villagecolony.fabric.brain;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;

/**
 * Quem perde a tarefa da colônia do {@code Brain} é avisado; quem a tem, não — AUD-12.
 *
 * <p>O aldeão sem a tarefa recebe uma cópia do próprio {@code Brain}, que guarda as
 * memórias e nenhuma tarefa: é o que fica quando outro mod troca a IA do aldeão.
 */
public class BrainHeartbeatGameTest implements FabricGameTest {

    private static final int GRACE = 20;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "brain_heartbeat",
            tickLimit = GRACE + BrainHeartbeat.SILENCE + 60)
    public void aWorkerWhoseBrainLostTheColonyTaskIsReported(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
            }
        }

        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 2, 1));
        Colony colony = fixture.colony();
        VillagerEntity keeps = worker(context, fixture, colony, new BlockPos(2, 2, 2));
        VillagerEntity lost = worker(context, fixture, colony, new BlockPos(5, 2, 5));

        try {
            Field brain = LivingEntity.class.getDeclaredField("brain");
            brain.setAccessible(true);
            brain.set(lost, lost.getBrain().copy());
        } catch (ReflectiveOperationException failure) {
            fixture.cleanUp();
            throw new AssertionError("não deu para trocar o Brain do aldeão de teste", failure);
        }

        context.runAtTick(GRACE, () -> BrainHeartbeat.inspect(world, colony));

        context.runAtTick(GRACE + BrainHeartbeat.SILENCE + 20, () -> {
            try {
                BrainHeartbeat.inspect(world, colony);

                context.assertTrue(BrainHeartbeat.warned(lost.getUuid()),
                        "o aldeão sem a tarefa da colônia no Brain não foi avisado");
                context.assertFalse(BrainHeartbeat.warned(keeps.getUuid()),
                        "o aldeão com a tarefa da colônia no Brain foi avisado");
            } finally {
                fixture.cleanUp();
            }

            context.complete();
        });
    }

    private static VillagerEntity worker(TestContext context, ColonyFixture fixture, Colony colony, BlockPos at) {
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, at);
        villager.setBreedingAge(0);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(ProfessionType.FARMER);
        fixture.owning(villager.getUuid());

        return villager;
    }
}
