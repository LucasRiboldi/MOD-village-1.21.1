package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.DirtPatch;
import com.villagecolony.fabric.integration.FarthestVillageSector;
import com.villagecolony.fabric.integration.GrassPatch;
import com.villagecolony.fabric.integration.RingSweep;
import com.villagecolony.fabric.integration.CactusPatch;
import com.villagecolony.fabric.integration.ClayPatch;
import com.villagecolony.fabric.integration.SandPatch;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Coleta materiais naturais expostos fora da zona habitada. */
public final class SurfaceGatheringWork {

    private static final int BREAKING_STAGES = 10;
    private static final int SWING_INTERVAL = 5;
    private static final int SEARCH_RADIUS = 48;
    private static final int REACH = 4;
    private static final int STILL_LIMIT = 300;
    private static final Map<UUID, Job> JOBS = new LinkedHashMap<>();
    private static UUID lastSearchWorker;

    private static final class Job {
        private final Task task;
        private final BlockPos center;
        private final Direction surfaceSector;
        private BlockPos target;
        private int progress;
        private int required;
        private int collected;
        private int still;
        private BlockPos lastPosition;

        private Job(Task task, BlockPos center, Direction surfaceSector) {
            this.task = task;
            this.center = center;
            this.surfaceSector = surfaceSector;
        }
    }

    private SurfaceGatheringWork() {
    }

    public static int run(ServerWorld world, Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        int open = 0;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (!isSupported(task) || !isOngoing(task)) {
                continue;
            }

            Optional<UUID> executor = task.executor();
            if (executor.isEmpty()) {
                continue;
            }

            JOBS.compute(executor.get(), (worker, current) -> {
                if (current != null && current.task.id().equals(task.id())) {
                    return current;
                }

                RingSweep.forget(worker);
                Direction sector = FarthestVillageSector.farthestLoadedSector(world, center, colony.id());
                return new Job(task, center, sector);
            });
            open++;
        }

        JOBS.entrySet().removeIf(entry -> !isOngoing(entry.getValue().task));

        if (open == 0) {
            IdleLog.record(colony.id(), "surface gathering", IdleReason.NO_TASK);
        } else {
            IdleLog.clear(colony.id(), "surface gathering");
        }

