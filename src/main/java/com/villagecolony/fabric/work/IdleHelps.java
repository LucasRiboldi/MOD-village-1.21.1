package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.ColonyTorches;
import com.villagecolony.fabric.integration.CropPatch;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * As ajudas de quem está sem tarefa do ofício — item 8 das escolhas do autor (2026-10-27), cada ajuda com
 * um ofício fixo:
 * <ul>
 *   <li><b>o pastor colhe e replanta a roça madura</b>, como o fazendeiro, sem arar terra nova; a colheita
 *       vai para o baú da colônia mais perto. Um pastor por colônia por vez;</li>
 *   <li><b>o lenhador cuida do bosque</b>: vai até ele e repõe a muda que falta e põe farinha de osso,
 *       sem esperar o relógio do bosque, no máximo uma vez a cada {@link #GROVE_EVERY};</li>
 *   <li><b>o carpinteiro põe a tocha da colônia</b> ao lado do caminho onde a luz de bloco é zero, sem
 *       tocha da colônia a menos de {@link #TORCH_SPACING} blocos; a tocha surge sem custo
 *       ({@code ColonyTorches}).</li>
 * </ul>
 */
public final class IdleHelps {

    static {
        ServerMemory.register(IdleHelps.class, IdleHelps::clearAll);
    }

    /** A que distância da lavoura ele colhe. */
    static final int REACH = 2;

    /** Quanto ele anda até a lavoura antes de largar. */
    static final int GIVE_UP_AFTER = 600;

    private static final class Help {
        final UUID worker;
        final BlockPos target;
        int walked;

        Help(UUID worker, BlockPos target) {
            this.worker = worker;
            this.target = target;
        }
    }

    private static final Map<UUID, Help> FIELD = new HashMap<>();

    /** De quanto em quanto tempo o lenhador sem tarefa cuida do bosque: meio minuto. */
    static final long GROVE_EVERY = 600;

    private static final Map<UUID, Help> GROVE = new HashMap<>();

    private static final Map<UUID, Long> GROVE_DONE = new HashMap<>();

    /** A menor distância entre duas tochas da colônia no caminho. */
    static final int TORCH_SPACING = 8;

    /** Colunas de caminho olhadas por passagem atrás de lugar escuro. */
    static final int TORCH_SCAN = 512;

    private static final Map<UUID, Help> TORCH = new HashMap<>();

    private static final Map<UUID, Integer> TORCH_CURSOR = new HashMap<>();

    private IdleHelps() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();

            if (world.getTime() % 10 == 5) {
                tick(world);
            }
        });
    }

    /** Se este aldeão está numa ajuda agora — conta como trabalho no {@code WorkTime}. */
    public static boolean isHelping(UUID workerId) {
        return FIELD.values().stream().anyMatch(help -> help.worker.equals(workerId))
                || GROVE.values().stream().anyMatch(help -> help.worker.equals(workerId))
                || TORCH.values().stream().anyMatch(help -> help.worker.equals(workerId));
    }

    private static void tick(ServerWorld world) {
        long now = world.getTime();

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (colony.isActive() && colony.isAttended(now)) {
                shepherdInTheField(world, colony);
                lumberjackInTheGrove(world, colony);
                carpenterLightsThePath(world, colony);
            }
        }
    }

    /** O pastor sem tarefa colhe e replanta a lavoura madura conhecida mais perto dele. */
    static void shepherdInTheField(ServerWorld world, Colony colony) {
        Help help = FIELD.get(colony.id());

        if (help != null && !(world.getEntity(help.worker) instanceof VillagerEntity helper && isFree(world, helper))) {
            stop(colony.id());
            help = null;
        }

        if (help == null) {
            for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
                if (worker.profession().filter(ProfessionType.SHEPHERD::equals).isEmpty()
                        || !(world.getEntity(worker.villagerId()) instanceof VillagerEntity shepherd)
                        || !isFree(world, shepherd)) {
                    continue;
                }

                Optional<BlockPos> ripe = com.villagecolony.fabric.integration.KnownFarmland.ripeNear(world, colony.id(), shepherd.getBlockPos());

                if (ripe.isEmpty()) {
                    return;
                }

                help = new Help(shepherd.getUuid(), ripe.get());
                FIELD.put(colony.id(), help);
                break;
            }
        }

        if (help == null || !(world.getEntity(help.worker) instanceof VillagerEntity shepherd)) {
            return;
        }

        if (!CropPatch.isRipe(world.getBlockState(help.target))) {
            stop(colony.id());
            return;
        }

        if (!shepherd.getBlockPos().isWithinDistance(help.target, REACH + 1)) {
            help.walked += 10;

            if (help.walked > GIVE_UP_AFTER) {
                stop(colony.id());
                return;
            }

            WorkTargets.set(shepherd.getUuid(), help.target, 1);
            return;
        }

        shepherd.swingHand(Hand.MAIN_HAND);
        int stored = harvestAt(world, colony.id(), help.target);

        ActionJournal.action(shepherd.getUuid(), "HARVESTED", "crop", stored, help.target, "helping the farmer");
        VillageColonyMod.LOGGER.info("Shepherd {} harvested {} at {} for the farmer — replanted",
                shepherd.getUuid().toString().substring(0, 8), stored, help.target.toShortString());
        stop(colony.id());
    }

    /** O lenhador sem tarefa vai ao bosque e cuida dele. */
    static void lumberjackInTheGrove(ServerWorld world, Colony colony) {
        long now = world.getTime();
        Help help = GROVE.get(colony.id());

        if (help != null && !(world.getEntity(help.worker) instanceof VillagerEntity helper && isFree(world, helper))) {
            stopGrove(colony.id());
            help = null;
        }

        if (help == null) {
            if (now - GROVE_DONE.getOrDefault(colony.id(), Long.MIN_VALUE / 2) < GROVE_EVERY) {
                return;
            }

            List<BlockPos> grove = VillageGrove.of(colony.id());
            BlockPos target = grove.isEmpty() ? MinecraftTypeAdapter.toBlockPos(colony.center()) : grove.getFirst();

            for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
                if (worker.profession().filter(ProfessionType.LUMBERJACK::equals).isPresent()
                        && world.getEntity(worker.villagerId()) instanceof VillagerEntity lumberjack
                        && isFree(world, lumberjack)) {
                    help = new Help(lumberjack.getUuid(), target);
                    GROVE.put(colony.id(), help);
                    break;
                }
            }
        }

        if (help == null || !(world.getEntity(help.worker) instanceof VillagerEntity lumberjack)) {
            return;
        }

        if (!lumberjack.getBlockPos().isWithinDistance(help.target, 4)) {
            help.walked += 10;

            if (help.walked > GIVE_UP_AFTER) {
                stopGrove(colony.id());
                GROVE_DONE.put(colony.id(), now);
                return;
            }

            WorkTargets.set(lumberjack.getUuid(), help.target.up(), 2);
            return;
        }

        lumberjack.swingHand(Hand.MAIN_HAND);
        int planted = VillageGrove.tendNow(world, colony);

        VillageColonyMod.LOGGER.info("Lumberjack {} tended the grove — {} sapling(s) planted, bone meal on the rest",
                lumberjack.getUuid().toString().substring(0, 8), planted);
        stopGrove(colony.id());
        GROVE_DONE.put(colony.id(), now);
    }

    /** O carpinteiro sem tarefa põe a tocha da colônia no lugar escuro do caminho. */
    static void carpenterLightsThePath(ServerWorld world, Colony colony) {
        Help help = TORCH.get(colony.id());

        if (help != null && !(world.getEntity(help.worker) instanceof VillagerEntity helper && isFree(world, helper))) {
            stopTorch(colony.id());
            help = null;
        }

        if (help == null) {
            for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
                if (worker.profession().filter(ProfessionType.CARPENTER::equals).isEmpty()
                        || !(world.getEntity(worker.villagerId()) instanceof VillagerEntity carpenter)
                        || !isFree(world, carpenter) || PathPaving.isPaving(carpenter.getUuid())
                        || BuilderPaving.isPaving(carpenter.getUuid())) {
                    continue;
                }

                Optional<BlockPos> dark = darkSpotBesideThePath(world, colony.id());

                if (dark.isEmpty()) {
                    return;
                }

                help = new Help(carpenter.getUuid(), dark.get());
                TORCH.put(colony.id(), help);
                break;
            }
        }

        if (help == null || !(world.getEntity(help.worker) instanceof VillagerEntity carpenter)) {
            return;
        }

        if (!carpenter.getBlockPos().isWithinDistance(help.target, 3)) {
            help.walked += 10;

            if (help.walked > GIVE_UP_AFTER) {
                stopTorch(colony.id());
                return;
            }

            WorkTargets.set(carpenter.getUuid(), help.target, 2);
            return;
        }

        if (world.getBlockState(help.target).isAir()) {
            carpenter.swingHand(Hand.MAIN_HAND);
            ColonyTorches.place(world, help.target);
            ActionJournal.action(carpenter.getUuid(), "PLACED", "colony_torch", 1, help.target, "dark path");
            VillageColonyMod.LOGGER.info("Carpenter {} lit the dark path with a colony torch at {} (Regra 127)",
                    carpenter.getUuid().toString().substring(0, 8), help.target.toShortString());
        }

        stopTorch(colony.id());
    }

    /**
     * O primeiro lugar escuro ao lado do caminho, a partir de onde a varredura parou: coluna de caminho com
     * luz de bloco zero, sem tocha da colônia a {@link #TORCH_SPACING} blocos, e chão firme e ar ao lado.
     */
    static Optional<BlockPos> darkSpotBesideThePath(ServerWorld world, UUID colonyId) {
        Set<Long> roads = PathLinks.roadsOf(colonyId);

        if (roads.isEmpty()) {
            return Optional.empty();
        }

        List<Long> order = new ArrayList<>(roads);
        order.sort(Long::compare);

        int start = TORCH_CURSOR.getOrDefault(colonyId, 0) % order.size();
        int scan = Math.min(TORCH_SCAN, order.size());

        for (int seen = 0; seen < scan; seen++) {
            int index = (start + seen) % order.size();
            int x = ColonyRoads.xOf(order.get(index));
            int z = ColonyRoads.zOf(order.get(index));

            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            BlockPos path = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);

            if (world.getLightLevel(LightType.BLOCK, path) > 0
                    || ColonyTorches.anyWithin(world, path, TORCH_SPACING)) {
                continue;
            }

            for (Direction way : Direction.Type.HORIZONTAL) {
                BlockPos beside = path.offset(way);

                if (!roads.contains(ColonyRoads.column(beside.getX(), beside.getZ()))
                        && world.getBlockState(beside).isAir()
                        && world.getBlockState(beside.down()).isSideSolidFullSquare(world, beside.down(), Direction.UP)) {
                    TORCH_CURSOR.put(colonyId, index + 1);
                    return Optional.of(beside.toImmutable());
                }
            }
        }

        TORCH_CURSOR.put(colonyId, start + scan);

        return Optional.empty();
    }

    private static void stopTorch(UUID colonyId) {
        Help help = TORCH.remove(colonyId);

        if (help != null) {
            WorkTargets.clear(help.worker);
        }
    }

    private static void stopGrove(UUID colonyId) {
        Help help = GROVE.remove(colonyId);

        if (help != null) {
            WorkTargets.clear(help.worker);
        }
    }

    /**
     * Colhe e replanta esta lavoura, e guarda o que sobra no baú da colônia mais perto.
     *
     * @return quantos itens foram guardados
     */
    static int harvestAt(ServerWorld world, UUID colonyId, BlockPos crop) {
        BlockState state = world.getBlockState(crop);
        List<ItemStack> drops = new ArrayList<>(Block.getDroppedStacks(state, world, crop, null, null, ItemStack.EMPTY));

        CropPatch.replant(world, crop, state, drops);

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId, MinecraftTypeAdapter.toColonyPos(crop));
        int stored = 0;

        for (ItemStack stack : drops) {
            int left = ColonyChests.deposit(world, chests, stack.getItem(), stack.getCount());
            stored += stack.getCount() - left;
        }

        return stored;
    }

    private static boolean isFree(ServerWorld world, VillagerEntity villager) {
        UUID id = villager.getUuid();

        return villager.isAlive() && !villager.isSleeping()
                && VillageColonyMod.TASKS.assignedTo(id).isEmpty()
                && WorkHours.isWorkTime(world, villager)
                && !StrandedWorkers.isStranded(id) && !PenEscape.isEscaping(id)
                && !ShepherdHerding.isHerding(id) && !GroundPickup.isHelping(id);
    }

    private static void stop(UUID colonyId) {
        Help help = FIELD.remove(colonyId);

        if (help != null) {
            WorkTargets.clear(help.worker);
        }
    }

    static void clearAll() {
        FIELD.clear();
        GROVE.clear();
        GROVE_DONE.clear();
        TORCH.clear();
        TORCH_CURSOR.clear();
    }
}
