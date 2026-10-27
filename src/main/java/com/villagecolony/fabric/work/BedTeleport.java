package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
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

    /**
     * Até onde a tentativa ainda é "no mesmo lugar": um túnel da subida e meio bloco de folga.
     *
     * <p>Era 1,5. No deserto (2026-10-21) o mineiro preso na duna alternava degrau e túnel, e o
     * túnel o levava 3 blocos para um lado e depois para o outro (x=110 ↔ 113): cada troca
     * parecia lugar novo, a conta recomeçava, e ele ficou 12 e 17 minutos assim até a noite.
     */
    static final double SAME_PLACE = ClimbReshape.TUNNEL_LENGTH + 1.5;

    private BedTeleport() {
    }

    static void clearAll() {
        ATTEMPTS.clear();
    }

    static void forgetAttempts(UUID workerId) {
        ATTEMPTS.remove(workerId);
    }

    /** Mais uma tentativa em {@code feet}; longe de onde a conta começou ({@link #SAME_PLACE}), ela recomeça. */
    static void attempt(UUID workerId, BlockPos feet) {
        Attempts last = ATTEMPTS.get(workerId);

        ATTEMPTS.put(workerId, last != null && last.from().isWithinDistance(feet, SAME_PLACE)
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

        return toSafety(world, villager, workerId,
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
     * Leva o aldeão para a cama e encerra a fuga em andamento. Ele fica de pé onde o corpo cabe
     * ({@link SafeLanding}, Regra 114): em cima do colchão se há espaço até a cabeça, senão ao lado.
     *
     * @return se havia cama, e lugar seguro junto dela, para onde ir
     */
    static boolean toBed(ServerWorld world, VillagerEntity villager, UUID workerId, String why) {
        Optional<BlockPos> bed = bedOf(world, villager);

        if (bed.isEmpty()) {
            return false;
        }

        Optional<Vec3d> spot = SafeLanding.near(world, villager, bed.get());

        if (spot.isEmpty()) {
            VillageColonyMod.LOGGER.info("Worker {} stays put — its bed at {} has no room to stand beside it ({})",
                    shortId(workerId), bed.get().toShortString(), why);

            return false;
        }

        land(world, villager, workerId, spot.get(), "its bed at " + bed.get().toShortString(), why);

        // E fecha a porta da casa — ADR-037 C3.
        HouseDoors.close(world, villager, bed.get());

        return true;
    }

    /**
     * A cama, e sem ela o chão do centro da vila — Regra 114: quem está preso sem cama não fica
     * preso para sempre (savana, 23-10: o construtor passou 4 h trocando de jeito de subir em y=55).
     *
     * @return se ele foi levado a algum lugar
     */
    static boolean toSafety(ServerWorld world, VillagerEntity villager, UUID workerId, String why) {
        return toBed(world, villager, workerId, why) || toVillage(world, villager, workerId, why);
    }

    /** O chão livre mais perto do centro da colônia dele. */
    static boolean toVillage(ServerWorld world, VillagerEntity villager, UUID workerId, String why) {
        Optional<BlockPos> center = VillageColonyMod.WORKERS.find(workerId)
                .flatMap(worker -> VillageColonyMod.COLONIES.find(worker.colonyId()))
                .map(colony -> MinecraftTypeAdapter.toBlockPos(colony.center()))
                .filter(world::isChunkLoaded);

        // O centro da colônia já é a altura do chão; a coluna de cima só quando ali não cabe.
        Optional<Vec3d> spot = center
                .flatMap(at -> SafeLanding.near(world, villager, at.down())
                        .or(() -> SafeLanding.near(world, villager,
                                world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, at).down())));

        if (spot.isEmpty()) {
            return false;
        }

        land(world, villager, workerId, spot.get(), "the village center", why);

        return true;
    }

    private static void land(ServerWorld world, VillagerEntity villager, UUID workerId, Vec3d spot, String where,
                             String why) {
        BlockPos from = villager.getBlockPos();

        villager.getBrain().forget(MemoryModuleType.WALK_TARGET);
        villager.getNavigation().stop();
        villager.requestTeleport(spot.x, spot.y, spot.z);

        if (StrandedWorkers.isStranded(workerId)) {
            StrandedWorkers.release(workerId);
        }

        ClimbOut.finish(world, villager, workerId);
        MineReturn.finish(workerId);
        StrandedEscape.forget(workerId);

        VillageColonyMod.LOGGER.info("Worker {} was teleported from {} to {} — {}",
                shortId(workerId), from.toShortString(), where, why);
    }

    private static String shortId(UUID workerId) {
        return workerId.toString().substring(0, 8);
    }
}
