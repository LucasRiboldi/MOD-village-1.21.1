package com.villagecolony.fabric.world;

import com.villagecolony.core.colony.model.VillageBounds;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A rua que sai da vila e a faz crescer — decisão do autor, 2026-10-03.
 *
 * <p><i>"Se houver 10 blocos de caminho conectados fora da vila e que
 * encostem em blocos de caminho de dentro da vila, só então a área desta
 * lateral da vila é aumentada até o limite máximo desses blocos."</i>
 *
 * <p>Antes, cada bloco de rua que a colônia assentava fora da caixa a
 * empurrava na hora, e o caminho do jogador não contava. Agora vale o
 * caminho que está no mundo, de quem quer que seja: a busca anda pela borda
 * da caixa, acha o caminho de dentro encostado nela e segue o caminho de fora
 * conectado a ele. Corrente com {@link #MIN_CHAIN} blocos ou mais entra
 * inteira; menor, não conta.
 *
 * <p>Só lê chunk carregado, como todo o projeto (§11). Conectado é vizinho
 * no plano, inclusive na diagonal, com um degrau de altura no máximo.
 */
public final class RoadSpill {

    /** Blocos de caminho conectados fora da vila para ela crescer. */
    public static final int MIN_CHAIN = 10;

    /** Teto de uma corrente seguida numa passagem: a borda inteira continua barata. */
    static final int MAX_CHAIN = 256;

    private RoadSpill() {
    }

    /** Os blocos de caminho de fora que fazem a vila crescer, se houver. */
    public static List<BlockPos> outsideChains(ServerWorld world, VillageBounds box) {
        List<BlockPos> grow = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();

        for (int x = box.minX(); x <= box.maxX(); x++) {
            follow(world, box, x, box.minZ(), seen, grow);
            follow(world, box, x, box.maxZ(), seen, grow);
        }

        for (int z = box.minZ() + 1; z < box.maxZ(); z++) {
            follow(world, box, box.minX(), z, seen, grow);
            follow(world, box, box.maxX(), z, seen, grow);
        }

        return grow;
    }

    /** O caminho de dentro nesta coluna da borda, e as correntes de fora encostadas nele. */
    private static void follow(
            ServerWorld world, VillageBounds box, int x, int z, Set<BlockPos> seen, List<BlockPos> grow) {

        Optional<BlockPos> inside = pathIn(world, x, z, box);

        if (inside.isEmpty()) {
            return;
        }

        for (BlockPos next : neighbours(world, inside.get())) {
            if (box.containsColumn(next.getX(), next.getZ()) || seen.contains(next)) {
                continue;
            }

            List<BlockPos> chain = chainFrom(world, box, next, seen);

            if (chain.size() >= MIN_CHAIN) {
                grow.addAll(chain);
            }
        }
    }

    /** A corrente de caminho fora da caixa a partir deste bloco. */
    private static List<BlockPos> chainFrom(ServerWorld world, VillageBounds box, BlockPos start, Set<BlockPos> seen) {
        List<BlockPos> chain = new ArrayList<>();
        Deque<BlockPos> open = new ArrayDeque<>();

        open.add(start);
        seen.add(start);

        while (!open.isEmpty() && chain.size() < MAX_CHAIN) {
            BlockPos at = open.poll();

            chain.add(at);

            for (BlockPos next : neighbours(world, at)) {
                if (!box.containsColumn(next.getX(), next.getZ()) && seen.add(next)) {
                    open.add(next);
                }
            }
        }

        return chain;
    }

    /** Os caminhos vizinhos no plano, com até um degrau de altura. */
    private static List<BlockPos> neighbours(ServerWorld world, BlockPos at) {
        List<BlockPos> found = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos next = at.add(dx, dy, dz);

                    if (isLoaded(world, next) && world.getBlockState(next).isOf(Blocks.DIRT_PATH)) {
                        found.add(next.toImmutable());
                        break;
                    }
                }
            }
        }

        return found;
    }

    /**
     * O caminho desta coluna na faixa de altura da vila, de cima para baixo.
     *
     * <p>Não pelo mapa de altura: folha de árvore, poste ou toldo por cima do
     * caminho viram o topo da coluna e o escondem — o GameTest mostrou isso
     * com a barreira do teto da arena.
     */
    static Optional<BlockPos> pathIn(ServerWorld world, int x, int z, VillageBounds box) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return Optional.empty();
        }

        for (int y = box.maxY(); y >= box.minY(); y--) {
            BlockPos at = new BlockPos(x, y, z);

            if (world.getBlockState(at).isOf(Blocks.DIRT_PATH)) {
                return Optional.of(at);
            }
        }

        return Optional.empty();
    }

    private static boolean isLoaded(ServerWorld world, BlockPos at) {
        return world.isChunkLoaded(at.getX() >> 4, at.getZ() >> 4);
    }
}
