package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockProtection;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O construtor sem obra calça a rua — pedido do autor, 2026-10-08 (A4): <i>"calça
 * as ruas substituindo grama no meio de caminhos por caminho"</i>. No playtest das
 * 15:14 ele ficou 82% ocioso, com a obra esperando material ou entre uma obra e outra.
 *
 * <p><b>O que é "no meio do caminho":</b> um bloco de grama ou terra na superfície,
 * dentro da caixa da vila, com caminho dos dois lados (norte e sul, ou leste e oeste)
 * ou em três dos quatro lados. Ele anda até lá, troca por caminho e tira a planta de
 * cima. A Regra 3 vale como na fuga ({@link BlockProtection#mayDigOut}): o chão do
 * bioma dentro de peça da vila entra, casa e bloco do jogador não.
 *
 * <p><b>A obra vem primeiro:</b> recebeu tarefa, larga a rua. Um por colônia, uma
 * passagem por meio segundo; a busca varre a caixa por fatias e só volta a varrer
 * depois de {@link #RESCAN} tiques sem achar nada.
 */
public final class BuilderPaving {

    static {
        ServerMemory.register(BuilderPaving.class, BuilderPaving::clearAll);
    }

    /** Até onde ele conta como "ao lado" do bloco a calçar. */
    static final int REACH = 3;

    /** Quanto ele anda até um bloco antes de desistir dele. */
    static final int GIVE_UP_AFTER = 600;

    /** Colunas olhadas por passagem da busca. */
    static final int COLUMNS_PER_PASS = 2_000;

    /** Depois de uma volta inteira sem nada, quanto esperar para varrer de novo: 2 min. */
    static final long RESCAN = 2_400;

    /** Um construtor calçando. */
    private static final class Job {
        final BlockPos target;
        int walked;

        Job(BlockPos target) {
            this.target = target;
        }
    }

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    /** Onde a busca de cada colônia parou, e quando a última volta vazia terminou. */
    private static final Map<UUID, Integer> CURSOR = new HashMap<>();
    private static final Map<UUID, Long> EMPTY_SINCE = new HashMap<>();

    /** Blocos que ele não alcançou: não volta a eles nesta sessão. */
    private static final Set<BlockPos> SKIPPED = new HashSet<>();

    private BuilderPaving() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();

            if (world.getTime() % 10 == 0) {
                tick(world);
            }
        });
    }

    /** Se este aldeão está calçando a rua — conta como trabalho no {@code WorkTime}. */
    public static boolean isPaving(UUID workerId) {
        return JOBS.containsKey(workerId);
    }

    private static void tick(ServerWorld world) {
        long now = world.getTime();

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (colony.isActive() && colony.isAttended(now)) {
                pass(world, colony);
            }
        }
    }

    /** Uma passagem desta colônia: segue o calçamento em curso, ou acha o próximo bloco. */
    static void pass(ServerWorld world, Colony colony) {
        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            if (worker.profession().filter(ProfessionType.BUILDER::equals).isEmpty()) {
                continue;
            }

            UUID id = worker.villagerId();

            if (!(world.getEntity(id) instanceof VillagerEntity builder) || !builder.isAlive()
                    || !VillageColonyMod.TASKS.assignedTo(id).isEmpty()
                    || !WorkHours.isWorkTime(world, builder)
                    || StrandedWorkers.isStranded(id)) {
                if (JOBS.remove(id) != null) {
                    WorkTargets.clear(id);
                }
                continue;
            }

            Job job = JOBS.get(id);

            if (job == null) {
                Optional<BlockPos> gap = nextGap(world, colony);

                if (gap.isEmpty()) {
                    continue;
                }

                job = new Job(gap.get());
                JOBS.put(id, job);
            }

            step(world, builder, job);

            // Um por colônia.
            return;
        }
    }

    private static void step(ServerWorld world, VillagerEntity builder, Job job) {
        UUID id = builder.getUuid();

        if (!isGap(world, job.target)) {
            // Alguém calçou, ou o chão mudou: o próximo.
            JOBS.remove(id);
            WorkTargets.clear(id);
            return;
        }

        if (!builder.getBlockPos().isWithinDistance(job.target, REACH)) {
            job.walked += 10;

            if (job.walked > GIVE_UP_AFTER) {
                SKIPPED.add(job.target);
                JOBS.remove(id);
                WorkTargets.clear(id);
                return;
            }

            Optional<BlockPos> leg = WalkLegs.towards(world, builder.getBlockPos(), job.target.up());
            WorkTargets.set(id, leg.orElse(job.target.up()), 2);
            return;
        }

        pave(world, job.target);
        builder.swingHand(Hand.MAIN_HAND);
        ActionJournal.action(id, "PAVED", "dirt_path", 1, job.target, "");
        VillageColonyMod.LOGGER.info("Builder {} paved the path at {} while the build waits",
                id.toString().substring(0, 8), job.target.toShortString());

        JOBS.remove(id);
        WorkTargets.clear(id);
    }

    /** Troca o bloco por caminho e tira a planta de cima. */
    static void pave(ServerWorld world, BlockPos ground) {
        BlockState above = world.getBlockState(ground.up());

        if (!above.isAir() && above.isReplaceable()) {
            world.breakBlock(ground.up(), false);
        }

        world.setBlockState(ground, Blocks.DIRT_PATH.getDefaultState());
    }

    /** O próximo bloco no meio do caminho, varrendo a caixa da vila por fatias. */
    static Optional<BlockPos> nextGap(ServerWorld world, Colony colony) {
        Optional<VillageBounds> box = colony.bounds();

        if (box.isEmpty()) {
            return Optional.empty();
        }

        Long empty = EMPTY_SINCE.get(colony.id());

        if (empty != null && world.getTime() - empty < RESCAN) {
            return Optional.empty();
        }

        int width = box.get().sizeX();
        int total = width * box.get().sizeZ();
        int start = CURSOR.getOrDefault(colony.id(), 0);

        for (int look = 0; look < COLUMNS_PER_PASS; look++) {
            int index = start + look;

            if (index >= total) {
                CURSOR.remove(colony.id());
                EMPTY_SINCE.put(colony.id(), world.getTime());
                return Optional.empty();
            }

            int x = box.get().minX() + index % width;
            int z = box.get().minZ() + index / width;

            if (world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) {
                continue;
            }

            BlockPos ground = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);

            if (!SKIPPED.contains(ground) && isGap(world, ground)) {
                CURSOR.put(colony.id(), index + 1);
                return Optional.of(ground.toImmutable());
            }
        }

        CURSOR.put(colony.id(), start + COLUMNS_PER_PASS);
        return Optional.empty();
    }

    /** Grama ou terra na superfície com caminho dos dois lados, ou em três dos quatro. */
    static boolean isGap(ServerWorld world, BlockPos ground) {
        BlockState state = world.getBlockState(ground);

        if (!(state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.DIRT) || state.isOf(Blocks.COARSE_DIRT))
                || !BlockProtection.mayDigOut(world, ground, state)) {
            return false;
        }

        BlockState above = world.getBlockState(ground.up());

        if (!above.isAir() && !above.isReplaceable()) {
            return false;
        }

        boolean north = isPath(world, ground.offset(Direction.NORTH));
        boolean south = isPath(world, ground.offset(Direction.SOUTH));
        boolean east = isPath(world, ground.offset(Direction.EAST));
        boolean west = isPath(world, ground.offset(Direction.WEST));
        int sides = (north ? 1 : 0) + (south ? 1 : 0) + (east ? 1 : 0) + (west ? 1 : 0);

        return (north && south) || (east && west) || sides >= 3;
    }

    private static boolean isPath(ServerWorld world, BlockPos at) {
        return world.getBlockState(at).isOf(Blocks.DIRT_PATH);
    }

    public static void clearAll() {
        JOBS.clear();
        CURSOR.clear();
        EMPTY_SINCE.clear();
        SKIPPED.clear();
    }
}
