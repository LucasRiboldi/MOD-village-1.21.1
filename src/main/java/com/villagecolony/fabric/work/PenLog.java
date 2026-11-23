package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ServerMemory;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Telemetria transitória dos currais e da criação (Regras 167 e 238). */
public final class PenLog {

    static {
        ServerMemory.register(PenLog.class, PenLog::clearAll);
    }

    private static final long SAMPLE_TICKS = 100;
    private static final long LOVE_TICKS = 600;
    private static final Map<UUID, Watch> WATCHES = new HashMap<>();
    private static final List<FedPair> FED = new ArrayList<>();

    private record Seen(BlockPos gate, String species, boolean young) {
    }

    private record Watch(ServerWorld world, long at, List<ShepherdPens.Pen> pens, Map<UUID, Seen> seen) {
        BlockPos gateAt(BlockPos at) {
            return pens.stream().filter(pen -> pen.reach().contains(at))
                    .map(ShepherdPens.Pen::gate).findFirst().orElse(null);
        }
    }

    private record FedPair(ServerWorld world, UUID colony, UUID first, UUID second, long at) {
        boolean matches(AnimalEntity child) {
            return world.getEntity(first) instanceof AnimalEntity a && world.getEntity(second) instanceof AnimalEntity b
                    && a.getType() == child.getType() && b.getType() == child.getType()
                    // Vanilla põe os dois pais em descanso antes de adicionar o recém-nascido ao mundo.
                    && a.getBreedingAge() == 6000 && b.getBreedingAge() == 6000
                    && (a.squaredDistanceTo(child) < 1.0 || b.squaredDistanceTo(child) < 1.0);
        }
    }

    private PenLog() {
    }

