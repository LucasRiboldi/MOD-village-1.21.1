package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A memória das árvores e mudas da vila — ADR-038 P3a: o lenhador vai direto à
 * árvore conhecida mais perto dele, e nunca anda até uma muda. Entram as
 * árvores que a busca acha, as que a floresta faz nascer e as mudas que se
 * replantam; muda que cresce vira árvore, e tronco derrubado sai. Em memória:
 * depois de reabrir o mundo, a busca reconstrói.
 */
public final class VillageTrees {

    static {
        ServerMemory.register(VillageTrees.class, VillageTrees::clearAll);
    }

    /** Teto por colônia, para a memória não crescer sem fim. */
    static final int MAX_KNOWN = 512;

    private static final Map<UUID, Set<BlockPos>> TREES = new HashMap<>();

    private static final Map<UUID, Set<BlockPos>> SAPLINGS = new HashMap<>();

    private VillageTrees() {
    }

    public static void clearAll() {
        TREES.clear();
        SAPLINGS.clear();
    }

    /** Uma árvore da vila: a base do tronco. */
    public static void rememberTree(UUID colonyId, BlockPos base) {
        add(TREES, colonyId, base);
        Set<BlockPos> saplings = SAPLINGS.get(colonyId);
        if (saplings != null) {
            saplings.remove(base);
        }
    }

    /** Uma muda da vila, que ainda não é alvo. */
    public static void rememberSapling(UUID colonyId, BlockPos at) {
        add(SAPLINGS, colonyId, at);
    }

    /** A muda replantada onde não se sabe a colônia: a mais perto dela. */
    public static void rememberSaplingNear(BlockPos at) {
        VillageColonyMod.COLONIES.findNearest(MinecraftTypeAdapter.toColonyPos(at), 96)
                .ifPresent(colony -> rememberSapling(colony.id(), at));
    }

    public static int knownTrees(UUID colonyId) {
        return TREES.getOrDefault(colonyId, Set.of()).size();
    }

    public static int knownSaplings(UUID colonyId) {
        return SAPLINGS.getOrDefault(colonyId, Set.of()).size();
    }

    /**
     * A árvore conhecida mais perto de {@code from} que ainda é tronco e que
     * {@code accepts} aceita. Primeiro promove as mudas que já cresceram.
     */
    public static Optional<BlockPos> nearest(
            ServerWorld world, UUID colonyId, BlockPos from, Predicate<BlockPos> accepts) {
        promoteGrown(world, colonyId);

        Set<BlockPos> trees = TREES.get(colonyId);
        if (trees == null) {
            return Optional.empty();
        }

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Iterator<BlockPos> it = trees.iterator(); it.hasNext(); ) {
            BlockPos base = it.next();
            Optional<Boolean> log = isLog(world, base);

            if (log.isEmpty()) {
                continue;
            }

            if (!log.get()) {
                it.remove();
                continue;
            }

            double distance = base.getSquaredDistance(from);

            if (distance < bestDistance && accepts.test(base)) {
                best = base;
                bestDistance = distance;
            }
        }

        return Optional.ofNullable(best);
    }

    private static void promoteGrown(ServerWorld world, UUID colonyId) {
        Set<BlockPos> saplings = SAPLINGS.get(colonyId);
        if (saplings == null) {
            return;
        }

        for (Iterator<BlockPos> it = saplings.iterator(); it.hasNext(); ) {
            BlockPos at = it.next();
            Optional<Boolean> log = isLog(world, at);

            if (log.isPresent() && log.get()) {
                it.remove();
                add(TREES, colonyId, at);
            } else if (log.isPresent() && world.getBlockState(at).isAir()) {
                it.remove();
            }
        }
    }

    /** Tronco aqui? Vazio quando o pedaço não está carregado (§11). */
    private static Optional<Boolean> isLog(ServerWorld world, BlockPos at) {
        WorldChunk chunk = world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4);

        return chunk == null ? Optional.empty() : Optional.of(TreeSpecies.isLog(chunk.getBlockState(at)));
    }

    private static void add(Map<UUID, Set<BlockPos>> map, UUID colonyId, BlockPos at) {
        Set<BlockPos> set = map.computeIfAbsent(colonyId, ignored -> new LinkedHashSet<>());

        if (set.size() >= MAX_KNOWN && !set.contains(at)) {
            Iterator<BlockPos> oldest = set.iterator();
            oldest.next();
            oldest.remove();
        }

        set.add(at.toImmutable());
    }

    /** Para o teste: a colônia em forma de posição. */
    static ColonyPos asColonyPos(BlockPos at) {
        return MinecraftTypeAdapter.toColonyPos(at);
    }
}
