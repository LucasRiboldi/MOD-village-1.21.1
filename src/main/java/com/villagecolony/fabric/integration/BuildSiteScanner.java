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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

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
     *
     * <p><b>A chave é a colônia, e não o centro dela.</b> Era o centro
     * até 2026-08-19, e isso apagava o cursor toda vez que a âncora
     * trocava — o que ela faz a cada trinta segundos, pela ADR-003. Com
     * um cursor novo por ciclo a varredura recomeçava do anel zero para
     * sempre e nunca passava do orçamento de uma passagem: dos dezesseis
     * mil colunas do raio de 64, as mesmas mil, de novo e de novo.
     *
     * <p>Ficou invisível enquanto havia lote perto do centro, porque a
     * busca achava antes de o orçamento acabar. A casa de planície da
     * Regra 24 é 7×7×7 contra os 5×5×4 da cabana, e com o lote de perto
     * acabado a colônia passou a varrer até o fim do orçamento — e a
     * travar ali.
     */
    private static final Map<UUID, Integer> NEXT_RING = new HashMap<>();

    private BuildSiteScanner() {
    }

    /**
     * Um lote escolhido, e para que lado ele se abre.
     *
     * <p>A direção existe por causa da Regra 17. Ela sempre foi
     * conhecida — {@code siteBesideRoadAt} escolhe um dos quatro lados
     * ao procurar — e era jogada fora depois de calcular o canto. A casa
     * saía com a porta apontando para onde a planta tinha sido escrita,
     * que em vila de verdade é o mato.
     *
     * @param origin o canto de onde a casa sobe — o menor x, y e z dela
     * @param doorSide para que lado fica a rua, visto de dentro do lote.
     *     É por esse lado que a casa se abre
     * @param size qual das plantas oferecidas coube aqui. Decisão do
     *     autor em 2026-08-20: a colônia levanta a maior que couber
     *     neste lote, e desce um degrau só onde a maior não cabe
     */
    public record Site(ColonyPos origin, Direction doorSide, ColonyPos size) {
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
    public static Optional<Site> find(
            ServerWorld world, UUID colonyId, ColonyPos center, int radius, ColonyPos size) {

        return find(world, colonyId, center, radius, List.of(size));
    }

    /**
     * Um lote para a maior destas plantas que couber nele.
     *
     * <p>Decidido pelo autor em 2026-08-20, e o motivo está num log: a
     * vila dele varreu o raio de 64 inteiro sem achar lugar para a casa
     * de planície — 49 colunas no nível exato da rua, fora das peças da
     * vila gerada e com sete blocos livres acima — enquanto três cabanas
     * de 25 colunas já estavam de pé ali. Exigir a planta grande era
     * parar de crescer.
     *
     * <p><b>A escolha é por lote, e não por vila.</b> Cada coluna
     * candidata é testada da maior planta para a menor, e a primeira que
     * servir vence. Assim a casa grande continua subindo onde há espaço
     * para ela, em vez de a vila inteira rebaixar o padrão porque um
     * canto é apertado.
     *
     * <p>Uma varredura só, e o mesmo teto de colunas: as plantas
     * dividem a passagem em vez de cada uma pedir a sua. Coluna que não
     * é estrada é recusada antes de olhar planta nenhuma, que é a
     * esmagadora maioria.
     *
     * @param plans da maior para a menor. A ordem é de quem chama, e é
     *     ela que define o que "maior" quer dizer
     */
    public static Optional<Site> find(
            ServerWorld world, UUID colonyId, ColonyPos center, int radius,
            List<ColonyPos> plans) {

        BlockPos from = MinecraftTypeAdapter.toBlockPos(center);

        int columns = 0;

        int startRing = NEXT_RING.getOrDefault(colonyId, 0);

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
                        NEXT_RING.put(colonyId, ring);

                        return Optional.empty();
                    }

                    Optional<Site> site = siteBesideRoadAt(
                            world, from.getX() + dx, from.getZ() + dz, from.getY(), plans);

                    if (site.isPresent()) {
                        NEXT_RING.remove(colonyId);

                        return site;
                    }
                }
            }
        }

        // Varreu tudo sem achar. Recomeçar do centro: a vila muda, o
        // jogador abre espaço, e o lote de ontem pode existir amanhã.
        NEXT_RING.remove(colonyId);

        return Optional.empty();
    }

    /**
     * Para que lado deste lote fica a rua — a Regra 17, do lado de fora.
     *
     * <p>Existe para a obra que volta do save. O lado escolhido não é
     * gravado em disco de propósito: ele é uma leitura do mundo, e o
     * mundo é a única fonte que continua certa depois de o jogador mexer
     * nele. Perguntar de novo custa quatro leituras de coluna e evita um
     * campo novo no save que poderia discordar do terreno.
     *
     * <p>Procura o caminho encostado em cada uma das quatro faces, na
     * altura em que o lote assenta. Vazio quando não há rua nenhuma em
     * volta — o jogador arrancou o caminho, e aí a casa fica com a porta
     * onde ela já estava.
     *
     * @param floor a altura do piso da casa. A rua fica um abaixo dele,
     *     porque é sobre ela que se anda
     */
    public static Optional<Direction> roadSideOf(
            ServerWorld world, ColonyPos origin, ColonyPos size) {

        int roadY = origin.y() - 1;

        for (Direction side : Direction.Type.HORIZONTAL) {
            for (int step = 0; step < Math.max(size.x(), size.z()); step++) {
                int x = side.getOffsetX() != 0
                        ? (side.getOffsetX() < 0 ? origin.x() - 1 : origin.x() + size.x())
                        : origin.x() + step;

                int z = side.getOffsetZ() != 0
                        ? (side.getOffsetZ() < 0 ? origin.z() - 1 : origin.z() + size.z())
                        : origin.z() + step;

                if (step >= (side.getOffsetX() != 0 ? size.z() : size.x())) {
                    break;
                }

                if (VillageRoad.isPaving(world, world.getBlockState(new BlockPos(x, roadY, z)))) {
                    return Optional.of(side);
                }
            }
        }

        return Optional.empty();
    }

    /** Esquece os cursores. Chamado ao descarregar o mundo. */
    public static void clearAll() {
        NEXT_RING.clear();
    }

    /**
     * Em que anel a busca desta colônia parou por falta de orçamento.
     *
     * <p>Existe para separar duas respostas que {@link #find} devolve
     * iguais: "varri o raio inteiro e não há lote" e "o orçamento deste
     * ciclo acabou no meio". Vazio quer dizer a primeira.
     *
     * <p>Não é estado novo — é o cursor que já existia, lido de fora. Ele
     * só fica gravado quando o teto de colunas estoura, e sai quando a
     * varredura completa o raio ou acha o lote.
     *
     * <p>Escrito depois da sessão de 2026-08-15, 00:28, em que a Fase 10
     * afirmou "no free lot beside a road within 64 blocks" quatorze vezes
     * sem nunca ter varrido raio nenhum inteiro: dezesseis mil colunas,
     * mil por ciclo, e a sessão não durou os dezessete ciclos que a conta
     * pede. Ver o E14 do §17.
     */
    public static OptionalInt sweepPausedAt(UUID colonyId) {
        Integer ring = NEXT_RING.get(colonyId);

        return ring == null ? OptionalInt.empty() : OptionalInt.of(ring);
    }

    /**
     * Se esta coluna é estrada, o lote livre ao lado dela.
     *
     * <p>Testa as quatro direções na ordem do enum, e a primeira que
     * servir vence. Não há critério melhor no MVP: as quatro são
     * igualmente boas, e escolher por sorteio faria a mesma vila crescer
     * diferente a cada sessão, o que é ruim de depurar.
     */
    private static Optional<Site> siteBesideRoadAt(
            ServerWorld world, int x, int z, int aroundY, List<ColonyPos> plans) {

        Optional<BlockPos> ground = groundInColumn(world, x, z, aroundY);

        // Rua é o que o jogo calça, e não um nome escrito aqui —
        // 2026-08-21. A vila de deserto calça com arenito liso, e
        // enquanto esta linha dizia `dirt_path` ela nunca teve beira de
        // rua: nascia, contratava, contava recurso e nunca achava lote.
        if (ground.isEmpty() || !VillageRoad.isPaving(world, world.getBlockState(ground.get()))) {
            return Optional.empty();
        }

        // A altura da rua, que a Regra 19 usa como régua do lote.
        int roadY = ground.get().getY();

        for (ColonyPos size : plans) {
            Optional<Site> site = siteFor(world, x, z, aroundY, roadY, size);

            if (site.isPresent()) {
                return site;
            }
        }

        return Optional.empty();
    }

    /** O lote desta planta ao lado desta rua, se houver. */
    private static Optional<Site> siteFor(
            ServerWorld world, int x, int z, int aroundY, int roadY, ColonyPos size) {

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

            Optional<Integer> floor =
                    flatGroundAt(world, originX, originZ, aroundY, roadY, size);

            if (floor.isPresent()) {
                // A rua fica do lado oposto àquele para onde o lote
                // cresceu: `side` aponta da rua para o lote, e a porta
                // olha de volta para ela.
                return Optional.of(new Site(
                        new ColonyPos(originX, floor.get(), originZ),
                        side.getOpposite(),
                        size));
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
        if (!isNothing(chunk.getBlockState(new BlockPos(x, aroundY + WINDOW_UP + 1, z)))) {
            return Optional.empty();
        }

        for (int y = aroundY + WINDOW_UP; y >= aroundY - WINDOW_DOWN; y--) {
            BlockPos pos = new BlockPos(x, y, z);

            if (!isNothing(chunk.getBlockState(pos))) {
                return Optional.of(pos);
            }
        }

        return Optional.empty();
    }

    /**
     * Se este bloco não conta como obstáculo para achar o chão.
     *
     * <p>Ar, e a cobertura do campo: grama, samambaia, flor, camada de
     * neve. A TASK-047, e o motivo dela está numa sessão inteira.
     *
     * <p>Em 2026-08-15, 00:42, duas colônias varreram o raio de 64 blocos
     * até o fim, duas vezes cada, e não acharam um lote — em duas vilas
     * de planície rodeadas de campo aberto. A causa: este laço devolvia o
     * bloco mais alto que não fosse ar, e em planície esse bloco é o tufo
     * de grama. {@code flatGroundAt} então recusava a coluna, porque tufo
     * não é chão. Um lote de sete por sete precisa das quarenta e nove
     * colunas limpas, e em planície nenhuma está.
     *
     * <p>Construction-System.md §PREPARING sempre mandou limpar grama,
     * flor e neve. O código pulava esse estado alegando que o lote só é
     * aceito quando não há nada em cima dele — e a alegação era verdadeira
     * e era exatamente o defeito.
     *
     * <p><b>Folha fica de fora, e é decisão.</b> O documento a lista, mas
     * aceitar folha como nada faria a colônia escolher lote debaixo de
     * copa — e a casa nasceria dentro da árvore. O guarda da janela pega
     * o tronco, não a copa baixa. Conservador de propósito, como a recusa
     * de lote com árvore em cima logo acima.
     *
     * <p>Quem limpa é o próprio construtor, sem código novo: ele escreve
     * o bloco no lugar, e o que estava ali sai. O que sobra é a moita
     * dentro de cômodo cujo projeto pede ar — o projeto não escreve nada
     * ali, e a grama fica. É cosmético e está registrado no §13.
     */
    private static boolean isNothing(BlockState state) {
        return state.isAir()
                || state.isIn(BlockTags.REPLACEABLE)
                || state.isIn(BlockTags.SMALL_FLOWERS);
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
            ServerWorld world, int originX, int originZ, int aroundY,
            int roadY, ColonyPos size) {

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

                // A Regra 19: no nível da rua, e não apenas plano entre
                // si. Um lote inteiro dois blocos acima do caminho é
                // plano e é uma varanda sem escada — a porta da Regra 17
                // daria para o alto de um degrau que ninguém sobe.
                if (ground.getY() != roadY) {
                    return Optional.empty();
                }

                // E a Regra 22: a casa não sobe onde já há coisa. Não
                // basta o chão estar bom; a coluna inteira, até o teto
                // da planta, precisa estar livre.
                if (!isClearAbove(world, x, z, roadY + 1, size.y())) {
                    return Optional.empty();
                }
            }
        }

        // O piso da casa vai sobre o chão, e não dentro dele. Como o
        // chão está no nível da rua, o piso fica na altura em que se
        // anda sobre ela.
        return Optional.of(roadY + 1);
    }

    /**
     * A coluna está livre da altura do piso até o teto da planta?
     *
     * <p>A Regra 22, de 2026-08-19. Até aqui o lote era julgado pelo
     * <b>chão</b>: a coluna respondia onde a casa assenta, e um único
     * bloco acima da janela reprovava. Isso deixava passar o que
     * estivesse dentro da caixa da casa e acima daquela janela — árvore
     * caída, cerca, poste, a quina de outra construção. A casa nascia
     * com aquilo dentro dela, e o construtor pulava os blocos ocupados
     * com {@code is in the way}.
     *
     * <p>Agora a pergunta é sobre o volume: cada coluna do lote, do piso
     * ao último nível da planta.
     *
     * <p><b>Planta não ocupa.</b> Grama alta, flor, samambaia e camada
     * de neve não reprovam o lote — quem constrói tira. É o outro lado
     * da mesma decisão do autor: recusar um lote de planície por causa
     * de um pé de margarida seria recusar a planície inteira.
     */
    private static boolean isClearAbove(
            ServerWorld world, int x, int z, int floor, int height) {

        WorldChunk chunk = world.getChunkManager().getWorldChunk(x >> 4, z >> 4);

        if (chunk == null) {
            // Chunk descarregado: não dá para afirmar que está livre, e
            // afirmar sem saber é como a casa nasce dentro da árvore.
            return false;
        }

        for (int y = floor; y < floor + height; y++) {
            BlockPos at = new BlockPos(x, y, z);

            if (!isNothing(chunk.getBlockState(at))) {
                return false;
            }

            // E de quem é este vazio. A cabana da colônia é oca e não tem
            // piso: o miolo dela é grama original no nível da rua, com o
            // volume livre até o teto, e passa em todas as perguntas que
            // se fazem ao mundo — nenhuma delas pergunta de quem aquilo é.
            //
            // Visto em jogo em 2026-08-20, 01:54: a vila ofereceu como
            // lote o interior de uma cabana levantada na véspera. Quem
            // recusava era o planejador, depois da busca, e isso não
            // bastava: achar um lote apaga o cursor, então a passagem
            // seguinte recomeçava do centro e reencontrava o mesmo miolo.
            // A recusa precisa acontecer aqui dentro, onde a varredura
            // pode seguir para o anel seguinte.
            if (BlockProtection.isColonyBuilt(at)) {
                return false;
            }
        }

        return true;
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
