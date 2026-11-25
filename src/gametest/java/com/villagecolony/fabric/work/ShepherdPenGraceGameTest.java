package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** Regra 355: o pastor que entrou no cercado para tosquiar não é tirado dele pelo socorro do curral, até o prazo. */
public class ShepherdPenGraceGameTest implements FabricGameTest {

    private static final int LOW = 2;

    private static final int HIGH = 8;

    /** Dentro do cercado, encostado na cerca do sul — onde o pastor do N05 pousava. */
    private static final BlockPos INSIDE = new BlockPos(5, 2, 7);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_pen_grace", tickLimit = 40)
    public void theShepherdShearingInsideThePenIsNotEscortedOutUntilTheDeadline(TestContext context) {
        pen(context);
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(0, 2, 0))));
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillageColonyMod.COLONIES.register(colony);

        VillagerEntity shepherd = context.spawnEntity(EntityType.VILLAGER, INSIDE);
        shepherd.setAiDisabled(true);
        UUID id = shepherd.getUuid();
        VillageColonyMod.WORKERS.register(id, colony.id()).assign(ProfessionType.SHEPHERD);
        owned.owning(id);
        // Reservada já no nascimento: o socorro do curral roda sozinho a cada 20 tiques e veria um pastor sem tosquia.
        Task shear = VillageColonyMod.TASKS.create(colony.id(), TaskType.COLLECT_WOOL,
                TaskPriority.PRODUCTION, ResourceType.WHITE_WOOL, 8);
        shear.reserveFor(id);

        context.waitAndRun(5, () -> {
            try {
                context.assertFalse(PenEscape.check(context.getWorld(), shepherd),
                        "Regra 355: o pastor com a tosquia reservada foi tirado do cercado");
                context.assertFalse(PenEscape.isEscaping(id), "Regra 355: o socorro assumiu o pastor que tosquia");

                ShepherdPenGrace.backdate(id, ShepherdPenGrace.GRACE_TICKS + 1);
                context.assertTrue(PenEscape.check(context.getWorld(), shepherd),
                        "Regra 355: passado o prazo, o socorro devia tirar o pastor do cercado");
                PenEscape.forget(id);

                VillageColonyMod.TASKS.remove(shear.id());
                context.assertTrue(PenEscape.check(context.getWorld(), shepherd),
                        "sem a tosquia, o pastor cercado continua sendo socorrido");
            } finally {
                PenEscape.forget(id);
                ShepherdPenGrace.forget(id);
                owned.cleanUp();
                shepherd.discard();
            }
            context.complete();
        });
    }

    /** Anel de cerca de carvalho de 2 a 8, com porteira, sobre pedra. */
    private static void pen(TestContext context) {
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
                for (int y = 2; y <= 4; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
                boolean ring = (x == LOW || x == HIGH) && z >= LOW && z <= HIGH
                        || (z == LOW || z == HIGH) && x >= LOW && x <= HIGH;
                if (ring) {
                    context.setBlockState(new BlockPos(x, 2, z), Blocks.OAK_FENCE.getDefaultState()
                            .with(FenceBlock.NORTH, (x == LOW || x == HIGH) && z > LOW)
                            .with(FenceBlock.SOUTH, (x == LOW || x == HIGH) && z < HIGH)
                            .with(FenceBlock.WEST, (z == LOW || z == HIGH) && x > LOW)
                            .with(FenceBlock.EAST, (z == LOW || z == HIGH) && x < HIGH));
                }
            }
        }
        context.setBlockState(new BlockPos(5, 2, HIGH), Blocks.OAK_FENCE_GATE.getDefaultState());
    }
}
