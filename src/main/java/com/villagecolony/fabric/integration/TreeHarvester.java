package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
 * Derruba a árvore, recolhe o que ela dá e replanta.
 *
 * <p>Primeiro código do mod que escreve no mundo. Tudo antes disto só
 * lia. Bloco quebrado por engano é dano no save do jogador, e é por isso
 * que as regras aqui são estreitas e explícitas:
 *
 * <ul>
 *   <li>só tronco e folha de árvore da tabela de {@link TreeSpecies} —
 *       tronco descascado e bloco de madeira são construção, não
 *       floresta;
 *   <li>só o que estiver ligado ao que se achou, e nunca tudo num raio;
 *   <li>só a copa daquele tronco: a folha é alcançada a partir dos
 *       troncos que caíram, não do ar em volta;
 *   <li>uma muda da própria espécie no lugar da base, para a floresta se
 *       repor.
 * </ul>
 *
 * <p>A ordem é a pedida pelo autor: derrubar a árvore inteira, recolher
 * tudo o que ela dropa, e só então replantar. Replantar antes planta uma
 * muda debaixo da própria árvore.
 *
 * <p>Nada cai no chão. Os drops são calculados pela mesma tabela de loot
 * que o jogo usaria e devolvidos a quem chamou, que os põe no baú — item
 * no chão despawna, cai n'água e é roubado por mob, e a contagem da
 * colônia passaria a mentir sem avisar.
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

    /**
     * Teto de folhas por árvore.
     *
     * <p>Uma copa de carvalho tem cerca de oitenta folhas; a de um
     * carvalho-escuro passa de duzentas. O teto corta a copa gigante em
     * duas colheitas em vez de fazer um ciclo pagar por ela inteira.
     */
    private static final int MAX_LEAVES = 160;

    /**
     * A que distância de um tronco a folha ainda é copa desta árvore.
     *
     * <p>Sem este limite, folhas encostadas umas nas outras ligariam uma
     * árvore à vizinha, e derrubar uma levaria a copa de meia floresta.
     * Seis blocos cobrem a copa de qualquer árvore do Overworld sem
     * atravessar para a de trás.
     */
    private static final int LEAF_REACH = 6;

    /**
     * Quanto acima da muda o caminho precisa estar livre.
     *
     * <p>Um carvalho comum sobe até sete blocos. Abrir oito deixa a muda
     * com espaço para virar árvore em vez de ficar plantada para sempre
     * debaixo da copa da árvore anterior.
     */
    private static final int SAPLING_CLEARANCE = 8;

    private TreeHarvester() {
    }

    /**
     * O que uma colheita rendeu.
     *
     * @param logs quantos troncos caíram
     * @param leaves quantas folhas foram colhidas
     * @param drops tudo o que os blocos deram, já somado por item
     * @param complete se a árvore desceu inteira — falso quando o teto
     *     cortou o tronco, e então não se replanta
     */
    public record Harvest(int logs, int leaves, List<ItemStack> drops, boolean complete) {

        public static Harvest nothing() {
            return new Harvest(0, 0, List.of(), false);
        }

        public boolean isEmpty() {
            return logs == 0 && leaves == 0;
        }
    }

    /**
     * Derruba a árvore que contém este tronco.
     *
     * <p>Percorre os troncos ligados por vizinhança, inclusive na
     * diagonal — árvore cresce torta e o tronco nem sempre é uma coluna
     * reta. Só troncos da mesma espécie: uma parede de bétula encostada
     * num carvalho é parede, não é a árvore.
     *
     * <p>Replanta na base quando o chão aceita muda. Não replanta em
     * pedra nem em areia, e isso não é erro: é a mesma resposta do
     * Vanilla para quem tenta plantar ali. Vale igual para o propágulo do
     * mangue, que quer lama.
     */
    public static Harvest fell(ServerWorld world, BlockPos anyLog) {
        TreeSpecies species = TreeSpecies.ofLog(stateAt(world, anyLog)).orElse(null);

        if (species == null) {
            return Harvest.nothing();
        }

        List<BlockPos> logs = connectedLogs(world, species, anyLog);

        if (logs.isEmpty()) {
            return Harvest.nothing();
        }

        BlockPos base = lowest(logs);
        boolean complete = logs.size() < MAX_LOGS;

        // A copa é achada antes de o tronco cair. Depois seria tarde: a
        // folha é alcançada a partir dos troncos, e sem eles não haveria
        // de onde partir.
        List<BlockPos> leaves = complete ? connectedLeaves(world, species, logs) : List.of();

        List<ItemStack> drops = new ArrayList<>();

        breakAll(world, logs, drops);
        breakAll(world, leaves, drops);

        if (!complete) {
            // Tronco cortado no teto é árvore pela metade: o que sobrou
            // continua de pé e ainda é o tronco desta árvore. Replantar
            // agora poria uma muda debaixo dele. A árvore desce no ciclo
            // seguinte — a busca reencontra o que ficou — e a muda entra
            // quando o último tronco tiver caído.
            VillageColonyMod.LOGGER.info(
                    "Tree at {} hit the {}-log ceiling — felling continues next cycle,"
                            + " no sapling yet",
                    base.toShortString(),
                    MAX_LOGS);
        } else {
            clearAbove(world, base);

            replant(world, species, base);
        }

        return new Harvest(logs.size(), leaves.size(), merge(drops), complete);
    }

    /**
     * Quantos troncos esta árvore tem, sem tocar em nada.
     *
     * <p>Serve para perguntar antes de derrubar: o tronco é removido sem
     * drop no mundo, então madeira que não caiba no baú do trabalhador é
     * madeira destruída. Ver {@code ChestDepositor.freeSpaceFor}.
     */
    public static int trunkSize(ServerWorld world, BlockPos anyLog) {
        return TreeSpecies.ofLog(stateAt(world, anyLog))
                .map(species -> connectedLogs(world, species, anyLog).size())
                .orElse(0);
    }

    /**
     * Quebra os blocos e guarda o que eles dariam.
     *
     * <p>{@code getDroppedStacks} é a mesma tabela de loot que o jogo
     * consultaria — é dela que vem a muda de vez em quando, a maçã do
     * carvalho e o graveto. Repetir essas probabilidades aqui seria
     * inventar uma segunda verdade sobre o que uma árvore dá.
     *
     * <p>A ferramenta é vazia de propósito: o aldeão colhe com a mão, e
     * é assim que a folha dá muda em vez de dar folha.
     */
    private static void breakAll(ServerWorld world, List<BlockPos> blocks, List<ItemStack> drops) {
        for (BlockPos pos : blocks) {
            BlockState state = stateAt(world, pos);

            if (state == null) {
                continue;
            }

            drops.addAll(Block.getDroppedStacks(
                    state, world, pos, null, null, ItemStack.EMPTY));

            world.removeBlock(pos, false);
        }
    }

    /** Soma os stacks do mesmo item, para o baú não receber gota a gota. */
    private static List<ItemStack> merge(List<ItemStack> drops) {
        List<ItemStack> merged = new ArrayList<>();

        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }

            ItemStack existing = null;

            for (ItemStack candidate : merged) {
                if (candidate.isOf(drop.getItem())) {
                    existing = candidate;

                    break;
                }
            }

            if (existing == null) {
                merged.add(drop.copy());
            } else {
                existing.increment(drop.getCount());
            }
        }

        return merged;
    }

    /** Os troncos da mesma espécie ligados a este, até o teto. */
    private static List<BlockPos> connectedLogs(
            ServerWorld world, TreeSpecies species, BlockPos start) {

        List<BlockPos> found = new ArrayList<>();

        if (!isBlock(world, start, species.log())) {
            return found;
        }

        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        queue.add(start);
        seen.add(start);

        while (!queue.isEmpty() && found.size() < MAX_LOGS) {
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
     * {@link #LEAF_REACH} de qualquer tronco, senão copas encostadas
     * ligariam uma árvore à vizinha e derrubar uma levaria a floresta.
     */
    private static List<BlockPos> connectedLeaves(
            ServerWorld world, TreeSpecies species, List<BlockPos> logs) {

        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>(logs);
        Deque<BlockPos> queue = new ArrayDeque<>(logs);

        while (!queue.isEmpty() && found.size() < MAX_LEAVES) {
            BlockPos current = queue.removeFirst();

            for (BlockPos neighbour : around(current)) {
                if (!seen.add(neighbour)) {
                    continue;
                }

                if (!isBlock(world, neighbour, species.leaves())) {
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

    private static boolean isWithinReachOfATrunk(BlockPos leaf, List<BlockPos> logs) {
        for (BlockPos log : logs) {
            if (leaf.isWithinDistance(log, LEAF_REACH)) {
                return true;
            }
        }

        return false;
    }

    /** Os vinte e seis vizinhos, inclusive as diagonais. */
    private static List<BlockPos> around(BlockPos pos) {
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

    /**
     * Abre a coluna acima da muda.
     *
     * <p>A copa desta árvore já saiu com a colheita. O que pode ter
     * sobrado ali é folha de outra árvore — a vizinha cuja copa passa por
     * cima desta base — e é ela que impediria a muda de crescer.
     *
     * <p>Para no primeiro bloco que não seja folha nem ar. Um telhado,
     * uma ponte ou uma varanda do jogador acima da árvore encerra a
     * limpeza ali: a muda não vai crescer, e isso é problema dela, não
     * licença para abrir buraco em construção alheia.
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

            if (!isAnyLeaves(state)) {
                return;
            }

            world.removeBlock(above, false);
        }
    }

    private static boolean isAnyLeaves(BlockState state) {
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
    private static BlockState stateAt(ServerWorld world, BlockPos pos) {
        WorldChunk chunk = loadedChunkAt(world, pos);

        return chunk == null ? null : chunk.getBlockState(pos);
    }

    private static WorldChunk loadedChunkAt(ServerWorld world, BlockPos pos) {
        return world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static boolean isBlock(ServerWorld world, BlockPos pos, Block block) {
        BlockState state = stateAt(world, pos);

        return state != null && state.isOf(block);
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
     * Muda da própria espécie no lugar da base, se o chão aceitar.
     *
     * <p>{@code canPlaceAt} é quem responde — a mesma pergunta que o
     * jogo faz quando o jogador tenta plantar. Repetir a regra aqui
     * seria inventar uma segunda verdade sobre o que é chão bom, e ela
     * envelheceria no primeiro bioma novo.
     */
    private static void replant(ServerWorld world, TreeSpecies species, BlockPos base) {
        BlockState here = stateAt(world, base);

        if (here == null || !here.isAir()) {
            return;
        }

        BlockState sapling = species.sapling().getDefaultState();

        if (!sapling.canPlaceAt(world, base)) {
            return;
        }

        world.setBlockState(base, sapling);
    }
}