    private static void clearAll() {
        WATCHES.clear();
        FED.clear();
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof AnimalEntity child && child.getBreedingAge() == -24_000) {
                onNewborn(world, child);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof AnimalEntity animal && animal.getWorld() instanceof ServerWorld world) {
                onDeath(world, animal, source.getName());
            }
        });
    }

    static void write(ServerWorld world, Colony colony, List<ShepherdPens.Pen> pens, List<BlockPos> openGates) {
        StringBuilder line = new StringBuilder();
        List<AnimalEntity> all = new ArrayList<>();
        for (PenFlock.Stock stock : PenFlock.read(world, pens).pens()) {
            var pen = stock.pen();
            all.addAll(stock.animals());
            line.append(" pen[gate=").append(pen.gate().toShortString()).append(" cells=").append(pen.reach().size())
                    .append(" animals=").append(stock.animals().size()).append(" large=").append(pen.large())
                    .append(" species=").append(counts(stock.animals())).append(']');
        }
        for (BlockPos gate : openGates) {
            line.append(" open[gate=").append(gate.toShortString()).append(']');
        }

        VillageColonyMod.LOGGER.info("VC_PENS colony={}{} stored={} species_total={}",
                shortId(colony.id()), line, all.size(), counts(all));
    }

    private static Map<String, String> counts(List<AnimalEntity> animals) {
        Map<String, int[]> bySpecies = new TreeMap<>();
        for (AnimalEntity animal : animals) {
            bySpecies.computeIfAbsent(species(animal), key -> new int[2])[animal.isBaby() ? 1 : 0]++;
        }
        Map<String, String> counts = new TreeMap<>();
        bySpecies.forEach((species, count) -> counts.put(species, count[0] + "adult/" + count[1] + "young"));
        return counts;
    }

    static void observe(ServerWorld world, Colony colony) {
        UUID id = colony.id();
        Watch before = WATCHES.get(id);
        long now = world.getTime();
        if (before != null && before.world() == world && now - before.at() < SAMPLE_TICKS) {
            return;
        }

        List<ShepherdPens.Pen> pens = ShepherdPens.pensOf(world, colony);
        Map<UUID, Seen> current = new HashMap<>();
        for (PenFlock.Stock stock : PenFlock.read(world, pens).pens()) {
            for (AnimalEntity animal : stock.animals()) {
                current.put(animal.getUuid(), new Seen(stock.pen().gate(), species(animal), animal.isBaby()));
            }
        }
        if (before != null && before.world() == world) {
            for (var entry : before.seen().entrySet()) {
                Seen next = current.get(entry.getKey());
                Seen previous = entry.getValue();
                if (next == null || !next.gate().equals(previous.gate())) {
                    var entity = world.getEntity(entry.getKey());
                    if (entity instanceof AnimalEntity animal && !animal.isAlive()) {
                        continue;
                    }
                    String event = "EXIT";
                    if (entity == null || pens.stream().noneMatch(pen -> pen.gate().equals(previous.gate()))) {
                        event = "UNOBSERVED";
                    }
                    membership(id, entry.getKey(), previous, event);
                }
            }
            for (var entry : current.entrySet()) {
                Seen previous = before.seen().get(entry.getKey());
                if (previous == null || !previous.gate().equals(entry.getValue().gate())) {
                    membership(id, entry.getKey(), entry.getValue(), "ENTER_OBSERVED");
                }
            }
        }
        WATCHES.put(id, new Watch(world, now, pens, current));
    }

    private static void membership(UUID colony, UUID animal, Seen seen, String event) {
        VillageColonyMod.LOGGER.info("VC_FLOCK_EVENT event={} colony={} animal={} species={} gate={} young={}",
                event, shortId(colony), animal, seen.species(), seen.gate().toShortString(), seen.young());
    }

    static void fed(ServerWorld world, Colony colony, VillagerEntity shepherd, AnimalEntity first, AnimalEntity second,
            Item food, int adults, int target, String source) {
        FED.removeIf(pair -> pair.world().getTime() - pair.at() >= LOVE_TICKS);
        FED.add(new FedPair(world, colony.id(), first.getUuid(), second.getUuid(), world.getTime()));
        VillageColonyMod.LOGGER.info(
                "VC_FLOCK_EVENT event=FED colony={} shepherd={} animal={} mate={} species={} at={} food={} source={} adults={} target={}",
                shortId(colony.id()), shepherd.getUuid(), first.getUuid(), second.getUuid(), species(first),
                first.getBlockPos().toShortString(), Registries.ITEM.getId(food), source, adults, target);
    }

    static void breedingWait(Colony colony, EntityType<?> species, int adults, int target, String reason) {
        VillageColonyMod.LOGGER.info("VC_FLOCK_EVENT event=BREED_WAIT colony={} species={} adults={} target={} reason={}",
                shortId(colony.id()), Registries.ENTITY_TYPE.getId(species), adults, target, reason);
    }

    private static void onNewborn(ServerWorld world, AnimalEntity child) {
        FED.removeIf(pair -> pair.world().getTime() - pair.at() >= LOVE_TICKS);
        for (var it = FED.iterator(); it.hasNext();) {
            FedPair pair = it.next();
            if (pair.world() == world && pair.matches(child)) {
                VillageColonyMod.LOGGER.info(
                        "VC_FLOCK_EVENT event=BIRTH colony={} animal={} species={} at={} parent={} mate={}",
                        shortId(pair.colony()), child.getUuid(), species(child), child.getBlockPos().toShortString(),
                        pair.first(), pair.second());
                it.remove();
                return;
            }
        }
    }

    private static void onDeath(ServerWorld world, AnimalEntity animal, String cause) {
        WATCHES.forEach((colony, watch) -> {
            if (watch.world() != world) {
                return;
            }
            BlockPos gate = watch.gateAt(animal.getBlockPos());
            if (gate != null) {
                VillageColonyMod.LOGGER.info(
                        "VC_FLOCK_EVENT event=DEATH colony={} animal={} species={} gate={} at={} young={} cause={}",
                        shortId(colony), animal.getUuid(), species(animal), gate.toShortString(),
                        animal.getBlockPos().toShortString(), animal.isBaby(), cause);
            }
        });
    }

    static void herdEvent(ServerWorld world, UUID shepherd, UUID animalId, BlockPos goal, String event, String detail) {
        String colony = VillageColonyMod.WORKERS.find(shepherd).map(worker -> shortId(worker.colonyId())).orElse("?");
        var animal = world.getEntity(animalId);
        var worker = world.getEntity(shepherd);
        VillageColonyMod.LOGGER.info(
                "VC_FLOCK_EVENT event={} colony={} shepherd={} animal={} species={} goal={} animal_at={} shepherd_at={} detail={}",
                event, colony, shepherd, animalId, animal instanceof AnimalEntity livestock ? species(livestock) : "?",
                goal.toShortString(), animal == null ? "?" : animal.getBlockPos().toShortString(),
                worker == null ? "?" : worker.getBlockPos().toShortString(), detail);
    }

    private static String species(AnimalEntity animal) {
        return Registries.ENTITY_TYPE.getId(animal.getType()).toString();
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
