package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.content.ModContent;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * As bancadas do mod que uma vila tem — a base das habilidades passivas (Regra 132, autor 2026-10-27):
 * cada bancada na caixa da vila liga as passivas do ofício dela. Lido do POI do jogo, com a resposta
 * guardada por {@link #CACHE_TICKS}.
 */
public final class BenchPresence {

    static {
        ServerMemory.register(BenchPresence.class, BenchPresence::clearAll);
    }

    /** Quanto a resposta vale. */
    static final long CACHE_TICKS = 600;

    /** A busca vai até aqui do centro, e mais a metade da caixa da vila. */
    static final int MARGIN = 16;

    private record Answer(List<BlockPos> benches, long until) {
    }

    private static final Map<String, Answer> CACHE = new HashMap<>();

    private BenchPresence() {
    }

    /** As bancadas deste ofício na vila. */
    public static List<BlockPos> of(ServerWorld world, Colony colony, ProfessionType profession) {
        String key = colony.id() + "|" + profession;
        long now = world.getTime();
        Answer cached = CACHE.get(key);

        if (cached != null && cached.until() > now) {
            return cached.benches();
        }

        var poi = ModContent.benchPoi(profession);
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        int radius = colony.bounds()
                .map(box -> Math.max(box.maxX() - box.minX(), box.maxZ() - box.minZ()) / 2 + MARGIN)
                .orElse(64);
        List<BlockPos> benches = poi == null ? List.of() : world.getPointOfInterestStorage()
                .getInCircle(entry -> entry.matchesKey(poi), center, radius, PointOfInterestStorage.OccupationStatus.ANY)
                .map(PointOfInterest::getPos)
                .map(BlockPos::toImmutable)
                .toList();

        CACHE.put(key, new Answer(benches, now + CACHE_TICKS));

        return benches;
    }

    /** Se a vila tem bancada deste ofício. */
    public static boolean has(ServerWorld world, Colony colony, ProfessionType profession) {
        return !of(world, colony, profession).isEmpty();
    }

    /** O ofício de uma bancada do mod, se o bloco é uma. */
    public static Optional<ProfessionType> professionOf(Block block) {
        for (ProfessionType profession : ProfessionType.values()) {
            if (ModContent.benchBlock(profession) == block) {
                return Optional.of(profession);
            }
        }

        return Optional.empty();
    }

    /** A vila em cuja caixa (com folga) este ponto fica, a mais perto do centro. */
    public static Optional<Colony> colonyAt(BlockPos at) {
        Colony best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
            int radius = colony.bounds()
                    .map(box -> Math.max(box.maxX() - box.minX(), box.maxZ() - box.minZ()) / 2 + MARGIN)
                    .orElse(64);
            double distance = Math.sqrt(center.getSquaredDistance(at));

            if (distance <= radius && distance < bestDistance) {
                best = colony;
                bestDistance = distance;
            }
        }

        return Optional.ofNullable(best);
    }

    /** A bancada deste ofício a até {@code reach} de {@code at}, se houver. */
    public static boolean near(ServerWorld world, Colony colony, ProfessionType profession, BlockPos at, int reach) {
        for (BlockPos bench : of(world, colony, profession)) {
            if (bench.isWithinDistance(at, reach)) {
                return true;
            }
        }

        return false;
    }

    /** Esquece o que sabia desta colônia: bancada posta ou quebrada. */
    public static void forget(UUID colonyId) {
        CACHE.keySet().removeIf(key -> key.startsWith(colonyId.toString()));
    }

    public static void clearAll() {
        CACHE.clear();
    }
}
