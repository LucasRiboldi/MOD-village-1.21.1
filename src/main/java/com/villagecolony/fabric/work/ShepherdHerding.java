package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A coleta de animais do pastor — ADR-038 P2b: sem tosquia, ele procura o
 * animal criável solto ou amarrado mais perto até a borda + 10 e põe a corda
 * (corda infinita: não gasta item). Para onde ele o leva — Regra 196
 * ({@link ShepherdPens}): para dentro do cercado com lugar mais perto da casa.
 * Só sem cercado, ou com todos lotados, o solto vai para a cerca mais perto da
 * cama (até {@link #MAX_TIED} por cerca); sem cerca, para um poste que ele põe ao
 * lado da casa.
 */
final class ShepherdHerding {

    static {
        ServerMemory.register(ShepherdHerding.class, ShepherdHerding::clearAll);
    }

    static final int REACH = 3;

    /** O prazo mínimo da coleta; a distância soma mais (Regra 269, {@link HerdDeadline}). */
    static final int TIMEOUT = 2_400;

    /** Entre uma desistência (ou nada a coletar) e a próxima tentativa. */
    static final int COOLDOWN = 1_200;

    /** Animais amarrados numa mesma cerca, no máximo. */
    static final int MAX_TIED = 8;

    /** {@code EXIT} e {@code ENTER}: o animal atravessa a porteira andando (Regra 299). */
    enum Phase { FETCH, EXIT, LEAD, JUMP_IN, ALIGN, ENTER, JUMP_OUT }

    static final class Herd {
        final UUID animal;
        /** A cerca onde amarrar, o poste a pôr, ou o portão do cercado. */
        final BlockPos fence;
        final ShepherdDelivery delivery = new ShepherdDelivery();
        Phase phase = Phase.FETCH;
        final long since;
        /** O cercado de destino, ou nulo. */
        ShepherdPens.Pen pen;
        /** Se {@link #fence} é o poste que ele ainda vai pôr. */
        boolean newPost;
        /** O cercado misto de onde o animal sai para o da espécie dele (Regra 72), ou nulo. */
        ShepherdPens.Pen from;
        /** Quando o animal começou a atravessar o portão andando. */
        long walking;
        /** O prazo desta coleta, em tiques desde {@link #since}; zero até o primeiro passo (Regra 269). */
        long deadline;

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
            entry.putBoolean("vaultDelivery", true);
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
            String saved = entry.getString("phase");
            if (entry.getBoolean("vaultDelivery") && entry.contains("inside")) {
                herd.phase = switch (saved) {
                    case "JUMP_IN", "ALIGN", "ENTER" -> Phase.LEAD;
                    case "JUMP_OUT" -> Phase.JUMP_OUT;
                    default -> "LEAD".equals(saved) ? Phase.LEAD : Phase.FETCH;
                };
            } else if ("LEAD".equals(saved)) {
                herd.phase = Phase.LEAD;
            }
            herd.newPost = entry.getBoolean("newPost");

            if (entry.contains("inside")) {
                herd.pen = new ShepherdPens.Pen(herd.fence, BlockPos.fromLong(entry.getLong("inside")),
                        BlockPos.fromLong(entry.getLong("outside")), java.util.Set.of(), false);
            }

            HERDS.put(shepherd, herd);
        }
    }

    static boolean atGate(Phase phase) {
        return phase == Phase.JUMP_IN || phase == Phase.ALIGN || phase == Phase.ENTER || phase == Phase.JUMP_OUT;
    }

    static boolean isHerding(UUID shepherd) {
        return HERDS.containsKey(shepherd);
    }

    /** Se este animal está numa coleta em andamento (Regra 299: o abate não o leva). */
    static boolean isHerded(UUID animal) {
        return HERDS.values().stream().anyMatch(herd -> herd.animal.equals(animal));
    }

    static Optional<Phase> phaseOf(UUID shepherd) {
        return Optional.ofNullable(HERDS.get(shepherd)).map(herd -> herd.phase);
    }

    /** Dá uma coleta a cada pastor da colônia que não está com ovelha escolhida nem em outra coleta. */
    static void plan(ServerWorld world, Colony colony, Predicate<UUID> shearing) {
        long now = world.getTime();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            UUID id = worker.villagerId();

            if (worker.profession().filter(ProfessionType.SHEPHERD::equals).isEmpty()
                    || PenEscape.isEscaping(id) || shearing.test(id) || HERDS.containsKey(id) || ShepherdRound.isWalking(id)
                    || now < NEXT_TRY.getOrDefault(id, Long.MIN_VALUE)
                    || !(world.getEntity(id) instanceof VillagerEntity shepherd)
                    || !WorkHours.isWorkTime(world, shepherd)) {
                continue;
            }

            // A ronda do dia vem antes da coleta (Regra 329).
            if (ShepherdRound.due(world, id) && ShepherdRound.start(world, colony, shepherd)) {
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
     * mais perto ou para um poste novo ao lado da casa, e só quando a vila não tem cercado com lugar (Regra 196). Sem
     * nada a recolher, separa um curral misto. Só recolhe dentro da caixa da vila; a vila sem caixa usa 48 blocos em
     * volta do centro (Regra 283).
     */
    static Optional<Herd> start(ServerWorld world, Colony colony, VillagerEntity shepherd) {
        BlockPos home = BedTeleport.bedOf(world, shepherd)
                .or(() -> VillageColonyMod.STORAGES.of(shepherd.getUuid())
                        .map(storage -> MinecraftTypeAdapter.toBlockPos(storage.chestPosition())))
                .orElse(shepherd.getBlockPos());

        List<AnimalEntity> candidates = HerdCandidates.of(world, colony, shepherd);

        Optional<BlockPos> fence = Optional.empty();
        Optional<BlockPos> newPost = Optional.empty();
        boolean fenceRead = false;

        for (AnimalEntity animal : candidates) {
            Optional<ShepherdPens.Pen> pen = ShepherdPens.forSpecies(world, colony, animal.getType(), shepherd, null, false);

            if (pen.isPresent()) {
                Herd herd = new Herd(animal.getUuid(), pen.get().gate(), world.getTime());
                herd.pen = pen.get();

                return Optional.of(herd);
            }

            // Com curral na vila, nunca amarra: o animal espera a próxima tentativa, e o log diz por que o curral não
            // serviu (Regra 255). Sem curral nenhum, só o solto vai para a cerca ou para o poste novo (Regra 196).
            if (!ShepherdPens.pensOf(world, colony).isEmpty()) {
                PenRefusal.log(world, colony, shepherd, animal);

                return PenRegroup.separate(world, colony, shepherd);
            }

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

        return PenRegroup.separate(world, colony, shepherd);
    }


    /** Condução a cada cinco tiques e travessia a cada tique para acompanhar a física do salto. */
    static void tick(ServerWorld world) {
        if (HERDS.isEmpty()) {
            return;
        }

        HERDS.entrySet().removeIf(entry -> {
            if (!atGate(entry.getValue().phase) && world.getTime() % 5 != 0) return false;
            boolean goesOn = step(world, entry.getKey(), entry.getValue());

            if (!goesOn) {
                HerdPatience.forget(entry.getKey());
            }

            return !goesOn;
        });
    }

    /**
     * Um passo da coleta.
     *
     * @return se ela continua
     */
    static boolean step(ServerWorld world, UUID shepherdId, Herd herd) {
        Entity animalEntity = world.getEntity(herd.animal);

        if (herd.pen != null && herd.pen.reach().isEmpty()) {
            var enclosure = FencedIn.penAt(world, herd.pen.inside());
            if (!enclosure.enclosed() || !enclosure.gates().contains(herd.fence)) {
                abort(world, shepherdId, herd);
                return false;
            }
            herd.pen = new ShepherdPens.Pen(herd.fence, herd.pen.inside(), herd.pen.outside(), enclosure.reach(), false);
        }
        if (herd.phase == Phase.JUMP_OUT && world.getEntity(shepherdId) instanceof VillagerEntity leaving) {
            return !herd.delivery.cross(world, leaving, herd.pen, false);
        }

        // Rumo a um curral, sem prazo e sem expediente: o animal só é solto dentro dele (Regra 309); a ida até o animal
        // tem a paciência da Regra 328.
        boolean toPen = herd.pen != null && herd.from == null;

        if (!(world.getEntity(shepherdId) instanceof VillagerEntity shepherd)
                || !(animalEntity instanceof AnimalEntity animal) || !animal.isAlive()
                || (!toPen && (!WorkHours.isWorkTime(world, shepherd)
                        || world.getTime() - herd.since > HerdDeadline.of(herd, shepherd, animal)))) {
            abort(world, shepherdId, herd);
            return false;
        }

        if (herd.phase == Phase.FETCH && herd.from != null) {
            // Separação: ele vai ao portão do cercado misto e o animal sai por ele, na corda.
            WorkTargets.set(shepherdId, herd.from.outside(), REACH - 1);

            if (shepherd.getBlockPos().isWithinDistance(herd.from.outside(), REACH)) {
                herd.phase = Phase.EXIT;
                herd.walking = world.getTime();
            }

            return true;
        }

        if (herd.phase == Phase.EXIT) {
            if (ShepherdPens.isOutside(herd.from, animal)) {
                PenTether.release(animal);
                animal.attachLeash(shepherd, true);
                FencedIn.closeWhenClear(herd.from.gate());
                herd.from = null;
                herd.phase = Phase.LEAD;
                return true;
            }
            if (world.getTime() - herd.walking > ShepherdPens.WALK_LIMIT) {
                FencedIn.closeWhenClear(herd.from.gate());
                VillageColonyMod.LOGGER.info("Shepherd {} gave up: the {} did not walk out of the pen at {}",
                        shepherdId.toString().substring(0, 8), animal.getType().getUntranslatedName(),
                        herd.from.gate().toShortString());
                abort(world, shepherdId, herd);
                return false;
            }
            ShepherdPens.walkThrough(world, herd.from, animal, herd.from.outside());
            return true;
        }

        if (herd.phase == Phase.FETCH) {
            ShepherdLead.walkTo(world, shepherd, animal.getBlockPos(), REACH - 1);

            // Regra 328: o desgarrado que ele não alcança fica para depois, e ele passa ao próximo.
            if (HerdPatience.losesPatience(shepherdId, herd.animal, Math.sqrt(shepherd.squaredDistanceTo(animal)),
                    world.getTime())) {
                HerdPatience.setAside(world, shepherdId, herd.animal, "it got no closer to it");
                abort(world, shepherdId, herd);
                return false;
            }

            if (shepherd.squaredDistanceTo(animal) <= REACH * REACH) {
                if (HerdCandidates.sheltersAnimal(world, animal.getBlockPos())) {
                    abort(world, shepherdId, herd);
                    return false;
                }
                Entity tiedTo = animal.getLeashHolder();

                animal.attachLeash(shepherd, true);

                if (tiedTo instanceof LeashKnotEntity knot) {
                    ShepherdLead.removeIfEmpty(world, knot);
                }

                herd.phase = Phase.LEAD;
            }

            return true;
        }

        if (animal.getLeashHolder() != shepherd) {
            if (toPen) {
                // A corda soltou: volta para buscar o animal (Regra 309).
                herd.delivery.reset();
                PenTether.release(animal);
                herd.phase = Phase.FETCH;
                return true;
            }
            abort(world, shepherdId, herd);
            return false;
        }

        BlockPos goal = herd.pen != null ? herd.pen.outside() : herd.fence;

        if (herd.pen != null && (atGate(herd.phase) || shepherd.getBlockPos().isWithinDistance(goal, REACH)
                || herd.pen.reach().contains(shepherd.getBlockPos()))) {
            if (!(world.getBlockState(herd.fence).getBlock() instanceof net.minecraft.block.FenceGateBlock)) {
                abort(world, shepherdId, herd);
                return false;
            }
            if (herd.phase == Phase.LEAD) {
                if (HERDS.values().stream().anyMatch(other -> other != herd && other.fence.equals(herd.fence)
                        && atGate(other.phase))) return true;
                FencedIn.holdForDelivery(world, herd.fence);
                ShepherdLead.alignAtGate(animal, goal);
                if (!herd.pen.reach().contains(shepherd.getBlockPos()) && !ShepherdPens.isInside(herd.pen, animal)
                        && animal.squaredDistanceTo(net.minecraft.util.math.Vec3d.ofBottomCenter(goal)) > REACH * REACH) {
                    ShepherdLead.walkTo(world, shepherd, goal, 0);
                    return true;
                }
                WorkTargets.clear(shepherdId);
                herd.phase = Phase.JUMP_IN;
                herd.walking = world.getTime();
            }
            if (world.getTime() - herd.walking > ShepherdPens.WALK_LIMIT * 3L) {
                FencedIn.closeWhenClear(herd.fence);
                PenTether.release(animal);
                if (HerdPatience.failedEntry(shepherdId, herd.animal, world.getTime())) {
                    HerdPatience.setAside(world, shepherdId, herd.animal, "no clear crossing into the pen");
                    abort(world, shepherdId, herd);
                    return false;
                }
                herd.delivery.reset();
                herd.phase = Phase.LEAD;
                return true;
            }
            herd.delivery.advance(world, shepherd, animal, herd);
            return true;
        }

        if (herd.pen == null && shepherd.getBlockPos().isWithinDistance(herd.fence, REACH)) {
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

        if (ShepherdLead.isTooFar(shepherd, animal)) {
            WorkTargets.set(shepherdId, animal.getBlockPos(), REACH - 1);
        } else {
            ShepherdLead.walkTo(world, shepherd, goal, REACH - 1);
        }

        return true;
    }

    private static void abort(ServerWorld world, UUID shepherdId, Herd herd) {
        VillageColonyMod.LOGGER.info("Shepherd {} dropped the herd in phase {} after {} ticks, heading for {}",
                shepherdId.toString().substring(0, 8), herd.phase, world.getTime() - herd.since,
                herd.pen != null ? herd.pen.gate().toShortString() : "a post");
        PenTether.release(world.getEntity(herd.animal));
        if (world.getEntity(herd.animal) instanceof AnimalEntity animal
                && animal.getLeashHolder() != null && animal.getLeashHolder().getUuid().equals(shepherdId)
                && !HerdDeadline.tieNearby(world, animal)) {
            animal.detachLeash(true, false);
        }

        WorkTargets.clear(shepherdId);
        NEXT_TRY.put(shepherdId, world.getTime() + COOLDOWN);
    }

    static Optional<BlockPos> nearestFence(ServerWorld world, BlockPos from) {
        return NearbyFences.nearestFence(world, from);
    }

    /** Quantos animais estão amarrados nesta cerca. */
    static int tiedAt(ServerWorld world, BlockPos fence) {
        List<AnimalEntity> tied = world.getEntitiesByClass(AnimalEntity.class, new Box(fence).expand(10),
                animal -> animal.getLeashHolder() instanceof LeashKnotEntity knot
                        && knot.getAttachedBlockPos().equals(fence));

        return tied.size();
    }
}
