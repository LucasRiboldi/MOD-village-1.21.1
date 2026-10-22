package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.coordination.BedPlan;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageFoundation;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cada aldeão dorme na cama mais perto do trabalho — Regra 74, autor de 2026-10-22. A decisão é do Core
 * ({@link BedPlan}); aqui se mede o trabalho, se escolhe a hora e se aplica.
 *
 * <p>O trabalho é onde o aldeão trabalhou de fato: a mediana das últimas {@link #SAMPLES} ações com lugar
 * do diário (bloco minerado, posto, colhido, árvore, abate…). Quem tem menos de {@link #MIN_SAMPLES} fica
 * onde está. A vila reavalia uma vez por dia, ao amanhecer, e só oferece cama livre que se alcança a pé da
 * vila ({@link ReachMap}). O baú acompanha a cama: o registro do baú velho cai, e a varredura de baús
 * reivindica o livre ao lado da cama nova; o velho, com o que tem, fica para a vila.
 */
final class BedChoice {

    static {
        ServerMemory.register(BedChoice.class, BedChoice::clearAll);
    }

    /** Quantas ações com lugar se guardam por aldeão. */
    static final int SAMPLES = 32;

    /** Quantas ações bastam para dizer onde ele trabalha. */
    static final int MIN_SAMPLES = 8;

    /** A janela do amanhecer, em tiques do dia: a primeira hora da manhã. */
    static final long DAWN_END = 1_000;

    /** As ações do diário que marcam o lugar do trabalho. */
    static final Set<String> WORK = Set.of(
            "MINED", "SHORED", "PLACED", "PAVED", "HARVESTED", "FELLED_TREE", "BUTCHERED", "SHEARED", "SMELTED");

    private static final Map<UUID, Deque<BlockPos>> WORKED = new HashMap<>();

    private static final Map<UUID, Long> DAY_DONE = new HashMap<>();

    private BedChoice() {
    }

    /** Uma ação do diário: se é trabalho com lugar, entra na conta de onde ele trabalha. */
    static void worked(UUID worker, String action, @Nullable BlockPos at) {
        if (at == null || !WORK.contains(action)) {
            return;
        }

        Deque<BlockPos> places = WORKED.computeIfAbsent(worker, id -> new ArrayDeque<>());
        places.addLast(at.toImmutable());

        while (places.size() > SAMPLES) {
            places.removeFirst();
        }
    }

    /** Onde este aldeão trabalha: a mediana de cada eixo das últimas ações; vazio sem ações bastantes. */
    static Optional<BlockPos> workOf(UUID worker) {
        Deque<BlockPos> places = WORKED.get(worker);

        if (places == null || places.size() < MIN_SAMPLES) {
            return Optional.empty();
        }

        return Optional.of(new BlockPos(
                median(places.stream().map(BlockPos::getX).toList()),
                median(places.stream().map(BlockPos::getY).toList()),
                median(places.stream().map(BlockPos::getZ).toList())));
    }

    private static int median(List<Integer> values) {
        List<Integer> sorted = new ArrayList<>(values);
        sorted.sort(Integer::compare);

        return sorted.get(sorted.size() / 2);
    }

    /** Uma passagem: ao amanhecer, uma vez por dia, cada colônia reavalia as camas. */
    static void pass(ServerWorld world) {
        long time = world.getTimeOfDay();

        if (Math.floorMod(time, 24_000L) >= DAWN_END) {
            return;
        }

        long day = Math.floorDiv(time, 24_000L);

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            if (DAY_DONE.getOrDefault(colony.id(), Long.MIN_VALUE) != day && colony.bounds().isPresent()) {
                DAY_DONE.put(colony.id(), day);
                reassign(world, colony, colony.bounds().get());
            }
        }
    }

    /**
     * Reavalia as camas desta colônia agora.
     *
     * @return quantos aldeões mudaram de cama
     */
    static int reassign(ServerWorld world, Colony colony, VillageBounds box) {
        Map<UUID, VillagerEntity> villagers = new HashMap<>();
        List<BedPlan.Sleeper> sleepers = new ArrayList<>();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            if (!(world.getEntity(worker.villagerId()) instanceof VillagerEntity villager)
                    || !villager.isAlive() || villager.isBaby()) {
                continue;
            }

            Optional<BlockPos> home = homeOf(world, villager).filter(pos -> inside(box, pos));

            if (home.isEmpty()) {
                continue;
            }

            villagers.put(villager.getUuid(), villager);
            ColonyPos work = worker.profession().isPresent()
                    ? workOf(villager.getUuid()).map(MinecraftTypeAdapter::toColonyPos).orElse(null)
                    : null;
            sleepers.add(new BedPlan.Sleeper(villager.getUuid(), MinecraftTypeAdapter.toColonyPos(home.get()), work,
                    !villager.isSleeping()));
        }

        List<ColonyPos> free = freeBeds(world, box);
        List<BedPlan.Move> moves = BedPlan.plan(sleepers, free);
        Set<ColonyPos> takenFree = moves.stream().map(BedPlan.Move::to).filter(free::contains).collect(Collectors.toSet());

        // A cama livre só vale se dá para chegar a ela a pé da vila; a ocupada já é casa de alguém.
        if (!takenFree.isEmpty()) {
            Set<BlockPos> reachable = ReachMap.reachableCells(world, colony);

            if (!reachable.isEmpty()) {
                List<ColonyPos> reachableFree = free.stream()
                        .filter(bed -> ReachMap.reaches(reachable, MinecraftTypeAdapter.toBlockPos(bed), 2))
                        .toList();

                if (reachableFree.size() < free.size()) {
                    moves = BedPlan.plan(sleepers, reachableFree);
                }
            }
        }

        apply(world, villagers, moves);

        return moves.size();
    }

    private static void apply(ServerWorld world, Map<UUID, VillagerEntity> villagers, List<BedPlan.Move> moves) {
        PointOfInterestStorage pois = world.getPointOfInterestStorage();

        // Numa troca as duas camas se soltam antes de alguma ser dada: dar primeiro acharia a outra ocupada.
        for (BedPlan.Move move : moves) {
            BlockPos from = MinecraftTypeAdapter.toBlockPos(move.from());

            if (pois.getType(from).isPresent()) {
                pois.releaseTicket(from);
            }
        }

        for (BedPlan.Move move : moves) {
            VillagerEntity villager = java.util.Objects.requireNonNull(villagers.get(move.id()));
            BlockPos to = MinecraftTypeAdapter.toBlockPos(move.to());

            VillageFoundation.giveHome(world, villager, to);
            VillageColonyMod.STORAGES.remove(move.id());

            String work = workOf(move.id()).map(BlockPos::toShortString).orElse("nowhere — it gives its bed up");
            VillageColonyMod.LOGGER.info("Worker {} moves home from {} to {} — its work is at {}",
                    move.id().toString().substring(0, 8), MinecraftTypeAdapter.toBlockPos(move.from()).toShortString(),
                    to.toShortString(), work);
            ActionJournal.action(move.id(), "MOVED_HOME", "", 1, to, "the bed nearest to its work");
        }
    }

    /** As camas da vila sem dono. */
    private static List<ColonyPos> freeBeds(ServerWorld world, VillageBounds box) {
        BlockPos middle = new BlockPos((box.minX() + box.maxX()) / 2, (box.minY() + box.maxY()) / 2,
                (box.minZ() + box.maxZ()) / 2);
        int radius = (int) Math.ceil(Math.sqrt(
                Math.pow(box.maxX() - box.minX(), 2) + Math.pow(box.maxZ() - box.minZ(), 2)) / 2) + 1;

        return world.getPointOfInterestStorage()
                .getInCircle(type -> type.matchesKey(PointOfInterestTypes.HOME), middle, radius,
                        PointOfInterestStorage.OccupationStatus.HAS_SPACE)
                .map(PointOfInterest::getPos)
                .filter(pos -> inside(box, pos))
                .map(MinecraftTypeAdapter::toColonyPos)
                .toList();
    }

    private static Optional<BlockPos> homeOf(ServerWorld world, VillagerEntity villager) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME)
                .filter(home -> home.dimension().equals(world.getRegistryKey()))
                .map(GlobalPos::pos);
    }

    private static boolean inside(VillageBounds box, BlockPos pos) {
        return pos.getX() >= box.minX() && pos.getX() <= box.maxX()
                && pos.getZ() >= box.minZ() && pos.getZ() <= box.maxZ();
    }

    /** Esquece o trabalho medido deste aldeão — quem sai da vila, e o teste ao terminar. */
    static void forget(UUID worker) {
        WORKED.remove(worker);
    }

    static void clearAll() {
        WORKED.clear();
        DAY_DONE.clear();
    }
}