        return open;
    }

    public static void tick(ServerWorld world) {
        if (JOBS.isEmpty()) {
            return;
        }

        List<UUID> searching = new ArrayList<>();
        for (var iterator = JOBS.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<UUID, Job> entry = iterator.next();
            if (!isOngoing(entry.getValue().task)) {
                WorkTargets.clear(entry.getKey());
                RingSweep.forget(entry.getKey());
                iterator.remove();
            } else if (entry.getValue().target == null) {
                searching.add(entry.getKey());
            } else {
                step(world, entry.getKey(), entry.getValue(), false);
                if (!isOngoing(entry.getValue().task)) {
                    WorkTargets.clear(entry.getKey());
                    RingSweep.forget(entry.getKey());
                    iterator.remove();
                }
            }
        }

        if (searching.isEmpty()) {
            return;
        }

        int start = lastSearchWorker == null ? 0 : (searching.indexOf(lastSearchWorker) + 1) % searching.size();
        for (int offset = 0; offset < searching.size(); offset++) {
            UUID workerId = searching.get((start + offset) % searching.size());
            Job job = JOBS.get(workerId);
            if (job != null && step(world, workerId, job, true)) {
                lastSearchWorker = workerId;
                return;
            }
        }
    }

    private static boolean step(ServerWorld world, UUID workerId, Job job, boolean maySearch) {
        Entity entity = world.getEntity(workerId);
        if (!(entity instanceof VillagerEntity villager)) {
            return false;
        }

        Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(workerId);
        if (storage.isEmpty()) {
            finish(job, workerId, "no personal chest");
            return false;
        }

        if (job.target == null) {
            if (!maySearch) {
                return false;
            }
            return findTarget(world, workerId, job);
        }

        BlockState state = world.getBlockState(job.target);
        if (state.isAir() || !BlockProtection.mayBreak(world, job.target, state)) {
            clearTarget(workerId, job);
            return false;
        }

        if (villager.squaredDistanceTo(
                job.target.getX() + 0.5, job.target.getY() + 0.5, job.target.getZ() + 0.5) > REACH * REACH) {
            if (WorkHours.isWorkTime(world, villager)) {
                if (job.lastPosition != null && job.lastPosition.equals(villager.getBlockPos())) {
                    job.still++;
                } else {
                    job.still = 0;
                    job.lastPosition = villager.getBlockPos().toImmutable();
                }
            }
            if (job.still >= STILL_LIMIT) {
                finish(job, workerId, "unable to reach " + job.target.toShortString());
            } else {
                WorkTargets.set(workerId, job.target, 1);
            }
            return false;
        }

        WorkTargets.clear(workerId);
        job.still = 0;
        job.lastPosition = villager.getBlockPos().toImmutable();
        mine(world, workerId, villager, storage.get(), job, state);
        return false;
    }

    private static boolean findTarget(ServerWorld world, UUID workerId, Job job) {
        // Areia, terra e relva são recursos de superfície: a colônia só
        // abre esta tarefa quando uma obra pediu o material, e a coleta
        // nunca deve raspar o terreno já ocupado pela vila.
        boolean outsideVillage = isOutsideVillage(job.task.targetResource());
        int protectedRadius = protectedRadius(job.task.targetResource());
        BlockPos searchCenter = outsideVillage
                ? job.center.offset(job.surfaceSector, protectedRadius + 1)
                : job.center;
        // <b>A coluna fora do setor sai de graça</b> — 2026-09-16. A busca
        // de terra e grama é um cone de 90° fora da vila, e a varredura é
        // um círculo: das 9.409 colunas de um raio 48, só um quarto servia,
        // e as outras gastavam orçamento para serem descartadas dentro do
        // teste. O log de 01:19 registrou 24 de 26 ciclos em "still
        // sweeping — the budget ran out — dirt", com as obras esperando
        // terra 93 vezes na semana.
        //
        // A pergunta é a mesma que o DirtPatch e o GrassPatch já faziam lá
        // dentro; o que muda é a hora — antes do orçamento, e sem tocar o
        // mundo. Ver RingSweep.around com filtro.
        java.util.function.Predicate<BlockPos> worthLooking = outsideVillage
                ? column -> FarthestVillageSector.isInSector(
                        job.center, column, job.surfaceSector, protectedRadius)
                : column -> true;

        Optional<BlockPos> found = RingSweep.around(
                workerId, searchCenter, SEARCH_RADIUS, worthLooking, column -> {
            if (job.task.targetResource() == ResourceType.SAND) {
                return SandPatch.in(world, column, job.center.getY())
                        .filter(pos -> BlockProtection.mayBreak(world, pos, world.getBlockState(pos)));
            }
            if (job.task.targetResource() == ResourceType.GRASS_BLOCK) {
                return GrassPatch.in(world, column, job.center.getY(), job.center, job.surfaceSector);
            }
            if (job.task.targetResource() == ResourceType.DIRT) {
                return DirtPatch.in(world, column, job.center.getY(), job.center, job.surfaceSector);
            }
            // <b>E o cacto</b> — 2026-09-19, pergunta do autor. Só o
            // TOPO: deixar a base é o replantio, e o cacto volta a
            // crescer dali. Ver CactusPatch.
            if (job.task.targetResource() == ResourceType.CACTUS) {
                return CactusPatch.in(world, column, job.center.getY());
            }
            // A argila do fundo do lago entra como bloco para a terracota
            // e como bola para o tijolo; o destino da tarefa escolhe o drop.
            if (job.task.targetResource() == ResourceType.CLAY_BALL
                    || job.task.targetResource() == ResourceType.CLAY) {
                return ClayPatch.in(world, column, job.center.getY());
            }
            return Optional.empty();
        });

        if (found.isEmpty()) {
            IdleLog.recordAt(
                    job.task.colonyId(), subject(job),
                    RingSweep.pausedAt(workerId).isPresent()
                            ? IdleReason.SWEEP_INCOMPLETE : IdleReason.NO_TARGET,
                    job.task.targetResource().name().toLowerCase(java.util.Locale.ROOT), world.getTime());
            return true;
        }

        IdleLog.clear(job.task.colonyId(), subject(job));
        job.target = found.get();
        job.progress = 0;
        job.required = 0;
        job.still = 0;
        WorkTargets.set(workerId, job.target, 1);
        return true;
    }

    private static void mine(
            ServerWorld world, UUID workerId, VillagerEntity villager,
            WorkerStorage storage, Job job, BlockState state) {
        if (job.required == 0) {
            job.required = BlockBreakTime.ticksFor(world, job.target, state, villager);
        }
        job.progress++;
        if (job.progress % SWING_INTERVAL == 1) {
            villager.swingHand(Hand.MAIN_HAND);
        }
        if (job.progress < job.required) {
            world.setBlockBreakingInfo(
                    villager.getId(), job.target, job.progress * BREAKING_STAGES / job.required);
            return;
        }

        world.setBlockBreakingInfo(villager.getId(), job.target, -1);
        // A pá do fundidor tem Toque Suave para recolher o bloco que a
        // fornalha transforma em terracota. Tijolos, porém, exigem as bolas
        // Vanilla, então esta ordem calcula a quebra com uma pá sem encanto.
        ItemStack tool = job.task.targetResource() == ResourceType.CLAY_BALL
                ? new ItemStack(Items.IRON_SHOVEL)
                : villager.getMainHandStack();
        List<ItemStack> drops = Block.getDroppedStacks(
                state, world, job.target, world.getBlockEntity(job.target), villager, tool);
        int amount = drops.stream()
                .filter(stack -> !stack.isEmpty())
                .filter(stack -> MinecraftTypeAdapter.toResourceType(stack.getItem())
                        .filter(job.task.targetResource()::equals).isPresent())
                .mapToInt(ItemStack::getCount).sum();

        if (amount == 0 || drops.stream().filter(stack -> !stack.isEmpty())
                .anyMatch(stack -> MinecraftTypeAdapter.toResourceType(stack.getItem())
                        .filter(job.task.targetResource()::equals).isEmpty())) {
            finish(job, workerId, "tool did not yield " + job.task.targetResource());
            return;
        }

        ColonyPos chest = storage.chestPosition();
        for (ItemStack drop : drops) {
            if (!drop.isEmpty() && ChestDepositor.freeSpaceFor(world, chest, drop.getItem()) < drop.getCount()) {
                finish(job, workerId, "personal chest has no room for " + drop.getItem());
                return;
            }
        }

        world.removeBlock(job.target, false);
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                ChestDepositor.deposit(world, chest, drop.getItem(), drop.getCount());
            }
        }
        job.collected += amount;
        VillageColonyMod.LOGGER.info(
                "{} {} gathered {} {} at {} — {}/{} this task",
                role(job), workerId, amount, job.task.targetResource(), job.target.toShortString(),
                job.collected, job.task.amount());

        if (job.collected >= job.task.amount()) {
            if (job.task.state() == TaskState.RESERVED) {
                job.task.start();
            }
            job.task.complete();
            finish(job, workerId, "natural resource order filled");
            return;
        }
        clearTarget(workerId, job);
    }

    private static void clearTarget(UUID workerId, Job job) {
        job.target = null;
        job.progress = 0;
        job.required = 0;
        job.still = 0;
        WorkTargets.clear(workerId);
    }

    private static void finish(Job job, UUID workerId, String why) {
        VillageColonyMod.LOGGER.info("{} {} stopped surface gathering — {}", role(job), workerId, why);
        if (isOngoing(job.task)) {
            job.task.release();
        }
        WorkTargets.clear(workerId);
        RingSweep.forget(workerId);
        job.target = null;
    }

    private static boolean isOngoing(Task task) {
        return task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING;
    }

    private static boolean isSupported(Task task) {
        return task.type() == TaskType.COLLECT_SURFACE_RESOURCE
                || task.type() == TaskType.COLLECT_SOIL;
    }

    private static boolean isOutsideVillage(ResourceType resource) {
        return resource == ResourceType.SAND
                || resource == ResourceType.GRASS_BLOCK
                || resource == ResourceType.DIRT
                || resource == ResourceType.CLAY_BALL
                || resource == ResourceType.CLAY;
    }

    private static int protectedRadius(ResourceType resource) {
        return resource == ResourceType.DIRT
                ? FarthestVillageSector.SOIL_PROTECTED_RADIUS
                : FarthestVillageSector.PROTECTED_RADIUS;
    }

    private static String subject(Job job) {
        return job.task.type() == TaskType.COLLECT_SOIL ? "farmer soil" : "smelter surface";
    }

    private static String role(Job job) {
        return job.task.type() == TaskType.COLLECT_SOIL ? "Farmer" : "Smelter";
    }

    public static void forget(UUID workerId) {
        Job job = JOBS.remove(workerId);
        if (job != null) {
            WorkTargets.clear(workerId);
            RingSweep.forget(workerId);
        }
    }

    public static void clearAll() {
        for (UUID workerId : JOBS.keySet()) {
            WorkTargets.clear(workerId);
            RingSweep.forget(workerId);
        }
        JOBS.clear();
        lastSearchWorker = null;
    }
}
