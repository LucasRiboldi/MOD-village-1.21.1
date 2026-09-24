package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A forma de uma árvore no mundo: o tronco ligado, as folhas naturais ao
 * alcance dele e as leituras de bloco que nunca forçam chunk — separado de
 * {@link TreeHarvester} em 2026-09-24, quando ele passou de 500 linhas.
 *
 * <p>O {@code TreeHarvester} decide o que derrubar e em que ordem; esta
 * classe responde o que pertence à árvore. Os comentários vieram junto sem
 * mudança.
 */
final class TreeShape {

    private TreeShape() {
    }

    /** Os troncos da mesma espécie ligados a este, até o teto. */
    static List<BlockPos> connectedLogs(
            ServerWorld world, TreeSpecies species, BlockPos start, int limit) {

        List<BlockPos> found = new ArrayList<>();

        if (!isBlock(world, start, species.log())) {
            return found;
        }

        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        queue.add(start);
        seen.add(start);

        while (!queue.isEmpty() && found.size() < limit) {
            BlockPos current = queue.removeFirst();

            found.add(current);

            for (BlockPos neighbour : around(current)) {
                if (seen.add(neighbour) && isBlock(world, neighbour, species.log())) {
                    queue.add(neighbour);
                }
            }
        }

        return found;
    }

    /**
     * A copa desta árvore.
     *
     * <p>Parte dos troncos, e não de um raio: folha que não se alcança a
     * partir do tronco que caiu não é copa dele. E para em
     * {@link TreeHarvester#LEAF_REACH} de qualquer tronco, senão copas encostadas
     * ligariam uma árvore à vizinha e derrubar uma levaria a floresta.
     *
     * <p>Só folha que nasceu ali. Ver {@link #isNaturalLeaf}.
     */
    static List<BlockPos> connectedLeaves(
            ServerWorld world, TreeSpecies species, List<BlockPos> logs) {

        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>(logs);
        Deque<BlockPos> queue = new ArrayDeque<>(logs);

        while (!queue.isEmpty() && found.size() < TreeHarvester.MAX_LEAVES) {
            BlockPos current = queue.removeFirst();

            for (BlockPos neighbour : around(current)) {
                if (!seen.add(neighbour)) {
                    continue;
                }

                if (!isNaturalLeaf(world, neighbour, species)) {
                    continue;
                }

                if (!isWithinReachOfATrunk(neighbour, logs)) {
                    continue;
                }

                found.add(neighbour);
                queue.add(neighbour);
            }
        }

        return found;
    }

    /**
     * Folha que nasceu ali, e não folha que o jogador pendurou.
     *
     * <p>O Vanilla já responde a essa pergunta: folha colocada à mão vem
     * com {@code persistent = true} e nunca apodrece; folha de árvore
     * crescida vem com {@code false} e vive presa ao tronco. É a única
     * marca no mundo que separa uma coisa da outra, e o mod não tem
     * nenhuma melhor.
     *
     * <p>Serve a duas coisas ao mesmo tempo: a copa que o trabalhador
     * colhe não inclui a decoração de ninguém, e um grupo de troncos sem
     * copa viva — casa de vila, cabana, pilar — deixa de ser confundido
     * com árvore. Ver {@link #plan}.
     */
    static boolean isNaturalLeaf(ServerWorld world, BlockPos pos, TreeSpecies species) {
        BlockState state = stateAt(world, pos);

        if (state == null) {
            // Chunk descarregado. {@code stateAt} devolve nulo de
            // propósito — pedir o chunk aqui forçaria carregamento dentro
            // do tique —, e esta era a única porta que não conferia.
            //
            // Derrubou o servidor de teste em 2026-08-20, quando a copa
            // passou a ser procurada em até 256 troncos: a busca alcança
            // muito mais longe que antes, e longe o bastante para sair do
            // que está carregado. Folha que não se pode ver não é folha
            // viva, e a árvore simplesmente não é escolhida.
            return false;
        }

        if (!state.isOf(species.leaves())) {
            return false;
        }

        return state.contains(LeavesBlock.PERSISTENT) && !state.get(LeavesBlock.PERSISTENT);
    }

    static boolean isWithinReachOfATrunk(BlockPos leaf, List<BlockPos> logs) {
        for (BlockPos log : logs) {
            if (leaf.isWithinDistance(log, TreeHarvester.LEAF_REACH)) {
                return true;
            }
        }

        return false;
    }

    /** Os vinte e seis vizinhos, inclusive as diagonais. */
    static List<BlockPos> around(BlockPos pos) {
        List<BlockPos> neighbours = new ArrayList<>(26);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        neighbours.add(pos.add(dx, dy, dz));
                    }
                }
            }
        }

        return neighbours;
    }

    /** Folha de qualquer espécie, desde que tenha nascido ali. */
    static boolean isAnyNaturalLeaf(BlockState state) {
        if (!state.contains(LeavesBlock.PERSISTENT) || state.get(LeavesBlock.PERSISTENT)) {
            return false;
        }

        for (TreeSpecies species : TreeSpecies.values()) {
            if (state.isOf(species.leaves())) {
                return true;
            }
        }

        return false;
    }

    /**
     * O estado de um bloco, ou {@code null} se o chunk não está
     * carregado.
     *
     * <p>Nunca {@code world.getBlockState} direto. Ele carrega o chunk
     * que faltar, e do tick do servidor isso significa gerar terreno
     * dentro do laço — foi assim que a thread travou em 2026-08-07, e a
     * Fase 8 repetiu o erro em 2026-08-08. Ver §11.
     */
    static BlockState stateAt(ServerWorld world, BlockPos pos) {
        WorldChunk chunk = loadedChunkAt(world, pos);

        return chunk == null ? null : chunk.getBlockState(pos);
    }

    static WorldChunk loadedChunkAt(ServerWorld world, BlockPos pos) {
        return world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    static boolean isBlock(ServerWorld world, BlockPos pos, Block block) {
        BlockState state = stateAt(world, pos);

        return state != null && state.isOf(block);
    }

    static boolean mayHarvest(ServerWorld world, BlockPos pos) {
        BlockState state = stateAt(world, pos);

        return state != null && BlockProtection.mayBreak(world, pos, state);
    }
}
