package com.villagecolony.fabric.integration;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Optional;

/**
 * Acha a árvore mais próxima do centro da colônia.
 *
 * <p>Qualquer árvore da tabela de {@link TreeSpecies}, e só o tronco.
 * Tronco descascado e bloco de madeira ficam de fora: são material de
 * construção do jogador, não floresta.
 *
 * <p>Nunca varre o volume do raio. Um raio de 64 em três dimensões são
 * milhões de blocos, e Performance-Rules.md §5 e §6 proíbem esse
 * caminho. Aqui se percorrem colunas, e de cada coluna se olha só a
 * faixa em torno da superfície — o mapa de altura já diz onde ela está.
 *
 * <p>Chunk não carregado é pulado sem forçar carregamento, pela ADR-002
 * e pela lição de 2026-08-07: forçar de dentro do ciclo do servidor
 * trava a thread.
 */
public final class TreeScanner {

    /**
     * Quantas colunas se olha por busca, no máximo.
     *
     * <p>O raio de 64 tem mais de dezesseis mil colunas, e cada uma custa
     * uma consulta de chunk e até vinte e cinco leituras de bloco. Sem
     * teto, uma colônia sem árvore por perto pagaria a varredura inteira
     * a cada ciclo.
     *
     * <p>Mil e vinte e quatro colunas cobrem um quadrado de dezesseis
     * blocos de lado em torno do centro. Era quatro mil até 2026-08-08,
     * quando o jogo do autor engasgou — ver §15.
     *
     * <p>A busca é em espiral a partir do centro, então parar no teto
     * significa "não achei perto", não "não achei". O ciclo seguinte
     * continua de onde este parou — ver {@link #NEXT_RING}.
     */
    private static final int MAX_COLUMNS = 1024;

    /**
     * Em que anel a próxima busca deste centro começa.
     *
     * <p>Mil e vinte e quatro colunas acabam no anel 16, e é só até ali
     * que uma busca alcança. O raio de 64 que ela recebe nunca era
     * atingido: em 2026-08-12 o lenhador da vila de {@code 1109,730}
     * passou a sessão inteira em "looking for a tree" porque a floresta
     * dele começa depois do décimo sexto bloco, e toda busca recomeçava
     * do centro para morrer no mesmo lugar.
     *
     * <p>Guardar onde parou faz o alcance crescer com o tempo sem custar
     * mais por ciclo: cada busca paga as mesmas mil colunas, e o anel
     * seguinte é problema do ciclo seguinte. Achar zera o cursor — a
     * árvore encontrada sai do mundo, e a próxima procura recomeça de
     * perto, que é onde o trabalhador prefere trabalhar.
     *
     * <p>Em memória e descartável. Perder isto ao reiniciar custa alguns
     * ciclos de busca perto do centro, e nada mais.
     */
    private static final java.util.Map<BlockPos, Integer> NEXT_RING = new java.util.HashMap<>();

    /**
     * Quantos blocos acima e abaixo da superfície se procura tronco.
     *
     * <p>O mapa de altura aponta o topo do que bloqueia movimento — a
     * copa, no caso de uma árvore. O tronco está abaixo dela.
     */
    private static final int SURFACE_MARGIN = 24;

    private TreeScanner() {
    }

    /**
     * O tronco mais próximo do centro, dentro do raio.
     *
     * <p>Devolve vazio quando não há nenhum ao alcance, e isso não é
     * erro: a colônia espera o ciclo seguinte em vez de mandar o
     * trabalhador para o horizonte.
     */
    public static Optional<BlockPos> findNearestLog(
            ServerWorld world, BlockPos center, int radius) {

        return findNearestLog(world, center, radius, log -> true);
    }

    /**
     * O tronco mais próximo que quem procura aceita.
     *
     * <p>O filtro existe porque a colônia passou a ter vários lenhadores
     * ao mesmo tempo. A busca parte sempre do centro e é determinística,
     * então sem filtro os cinco lenhadores recebem a mesma árvore, quatro
     * deles chegam num tronco que já caiu e o jogador vê meia vila em
     * volta de um toco. Quem chama diz quais troncos já estão tomados; a
     * espiral simplesmente continua.
     *
     * <p>Recusado não conta para o teto de colunas de propósito: a coluna
     * foi olhada e custou o mesmo. Um lenhador cercado de árvores tomadas
     * desiste no mesmo lugar em que desistiria se não houvesse árvore
     * nenhuma, e tenta de novo depois.
     */
    public static Optional<BlockPos> findNearestLog(
            ServerWorld world,
            BlockPos center,
            int radius,
            java.util.function.Predicate<BlockPos> accepts) {

        int columns = 0;

        int startRing = NEXT_RING.getOrDefault(center, 0);

        if (startRing > radius) {
            startRing = 0;
        }

        for (int ring = startRing; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {

                    // Só a casca do anel: o miolo já foi visto nos anéis
                    // anteriores. O salto pula o miolo inteiro em vez de
                    // percorrê-lo descartando — a primeira versão
                    // iterava mais de um milhão de posições para olhar
                    // quatro mil colunas.
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        dz = ring - 1;

                        continue;
                    }

                    if (++columns > MAX_COLUMNS) {
                        // Recomeça neste anel, e não no seguinte: ele
                        // ficou pela metade. Reolhar a primeira metade
                        // custa colunas que já custariam de qualquer
                        // forma, e é mais barato que guardar em que
                        // ponto do anel a busca estava.
                        NEXT_RING.put(center.toImmutable(), ring);

                        return Optional.empty();
                    }

                    Optional<BlockPos> log = logInColumn(
                            world, center.getX() + dx, center.getZ() + dz);

                    if (log.isPresent() && accepts.test(log.get())) {
                        NEXT_RING.remove(center);

                        return log;
                    }
                }
            }
        }

        // Varreu até a borda do raio sem achar nada. Recomeçar do centro
        // é o certo: a floresta cresce, e a muda replantada perto volta a
        // ser árvore antes de a busca dar a volta inteira de novo.
        NEXT_RING.remove(center);

        return Optional.empty();
    }

    /** Esquece os cursores, junto com o resto do estado em memória. */
    public static void clearAll() {
        NEXT_RING.clear();
    }

    /** O tronco mais baixo desta coluna, se houver. */
    private static Optional<BlockPos> logInColumn(ServerWorld world, int x, int z) {
        WorldChunk chunk = world.getChunkManager().getWorldChunk(x >> 4, z >> 4);

        if (chunk == null) {
            return Optional.empty();
        }

        int surface = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, x & 15, z & 15);

        int top = Math.min(surface + 1, world.getTopY() - 1);
        int bottom = Math.max(surface - SURFACE_MARGIN, world.getBottomY());

        for (int y = bottom; y <= top; y++) {
            BlockPos pos = new BlockPos(x, y, z);

            if (TreeSpecies.isLog(chunk.getBlockState(pos))) {
                return Optional.of(pos);
            }
        }

        return Optional.empty();
    }
}
