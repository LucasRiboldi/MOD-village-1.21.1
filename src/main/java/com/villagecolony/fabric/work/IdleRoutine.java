package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.RoadNetwork;
import com.villagecolony.fabric.integration.VillageFocus;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A rotina à vista de quem está sem tarefa — Regra 219: no expediente ele vai à bancada, confere o baú, procura um vizinho e anda
 * pela rua, uma coisa de cada vez, depois de um minuto sem tarefa ({@link IdleHelps#HELP_AFTER_IDLE}): quem acabou uma tarefa
 * espera a próxima onde está. É aparência: o {@link WorkTime} continua contando esse tempo como ócio.
 *
 * <p>Só dá o destino: quem anda é o cérebro do jogo, como na {@link EveningGathering}.
 */
public final class IdleRoutine {

    /** O que ele vai ver, na ordem em que alterna. */
    enum Errand { BENCH, CHEST, NEIGHBOUR, STREET }

    private static final int EVERY = 100;
    /** Até onde um passeio leva: o ocioso continua perto de onde estava. */
    static final int REACH = 32;
    /** Quem está mais fundo que isto, na mina, fica: subir não é passeio. */
    static final int MAX_CLIMB = 6;
    /** Quanto ele fica no lugar antes do próximo passeio. */
    static final int LINGER = 300;
    private static final float SPEED = 0.45F;
    private static final int ARRIVED = 2;

    private static final Map<UUID, Long> NEXT_AT = new HashMap<>();
    private static final Map<UUID, Integer> TURN = new HashMap<>();

    static {
        ServerMemory.register(IdleRoutine.class, () -> {
            NEXT_AT.clear();
            TURN.clear();
        });
    }

    private IdleRoutine() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey().equals(World.OVERWORLD) && world.getTime() % EVERY == 0) {
                for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
                    if (VillageFocus.isWorking(world, colony.id())) {
                        pass(world, colony);
                    }
                }
            }
        });
    }

    static void pass(ServerWorld world, Colony colony) {
        long now = world.getTime();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            UUID id = worker.villagerId();

            if (world.getEntity(id) instanceof VillagerEntity villager
                    && !villager.isBaby()
                    && NEXT_AT.getOrDefault(id, Long.MIN_VALUE) <= now
                    && IdleWorkers.isFree(world, villager, null)
                    && IdleHelps.isFree(world, villager, null)
                    && !villager.getBrain().hasMemoryModule(MemoryModuleType.WALK_TARGET)) {
                sendOut(world, colony, villager);
                NEXT_AT.put(id, now + LINGER);
            }
        }
    }

    /** Para o teste: ninguém mais está esperando a vez do próximo passeio. */
    static void forgetWaits() {
        NEXT_AT.clear();
    }

    /** O próximo lugar da rotina que exista e esteja ao alcance. */
    private static void sendOut(ServerWorld world, Colony colony, VillagerEntity villager) {
        UUID id = villager.getUuid();
        int start = TURN.getOrDefault(id, 0);
        Errand[] errands = Errand.values();

        for (int step = 0; step < errands.length; step++) {
            int turn = start + step;
            Optional<VillagerEntity> neighbour = errands[turn % errands.length] == Errand.NEIGHBOUR
                    ? neighbourOf(world, villager)
                    : Optional.empty();
            Optional<BlockPos> spot = switch (errands[turn % errands.length]) {
                case BENCH -> benchOf(villager);
                case CHEST -> VillageColonyMod.STORAGES.of(id)
                        .map(storage -> MinecraftTypeAdapter.toBlockPos(storage.chestPosition()));
                case NEIGHBOUR -> neighbour.map(VillagerEntity::getBlockPos);
                case STREET -> streetNear(world, colony, villager);
            };

            if (spot.isPresent() && isInReach(villager.getBlockPos(), spot.get()) && isInTheVillage(colony, spot.get())) {
                villager.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.get(), SPEED, ARRIVED));
                neighbour.ifPresent(other -> villager.getBrain().remember(
                        MemoryModuleType.LOOK_TARGET, new EntityLookTarget(other, true)));
                TURN.put(id, (turn + 1) % errands.length);

                return;
            }
        }
    }

    /** O passeio não sai da caixa da vila; vila ainda sem caixa não passeia. */
    private static boolean isInTheVillage(Colony colony, BlockPos spot) {
        return colony.bounds()
                .filter(box -> spot.getX() >= box.minX() && spot.getX() <= box.maxX()
                        && spot.getZ() >= box.minZ() && spot.getZ() <= box.maxZ())
                .isPresent();
    }

    private static boolean isInReach(BlockPos from, BlockPos to) {
        return !from.equals(to) && from.isWithinDistance(to, REACH) && Math.abs(from.getY() - to.getY()) <= MAX_CLIMB;
    }

    private static Optional<BlockPos> benchOf(VillagerEntity villager) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE).map(site -> site.pos());
    }

    private static Optional<VillagerEntity> neighbourOf(ServerWorld world, VillagerEntity villager) {
        Box around = villager.getBoundingBox().expand(REACH / 2.0);

        return world.getEntitiesByClass(VillagerEntity.class, around, other -> other != villager && other.isAlive())
                .stream()
                .min(Comparator.comparingDouble(villager::squaredDistanceTo));
    }

    /** Uma coluna da rede ao alcance, sorteada: a rua é a do grafo (Regra 216). */
    private static Optional<BlockPos> streetNear(ServerWorld world, Colony colony, VillagerEntity villager) {
        List<Long> near = new ArrayList<>();
        BlockPos at = villager.getBlockPos();

        for (long column : RoadNetwork.of(colony.id()).map(ColonyRoads::columns).orElse(List.of())) {
            int dx = ColonyRoads.xOf(column) - at.getX();
            int dz = ColonyRoads.zOf(column) - at.getZ();

            if (dx * dx + dz * dz <= REACH * REACH) {
                near.add(column);
            }
        }

        if (near.isEmpty()) {
            return Optional.empty();
        }

        long column = near.get(villager.getRandom().nextInt(near.size()));
        int x = ColonyRoads.xOf(column);
        int z = ColonyRoads.zOf(column);

        return world.isChunkLoaded(x >> 4, z >> 4)
                ? Optional.of(new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z))
                : Optional.empty();
    }
}
