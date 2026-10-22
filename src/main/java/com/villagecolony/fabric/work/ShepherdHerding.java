package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.ResourceReach;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A coleta de animais do pastor — ADR-038 P2b: sem tosquia, ele procura o
 * animal criável solto mais perto até a borda + 10 e põe a corda (corda
 * infinita: não gasta item). Para onde ele o leva — regra do autor, 2026-10-11
 * ({@link ShepherdPens}): para dentro do cercado com lugar mais perto da casa;
 * sem cercado com lugar, para a cerca mais perto da cama (até {@link #MAX_TIED}
 * por cerca); sem cerca, para um poste que ele põe ao lado da casa.
 */
final class ShepherdHerding {

    static {
        ServerMemory.register(ShepherdHerding.class, ShepherdHerding::clearAll);
    }

    static final int REACH = 3;

    /** Quanto a coleta pode durar antes de ele desistir. */
    static final int TIMEOUT = 2_400;

    /** Entre uma desistência (ou nada a coletar) e a próxima tentativa. */
    static final int COOLDOWN = 1_200;

    /** Até onde da cama procurar a cerca. */
    static final int FENCE_RADIUS = 16;

    /** Animais amarrados numa mesma cerca, no máximo. */
    static final int MAX_TIED = 8;

    enum Phase { FETCH, LEAD }

    static final class Herd {
        final UUID animal;
        /** A cerca onde amarrar, o poste a pôr, ou o portão do cercado. */
        final BlockPos fence;
        Phase phase = Phase.FETCH;
        final long since;
        /** O cercado de destino, ou nulo. */
        ShepherdPens.Pen pen;
        /** Se {@link #fence} é o poste que ele ainda vai pôr. */
        boolean newPost;
        /** O cercado misto de onde o animal sai para o da espécie dele (Regra 72), ou nulo. */
        ShepherdPens.Pen from;

        Herd(UUID animal, BlockPos fence, long since) {
            this.animal = animal;
            this.fence = fence;
            this.since = since;
        }
    }

    private static final Map<UUID, Herd> HERDS = new HashMap<>();

    private static final Map<UUID, Long> NEXT_TRY = new HashMap<>();

    private ShepherdHerding() {
    }

    static void clearAll() {
        HERDS.clear();
        NEXT_TRY.clear();
    }

    /**
     * As coletas em andamento, para o save — ADR-039 C. Sem elas, o animal que
     * estava na corda ao fechar o mundo voltava preso ao pastor, sem ninguém o levar.
     */
    static net.minecraft.nbt.NbtCompound save() {
        net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();

        HERDS.forEach((shepherd, herd) -> {
            net.minecraft.nbt.NbtCompound entry = new net.minecraft.nbt.NbtCompound();

            entry.putUuid("animal", herd.animal);
            entry.putLong("fence", herd.fence.asLong());
            entry.putString("phase", herd.phase.name());
            entry.putLong("since", herd.since);
            entry.putBoolean("newPost", herd.newPost);

            if (herd.from != null && herd.phase == Phase.FETCH) {
                // A separação que ainda não tirou o animal do cercado recomeça do zero.
                return;
            }

            if (herd.pen != null) {
                entry.putLong("inside", herd.pen.inside().asLong());
                entry.putLong("outside", herd.pen.outside().asLong());
            }

            nbt.put(shepherd.toString(), entry);
        });

        return nbt;
    }

    static void load(net.minecraft.nbt.NbtCompound nbt) {
        for (String key : nbt.getKeys()) {
            UUID shepherd = com.villagecolony.fabric.integration.WorkMemoryKeys.uuid(key);
            net.minecraft.nbt.NbtCompound entry = nbt.getCompound(key);

            if (shepherd == null || !entry.containsUuid("animal")) {
                continue;
            }

            Herd herd = new Herd(entry.getUuid("animal"), BlockPos.fromLong(entry.getLong("fence")), entry.getLong("since"));
            herd.phase = "LEAD".equals(entry.getString("phase")) ? Phase.LEAD : Phase.FETCH;
            herd.newPost = entry.getBoolean("newPost");

            if (entry.contains("inside")) {
                herd.pen = new ShepherdPens.Pen(herd.fence, BlockPos.fromLong(entry.getLong("inside")),
                        BlockPos.fromLong(entry.getLong("outside")), java.util.Set.of());
            }

            HERDS.put(shepherd, herd);
        }
    }

    static boolean isHerding(UUID shepherd) {
        return HERDS.containsKey(shepherd);
    }

    static Optional<Phase> phaseOf(UUID shepherd) {
        return Optional.ofNullable(HERDS.get(shepherd)).map(herd -> herd.phase);
    }

    /** Dá uma coleta a cada pastor da colônia que está sem tosquia e sem coleta. */
    static void plan(ServerWorld world, Colony colony, Predicate<UUID> shearing) {
        long now = world.getTime();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            UUID id = worker.villagerId();

            if (worker.profession().filter(ProfessionType.SHEPHERD::equals).isEmpty()
                    || shearing.test(id) || HERDS.containsKey(id)
                    || now < NEXT_TRY.getOrDefault(id, Long.MIN_VALUE)
                    || !(world.getEntity(id) instanceof VillagerEntity shepherd)
                    || !WorkHours.isWorkTime(world, shepherd)) {
                continue;
            }

            Optional<Herd> herd = start(world, colony, shepherd);

            if (herd.isPresent()) {
                HERDS.put(id, herd.get());
            } else {
                NEXT_TRY.put(id, now + COOLDOWN);
            }
        }
    }

    /**
     * A coleta deste pastor: o animal solto (ou amarrado em poste) mais perto, para o cercado da espécie
     * dele (Regra 72, {@link ShepherdPens#forSpecies}); sem cercado com lugar, o solto vai para a cerca
     * mais perto ou para um poste novo ao lado da casa. Sem nada a recolher, separa um curral misto.
     */
    static Optional<Herd> start(ServerWorld world, Colony colony, VillagerEntity shepherd) {
        BlockPos home = BedTeleport.bedOf(world, shepherd)
                .or(() -> VillageColonyMod.STORAGES.of(shepherd.getUuid())
                        .map(storage -> MinecraftTypeAdapter.toBlockPos(storage.chestPosition())))
                .orElse(shepherd.getBlockPos());

        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        int margin = ResourceReach.EDGE_MARGIN;
        Box area = colony.bounds()
                .map(box -> new Box(box.minX() - margin, box.minY() - 8, box.minZ() - margin,
                        box.maxX() + margin + 1, box.maxY() + 8, box.maxZ() + margin + 1))
                .orElseGet(() -> new Box(center).expand(48));
        List<AnimalEntity> candidates = new java.util.ArrayList<>();

        for (EntityType<? extends AnimalEntity> species : ShepherdFlock.SPECIES) {
            candidates.addAll(world.getEntitiesByType(species, area, candidate -> candidate.isAlive()
                    && candidate.canBeLeashed()
                    && (!candidate.isLeashed() || candidate.getLeashHolder() instanceof LeashKnotEntity)
                    && !ShepherdFlock.isPenned(world, candidate)));
        }

        candidates.sort(java.util.Comparator.comparingDouble(animal -> animal.squaredDistanceTo(shepherd)));

        Optional<BlockPos> fence = Optional.empty();
        Optional<BlockPos> newPost = Optional.empty();
        boolean fenceRead = false;

        for (AnimalEntity animal : candidates) {
            Optional<ShepherdPens.Pen> pen = ShepherdPens.forSpecies(world, colony, animal.getType(), home, null, false);

            if (pen.isPresent()) {
                Herd herd = new Herd(animal.getUuid(), pen.get().gate(), world.getTime());
                herd.pen = pen.get();

                return Optional.of(herd);
            }

            // Sem cercado com lugar: só o solto vai para a cerca ou para o poste novo.
            if (animal.isLeashed() || ShepherdFlock.isKept(world, animal)) {
                continue;
            }

            if (!fenceRead) {
                fenceRead = true;
                fence = nearestFence(world, home).filter(at -> tiedAt(world, at) < MAX_TIED);
                newPost = fence.isPresent() ? Optional.empty() : ShepherdPens.postBesideHome(world, colony.id(), home);
            }

            if (fence.isEmpty() && newPost.isEmpty()) {
                return Optional.empty();
            }

            BlockPos tieAt = fence.isPresent() ? fence.get() : newPost.get();
            Herd herd = new Herd(animal.getUuid(), tieAt, world.getTime());
            herd.newPost = newPost.isPresent();

            return Optional.of(herd);
        }

        return separate(world, colony, home);
    }

    /**
     * A separação dos currais mistos — Regra 72: um animal de um cercado com mais de uma espécie, para o
     * cercado só da espécie dele (ou um vazio) com lugar, para que cada tipo acabe no seu quando a vila tem
     * vários currais. Um por vez, e só quando não há animal solto a recolher.
     */
    static Optional<Herd> separate(ServerWorld world, Colony colony, BlockPos home) {
        for (ShepherdPens.Pen mixed : ShepherdPens.pensOf(world, colony)) {
            Map<EntityType<?>, Integer> inside = ShepherdPens.speciesIn(world, mixed);

            if (inside.size() < 2) {
                continue;
            }

            for (AnimalEntity animal : world.getEntitiesByClass(AnimalEntity.class,
                    new Box(mixed.inside()).expand(FencedIn.RADIUS),
                    candidate -> candidate.isAlive() && candidate.canBeLeashed() && !candidate.isLeashed()
                            && mixed.reach().contains(candidate.getBlockPos()))) {
                Optional<ShepherdPens.Pen> own =
                        ShepherdPens.forSpecies(world, colony, animal.getType(), home, mixed, true);

                if (own.isPresent()) {
                    Herd herd = new Herd(animal.getUuid(), own.get().gate(), world.getTime());
                    herd.pen = own.get();
                    herd.from = mixed;

                    VillageColonyMod.LOGGER.info("Colony {} separates the pens — a {} goes from the pen at {} to the one at {}",
                            colony.id().toString().substring(0, 8), animal.getType().getUntranslatedName(),
                            mixed.gate().toShortString(), own.get().gate().toShortString());

                    return Optional.of(herd);
                }
            }
        }

        return Optional.empty();
    }

    /** Um passo de cada coleta, a cada cinco tiques. */
    static void tick(ServerWorld world) {
        if (HERDS.isEmpty() || world.getTime() % 5 != 0) {
            return;
        }

        HERDS.entrySet().removeIf(entry -> !step(world, entry.getKey(), entry.getValue()));
    }

    /**
     * Um passo da coleta.
     *
     * @return se ela continua
     */
    static boolean step(ServerWorld world, UUID shepherdId, Herd herd) {
        Entity animalEntity = world.getEntity(herd.animal);

        if (!(world.getEntity(shepherdId) instanceof VillagerEntity shepherd)
                || !(animalEntity instanceof AnimalEntity animal) || !animal.isAlive()
                || !WorkHours.isWorkTime(world, shepherd)
                || world.getTime() - herd.since > TIMEOUT) {
            abort(world, shepherdId, herd);
            return false;
        }

        if (herd.phase == Phase.FETCH && herd.from != null) {
            // Separação: ele vai ao portão do cercado misto e o animal sai por ele, na corda.
            WorkTargets.set(shepherdId, herd.from.outside(), REACH - 1);

            if (shepherd.getBlockPos().isWithinDistance(herd.from.outside(), REACH)) {
                ShepherdPens.bringOut(world, herd.from, animal, shepherd);
                herd.phase = Phase.LEAD;
            }

            return true;
        }

        if (herd.phase == Phase.FETCH) {
            WorkTargets.set(shepherdId, animal.getBlockPos(), REACH - 1);

            if (shepherd.squaredDistanceTo(animal) <= REACH * REACH) {
                animal.attachLeash(shepherd, true);
                herd.phase = Phase.LEAD;
            }

            return true;
        }

        if (animal.getLeashHolder() != shepherd) {
            abort(world, shepherdId, herd);
            return false;
        }

        BlockPos goal = herd.pen != null ? herd.pen.outside() : herd.fence;

        WorkTargets.set(shepherdId, goal, REACH - 1);

        if (herd.pen != null && shepherd.getBlockPos().isWithinDistance(goal, REACH)) {
            ShepherdPens.bringIn(world, herd.pen, animal);
            WorkTargets.clear(shepherdId);

            // A ovelha no curral também ganha grama, para recriar a lã (PostGrass, autor 2026-10-21).
            if (animal instanceof net.minecraft.entity.passive.SheepEntity) {
                PostGrass.inPen(world, animal.getBlockPos());
            }
            VillageColonyMod.LOGGER.info("Shepherd {} brought a {} into the pen at {}",
                    shepherdId.toString().substring(0, 8), animal.getType().getUntranslatedName(),
                    herd.pen.gate().toShortString());
            return false;
        }

        if (shepherd.getBlockPos().isWithinDistance(herd.fence, REACH)) {
            // Sem poste ali: ele põe um, com a cerca do baú.
            boolean posted = !herd.newPost || VillageColonyMod.WORKERS.find(shepherdId)
                    .map(worker -> ShepherdPens.placePost(world, worker.colonyId(), herd.fence))
                    .orElse(false);

            if (!posted) {
                abort(world, shepherdId, herd);
                return false;
            }

            LeashKnotEntity knot = LeashKnotEntity.getOrCreate(world, herd.fence);

            animal.attachLeash(knot, true);
            WorkTargets.clear(shepherdId);

            // A ovelha amarrada ganha grama ao pé do poste para recriar a lã (PostGrass).
            if (animal instanceof net.minecraft.entity.passive.SheepEntity) {
                PostGrass.ensure(world, herd.fence);
            }
            VillageColonyMod.LOGGER.info("Shepherd {} brought a {} in and tied it to the fence at {}",
                    shepherdId.toString().substring(0, 8), animal.getType().getUntranslatedName(),
                    herd.fence.toShortString());
            return false;
        }

        return true;
    }

    private static void abort(ServerWorld world, UUID shepherdId, Herd herd) {
        if (world.getEntity(herd.animal) instanceof AnimalEntity animal
                && animal.getLeashHolder() != null && animal.getLeashHolder().getUuid().equals(shepherdId)) {
            animal.detachLeash(true, false);
        }

        WorkTargets.clear(shepherdId);
        NEXT_TRY.put(shepherdId, world.getTime() + COOLDOWN);
    }

    /** A cerca mais perto deste ponto, até {@link #FENCE_RADIUS}. */
    static Optional<BlockPos> nearestFence(ServerWorld world, BlockPos from) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos at : BlockPos.iterate(from.add(-FENCE_RADIUS, -3, -FENCE_RADIUS),
                from.add(FENCE_RADIUS, 3, FENCE_RADIUS))) {
            WorldChunk chunk = world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4);

            if (chunk != null && chunk.getBlockState(at).isIn(BlockTags.FENCES)) {
                double distance = at.getSquaredDistance(from);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = at.toImmutable();
                }
            }
        }

        return Optional.ofNullable(best);
    }

    /** Quantos animais estão amarrados nesta cerca. */
    static int tiedAt(ServerWorld world, BlockPos fence) {
        List<AnimalEntity> tied = world.getEntitiesByClass(AnimalEntity.class, new Box(fence).expand(10),
                animal -> animal.getLeashHolder() instanceof LeashKnotEntity knot
                        && knot.getAttachedBlockPos().equals(fence));

        return tied.size();
    }
}
