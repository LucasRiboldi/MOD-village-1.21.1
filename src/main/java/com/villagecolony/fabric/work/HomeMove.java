package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.VillageFoundation;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Quem não alcança o trabalho porque mora abaixo dele muda de casa — decisão do autor, 2026-10-21.
 *
 * <p>Na vila do deserto em duas alturas (y63 à beira d'água, y80+ em cima) o construtor e o lenhador
 * que moravam embaixo não achavam caminho até a obra e as árvores de cima. A escada fixa e a subida
 * por pilar saíram (a movimentação precisa ser natural); no lugar delas, depois de {@link #STRIKES}
 * desistências de caminhada a {@link MinerSteps#CLIMB_OUT_BELOW} ou mais abaixo do destino, o aldeão
 * passa a dormir numa cama livre da vila na altura do destino. Nada muda no mundo: à noite, sem
 * caminho para casa, o jogo já o leva para a cama (NightHome), e de manhã ele começa lá em cima.
 *
 * <p>Sem cama livre lá em cima, ele troca de casa com um aldeão sem ofício que mora lá: este não tem
 * trabalho a alcançar, e na bancada a vila do deserto tinha todas as camas de cima ocupadas.
 *
 * <p>O baú dele é solto junto: a varredura de baús (ChestScanner) reivindica, no ciclo seguinte, o
 * baú livre ao lado da cama nova.
 */
final class HomeMove {

    static {
        ServerMemory.register(HomeMove.class, HomeMove::clearAll);
    }

    /** Desistências abaixo do destino antes de mudar de casa. */
    static final int STRIKES = 2;

    /** Quanto acima ou abaixo do destino a cama nova pode ficar. */
    static final int LEVEL = 3;

    /** Até onde do destino se procura cama livre. */
    static final int RADIUS = 48;

    private static final Map<UUID, BlockPos> PENDING = new HashMap<>();

    private static final Map<UUID, Integer> COUNTS = new HashMap<>();

    /** Quando cada aldeão sem casa para onde ir foi avisado no log: uma vez a cada {@link #QUIET_TICKS}. */
    private static final Map<UUID, Long> WARNED = new HashMap<>();

    static final long QUIET_TICKS = 6_000;

    private HomeMove() {
    }

    /** Desistiu da caminhada rumo a {@code way}: a próxima passagem confere se ele estava abaixo. */
    static void gaveUpOnTheWayTo(UUID workerId, BlockPos way) {
        if (way != null) {
            PENDING.put(workerId, way.toImmutable());
        }
    }

    /** Uma passagem: conta as desistências abaixo do destino e muda de casa quem chegou ao limite. */
    static void pass(ServerWorld world) {
        if (PENDING.isEmpty()) {
            return;
        }

        Map<UUID, BlockPos> due = new HashMap<>(PENDING);
        PENDING.clear();

        due.forEach((workerId, way) -> {
            if (!(world.getEntity(workerId) instanceof VillagerEntity villager) || !villager.isAlive()
                    || way.getY() - villager.getBlockY() < MinerSteps.CLIMB_OUT_BELOW) {
                return;
            }

            if (COUNTS.merge(workerId, 1, Integer::sum) >= STRIKES) {
                COUNTS.remove(workerId);
                move(world, villager, way);
            }
        });
    }

    /**
     * Dá ao aldeão a cama livre da vila na altura de {@code way}, a mais perto dele.
     *
     * @return se mudou de casa
     */
    static boolean move(ServerWorld world, VillagerEntity villager, BlockPos way) {
        Optional<VillageBounds> box = VillageColonyMod.WORKERS.find(villager.getUuid())
                .map(Worker::colonyId)
                .flatMap(VillageColonyMod.COLONIES::find)
                .flatMap(Colony::bounds);

        if (box.isEmpty()) {
            return false;
        }

        BlockPos old = villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME)
                .map(GlobalPos::pos).orElse(null);

        // A casa de hoje já fica na altura do trabalho: a desistência veio de quando ele passava por baixo.
        if (old != null && way.getY() - old.getY() < MinerSteps.CLIMB_OUT_BELOW) {
            return false;
        }

        // A cama nova fica ao menos CLIMB_OUT_BELOW acima da velha: trocar de casa na mesma altura não aproxima.
        int lowest = old == null ? Integer.MIN_VALUE : old.getY() + MinerSteps.CLIMB_OUT_BELOW;
        Optional<BlockPos> bed = freeBedNear(world, box.get(), way).filter(pos -> pos.getY() >= lowest);

        if (bed.isEmpty()) {
            Optional<VillagerEntity> partner = old == null ? Optional.empty()
                    : idleNeighbourUpThere(world, box.get(), way, villager)
                            .filter(other -> homeOf(other).orElseThrow().getY() >= lowest);

            if (partner.isEmpty()) {
                if (world.getTime() - WARNED.getOrDefault(villager.getUuid(), Long.MIN_VALUE / 2) < QUIET_TICKS) {
                    return false;
                }

                WARNED.put(villager.getUuid(), world.getTime());
                VillageColonyMod.LOGGER.info("Worker {} cannot reach its work at {} from below and no bed up there is"
                                + " free or held by a villager without a job",
                        villager.getUuid().toString().substring(0, 8), way.toShortString());

                return false;
            }

            BlockPos theirs = homeOf(partner.get()).orElseThrow();
            PointOfInterestStorage pois = world.getPointOfInterestStorage();
            pois.releaseTicket(old);
            pois.releaseTicket(theirs);
            VillageFoundation.giveHome(world, partner.get(), old);
            VillageColonyMod.STORAGES.remove(partner.get().getUuid());
            bed = Optional.of(theirs);
        }

        VillageFoundation.giveHome(world, villager, bed.get());
        VillageColonyMod.STORAGES.remove(villager.getUuid());

        VillageColonyMod.LOGGER.info("Worker {} moves home from {} to {} — it could not reach its work at {} from below",
                villager.getUuid().toString().substring(0, 8), old == null ? "nowhere" : old.toShortString(),
                bed.get().toShortString(), way.toShortString());
        ActionJournal.action(villager.getUuid(), "MOVED_HOME", "", 1, bed.get(), "could not reach its work from below");

        return true;
    }

    /** A cama livre (sem morador) dentro da caixa da vila, a até {@link #LEVEL} da altura do destino. */
    static Optional<BlockPos> freeBedNear(ServerWorld world, VillageBounds box, BlockPos way) {
        return world.getPointOfInterestStorage()
                .getInCircle(type -> type.matchesKey(PointOfInterestTypes.HOME), way, RADIUS,
                        PointOfInterestStorage.OccupationStatus.HAS_SPACE)
                .map(PointOfInterest::getPos)
                .filter(pos -> Math.abs(pos.getY() - way.getY()) <= LEVEL
                        && pos.getX() >= box.minX() && pos.getX() <= box.maxX()
                        && pos.getZ() >= box.minZ() && pos.getZ() <= box.maxZ())
                .min(Comparator.comparingDouble(pos -> pos.getSquaredDistance(way)));
    }

    /** O aldeão sem ofício da vila cuja cama fica na altura de {@code way}, o de cama mais perto dele. */
    private static Optional<VillagerEntity> idleNeighbourUpThere(
            ServerWorld world, VillageBounds box, BlockPos way, VillagerEntity mover) {
        net.minecraft.util.math.Box area = new net.minecraft.util.math.Box(box.minX(), box.minY() - LEVEL, box.minZ(),
                box.maxX() + 1, box.maxY() + LEVEL + 1, box.maxZ() + 1);

        return world.getEntitiesByClass(VillagerEntity.class, area, other -> other.isAlive() && !other.isBaby()
                        && !other.getUuid().equals(mover.getUuid())
                        && VillageColonyMod.WORKERS.find(other.getUuid()).flatMap(Worker::profession).isEmpty()
                        && homeOf(other).filter(home -> Math.abs(home.getY() - way.getY()) <= LEVEL).isPresent())
                .stream()
                .min(Comparator.comparingDouble(other -> homeOf(other).orElseThrow().getSquaredDistance(way)));
    }

    private static Optional<BlockPos> homeOf(VillagerEntity villager) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME).map(GlobalPos::pos);
    }

    static void clearAll() {
        PENDING.clear();
        COUNTS.clear();
        WARNED.clear();
    }
}
