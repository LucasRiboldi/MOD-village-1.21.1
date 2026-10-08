package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    static {
        ServerMemory.register(CropPatch.class, CropPatch::clearAll);
    }

    /** Quanto acima e abaixo do centro se procura. */
    private static final int LEVELS = 6;

    /**
     * O primeiro canteiro vazio já visto numa volta pausada.
     *
     * <p>O cursor do {@link RingSweep} guarda só onde retomar. Mas o
     * canteiro vazio é uma resposta colhida de passagem, e precisa
     * atravessar a mesma pausa: se a primeira fatia o vê e a segunda
     * fatia não, a colônia ainda viu o canteiro.
     */
    private static final Map<UUID, BlockPos> EMPTY_PLOTS = new HashMap<>();

    /**
     * A terra arada que a varredura já achou, por colônia — A-8, 2026-10-02.
     * O fazendeiro olha estas células antes de varrer o raio: a roça não muda
     * de lugar, e na sessão de 02-10 a varredura não terminou no prazo 23 vezes.
     */
    private static final Map<UUID, java.util.LinkedHashSet<BlockPos>> KNOWN = new HashMap<>();

    /** Quantas células de roça cada colônia lembra. */
    private static final int MAX_KNOWN = 4_096;

    /**
     * Quantas células conhecidas uma passagem olha — E3, 2026-10-08: cursor
     * incremental sobre a roça conhecida, em vez de revarrer o raio inteiro.
     */
    static final int KNOWN_PER_PASS = 512;

    /** De quanto em quanto tempo a varredura do raio volta, para achar roça nova: 2 min. */
    static final long EXPANSION_EVERY = 2_400;

    /** Onde a volta pela roça conhecida parou, por colônia. */
    private static final Map<UUID, Integer> KNOWN_CURSOR = new HashMap<>();

    /** O primeiro canteiro vazio visto na volta em curso pela roça conhecida. */
    private static final Map<UUID, BlockPos> LAP_PLOT = new HashMap<>();

    /** Quando a varredura do raio fechou pela última vez, por colônia. */
    private static final Map<UUID, Long> LAST_EXPANSION = new HashMap<>();

    private CropPatch() {
    }

    /** Se este bloco é lavoura pronta para colher. */
    public static boolean isRipe(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMature(state);
    }

    /**
     * A lavoura madura mais perto deste centro, numa passagem só.
     *
     * <p>Em anéis, do centro para fora: a primeira que aparece é a mais
     * perto, e a busca acaba nela. Chunk fora de memória é pulado sem
     * forçar carregamento — ADR-002.
     *
     * <p><b>Sem cursor, e por isso sem retomada</b>: a pergunta aqui é
     * <i>"há lavoura madura agora?"</i>, feita de fora do ciclo de
     * trabalho. Quem varre o campo de verdade é {@link #survey}, que
     * atravessa passagens. O dono sorteado garante que esta chamada não
     * mexa no cursor de colônia nenhuma.
     *
     * <p>Vale para raio pequeno, que é onde ela é usada: acima de
     * {@link RingSweep#MAX_COLUMNS} colunas a resposta vazia passa a
     * querer dizer <i>"não terminei de olhar"</i>, e esta assinatura não
     * tem como dizer isso.
     */
    public static Optional<BlockPos> ripeNear(ServerWorld world, BlockPos center, int radius) {
        return survey(world, UUID.randomUUID(), center, radius).ripe();
    }

    /**
     * O que o fazendeiro tem para fazer em volta do centro — 2026-09-05.
     *
     * <p><b>Uma varredura, três respostas.</b> A lavoura madura, o
     * canteiro arado e vazio e a terra de arar percorrem exatamente as
     * mesmas colunas: perguntar as três em passagens separadas
     * triplicaria o custo justamente no caso que virou comum — a vila com
     * a lavoura toda plantada e nada maduro, que é onde o fazendeiro
     * passou 86 dos 81 ciclos da sessão de 2026-09-04.
     *
     * <p><b>E ela para cedo quando pode.</b> Lavoura madura ganha de
     * tudo, então achá-la encerra a busca na hora; sem ela, a varredura
     * segue até o orçamento de colunas da passagem acabar.
     *
     * <p><b>E ela retoma de onde parou</b> (P1.5): a espiral é a do
     * {@link RingSweep}, com cursor por dono; antes, toda passagem recomeçava do
     * centro e nunca passava do anel 22 do raio 32.
     *
     * <p>Chunk fora de memória é pulado sem forçar carregamento — ADR-002.
     *
     * @param colonyId de quem é a varredura, para o cursor saber onde
     *     retomar. A lavoura da vila é da vila, e dois fazendeiros
     *     dividem a mesma volta em vez de varrerem o mesmo campo duas
     *     vezes
     */
    public static Field survey(
            ServerWorld world, UUID colonyId, BlockPos center, int radius) {

        // O canteiro vazio é achado de passagem: a busca só <b>para</b>
        // por lavoura madura, e guarda o primeiro canteiro que cruzar
        // para o caso de não haver nenhuma. Uma casa, e não duas
        // varreduras — é a razão de este método existir.
        BlockPos remembered = rememberedEmptyPlot(world, colonyId, center, radius);

        // A roça conhecida primeiro — A-8 e E3. Ela responde sozinha (maduro,
        // canteiro vazio, ou "nada" com a volta completa); o raio só é varrido
        // quando ela está vazia ou quando a expansão periódica vence.
        boolean expansionDue = world.getTime() - LAST_EXPANSION.getOrDefault(colonyId, Long.MIN_VALUE / 2)
                >= EXPANSION_EVERY;
        boolean sweeping = RingSweep.pausedAt(colonyId, RingSweep.Scan.FARMING).isPresent();
        Optional<Field> known = expansionDue && sweeping
                ? Optional.empty()
                : fromKnownFarmland(world, colonyId, remembered, expansionDue);

        if (known.isPresent()) {
            return known.get();
        }

        BlockPos[] plot = {remembered};

        Optional<BlockPos> ripe = RingSweep.around(colonyId, RingSweep.Scan.FARMING, center, radius,
                column -> true, at -> {
            WorldChunk chunk = loadedChunk(world, at);

            if (chunk == null) {
                return Optional.empty();
            }

            for (int dy = LEVELS; dy >= -LEVELS; dy--) {
                BlockPos column = new BlockPos(at.getX(), center.getY() + dy, at.getZ());
                BlockState state = chunk.getBlockState(column);

                if (isRipe(state)) {
                    learn(colonyId, column.down());
                    return Optional.of(column);
                }

                if (isFarmland(state)) {
                    learn(colonyId, column);
                }

                if (plot[0] == null && isEmptyPlot(chunk, column)) {
                    plot[0] = column.toImmutable();
                }
            }

            return Optional.empty();
        });

        boolean incomplete = RingSweep.pausedAt(colonyId, RingSweep.Scan.FARMING).isPresent();

        if (!incomplete) {
            LAST_EXPANSION.put(colonyId, world.getTime());
        }

        if (ripe.isPresent() || !incomplete || plot[0] == null) {
            EMPTY_PLOTS.remove(colonyId);
        } else {
            EMPTY_PLOTS.put(colonyId, plot[0]);
        }

        return new Field(ripe.orElse(null), plot[0], incomplete);
    }

    /**
     * O trabalho que a roça conhecida já tem — E3: cursor incremental, no máximo
     * {@link #KNOWN_PER_PASS} células por passagem. Maduro encerra na hora; no fim
     * da volta vale o primeiro canteiro vazio, ou "nada" (volta completa, sem
     * varrer o raio) — a não ser que a expansão periódica tenha vencido, e então o
     * raio é varrido para achar roça nova. Célula que deixou de ser terra arada sai.
     *
     * @return vazio quando o raio precisa ser varrido
     */
    private static Optional<Field> fromKnownFarmland(
            ServerWorld world, UUID colonyId, BlockPos remembered, boolean expansionDue) {

        java.util.LinkedHashSet<BlockPos> cells = KNOWN.get(colonyId);

        if (cells == null || cells.isEmpty()) {
            return Optional.empty();
        }

        int start = KNOWN_CURSOR.getOrDefault(colonyId, 0);
        int index = 0;
        int looked = 0;

        for (java.util.Iterator<BlockPos> it = cells.iterator(); it.hasNext(); index++) {
            BlockPos cell = it.next();

            if (index < start) {
                continue;
            }

            if (looked++ >= KNOWN_PER_PASS) {
                KNOWN_CURSOR.put(colonyId, index);

                return Optional.of(new Field(null, null, true));
            }

            WorldChunk chunk = loadedChunk(world, cell);

            if (chunk == null) {
                continue;
            }

            if (!isFarmland(chunk.getBlockState(cell))) {
                it.remove();
                index--;
                continue;
            }

            if (isRipe(chunk.getBlockState(cell.up()))) {
                KNOWN_CURSOR.put(colonyId, index);

                return Optional.of(new Field(cell.up().toImmutable(), LAP_PLOT.get(colonyId), false));
            }

            if (!LAP_PLOT.containsKey(colonyId) && chunk.getBlockState(cell.up()).isAir()) {
                LAP_PLOT.put(colonyId, cell.toImmutable());
            }
        }

        // A volta fechou.
        KNOWN_CURSOR.remove(colonyId);
        BlockPos plot = LAP_PLOT.remove(colonyId);

        if (plot == null) {
            plot = remembered;
        }

        if (plot == null && expansionDue) {
            return Optional.empty();
        }

        return Optional.of(new Field(null, plot, false));
    }

    private static void learn(UUID colonyId, BlockPos farmland) {
        java.util.LinkedHashSet<BlockPos> cells = KNOWN.computeIfAbsent(colonyId, id -> new java.util.LinkedHashSet<>());

        if (cells.size() < MAX_KNOWN) {
            cells.add(farmland.toImmutable());
        }
    }

    /**
     * O canteiro vazio lembrado ainda pertence a esta busca e ainda está
     * vazio.
     */
    private static BlockPos rememberedEmptyPlot(
            ServerWorld world, UUID colonyId, BlockPos center, int radius) {

        BlockPos remembered = EMPTY_PLOTS.get(colonyId);

        if (remembered == null) {
            return null;
        }

        if (!insideSurvey(remembered, center, radius)) {
            EMPTY_PLOTS.remove(colonyId);

            return null;
        }

        WorldChunk chunk = loadedChunk(world, remembered);

        if (chunk == null || !isEmptyPlot(chunk, remembered)) {
            EMPTY_PLOTS.remove(colonyId);

            return null;
        }

        return remembered;
    }

    /** O chunk já carregado desta coluna, ou nada se ele está fora de memória. */
    private static WorldChunk loadedChunk(ServerWorld world, BlockPos at) {
        return world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4);
    }

    /** Se este bloco ainda cabe na janela vertical e horizontal da volta. */
    private static boolean insideSurvey(BlockPos plot, BlockPos center, int radius) {
        return Math.abs(plot.getX() - center.getX()) <= radius
                && Math.abs(plot.getZ() - center.getZ()) <= radius
                && Math.abs(plot.getY() - center.getY()) <= LEVELS;
    }

    /** Terra arada vazia já lida de um chunk carregado. */
    private static boolean isEmptyPlot(WorldChunk chunk, BlockPos at) {
        return isFarmland(chunk.getBlockState(at)) && chunk.getBlockState(at.up()).isAir();
    }

    /** Esquece a memória de uma colônia. Chamado por testes e limpeza. */
    public static void forget(UUID colonyId) {
        EMPTY_PLOTS.remove(colonyId);
        KNOWN.remove(colonyId);
        KNOWN_CURSOR.remove(colonyId);
        LAP_PLOT.remove(colonyId);
        LAST_EXPANSION.remove(colonyId);
    }

    /** Esquece todos os canteiros lembrados. Chamado ao descarregar. */
    public static void clearAll() {
        EMPTY_PLOTS.clear();
        KNOWN.clear();
        KNOWN_CURSOR.clear();
        LAP_PLOT.clear();
        LAST_EXPANSION.clear();
    }

    /**
     * O que a varredura achou: o mais perto de cada coisa, ou nada.
     *
     * <p>A ordem dos campos é a ordem de prioridade do fazendeiro:
     * colher ganha de semear.
     *
     * <p><b>Arar saiu daqui em 2026-09-05</b>, e saiu por queixa do
     * autor: <i>"não podem arar qualquer lugar"</i>. Ele arava toda terra
     * hidratada que achasse, e a sessão das 20:30 deixou <b>trinta e três
     * quadrados soltos espalhados por catorze blocos de vila</b>. Quem
     * abre roça agora é a obra, com a planta do próprio jogo — ver
     * {@code FarmPlans}.
     */
    public record Field(BlockPos nearestRipe, BlockPos nearestPlot, boolean incomplete) {

        /** A lavoura madura mais perto. */
        public Optional<BlockPos> ripe() {
            return Optional.ofNullable(nearestRipe);
        }

        /** O canteiro arado e vazio mais perto. */
        public Optional<BlockPos> emptyPlot() {
            return Optional.ofNullable(nearestPlot);
        }

        /**
         * Se o orçamento acabou antes de a volta fechar.
         *
         * <p><b>"Não achei" e "não terminei de olhar" são respostas
         * diferentes</b>, e confundi-las é o log mentindo justamente no
         * caso que ele existe para explicar — a frase é do
         * {@link RingSweep}, e vale aqui pelo mesmo motivo. Enquanto isto
         * for verdade, nada de vazio prova que o campo está vazio: prova
         * só que esta passagem não chegou ao fim.
         */
        @Override
        public boolean incomplete() {
            return incomplete;
        }
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

    /** Terra arada com nada plantada em cima — 2026-09-05. */
    public static boolean isEmptyPlot(ServerWorld world, BlockPos at) {
        return isFarmland(world.getBlockState(at)) && world.getBlockState(at.up()).isAir();
    }

    /**
     * Planta esta semente no canteiro — 2026-09-05.
     *
     * <p>Quem sabe o que a semente vira é o item, e não uma tabela:
     * {@code BlockItem} carrega o bloco que ele coloca, e se esse bloco é
     * {@code CropBlock} então é semente. Trigo, cenoura, batata,
     * beterraba e o que um datapack acrescentar entram sem serem citados
     * — é a mesma escolha que o {@link #replant} já fazia.
     *
     * @return se a muda entrou
     */
    public static boolean sow(ServerWorld world, BlockPos plot, Item seed) {
        if (!isEmptyPlot(world, plot) || !(seed instanceof BlockItem item)
                || !(item.getBlock() instanceof CropBlock crop)) {

            return false;
        }

        world.setBlockState(plot.up(), crop.getDefaultState(), Block.NOTIFY_ALL);

        return true;
    }

    /** Se este item é semente de lavoura — a pergunta do {@link #sow}. */
    public static boolean isSeed(Item item) {
        return item instanceof BlockItem block && block.getBlock() instanceof CropBlock;
    }
}
