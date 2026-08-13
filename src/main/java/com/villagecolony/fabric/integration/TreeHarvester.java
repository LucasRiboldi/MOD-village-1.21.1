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
 *   <li>só grupo de tronco com copa viva: folha da mesma espécie que
 *       nasceu ali, e não folha pendurada à mão. Sem copa não é árvore,
 *       é construção — casa de vila é feita de tronco, e o carvalho da
 *       planície é o mesmo carvalho da floresta;
 *   <li>só o que estiver ligado ao que se achou, e nunca tudo num raio;
 *   <li>só a copa daquele tronco: a folha é alcançada a partir dos
 *       troncos que caíram, não do ar em volta;
 *   <li>uma muda da própria espécie no lugar da base, para a floresta se
 *       repor.
 * </ul>
 *
 * <p><b>A árvore é a exceção da regra de 2026-08-13</b> — nunca destruir
 * bloco da vila original nem bloco posto pelo jogador. O lenhador derruba
 * árvore onde a achar, inclusive dentro dos limites que o jogo registra
 * para a vila, e por isso a colheita não consulta
 * {@link BlockProtection}. Quem separa a casa da floresta aqui é a regra
 * da copa, não a proteção. A única parte desta classe que pergunta é a
 * limpeza da coluna da muda, que toca bloco de outra árvore.
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
     * A colheita inteira, decidida antes de qualquer bloco cair.
     *
     * <p>É o que a Regra 2 pediu: o trabalhador quebra um bloco de cada
     * vez, ao longo de muitos ticks, e precisa saber desde o começo
     * quais blocos são desta árvore. Descobrir isso durante a colheita
     * não daria certo — a copa é alcançada a partir dos troncos, e
     * depois de o primeiro cair já não há de onde partir.
     *
     * <p>Decidir uma vez também é o que segura o custo: a varredura da
     * árvore é a parte cara, e ela acontece no tick em que a colheita
     * começa e em mais nenhum.
     *
     * @param blocks troncos primeiro, copa depois, na ordem em que caem
     * @param complete se a árvore desce inteira — falso quando o teto
     *     cortou o tronco, e então não se replanta
     */
    public record Plan(
            TreeSpecies species,
            BlockPos base,
            List<BlockPos> blocks,
            int logs,
            int leaves,
            boolean complete) {

        public static Plan nothing() {
            return new Plan(null, null, List.of(), 0, 0, false);
        }

        public boolean isEmpty() {
            return blocks.isEmpty();
        }
    }

    /**
     * O que há para colher nesta árvore, sem tocar em nada.
     *
     * <p>Percorre os troncos ligados por vizinhança, inclusive na
     * diagonal — árvore cresce torta e o tronco nem sempre é uma coluna
     * reta. Só troncos da mesma espécie: uma parede de bétula encostada
     * num carvalho é parede, não é a árvore.
     */
    public static Plan plan(ServerWorld world, BlockPos anyLog) {
        TreeSpecies species = TreeSpecies.ofLog(stateAt(world, anyLog)).orElse(null);

        if (species == null) {
            return Plan.nothing();
        }

        List<BlockPos> logs = connectedLogs(world, species, anyLog);

        if (logs.isEmpty()) {
            return Plan.nothing();
        }

        // A copa é achada antes de o tronco cair. Depois seria tarde: a
        // folha é alcançada a partir dos troncos, e sem eles não haveria
        // de onde partir.
        List<BlockPos> canopy = connectedLeaves(world, species, logs);

        // Sem copa viva não é árvore, e não se toca. Ver #isNaturalLeaf.
        //
        // Achada antes do teto de propósito: até 2026-08-12 a copa só era
        // procurada quando a árvore cabia no teto, e era justamente a
        // construção grande — a que passa de 24 troncos — que escapava do
        // teste sem nunca ser olhada.
        if (canopy.isEmpty()) {
            return Plan.nothing();
        }

        boolean complete = logs.size() < MAX_LOGS;

        List<BlockPos> leaves = complete ? canopy : List.of();

        List<BlockPos> blocks = new ArrayList<>(logs);
        blocks.addAll(leaves);

        return new Plan(
                species, lowest(logs), List.copyOf(blocks), logs.size(), leaves.size(), complete);
    }

    /**
     * Quebra um bloco da colheita e devolve o que ele deu.
     *
     * <p>Confere a espécie antes de quebrar, e é por isso que recebe o
     * plano em vez de só a posição. Entre planejar e chegar neste bloco
     * passam-se dezenas de ticks, e nesse meio-tempo o jogador pode ter
     * derrubado o tronco e posto uma tábua no lugar. Quebrar o que está
     * ali sem perguntar seria quebrar a construção dele.
     *
     * <p>Devolve lista vazia quando o bloco já não é desta árvore, e
     * isso não é erro: é a colheita encontrando o mundo mudado.
     */
    public static List<ItemStack> breakOne(ServerWorld world, Plan plan, BlockPos pos) {
        TreeSpecies species = plan.species();

        if (species == null) {
            return List.of();
        }

        // Folha do jogador não entra nem por engano: entre planejar e
        // chegar aqui ele pode ter posto uma no lugar da que caiu.
        if (!isBlock(world, pos, species.log()) && !isNaturalLeaf(world, pos, species)) {
            return List.of();
        }

        List<ItemStack> drops = new ArrayList<>();

        breakAll(world, List.of(pos), drops);

        return drops;
    }

    /**
     * Fecha a colheita: abre a coluna e replanta.
     *
     * <p>Só depois do último bloco, e é a ordem pedida pelo autor —
     * derrubar a árvore inteira, recolher, e só então replantar.
     * Replantar antes planta uma muda debaixo da própria árvore.
     *
     * <p>Replanta na base quando o chão aceita muda. Não replanta em
     * pedra nem em areia, e isso não é erro: é a mesma resposta do
     * Vanilla para quem tenta plantar ali. Vale igual para o propágulo do
     * mangue, que quer lama.
     */
    public static void finish(ServerWorld world, Plan plan) {
        if (plan.isEmpty()) {
            return;
        }

        if (!plan.complete()) {
            // Tronco cortado no teto é árvore pela metade: o que sobrou
            // continua de pé e ainda é o tronco desta árvore. Replantar
            // agora poria uma muda debaixo dele. A árvore desce na
            // colheita seguinte — a busca reencontra o que ficou — e a
            // muda entra quando o último tronco tiver caído.
            VillageColonyMod.LOGGER.info(
                    "Tree at {} hit the {}-log ceiling — felling continues next time,"
                            + " no sapling yet",
                    plan.base().toShortString(),
                    MAX_LOGS);

            return;
        }

        clearAbove(world, plan.base());

        replant(world, plan.species(), plan.base());
    }

    /**
     * Derruba a árvore inteira de uma vez.
     *
     * <p>Os mesmos passos que o trabalhador dá ao longo de muitos ticks
     * — planejar, quebrar bloco a bloco, fechar — só que sem esperar
     * entre eles. As regras da colheita moram no plano, e não aqui, para
     * que quem testa esta porta esteja testando as regras que o jogo
     * percorre, e não uma segunda versão delas.
     *
     * <p>Em jogo quem colhe é {@code LumberjackWork}, no ritmo da Regra
     * 2. Este caminho serve a quem precisa da árvore no chão agora.
     */
    public static Harvest fell(ServerWorld world, BlockPos anyLog) {
        Plan plan = plan(world, anyLog);

        if (plan.isEmpty()) {
            return Harvest.nothing();
        }

        List<ItemStack> drops = new ArrayList<>();

        for (BlockPos pos : plan.blocks()) {
            drops.addAll(breakOne(world, plan, pos));
        }

        finish(world, plan);

        return new Harvest(plan.logs(), plan.leaves(), merge(drops), plan.complete());
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
     *
     * <p>Só folha que nasceu ali. Ver {@link #isNaturalLeaf}.
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
    private static boolean isNaturalLeaf(ServerWorld world, BlockPos pos, TreeSpecies species) {
        BlockState state = stateAt(world, pos);

        if (!state.isOf(species.leaves())) {
            return false;
        }

        return state.contains(LeavesBlock.PERSISTENT) && !state.get(LeavesBlock.PERSISTENT);
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
     *
     * <p>Folha pendurada à mão encerra do mesmo jeito. Ela é construção
     * como qualquer outra — ver {@link #isNaturalLeaf} —, e a única
     * diferença é que aqui vale a folha de qualquer espécie: a copa que
     * cobre esta base pode ser da árvore vizinha.
     *
     * <p>É o único lugar da colheita que quebra bloco que não é desta
     * árvore, e por isso o único que pergunta a
     * {@link BlockProtection}: a copa que passa por cima da muda pode ser
     * de uma árvore que o jogo gerou junto com a vila. A árvore desta
     * colheita não passa por lá, pela exceção do autor.
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

            if (!isAnyNaturalLeaf(state)) {
                return;
            }

            if (!BlockProtection.mayBreak(world, above, state)) {
                return;
            }

            world.removeBlock(above, false);
        }
    }

    /** Folha de qualquer espécie, desde que tenha nascido ali. */
    private static boolean isAnyNaturalLeaf(BlockState state) {
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
