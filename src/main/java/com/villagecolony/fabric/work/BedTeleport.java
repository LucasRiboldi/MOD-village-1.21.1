package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O aldeão preso vai para a cama dele — ADR-036 item 15, que substitui o "nenhum
 * teletransporte" do E47 em dois casos: {@link #STUCK_ATTEMPTS} tentativas de
 * se soltar sem sair do lugar ({@link ClimbOut}), e à noite sem caminho para
 * casa ({@link NightHome}).
 */
final class BedTeleport {

    static {
        ServerMemory.register(BedTeleport.class, BedTeleport::clearAll);
    }

    /** Uma troca de jeito da subida conta como tentativa de se soltar. */
    private record Attempts(BlockPos from, int count) {
    }

    private static final Map<UUID, Attempts> ATTEMPTS = new HashMap<>();

    /** Tentativas de se soltar no mesmo lugar antes de ir para a cama. */
    static final int STUCK_ATTEMPTS = 5;

    /** A altura do colchão: o aldeão fica de pé em cima da cama. */
    private static final double BED_TOP = 0.5625;

    private BedTeleport() {
    }

    static void clearAll() {
        ATTEMPTS.clear();
    }

    static void forgetAttempts(UUID workerId) {
        ATTEMPTS.remove(workerId);
    }

    /** Mais uma tentativa em {@code feet}; longe da anterior, a conta recomeça. */
    static void attempt(UUID workerId, BlockPos feet) {
        Attempts last = ATTEMPTS.get(workerId);

        ATTEMPTS.put(workerId, last != null && last.from().isWithinDistance(feet, 1.5)
                ? new Attempts(last.from(), last.count() + 1)
                : new Attempts(feet.toImmutable(), 1));
    }

    static int attempts(UUID workerId) {
        Attempts last = ATTEMPTS.get(workerId);

        return last == null ? 0 : last.count();
    }

    /**
     * Com {@link #STUCK_ATTEMPTS} tentativas no mesmo lugar, vai para a cama.
     *
     * @return se foi; sem cama, a conta recomeça e a subida continua
     */
    static boolean tooManyAttempts(ServerWorld world, VillagerEntity villager, UUID workerId) {
        Attempts last = ATTEMPTS.get(workerId);

        if (last == null || last.count() < STUCK_ATTEMPTS) {
            return false;
        }

        ATTEMPTS.remove(workerId);

        return toBed(world, villager, workerId,
                last.count() + " attempts to get free without leaving " + last.from().toShortString());
    }

    /**
     * A cama dele, pela memória {@code HOME} do Vanilla, se está neste mundo,
     * com o pedaço carregado (§11, nunca carregar) e ainda é cama.
     */
    static Optional<BlockPos> bedOf(ServerWorld world, VillagerEntity villager) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME)
                .filter(home -> home.dimension().equals(world.getRegistryKey()))
                .map(home -> home.pos())
                .filter(bed -> {
                    WorldChunk chunk = world.getChunkManager().getWorldChunk(bed.getX() >> 4, bed.getZ() >> 4);

                    return chunk != null && chunk.getBlockState(bed).getBlock() instanceof BedBlock;
                });
    }

    /**
     * Leva o aldeão para a cama e encerra a fuga em andamento.
     *
     * @return se havia cama para onde ir
     */
    static boolean toBed(ServerWorld world, VillagerEntity villager, UUID workerId, String why) {
        Optional<BlockPos> bed = bedOf(world, villager);

        if (bed.isEmpty()) {
            return false;
        }

        BlockPos from = villager.getBlockPos();

        villager.getBrain().forget(MemoryModuleType.WALK_TARGET);
        villager.getNavigation().stop();
        villager.requestTeleport(bed.get().getX() + 0.5, bed.get().getY() + BED_TOP, bed.get().getZ() + 0.5);

        // E fecha a porta da casa — ADR-037 C3.
        HouseDoors.close(world, villager, bed.get());

        if (StrandedWorkers.isStranded(workerId)) {
            StrandedWorkers.release(workerId);
        }

        ClimbOut.finish(world, villager, workerId);
        MineReturn.finish(workerId);
        StrandedEscape.forget(workerId);

        VillageColonyMod.LOGGER.info("Worker {} was teleported from {} to its bed at {} — {}",
                workerId.toString().substring(0, 8), from.toShortString(), bed.get().toShortString(), why);

        return true;
    }
}
