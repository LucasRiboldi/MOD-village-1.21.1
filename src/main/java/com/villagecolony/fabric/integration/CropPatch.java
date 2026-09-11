package com.villagecolony.fabric.integration;

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

import java.util.List;
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

    /** Quanto acima e abaixo do centro se procura. */
    private static final int LEVELS = 6;

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
     * <p><b>E ela retoma de onde parou</b> — P1.5, 2026-09-11. Antes
     * disto a espiral era escrita aqui à mão, com orçamento de 2.048
     * colunas e <b>sem cursor</b>: toda passagem recomeçava do centro, e
     * o quadrado de raio 32 tem <b>4.225 colunas</b>. A varredura fechava
     * o anel 22 (45² = 2.025 colunas) e abortava no 23 — <b>48% da área
     * prometida</b>, sempre a mesma metade, para sempre.
     *
     * <p>Isso não era só desperdício. O {@code ConstructionPlanner} abre
     * roça até {@code FarmerWork.reach()} do centro, que valia 32 — então
     * <b>uma roça que a própria colônia mandou construir entre 23 e 32
     * blocos era invisível ao fazendeiro dela</b>. É o defeito da roça a
     * 105 blocos de 2026-09-05 de novo, em escala menor e por dentro: as
     * duas medidas concordavam no nome e discordavam no efeito.
     *
     * <p>O conserto não foi aumentar o teto — isso multiplicaria por dois
     * o custo da passagem justamente no caso comum, com o ciclo da
     * colônia já em 112 ms. Foi passar a espiral para o
     * {@link RingSweep}, que guarda anel <b>e coluna</b> por dono e já
     * carrega as três lições que as outras três espirais do projeto
     * aprenderam uma por vez. Esta era a quarta escrita à mão.
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
        BlockPos[] plot = {null};

        Optional<BlockPos> ripe = RingSweep.around(colonyId, center, radius, at -> {
            WorldChunk chunk = world.getChunkManager()
                    .getWorldChunk(at.getX() >> 4, at.getZ() >> 4);

            if (chunk == null) {
                return Optional.empty();
            }

            for (int dy = LEVELS; dy >= -LEVELS; dy--) {
                BlockPos column = new BlockPos(at.getX(), center.getY() + dy, at.getZ());

                if (isRipe(world.getBlockState(column))) {
                    return Optional.of(column);
                }

                if (plot[0] == null && isEmptyPlot(world, column)) {
                    plot[0] = column;
                }
            }

            return Optional.empty();
        });

        return new Field(ripe.orElse(null), plot[0], RingSweep.pausedAt(colonyId).isPresent());
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
