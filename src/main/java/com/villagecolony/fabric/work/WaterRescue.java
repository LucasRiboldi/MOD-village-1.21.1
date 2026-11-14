package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.VillageFocus;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Aldeão preso na água — Regra 285 (autor, jogo da 0.4.84). Quem nada boia e muda de bloco, então o guarda de
 * imobilidade ({@link WorkStall}) nunca o vê parado. Esta vigia conta o tempo na água a céu aberto (a da mina é do
 * mineiro, Regra 278): passado {@link #STUCK_AFTER}, ele vai para a margem seca mais perto; se em mais
 * {@link #ICE_AFTER} continua na água, cada pulo que deixa os pés fora d'água ganha um bloco de gelo embaixo, para
 * ele pousar e subir dali. O gelo fica, e cada um sai na linha {@code VC_WATER_ICE}.
 */
public final class WaterRescue {

    static {
        ServerMemory.register(WaterRescue.class, WaterRescue::clearAll);
    }

    /** Tiques seguidos na água até ser tratado como preso. */
    static final int STUCK_AFTER = 200;

    /** Tiques de ida à margem antes do gelo. */
    static final int ICE_AFTER = 200;

    /** O raio da busca pela margem, em blocos. */
    static final int SHORE_RADIUS = 8;

    /** Quantos blocos de gelo um resgate põe, no máximo. */
    static final int ICE_LIMIT = 6;

    private static final Map<UUID, Integer> IN_WATER = new HashMap<>();

    private static final Map<UUID, Integer> ICE_PLACED = new HashMap<>();

    private WaterRescue() {
    }

    /** Uma passada por tique sobre os trabalhadores das vilas que trabalham. */
    public static void tick(ServerWorld world) {
        for (Worker worker : List.copyOf(VillageColonyMod.WORKERS.all())) {
            if (world.getEntity(worker.villagerId()) instanceof VillagerEntity villager
                    && VillageFocus.isWorking(world, worker.colonyId())) {
                watch(world, villager);
            } else {
                forget(worker.villagerId());
            }
        }
    }

    static void watch(ServerWorld world, VillagerEntity villager) {
        UUID id = villager.getUuid();
        BlockPos feet = villager.getBlockPos();

        // Saiu só quem está de pé em chão firme fora d'água; o pulo acima da superfície não zera a conta.
        boolean landed = !villager.isTouchingWater() && villager.isOnGround() && world.getFluidState(feet.down()).isEmpty();
        if (!villager.isTouchingWater() && !landed) {
            if (IN_WATER.containsKey(id) && IN_WATER.get(id) >= STUCK_AFTER + ICE_AFTER) {
                iceUnderTheJump(world, villager);
            }
            return;
        }

        if (landed || !underOpenSky(world, feet)) {
            if (IN_WATER.containsKey(id) && IN_WATER.get(id) >= STUCK_AFTER) {
                VillageColonyMod.LOGGER.info("VC_WATER_RESCUE worker={} out=true at={} ticks={} ice={}",
                        id.toString().substring(0, 8), feet.toShortString(), IN_WATER.get(id), ICE_PLACED.getOrDefault(id, 0));
            }
            forget(id);
            return;
        }

        int ticks = IN_WATER.merge(id, 1, Integer::sum);

        if (ticks < STUCK_AFTER) {
            return;
        }

        if (ticks == STUCK_AFTER || ticks % 40 == 0) {
            Optional<BlockPos> shore = shoreNear(world, feet);
            if (ticks == STUCK_AFTER) {
                VillageColonyMod.LOGGER.info("VC_WATER_RESCUE worker={} stuck=true at={} shore={}",
                        id.toString().substring(0, 8), feet.toShortString(), shore.map(BlockPos::toShortString).orElse("none"));
            }
            shore.ifPresent(at -> villager.getNavigation().startMovingTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 1.0));
        }

        if (ticks >= STUCK_AFTER + ICE_AFTER) {
            iceUnderTheJump(world, villager);
        }
    }

    /**
     * O gelo do pulo: com os pés fora d'água e água logo abaixo, o bloco de baixo vira gelo e ele pousa nele. Só água
     * parada ou corrente vira gelo; nada sólido é trocado.
     */
    static boolean iceUnderTheJump(ServerWorld world, VillagerEntity villager) {
        UUID id = villager.getUuid();
        BlockPos feet = villager.getBlockPos();
        BlockPos below = feet.down();

        if (ICE_PLACED.getOrDefault(id, 0) >= ICE_LIMIT
                || !world.getFluidState(feet).isEmpty()
                || !world.getFluidState(below).isIn(FluidTags.WATER)) {
            return false;
        }

        world.setBlockState(below, Blocks.ICE.getDefaultState());
        int placed = ICE_PLACED.merge(id, 1, Integer::sum);
        VillageColonyMod.LOGGER.info("VC_WATER_ICE worker={} at={} n={}", id.toString().substring(0, 8), below.toShortString(), placed);
        return true;
    }

    /** A céu aberto acima da coluna de água: a água conta no mapa de altura, então o céu é olhado acima dela. */
    static boolean underOpenSky(ServerWorld world, BlockPos feet) {
        BlockPos top = feet;
        for (int i = 0; i < 32 && !world.getFluidState(top).isEmpty(); i++) {
            top = top.up();
        }
        return world.isSkyVisible(top);
    }

    /** A margem seca mais perto: chão firme fora d'água, pés e cabeça livres. */
    static Optional<BlockPos> shoreNear(ServerWorld world, BlockPos from) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos at : BlockPos.iterateOutwards(from, SHORE_RADIUS, 3, SHORE_RADIUS)) {
            if (isShore(world, at)) {
                double distance = at.getSquaredDistance(from);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = at.toImmutable();
                }
            }
        }

        return Optional.ofNullable(best);
    }

    static boolean isShore(ServerWorld world, BlockPos at) {
        BlockState floor = world.getBlockState(at.down());

        return world.getFluidState(at).isEmpty() && world.getFluidState(at.down()).isEmpty()
                && world.getBlockState(at).getCollisionShape(world, at).isEmpty()
                && world.getBlockState(at.up()).getCollisionShape(world, at.up()).isEmpty()
                && floor.isSideSolidFullSquare(world, at.down(), net.minecraft.util.math.Direction.UP);
    }

    /** Há quantos tiques ele está na água, para o teste. */
    static int ticksInWater(UUID id) {
        return IN_WATER.getOrDefault(id, 0);
    }

    static void forget(UUID id) {
        IN_WATER.remove(id);
        ICE_PLACED.remove(id);
    }

    static void clearAll() {
        IN_WATER.clear();
        ICE_PLACED.clear();
    }
}
