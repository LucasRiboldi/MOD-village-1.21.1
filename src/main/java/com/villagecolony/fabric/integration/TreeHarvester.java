package com.villagecolony.fabric.integration;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
 * Derruba a árvore e replanta.
 *
 * <p>Primeiro código do mod que escreve no mundo. Tudo antes disto só
 * lia. Bloco quebrado por engano é dano no save do jogador, e é por isso
 * que as regras aqui são estreitas e explícitas:
 *
 * <ul>
 *   <li>só {@code oak_log}, nada mais — folha, terra e construção do
 *       jogador ficam onde estão;
 *   <li>só troncos ligados ao que se achou, e não tudo num raio;
 *   <li>uma muda no lugar da base, para a floresta se repor.
 * </ul>
 *
 * <p>Decidido pelo autor em 2026-08-08. Ver §10.
 */
public final class TreeHarvester {

    /**
     * Teto de troncos por árvore.
     *
     * <p>Carvalho comum tem entre quatro e sete. O teto não existe para
     * eles: existe para o carvalho gigante de bioma escuro e para
     * qualquer construção de tronco que o jogador tenha feito e que
     * esteja encostada numa árvore. Sem teto, uma casa de madeira ligada
     * a uma árvore viraria estoque da colônia.
     */
    private static final int MAX_LOGS = 24;

    private TreeHarvester() {
    }

    /**
     * Derruba a árvore que contém este tronco.
     *
     * <p>Percorre os troncos ligados por vizinhança, inclusive na
     * diagonal — carvalho cresce torto e o tronco nem sempre é uma
     * coluna reta.
     *
     * <p>Replanta na base quando o chão aceita muda. Não replanta em
     * pedra nem em areia, e isso não é erro: é a mesma resposta do
     * Vanilla para quem tenta plantar ali.
     *
     * @return quantos troncos foram derrubados
     */
    public static int fell(ServerWorld world, BlockPos anyLog) {
        List<BlockPos> logs = connectedLogs(world, anyLog);

        if (logs.isEmpty()) {
            return 0;
        }

        BlockPos base = lowest(logs);

        for (BlockPos log : logs) {
            // Sem drop: a madeira vai direto para o baú do trabalhador,
            // por decisão do autor. Item no chão despawna, cai n'água e
            // é roubado por mob, e a contagem passaria a mentir.
            //
            // Só se o chunk estiver carregado — connectedLogs já os
            // filtrou, e escrever num chunk descarregado o carregaria à
            // força, na thread do servidor.
            if (loadedChunkAt(world, log) != null) {
                world.removeBlock(log, false);
            }
        }

        replant(world, base);

        return logs.size();
    }

    /** Os troncos ligados a este, até o teto. */
    private static List<BlockPos> connectedLogs(ServerWorld world, BlockPos start) {
        List<BlockPos> found = new ArrayList<>();

        if (!isOakLog(world, start)) {
            return found;
        }

        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        queue.add(start);
        seen.add(start);

        while (!queue.isEmpty() && found.size() < MAX_LOGS) {
            BlockPos current = queue.removeFirst();

            found.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {

                        BlockPos neighbour = current.add(dx, dy, dz);

                        if (!seen.add(neighbour)) {
                            continue;
                        }

                        if (isOakLog(world, neighbour)) {
                            queue.add(neighbour);
                        }
                    }
                }
            }
        }

        return found;
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
    private static BlockState stateAt(ServerWorld world, BlockPos pos) {
        WorldChunk chunk = loadedChunkAt(world, pos);

        return chunk == null ? null : chunk.getBlockState(pos);
    }

    private static WorldChunk loadedChunkAt(ServerWorld world, BlockPos pos) {
        return world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static boolean isOakLog(ServerWorld world, BlockPos pos) {
        BlockState state = stateAt(world, pos);

        return state != null && state.isOf(Blocks.OAK_LOG);
    }

    private static BlockPos lowest(List<BlockPos> logs) {
        BlockPos lowest = logs.get(0);

        for (BlockPos log : logs) {
            if (log.getY() < lowest.getY()) {
                lowest = log;
            }
        }

        return lowest;
    }

    /**
     * Muda no lugar da base, se o chão aceitar.
     *
     * <p>{@code canPlaceAt} é quem responde — a mesma pergunta que o
     * jogo faz quando o jogador tenta plantar. Repetir a regra aqui
     * seria inventar uma segunda verdade sobre o que é chão bom.
     */
    private static void replant(ServerWorld world, BlockPos base) {
        BlockState here = stateAt(world, base);

        if (here == null || !here.isAir()) {
            return;
        }

        if (!Blocks.OAK_SAPLING.getDefaultState().canPlaceAt(world, base)) {
            return;
        }

        world.setBlockState(base, Blocks.OAK_SAPLING.getDefaultState());
    }
}
