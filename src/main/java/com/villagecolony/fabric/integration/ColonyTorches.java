package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.content.ColonyTorchBlock;
import com.villagecolony.fabric.content.ModContent;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.poi.PointOfInterestStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * As tochas da colônia — Regra 127, item 8 das escolhas do autor (2026-10-27).
 *
 * <p><b>O anel de luz.</b> A luz do jogo tem teto 15; a tocha dá 15 e põe blocos de luz invisíveis em volta
 * ({@link ColonyTorchBlock#lightRing}), e a área clara fica cerca de três vezes a da tocha comum. Quebrada
 * a tocha, o anel sai.
 *
 * <p><b>O bloqueio.</b> Monstro que nasce sozinho no escuro não nasce a {@link #NO_SPAWN} blocos de uma
 * tocha da colônia — o dobro do alcance da luz. Não vale para criador de monstro nem invasão. A tocha é um
 * ponto de interesse do jogo, que guarda onde ela está; a resposta por chunk fica guardada por
 * {@link #CACHE_TICKS}.
 */
public final class ColonyTorches {

    static {
        ServerMemory.register(ColonyTorches.class, ColonyTorches::clearAll);
    }

    /** O raio em que monstro não nasce sozinho. */
    public static final int NO_SPAWN = 30;

    /** Por quanto tempo a resposta de um chunk vale. */
    static final long CACHE_TICKS = 200;

    private record Answer(boolean near, long until) {
    }

    private static final Map<Long, Answer> NEAR = new HashMap<>();

    private ColonyTorches() {
    }

    /** Põe a tocha da colônia em {@code at}; o anel de luz acende com ela ({@link ColonyTorchBlock#lightRing}). */
    public static void place(ServerWorld world, BlockPos at) {
        world.setBlockState(at, ModContent.COLONY_TORCH.getDefaultState());
        NEAR.clear();
    }

    /** Até onde da boca a mina é segura com a bancada do mineiro na vila (Regra 132). */
    public static final int SAFE_MINE = 64;

    /**
     * Se este ponto fica dentro da mina de uma vila com bancada do mineiro: a até {@link #SAFE_MINE} da
     * entrada e não acima dela — a superfície em volta continua como era.
     */
    public static boolean inASafeMine(ServerWorld world, BlockPos pos) {
        for (com.villagecolony.core.construction.model.Mine mine : VillageColonyMod.MINES.all()) {
            BlockPos entry = MinecraftTypeAdapter.toBlockPos(mine.shaft().entry());

            if (pos.getY() > entry.getY() + 1 || Math.abs(pos.getX() - entry.getX()) > SAFE_MINE
                    || Math.abs(pos.getZ() - entry.getZ()) > SAFE_MINE) {
                continue;
            }

            if (VillageColonyMod.COLONIES.find(mine.colonyId())
                    .filter(colony -> BenchPresence.has(world, colony, com.villagecolony.core.worker.model.ProfessionType.MINER))
                    .isPresent()) {
                return true;
            }
        }

        return false;
    }

    /** Se este nascimento de monstro no escuro é barrado por uma tocha da colônia perto. */
    public static boolean suppresses(ServerWorldAccess access, SpawnReason reason, BlockPos pos) {
        if (reason != SpawnReason.NATURAL && reason != SpawnReason.CHUNK_GENERATION) {
            return false;
        }

        ServerWorld world = access.toServerWorld();

        if (inASafeMine(world, pos)) {
            return true;
        }

        long now = world.getTime();
        long chunk = ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4);
        Answer cached = NEAR.get(chunk);

        if (cached != null && cached.until() > now) {
            return cached.near();
        }

        boolean near = isNear(world, pos);
        NEAR.put(chunk, new Answer(near, now + CACHE_TICKS));

        return near;
    }

    /** Se há tocha da colônia a {@link #NO_SPAWN} blocos de {@code pos}. */
    public static boolean isNear(ServerWorld world, BlockPos pos) {
        return anyWithin(world, pos, NO_SPAWN);
    }

    /** Se há tocha da colônia a {@code radius} blocos de {@code pos}. */
    public static boolean anyWithin(ServerWorld world, BlockPos pos, int radius) {
        return world.getPointOfInterestStorage()
                .getInCircle(type -> type.matchesKey(ModContent.colonyTorchPoi()), pos, radius,
                        PointOfInterestStorage.OccupationStatus.ANY)
                .findAny()
                .isPresent();
    }

    static void clearAll() {
        NEAR.clear();
    }
}
