package com.villagecolony.fabric.integration;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.List;
import java.util.Optional;

/**
 * A lavoura: o que está maduro, e como se replanta — 2026-08-27.
 *
 * <p><b>Quem responde é o bloco, e não uma lista de nomes.</b> É a regra
 * de ouro da ADR-009, e o mesmo caminho que o minério tomou de manhã:
 * {@link CropBlock#isMature} é a pergunta que o próprio jogo faz, e ela
 * vale para trigo, cenoura, batata e beterraba sem que nenhum deles
 * precise ser escrito aqui — e para o que um datapack plantar depois.
 *
 * <p><b>Replantar sai da colheita.</b> O jogo devolve a semente junto
 * com a comida, e é dela que a nova muda sai: o fazendeiro não gasta
 * estoque para repor o que colheu. É a Regra 7 do lenhador aplicada onde
 * ela nasceu — colher sem replantar deixaria a vila com um campo de
 * terra arada vazia e uma refeição só.
 */
public final class CropPatch {

    /**
     * Quantas colunas se olham por busca.
     *
     * <p>A lavoura da vila fica em volta do centro e é pequena; o teto
     * existe para o caso de não haver nenhuma, e é o mesmo espírito do
     * orçamento da varredura de lote — Performance-Rules.md §6.
     */
    private static final int COLUMNS_PER_SEARCH = 2048;

    /** Quanto acima e abaixo do centro se procura. */
    private static final int LEVELS = 6;

    private CropPatch() {
    }

    /** Se este bloco é lavoura pronta para colher. */
    public static boolean isRipe(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMature(state);
    }

    /**
     * A lavoura madura mais perto deste centro.
     *
     * <p>Em anéis, do centro para fora: a primeira que aparece é a mais
     * perto, e a busca acaba nela. Chunk fora de memória é pulado sem
     * forçar carregamento — ADR-002.
     */
    public static Optional<BlockPos> ripeNear(ServerWorld world, BlockPos center, int radius) {
        int looked = 0;

        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {

                    // Só a casca do anel; o miolo já foi visto.
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        dz = ring - 1;

                        continue;
                    }

                    if (++looked > COLUMNS_PER_SEARCH) {
                        return Optional.empty();
                    }

                    Optional<BlockPos> found = ripeInColumn(
                            world, center.getX() + dx, center.getZ() + dz, center.getY());

                    if (found.isPresent()) {
                        return found;
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static Optional<BlockPos> ripeInColumn(ServerWorld world, int x, int z, int aroundY) {
        WorldChunk chunk = world.getChunkManager().getWorldChunk(x >> 4, z >> 4);

        if (chunk == null) {
            return Optional.empty();
        }

        for (int dy = LEVELS; dy >= -LEVELS; dy--) {
            BlockPos at = new BlockPos(x, aroundY + dy, z);

            if (isRipe(world.getBlockState(at))) {
                return Optional.of(at);
            }
        }

        return Optional.empty();
    }

    /**
     * Colhe e replanta, tirando a muda do que caiu.
     *
     * <p>O bloco sai antes de a muda entrar, e a muda é retirada de
     * {@code drops}: o que vai para o baú é o que sobra depois de repor a
     * lavoura. Sem semente no que caiu — um datapack estranho, ou a
     * colheita já pobre — o campo fica arado e vazio, e isso é dito no
     * log em vez de fingir que replantou.
     *
     * @param drops o que a colheita devolveu. A muda é <b>removida</b>
     *     daqui quando o replantio acontece
     * @return se a lavoura foi reposta
     */
    public static boolean replant(
            ServerWorld world, BlockPos at, BlockState harvested, List<ItemStack> drops) {

        world.removeBlock(at, false);

        Block crop = harvested.getBlock();

        for (ItemStack dropped : drops) {
            if (!(dropped.getItem() instanceof BlockItem item) || item.getBlock() != crop) {
                continue;
            }

            if (dropped.isEmpty()) {
                continue;
            }

            world.setBlockState(at, crop.getDefaultState(), Block.NOTIFY_ALL);

            dropped.decrement(1);

            return true;
        }

        // Trigo devolve semente, que não é o próprio bloco: a semente é
        // um item cuja colocação dá o bloco de trigo. Quem sabe disso é
        // o item, e é a ele que se pergunta.
        for (ItemStack dropped : drops) {
            if (dropped.isEmpty() || !(dropped.getItem() instanceof BlockItem item)) {
                continue;
            }

            if (!(item.getBlock() instanceof CropBlock)) {
                continue;
            }

            world.setBlockState(at, item.getBlock().getDefaultState(), Block.NOTIFY_ALL);

            dropped.decrement(1);

            return true;
        }

        return false;
    }

    /** Se este bloco é terra arada — onde a lavoura cabe. */
    public static boolean isFarmland(BlockState state) {
        return state.isOf(Blocks.FARMLAND);
    }
}
