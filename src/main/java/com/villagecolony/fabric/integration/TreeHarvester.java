package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
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
     * Quanto acima da muda o caminho precisa estar livre.
     *
     * <p>Um carvalho comum sobe até sete blocos. Abrir oito deixa a muda
     * com espaço para virar árvore em vez de ficar plantada para sempre
     * debaixo da copa da árvore anterior.
     */
    private static final int SAPLING_CLEARANCE = 8;

    /**
     * Derruba a árvore que contém este tronco.
     *
     * <p>Percorre os troncos ligados por vizinhança, inclusive na
     * diagonal — carvalho cresce torto e o tronco nem sempre é uma
     * coluna reta.
     *
     * <p>A ordem é a pedida pelo autor em 2026-08-08: derrubar a árvore
     * inteira, recolher a madeira, e só então replantar. Replantar antes
     * de o tronco descer inteiro planta uma muda debaixo da própria
     * árvore.
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

        // Tronco cortado no teto é árvore pela metade: o que sobrou
        // continua de pé e ainda é o tronco desta árvore. Replantar
        // agora poria uma muda debaixo dele. A árvore desce no ciclo
        // seguinte — a busca reencontra o que ficou — e a muda entra
        // quando o último tronco tiver caído.
        if (logs.size() >= MAX_LOGS) {
            VillageColonyMod.LOGGER.info(
                    "Tree at {} hit the {}-log ceiling — felling continues next cycle,"
                            + " no sapling yet",
                    base.toShortString(),
                    MAX_LOGS);

            return logs.size();
        }

        clearAbove(world, base);

        replant(world, base);

        return logs.size();
    }

    /**
     * Abre a coluna acima da muda.
     *
     * <p>A copa da árvore derrubada fica de pé — folha não é tronco, e a
     * regra do autor sempre foi não encostar nela. Só que a folha logo
     * acima da base é justamente o que impede a muda de crescer: ela
     * fica plantada indefinidamente, e a floresta não se repõe. Este é o
     * único lugar onde folha é tocada, e mesmo aqui é uma coluna de um
     * bloco de largura.
     *
     * <p>Para no primeiro bloco que não seja folha nem ar. Um telhado, uma
     * ponte ou uma varanda do jogador acima da árvore encerra a limpeza
     * ali: a muda não vai crescer, e isso é problema dela, não licença
     * para abrir buraco em construção alheia.
     *
     * <p>Sem drop, pela mesma razão dos troncos: item no chão despawna e
     * a contagem passaria a mentir.
     */
    private static void clearAbove(ServerWorld world, BlockPos base) {
        for (int height = 1; height <= SAPLING_CLEARANCE; height++) {
            BlockPos above = base.up(height);
            BlockState state = stateAt(world, above);

            if (state == null) {
                return;
            }

            if (state.isAir()) {
                continue;
            }

            if (!state.isOf(Blocks.OAK_LEAVES)) {
                return;
            }

            world.removeBlock(above, false);
        }
    }

    /**
     * Quantos troncos esta árvore tem, sem tocar em nada.
     *
     * <p>Serve para perguntar antes de derrubar: o tronco é removido sem
     * drop, então madeira que não caiba no baú do trabalhador é madeira
     * destruída. Ver {@code ChestDepositor.freeSpaceFor}.
     */
    public static int trunkSize(ServerWorld world, BlockPos anyLog) {
        return connectedLogs(world, anyLog).size();
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
