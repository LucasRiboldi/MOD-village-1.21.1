package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Onde a próxima casa cabe — a metade da Regra 6 que olha o mundo.
 *
 * <p>A regra do autor, de 2026-08-14: <b>estrada primeiro, casa ligada a
 * ela</b>. Nunca casa isolada com estrada depois.
 *
 * <p>A leitura que este código faz da regra: um lote só serve se
 * encostar numa estrada que <b>já existe</b>. É a forma mais barata de
 * obedecer — a vila de planície nasce cheia de rua, e o espaço livre ao
 * lado dela é o que sobra de fato. A vila cresce colada ao que já é vila,
 * em vez de espalhar casas pelo campo.
 *
 * <p><b>O que ainda falta desta regra</b>, e fica dito para não parecer
 * feito: quando não houver mais lote encostado em rua, a colônia precisa
 * <b>estender</b> a rua para abrir lote novo. Não está escrito. Até lá, a
 * vila cresce enquanto houver beira de rua livre, e para quando não
 * houver — o que é um limite mais apertado que o da regra, nunca mais
 * frouxo.
 *
 * <hr>
 *
 * <p><b>As três decisões que a implementação teve de tomar</b>, delegadas
 * pelo autor em 2026-08-14:
 *
 * <ol>
 *   <li><b>A que distância da estrada.</b> Encostada: pelo menos um
 *       bloco da borda do lote ortogonalmente vizinho a um bloco de
 *       caminho. Um número maior que um abriria quintal entre a casa e a
 *       rua, e vila de planície não tem quintal;
 *   <li><b>Quanto de estrada por vez.</b> Nenhuma, por ora — ver acima.
 *       Quando existir, um trecho por casa: rua que cresce sozinha vira
 *       rua sem nada em volta;
 *   <li><b>Até que desnível vale aplainar.</b> {@link #MAX_SLOPE} blocos
 *       de diferença dentro do lote. Acima disso procura-se outro lugar,
 *       porque terraplanar mais que isso deixa cicatriz no terreno e a
 *       casa parece enterrada de um lado.
 * </ol>
 *
 * <hr>
 *
 * <p><b>Custo.</b> Mesma disciplina de {@code TreeScanner}: anéis a
 * partir do centro, teto de colunas por chamada e um cursor que retoma no
 * anel onde parou. Sem isso, uma varredura de raio 64 olharia dezesseis
 * mil colunas dentro de um tick — e travar o servidor com varredura é
 * erro que este projeto já cometeu duas vezes (§11).
 */
public final class BuildSiteScanner {

    /** Quantas colunas uma chamada pode olhar. Ver {@code TreeScanner}. */
    private static final int MAX_COLUMNS = 1024;

    /**
     * Quanto acima do nível da colônia ainda se procura chão.
     *
     * <p>Apertado, e igual ao {@link #MAX_SLOPE} por coerência: um lote
     * mais alto que isso é morro, não continuação da vila. Se em jogo
     * ficar apertado demais, é uma constante — e o sintoma será claro,
     * "não achou lote" com terreno visivelmente bom em volta.
     *
     * <p>Também é o que mantém a busca fora do céu. A arena do gametest
     * é fechada por barreira oito blocos acima do chão, e uma janela
     * larga para cima encontrava o teto em vez do terreno.
     */
    private static final int WINDOW_UP = BuildSiteScanner.MAX_SLOPE;

    /**
     * Quanto abaixo.
     *
     * <p>Mais folgado que para cima por dois motivos: o centro da
     * colônia vem das camas, que ficam no piso das casas, um ou dois
     * blocos acima da rua; e uma vila em encosta se estende morro
     * abaixo, não morro acima.
     */
    private static final int WINDOW_DOWN = 8;

    /**
     * Quanto desnível o lote pode ter, em blocos.
     *
     * <p>Dois é o que um jogador aplaina sem pensar. Três já é degrau, e
     * uma casa assentada num degrau fica com o piso enterrado de um lado
     * e no ar do outro.
     */
    public static final int MAX_SLOPE = 2;

    /**
     * Onde cada colônia parou de procurar.
     *
     * <p>Um lote não aparece de um ciclo para o outro, e recomeçar do
     * centro a cada ciclo custaria o teto inteiro de colunas para
     * reencontrar as mesmas casas.
     */
    private static final Map<BlockPos, Integer> NEXT_RING = new HashMap<>();

    private BuildSiteScanner() {
    }

    /**
     * Um lote para uma casa deste tamanho, encostado em estrada.
     *
     * @param size quanto a casa ocupa, do {@code Blueprint}
     * @return o canto de onde a casa sobe — o menor x, y e z dela —, ou
     *     vazio quando não há lote agora. Vazio não é erro: significa
     *     "não achei nesta passagem", e a passagem seguinte continua de
     *     onde esta parou
     */
    public static Optional<ColonyPos> find(
            ServerWorld world, ColonyPos center, int radius, ColonyPos size) {

        BlockPos from = MinecraftTypeAdapter.toBlockPos(center);

        int columns = 0;

        int startRing = NEXT_RING.getOrDefault(from, 0);

        if (startRing > radius) {
            startRing = 0;
        }

        for (int ring = startRing; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {

                    // Só a casca do anel; o miolo já foi visto.
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        dz = ring - 1;

                        continue;
                    }

                    if (++columns > MAX_COLUMNS) {
                        NEXT_RING.put(from.toImmutable(), ring);

                        return Optional.empty();
                    }

                    Optional<ColonyPos> site = siteBesideRoadAt(
                            world, from.getX() + dx, from.getZ() + dz, from.getY(), size);

                    if (site.isPresent()) {
                        NEXT_RING.remove(from);

                        return site;
                    }
                }
            }
        }

        // Varreu tudo sem achar. Recomeçar do centro: a vila muda, o
        // jogador abre espaço, e o lote de ontem pode existir amanhã.
        NEXT_RING.remove(from);

        return Optional.empty();
    }

    /** Esquece os cursores. Chamado ao descarregar o mundo. */
    public static void clearAll() {
        NEXT_RING.clear();
    }

    /**
     * Se esta coluna é estrada, o lote livre ao lado dela.
     *
     * <p>Testa as quatro direções na ordem do enum, e a primeira que
     * servir vence. Não há critério melhor no MVP: as quatro são
     * igualmente boas, e escolher por sorteio faria a mesma vila crescer
     * diferente a cada sessão, o que é ruim de depurar.
     */
    private static Optional<ColonyPos> siteBesideRoadAt(
            ServerWorld world, int x, int z, int aroundY, ColonyPos size) {

        Optional<BlockPos> ground = groundInColumn(world, x, z, aroundY);

        if (ground.isEmpty() || !world.getBlockState(ground.get()).isOf(Blocks.DIRT_PATH)) {
            return Optional.empty();
        }

        for (Direction side : Direction.Type.HORIZONTAL) {
            // O lote começa no bloco seguinte à estrada — encostado nela,
            // que é a decisão 1.
            int lotX = x + side.getOffsetX();
            int lotZ = z + side.getOffsetZ();

            // A casa se estende para longe da estrada, e não por cima
            // dela: partindo da beira, o canto do lote recua meia casa
            // nos eixos que não são o da direção.
            int originX = side.getOffsetX() < 0 ? lotX - size.x() + 1 : lotX;
            int originZ = side.getOffsetZ() < 0 ? lotZ - size.z() + 1 : lotZ;

            Optional<Integer> floor = flatGroundAt(world, originX, originZ, aroundY, size);

            if (floor.isPresent()) {
                return Optional.of(new ColonyPos(originX, floor.get(), originZ));
            }
        }

        return Optional.empty();
    }

    /**
     * O bloco de chão no alto desta coluna, dentro da janela da vila.
     *
     * <p><b>Não usa o mapa de alturas</b>, e a razão foi medida: a arena
     * do gametest é fechada por barreiras, e {@code MOTION_BLOCKING}
     * devolve o teto de barreira — oito blocos acima da grama. Num mundo
     * de verdade o mapa daria a superfície e estaria certo; num mundo
     * fechado dá o teto, e o código que confiasse nele procuraria lote
     * dentro da laje.
     *
     * <p>A janela também é uma decisão, e é a quarta desta fase: o lote
     * tem de estar entre {@link #WINDOW_UP} acima e {@link #WINDOW_DOWN}
     * abaixo do nível do centro da colônia. Vila não constrói no alto do
     * morro que a olha de cima, e a janela é o que torna a busca barata
     * — uma coluna custa poucas leituras, e não uma varredura do céu ao
     * bedrock.
     */
    private static Optional<BlockPos> groundInColumn(
            ServerWorld world, int x, int z, int aroundY) {

        WorldChunk chunk = world.getChunkManager().getWorldChunk(x >> 4, z >> 4);

        if (chunk == null) {
            // Chunk descarregado. Pedir por ele aqui forçaria
            // carregamento dentro do tick — o defeito que travou o
            // servidor duas vezes neste projeto (§11).
            return Optional.empty();
        }

        // Qualquer coisa acima da janela reprova a coluna inteira, e não
        // é detalhe: sem esta pergunta a janela *recorta* o morro. Uma
        // torre de quatro blocos era lida como dois — a altura do teto
        // da janela — e um lote com desnível de quatro passava pelo
        // limite de dois. A casa nasceria enfiada na encosta. Achado
        // pelo gametest do desnível, em 2026-08-14.
        //
        // Efeito colateral assumido: lote com árvore em cima é recusado,
        // porque o tronco está acima da janela. Conservador de
        // propósito — a colônia procura outro lugar em vez de derrubar
        // o que não planejou.
        if (!chunk.getBlockState(new BlockPos(x, aroundY + WINDOW_UP + 1, z)).isAir()) {
            return Optional.empty();
        }

        for (int y = aroundY + WINDOW_UP; y >= aroundY - WINDOW_DOWN; y--) {
            BlockPos pos = new BlockPos(x, y, z);

            if (!chunk.getBlockState(pos).isAir()) {
                return Optional.of(pos);
            }
        }

        return Optional.empty();
    }

    /**
     * A altura em que a casa assenta, se este lote servir.
     *
     * <p>Serve quando todas as colunas dele são chão natural, o desnível
     * cabe em {@link #MAX_SLOPE}, e nada ali é peça de vila ou coisa que
     * o jogador pôs — a Regra 3 vale para escolher lugar tanto quanto
     * para quebrar bloco. Construir por cima da casa de alguém seria a
     * pior forma de desobedecê-la.
     *
     * @return a altura do chão mais baixo do lote. A casa assenta no
     *     mais baixo para que nenhuma parte dela nasça enterrada; o que
     *     ficar acima é degrau que a preparação resolve
     */
    private static Optional<Integer> flatGroundAt(
            ServerWorld world, int originX, int originZ, int aroundY, ColonyPos size) {

        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;

        for (int dx = 0; dx < size.x(); dx++) {
            for (int dz = 0; dz < size.z(); dz++) {
                int x = originX + dx;
                int z = originZ + dz;

                Optional<BlockPos> found = groundInColumn(world, x, z, aroundY);

                if (found.isEmpty()) {
                    return Optional.empty();
                }

                BlockPos ground = found.get();

                if (!isNaturalGround(world.getBlockState(ground))) {
                    return Optional.empty();
                }

                if (BlockProtection.isVillageOriginal(world, ground)) {
                    return Optional.empty();
                }

                lowest = Math.min(lowest, ground.getY());
                highest = Math.max(highest, ground.getY());

                if (highest - lowest > MAX_SLOPE) {
                    return Optional.empty();
                }
            }
        }

        // O piso da casa vai sobre o chão, e não dentro dele.
        return Optional.of(lowest + 1);
    }

    /**
     * Se dá para assentar uma casa sobre este bloco.
     *
     * <p>Chão de vila, e não qualquer bloco sólido: pedra à mostra,
     * madeira e lã são, respectivamente, montanha, casa e casa de
     * alguém. O caminho de terra fica de fora de propósito — a casa
     * encosta na rua, não sobe em cima dela.
     */
    private static boolean isNaturalGround(BlockState state) {
        return state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.DIRT)
                || state.isOf(Blocks.COARSE_DIRT)
                || state.isOf(Blocks.PODZOL)
                || state.isIn(BlockTags.SAND);
    }
}
