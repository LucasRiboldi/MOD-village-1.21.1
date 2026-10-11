package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Quem vai e volta na superfície a caminho da boca, sem chegar mais perto da
 * pedra nem descer, larga a pedra no prazo da descida — 2.2 do roteiro de
 * 10-10 ({@link MineLease#outOfTimeOnDescent}).
 *
 * <p>Antes, na descida os dois prazos eram zerados a cada tique, e o dono do
 * único ramal da colônia ficou três sessões indo e voltando entre a boca e um
 * patamar de pedra maciça.
 *
 * <p>O cenário: uma mina de dois níveis no alto da arena (a boca a trinta do
 * piso, o nível de agora dez abaixo), e o mineiro num patamar ao lado da boca,
 * mudado de bloco a cada tique para o guarda de imobilidade não falar.
 */
public class MineDescentLeaseGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    private static final BlockPos MOUTH = new BlockPos(2, 30, 6);

    private static final BlockPos PERCH = new BlockPos(6, 30, 6);

    private static final BlockPos STEP = new BlockPos(6, 30, 7);

    /** O prazo normal, encurtado: a descida tem três vezes ele. */
    private static final int LEASE = 40;

    private static final int DESCENT = LEASE * MineLease.DESCENT_FACTOR;

    private static final int SLACK = 20;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_descent_lease",
            tickLimit = DESCENT + 4 * SLACK)
    public void theMinerWhoGoesBackAndForthOnTheWayDownLosesTheStone(TestContext context) {
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT.getDefaultState());
            }
        }

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        context.setBlockState(MOUTH.down(), Blocks.STONE.getDefaultState());
        context.setBlockState(PERCH.down(), Blocks.DIRT.getDefaultState());
        context.setBlockState(STEP.down(), Blocks.DIRT.getDefaultState());

        MineLease.shortenTo(LEASE);
        MinerDetours.allowDetoursPerTarget(0);

        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST)));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(MOUTH));
        MineShaft now = MineShaft.from(mouth, Side.EAST).deepened();

        // As primeiras posições da ordem de escavação do nível de agora são pedra.
        for (int index = 0; index < 9; index++) {
            world.setBlockState(MinecraftTypeAdapter.toBlockPos(now.positionAt(index)), Blocks.STONE.getDefaultState());
        }

        VillageColonyMod.MINES.restore(Mine.restore(colony.id(), now, 0));
        MineEntrance.opened(colony.id(), mouth);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, PERCH);
        villager.setBreedingAge(0);

        UUID minerId = villager.getUuid();
        Worker worker = VillageColonyMod.WORKERS.register(minerId, colony.id());
        worker.assign(ProfessionType.MINER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(minerId,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST))));
        owned.owning(minerId);

        Task task = VillageColonyMod.TASKS.create(colony.id(), TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION, ResourceType.COBBLESTONE, 16);
        task.reserveFor(minerId);

        MinerWork.run(world, colony);

        int[] passes = { 0 };
        int[] releasedAt = { -1 };
        int[] stillnessAtRelease = { -1 };
        BlockPos[] stone = { null };
        boolean[] descending = { false };
        BlockPos absolutePerch = context.getAbsolutePos(PERCH);
        BlockPos absoluteStep = context.getAbsolutePos(STEP);

        context.runAtEveryTick(() -> {
            passes[0]++;

            BlockPos at = passes[0] % 2 == 0 ? absolutePerch : absoluteStep;

            villager.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5,
                    villager.getYaw(), villager.getPitch());

            if (stone[0] == null) {
                MinerProbe.targetOf(minerId).ifPresent(target -> {
                    stone[0] = target;
                    descending[0] = MineEntrance.legDown(world, colony.id(), at, target).isPresent();
                });
            }

            if (releasedAt[0] < 0 && task.state() != TaskState.RESERVED && task.state() != TaskState.EXECUTING) {
                releasedAt[0] = passes[0];
                stillnessAtRelease[0] = MinerProbe.stillnessOf(minerId);
            }
        });

        context.runAtTick(DESCENT + 3 * SLACK, () -> {
            try {
                context.assertTrue(WorkHours.isWorkTime(world, villager),
                        "a arena não está em expediente, e fora dele nenhum prazo corre");
                context.assertTrue(stone[0] != null,
                        "o mineiro nunca mirou uma pedra — o cenário não montou");
                context.assertTrue(descending[0],
                        "a pedra " + stone[0].toShortString() + " não pedia a descida pela boca,"
                                + " e o que este teste mede é o prazo da descida");
                context.assertTrue(releasedAt[0] >= 0,
                        "o mineiro foi e voltou " + passes[0] + " tiques na descida e a tarefa nunca"
                                + " voltou para a fila: " + MinerReport.report(world, colony).orElse("(sem relatório)"));
                context.assertTrue(releasedAt[0] > LEASE + SLACK,
                        "a tarefa voltou no tique " + releasedAt[0] + ": na descida o prazo normal"
                                + " não corre, e quem falou não foi o prazo da descida");
                context.assertTrue(releasedAt[0] <= DESCENT + SLACK,
                        "a tarefa voltou no tique " + releasedAt[0] + ", e o prazo da descida é de " + DESCENT);
                context.assertTrue(stillnessAtRelease[0] < MinerWork.STILL_LIMIT,
                        "quem devolveu foi o guarda de imobilidade (" + stillnessAtRelease[0] + ")");
            } finally {
                if (stone[0] != null) {
                    MineMarks.dug(stone[0]);
                }

                MineLease.restoreLimit();
                MinerDetours.restoreDetours();
                MineEntrance.forget(colony.id());
                VillageColonyMod.MINES.removeOfColony(colony.id());

                for (int index = 0; index < 9; index++) {
                    world.setBlockState(MinecraftTypeAdapter.toBlockPos(now.positionAt(index)),
                            Blocks.AIR.getDefaultState());
                }

                owned.cleanUp();
            }

            context.complete();
        });
    }
}
