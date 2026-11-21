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
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;
import java.util.UUID;

/**
 * A descida cavada — Regra 336, ADR-061: o mineiro só pisa no degrau aberto, e o degrau que falta ele cava de pé no
 * último, com 3 pistas de largura e 3 de altura, e põe piso no vão de caverna dentro da montanha.
 */
public class MineStepDownGameTest implements FabricGameTest {

    private static final int SIDE = 41;

    private static final int DEPTH = 36;

    private static final int GROUND = 60;

    /**
     * Do jogo da 0.4.93: na boca, com a escada por cavar, o mineiro não se mexia e devolvia a pedra. Regra 348 (jogo da
     * 0.4.111): ele desce cavando, mas só a escada do desenho que sai da boca — nenhum bloco fora dela sai do morro.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_step_down", tickLimit = 1400)
    public void theMinerDigsTheStairsDownFromTheMouth(TestContext context) {
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();
        // Longe da grade dos testes: o morro de 41 blocos invadia a arena dos vizinhos na bateria inteira.
        BlockPos away = context.getAbsolutePos(BlockPos.ORIGIN).add(2_000_000, 0, 2_600_000).withY(0);
        BlockPos low = away.add(0, GROUND - DEPTH + 1, 0);
        BlockPos chest = away.add(1, GROUND + 1, 1);
        BlockPos mouth = away.add(SIDE / 2, GROUND + 1, SIDE / 2);

        // Aldeão que anda precisa de chunk carregado e com tique de entidade; a grade de teste não garante isso.
        forceChunks(world, low, true);
        hill(world, low);
        world.setBlockState(chest, Blocks.CHEST.getDefaultState());

        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(chest));
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        ColonyPos mouthAt = MinecraftTypeAdapter.toColonyPos(mouth);
        MineShaft now = MineShaft.from(mouthAt, Side.EAST).deepened();

        // O nível de agora já aberto em volta da entrada dele; a escada da boca até lá, por cavar (o jogo da 0.4.93).
        BlockPos level = MinecraftTypeAdapter.toBlockPos(now.entry());

        for (BlockPos at : BlockPos.iterate(level.add(-1, 0, -1), level.add(1, 2, 1))) {
            world.setBlockState(at.toImmutable(), Blocks.AIR.getDefaultState(), 18);
        }

        VillageColonyMod.MINES.restore(Mine.restore(colony.id(), now, 0));
        MineEntrance.opened(colony.id(), mouthAt);

        VillagerEntity villager = EntityType.VILLAGER.create(world);
        context.assertTrue(villager != null, "o aldeão do teste não nasceu");
        villager.setBreedingAge(0);
        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        villager.refreshPositionAndAngles(mouth.getX() + 0.5, mouth.getY(), mouth.getZ() + 0.5, 0, 0);
        world.spawnEntity(villager);

        UUID minerId = villager.getUuid();
        Worker worker = VillageColonyMod.WORKERS.register(minerId, colony.id());
        worker.assign(ProfessionType.MINER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(minerId, MinecraftTypeAdapter.toColonyPos(chest)));
        owned.owning(minerId);

        Task task = VillageColonyMod.TASKS.create(colony.id(), TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION, ResourceType.COBBLESTONE, 16);
        task.reserveFor(minerId);

        int[] lowest = { mouth.getY() };
        int[] releasedAt = { -1 };
        int[] passes = { 0 };

        context.runAtEveryTick(() -> {
            passes[0]++;
            MinerWork.run(world, colony);
            lowest[0] = Math.min(lowest[0], villager.getBlockY());

            if (releasedAt[0] < 0 && task.state() != TaskState.RESERVED && task.state() != TaskState.EXECUTING) {
                releasedAt[0] = passes[0];
            }
        });

        java.util.Set<BlockPos> planned = new java.util.HashSet<>();

        MineShaft.from(mouthAt, Side.EAST).plannedCells().forEach(at -> planned.add(MinecraftTypeAdapter.toBlockPos(at)));
        VillageColonyMod.MINES.of(colony.id()).orElseThrow().plannedCells()
                .forEach(at -> planned.add(MinecraftTypeAdapter.toBlockPos(at)));

        context.runAtTick(1300, () -> {
            try {
                context.assertTrue(lowest[0] <= mouth.getY() - 4, "Regra 336: o mineiro não desceu a escada que"
                        + " faltava; o mais baixo foi y " + lowest[0] + ", a boca em y " + mouth.getY()
                        + "; devolveu a pedra no tique " + releasedAt[0]);

                for (BlockPos at : BlockPos.iterate(low, low.add(SIDE - 1, DEPTH - 1, SIDE - 1))) {
                    boolean opened = at.getX() >= level.getX() - 1 && at.getX() <= level.getX() + 1
                            && at.getZ() >= level.getZ() - 1 && at.getZ() <= level.getZ() + 1
                            && at.getY() >= level.getY() && at.getY() <= level.getY() + 2;

                    context.assertTrue(opened || planned.contains(at) || !world.getBlockState(at).isAir(),
                            "Regra 348: o mineiro cavou fora da escada do desenho em " + at.toShortString());
                }
            } finally {
                MinerProbe.targetOf(minerId).ifPresent(MineMarks::dug);
                MineEntrance.forget(colony.id());
                VillageColonyMod.MINES.removeOfColony(colony.id());
                villager.discard();
                owned.cleanUp();
                clear(world, low);
                forceChunks(world, low, false);
            }

            context.complete();
        });
    }

    /** O degrau aberto sobre uma caverna, dentro da montanha, ganha piso de pedra antes de ele pisar. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_step_floor", tickLimit = 400)
    public void aStepOverACaveGetsAFloor(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos low = context.getAbsolutePos(new BlockPos(0, GROUND + 40 - DEPTH, 0));
        BlockPos feet = context.getAbsolutePos(new BlockPos(SIDE / 2, GROUND + 30, SIDE / 2));

        hill(world, low);
        // Um teto de pedra: a escada fica dentro da montanha.
        for (int y = 1; y <= 4; y++) {
            for (BlockPos at : BlockPos.iterate(feet.add(-3, y, -3), feet.add(3, y, 3))) {
                world.setBlockState(at.toImmutable(), y == 4 ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState());
            }
        }
        world.setBlockState(feet, Blocks.AIR.getDefaultState());
        world.setBlockState(feet.up(), Blocks.AIR.getDefaultState());
        world.setBlockState(feet.up(2), Blocks.AIR.getDefaultState());

        // A caverna embaixo do próximo degrau, a leste.
        BlockPos next = feet.east().down();
        for (BlockPos at : BlockPos.iterate(next.add(-1, -3, -1), next.add(1, -1, 1))) {
            world.setBlockState(at.toImmutable(), Blocks.AIR.getDefaultState());
        }

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, context.getRelativePos(feet));
        villager.setBreedingAge(0);
        villager.setAiDisabled(true);
        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        villager.refreshPositionAndAngles(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0, 0);

        BlockPos aim = feet.add(20, -20, 0);
        Optional<MineStepDown> step = MineStepDown.start(world, feet, aim, Direction.EAST);
        boolean[] placed = { false };

        context.assertTrue(step.isPresent(), "Regra 336: não há degrau a cavar a leste, sobre a caverna");

        context.runAtEveryTick(() -> {
            if (placed[0]) {
                return;
            }

            step.get().tick(world, villager, null);

            if (world.getBlockState(next.down()).isOf(Blocks.COBBLESTONE)) {
                placed[0] = true;
            }
        });

        context.runAtTick(350, () -> {
            try {
                context.assertTrue(placed[0], "Regra 336: o degrau sobre a caverna ficou sem piso em "
                        + next.down().toShortString() + ": " + world.getBlockState(next.down()));
                context.assertTrue(isThreeByThree(world, next),
                        "Regra 336: o degrau sobre a caverna não abriu 3 por 3 em " + next.toShortString());
            } finally {
                villager.discard();
                clear(world, low);
            }

            context.complete();
        });
    }

    private static void forceChunks(ServerWorld world, BlockPos low, boolean force) {
        for (int x = low.getX() >> 4; x <= (low.getX() + SIDE) >> 4; x++) {
            for (int z = low.getZ() >> 4; z <= (low.getZ() + SIDE) >> 4; z++) {
                world.setChunkForced(x, z, force);

                if (force) {
                    world.getChunk(x, z);
                }
            }
        }
    }

    private static void hill(ServerWorld world, BlockPos low) {
        for (BlockPos at : BlockPos.iterate(low, low.add(SIDE - 1, DEPTH - 1, SIDE - 1))) {
            world.setBlockState(at.toImmutable(), at.getY() == low.getY() + DEPTH - 1
                    ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.STONE.getDefaultState(), 18);
        }
    }

    private static void clear(ServerWorld world, BlockPos low) {
        for (BlockPos at : BlockPos.iterate(low, low.add(SIDE - 1, DEPTH + 6, SIDE - 1))) {
            world.setBlockState(at.toImmutable(), Blocks.AIR.getDefaultState(), 18);
        }
    }

    /** Três pistas de largura, em x ou em z, e três de altura, onde ele está (Regra 119: pistas à esquerda). */
    private static boolean isThreeByThree(ServerWorld world, BlockPos feet) {
        return isThreeByThreeAlong(world, feet, Direction.EAST) || isThreeByThreeAlong(world, feet, Direction.SOUTH);
    }

    /** Alguma faixa de 3 pistas que contém o pé, com 3 de altura. */
    private static boolean isThreeByThreeAlong(ServerWorld world, BlockPos feet, Direction across) {
        for (int start = 1 - MineShaft.STAIR_LANES; start <= 0; start++) {
            if (isOpen(world, feet, across, start)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isOpen(ServerWorld world, BlockPos feet, Direction across, int start) {
        for (int lane = start; lane < start + MineShaft.STAIR_LANES; lane++) {
            for (int height = 0; height < MineShaft.STAIR_HEADROOM; height++) {
                if (!world.getBlockState(feet.offset(across, lane).up(height)).isAir()) {
                    return false;
                }
            }
        }

        return true;
    }
}
