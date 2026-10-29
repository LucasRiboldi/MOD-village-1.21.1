package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.coordination.BedPlan;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestMarker;
import com.villagecolony.fabric.integration.ChestScanner;
import com.villagecolony.fabric.integration.VillageFoundation;
import com.villagecolony.fabric.world.GeneratedVillages;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Cada aldeão dorme na cama mais perto do trabalho — Regra 74, autor de 2026-10-22. A decisão é do Core
 * ({@link BedPlan}); aqui se mede o trabalho, se escolhe a hora e se aplica.
 *
 * <p>O trabalho é onde o aldeão trabalhou de fato: a mediana das últimas {@link #SAMPLES} ações com lugar
 * do diário (bloco minerado, posto, colhido, árvore, abate…). Quem tem menos de {@link #MIN_SAMPLES} fica
 * onde está. A vila reavalia uma vez por dia, ao amanhecer, e só oferece cama livre que se alcança a pé da
 * vila ({@link ReachMap}) e que tem baú livre ao lado. O baú acompanha a cama: numa troca cada um fica com o
 * baú do outro, remarcado com o próprio ofício; na cama livre, com o baú livre ao lado dela. A mudança é uma
 * transação (R-043 do estudo v2): sem cama ou sem baú, ninguém muda.
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
     * <p>A cama livre só entra no plano se se chega a ela a pé da vila e se há baú livre ao lado dela; a
     * distância é a caminhada pelo mapa a pé ({@link Walks}), e a linha reta só onde o mapa não alcança.
     *
     * @return quantos aldeões mudaram de cama
     */
    static int reassign(ServerWorld world, Colony colony, VillageBounds box) {
        Map<UUID, VillagerEntity> villagers = new HashMap<>();
        Map<UUID, Optional<ProfessionType>> professions = new HashMap<>();
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
            professions.put(villager.getUuid(), worker.profession());
            ColonyPos work = worker.profession().isPresent()
                    ? workOf(villager.getUuid()).map(MinecraftTypeAdapter::toColonyPos).orElse(null)
                    : null;
            sleepers.add(new BedPlan.Sleeper(villager.getUuid(), MinecraftTypeAdapter.toColonyPos(home.get()), work,
                    !villager.isSleeping()));
        }

        Set<ColonyPos> banned = ladderFloorBeds(world, box);
        boolean anyWork = sleepers.stream().anyMatch(sleeper -> sleeper.work() != null);

        if (!anyWork && sleepers.stream().noneMatch(sleeper -> banned.contains(sleeper.bed()))) {
            return 0;
        }

        Set<BlockPos> reachable = ReachMap.reachableCells(world, colony);
        List<ColonyPos> free = freeBeds(world, box).stream()
                .filter(bed -> !banned.contains(bed))
                .filter(bed -> reachable.isEmpty()
                        || ReachMap.reaches(reachable, MinecraftTypeAdapter.toBlockPos(bed), 2))
                .filter(bed -> ChestScanner.freeChestBeside(world, MinecraftTypeAdapter.toBlockPos(bed),
                        VillageColonyMod.STORAGES, Optional.empty()).isPresent())
                .toList();
        Walks walks = new Walks(reachable);

        // Primeiro sai quem dorme no andar de baixo da casa de escada de mão; ele e quem não pôde sair ficam
        // fora da troca do dia, e a cama de lá não se oferece.
        List<BedPlan.Move> leaving = BedPlan.leave(sleepers, free, banned, walks::between);
        Map<UUID, ColonyPos> movedTo = new HashMap<>();
        leaving.forEach(move -> movedTo.put(move.id(), move.to()));
        List<BedPlan.Sleeper> after = sleepers.stream()
                .map(sleeper -> movedTo.containsKey(sleeper.id()) || banned.contains(sleeper.bed())
                        ? new BedPlan.Sleeper(sleeper.id(), movedTo.getOrDefault(sleeper.id(), sleeper.bed()),
                                sleeper.work(), false)
                        : sleeper)
                .toList();
        List<ColonyPos> rest = free.stream().filter(bed -> !movedTo.containsValue(bed)).toList();
        List<BedPlan.Move> moves = new ArrayList<>(leaving);

        if (anyWork) {
            moves.addAll(BedPlan.plan(after, rest, walks::between));
        }

        return apply(world, villagers, professions, sleepers, moves);
    }

    /** As camas no andar de baixo das casas de escada de mão da vila ({@link GeneratedVillages#ladderLowerFloorsAt}). */
    private static Set<ColonyPos> ladderFloorBeds(ServerWorld world, VillageBounds box) {
        List<net.minecraft.util.math.BlockBox> floors = GeneratedVillages.ladderLowerFloorsAt(world,
                (box.minX() + box.maxX()) / 2, (box.minZ() + box.maxZ()) / 2);
        Set<ColonyPos> beds = new HashSet<>();

        if (floors.isEmpty()) {
            return beds;
        }

        for (net.minecraft.util.math.BlockBox floor : floors) {
            world.getPointOfInterestStorage()
                    .getInSquare(type -> type.matchesKey(PointOfInterestTypes.HOME), floor.getCenter(),
                            Math.max(floor.getBlockCountX(), floor.getBlockCountZ()) / 2 + 1,
                            PointOfInterestStorage.OccupationStatus.ANY)
                    .map(PointOfInterest::getPos)
                    .filter(floor::contains)
                    .map(MinecraftTypeAdapter::toColonyPos)
                    .forEach(beds::add);
        }

        return beds;
    }

    /**
     * Aplica as mudanças como uma transação: primeiro se confere, para cada uma, que a cama nova existe e que
     * há o baú que vai com ela — o do antigo dono, numa troca, ou o livre ao lado da cama livre; a mudança que
     * falha cai junto com a outra metade da troca, e quem não muda fica como estava. Só então as camas se
     * soltam, se dão, e o baú é registrado e marcado com o ofício do novo dono.
     */
    private static int apply(ServerWorld world, Map<UUID, VillagerEntity> villagers,
            Map<UUID, Optional<ProfessionType>> professions, List<BedPlan.Sleeper> sleepers,
            List<BedPlan.Move> moves) {

        PointOfInterestStorage pois = world.getPointOfInterestStorage();
        Map<ColonyPos, UUID> ownerOf = new HashMap<>();

        for (BedPlan.Sleeper sleeper : sleepers) {
            ownerOf.put(sleeper.bed(), sleeper.id());
        }

        Map<UUID, BlockPos> chestFor = new HashMap<>();
        Set<UUID> failed = new HashSet<>();

        for (BedPlan.Move move : moves) {
            BlockPos to = MinecraftTypeAdapter.toBlockPos(move.to());
            UUID previous = ownerOf.get(move.to());
            Optional<BlockPos> chest = previous != null
                    ? VillageColonyMod.STORAGES.of(previous)
                            .map(storage -> MinecraftTypeAdapter.toBlockPos(storage.chestPosition()))
                    : ChestScanner.freeChestBeside(world, to, VillageColonyMod.STORAGES,
                            professions.getOrDefault(move.id(), Optional.empty()));

            if (pois.getType(to).isEmpty() || chest.isEmpty()) {
                failed.add(move.id());
            } else {
                chestFor.put(move.id(), chest.get());
            }
        }

        // A troca é de dois: se um lado falha, o outro também não muda.
        for (BedPlan.Move move : moves) {
            UUID previous = ownerOf.get(move.to());

            if (previous != null && failed.contains(previous)) {
                failed.add(move.id());
            }
        }

        List<BedPlan.Move> done = moves.stream().filter(move -> !failed.contains(move.id())).toList();

        // Numa troca as duas camas se soltam antes de alguma ser dada: dar primeiro acharia a outra ocupada.
        for (BedPlan.Move move : done) {
            BlockPos from = MinecraftTypeAdapter.toBlockPos(move.from());

            if (pois.getType(from).isPresent()) {
                pois.releaseTicket(from);
            }
        }

        for (BedPlan.Move move : done) {
            VillagerEntity villager = Objects.requireNonNull(villagers.get(move.id()));
            BlockPos to = MinecraftTypeAdapter.toBlockPos(move.to());
            BlockPos chest = Objects.requireNonNull(chestFor.get(move.id()));

            VillageFoundation.giveHome(world, villager, to);
            VillageColonyMod.STORAGES.register(WorkerStorage.of(move.id(), MinecraftTypeAdapter.toColonyPos(chest)));
            professions.getOrDefault(move.id(), Optional.empty())
                    .ifPresent(profession -> ChestMarker.markAt(world, chest, profession));

            String work = workOf(move.id()).map(BlockPos::toShortString).orElse("nowhere — it gives its bed up");
            VillageColonyMod.LOGGER.info("Worker {} moves home from {} to {}, with the chest at {} — its work is at {}",
                    move.id().toString().substring(0, 8), MinecraftTypeAdapter.toBlockPos(move.from()).toShortString(),
                    to.toShortString(), chest.toShortString(), work);
            ActionJournal.action(move.id(), "MOVED_HOME", "", 1, to, "the bed nearest to its work");
        }

        if (!failed.isEmpty()) {
            VillageColonyMod.LOGGER.info("{} bed moves were dropped — no bed or no chest to go with it", failed.size());
        }

        return done.size();
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
