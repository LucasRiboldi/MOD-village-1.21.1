package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ColonySweepCursor;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.HashSet;
import java.util.Set;
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
 * partir do centro, teto de colunas por chamada e um cursor que retoma na
 * coluna onde parou. Sem isso, uma varredura de raio 64 olharia dezesseis
 * mil colunas dentro de um tick — e travar o servidor com varredura é
 * erro que este projeto já cometeu duas vezes (§11).
 */
public final class BuildSiteScanner {

    /**
     * Quantas colunas uma chamada pode olhar. Ver {@code TreeScanner}.
     *
     * <p>Pública porque é ela que dá o número de passagens que um raio
     * custa, e há teste que afirma esse custo. Deixá-lo escrito à mão lá
     * faria o teste passar por acidente no dia em que este teto mudasse.
     */
    public static final int MAX_COLUMNS = 1024;

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
     * Quanto uma coluna do lote pode fugir do nível da rua.
     *
     * <p><b>Decisão do autor, 2026-09-15:</b> <i>"permitir somente 1 bloco
     * de desnivel da estrada"</i>.
     *
     * <p><b>A medição que autorizou a mudança.</b> A pesquisa de 09-11
     * ({@code docs/research/terraplanagem-da-vila.md} §8) registrou a regra
     * que o próprio autor impôs: <i>"Medir primeiro. Se a recusa por
     * desnível dominar, a inferência vira fato e a frente abre"</i>. Duas
     * sessões responderam, e o número é estável: <b>35,0%</b> e
     * <b>33,6%</b> das recusas de lote eram {@code OFF_ROAD_LEVEL},
     * a segunda maior causa atrás só da área de estrada.
     *
     * <p>A régua era <b>exata</b> — {@code ground.getY() != roadY} —, e num
     * terreno de planície ondulada isso reprova quase tudo. A Regra 19
     * mirava o lote <i>"dois blocos acima do caminho"</i>, a varanda sem
     * escada; um bloco é o degrau que um jogador sobe sem pensar, e o
     * Vanilla o trata assim em toda parte.
     *
     * <p><b>O que isto NÃO faz: mover terra.</b> A preparação do canteiro
     * tira planta e não aterra — ver {@code SitePreparation} —, então a
     * coluna um abaixo da rua fica com um vão de um bloco sob o piso, que
     * assenta em {@code roadY + 1}. O autor foi avisado e escolheu assim
     * para a vila voltar a crescer. Aterrar continua sendo a frente de
     * terraplanagem que a pesquisa desenhou (§6), e ela segue aberta.
     */
    private static final int ROAD_LEVEL_TOLERANCE = 1;

    /**
     * Quanto da base precisa estar no nível exato da rua, em por cento.
     *
     * <p><b>Decisão do autor, 2026-09-19:</b> <i>"aceitar uma base da
     * construção que tenha mais de 90% dos blocos no mesmo nível
     * (tentando corrigir o fato de criar uma zona usando a altura de um
     * bloco porém todo resto da base estar acima do nível do solo,
     * construção fica voando)"</i>.
     *
     * <p>A {@link #ROAD_LEVEL_TOLERANCE} é <b>por coluna</b> e nada
     * exigia que as colunas concordassem entre si. Esta é a régua do
     * conjunto.
     *
     * <p><b>Medida ENTRE AS COLUNAS, e não contra a rua</b> — segunda
     * decisão do autor no mesmo dia, e ela é o que faz a regra
     * funcionar. Medir contra a rua reprovaria o lote inteiro um bloco
     * acima dela, que é o caso que o próprio autor mandou <b>aceitar</b>
     * em 09-15 e que o {@code oneBlockOffTheRoadLevelIsStillALot}
     * protege.
     *
     * <p>E a casa passa a assentar no nível da <b>base</b>, não no da
     * rua. Sem essa metade a regra não consertaria nada: um lote todo um
     * acima tem 100% das colunas no mesmo nível, passaria, e a casa
     * continuaria assentando em {@code roadY + 1} — voando sobre o
     * próprio terreno.
     */
    private static final int LEVEL_BASE_PERCENT = 90;

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
     *
     * <p><b>E é a coluna, não o anel — 2026-08-25.</b> Até aqui só o
     * anel era guardado, e a passagem seguinte recomeçava do primeiro
     * bloco dele: a casca de um anel de raio 64 tem quinhentas e doze
     * colunas, e re-perguntar por elas gastava orçamento para chegar às
     * respostas que a passagem anterior já tinha. Custava duas passagens
     * a mais das dezessete que o raio pede — um minuto de vila parada
     * por varredura, para sempre.
     */
    private static final Map<UUID, Sweep> SWEEPS = new HashMap<>();

    /**
     * Quanto o centro pode andar sem a varredura recomeçar — 2026-08-26.
     *
     * <p>Os anéis são medidos a partir do centro, e o centro anda. Até
     * aqui o cursor sobrevivia a <b>qualquer</b> movimento — decisão de
     * 08-19, tomada quando a âncora trocava a cada trinta segundos e
     * zerar a busca a cada troca fazia ela nunca sair do lugar.
     *
     * <p>A ADR-003 Emenda 4 mudou a premissa: hoje o centro só anda pela
     * sonda, e raramente — foram três movimentos em treze minutos na
     * sessão de 2026-08-25. O preço de sobreviver a todos ficou visível:
     * depois de um movimento, a varredura retomada pula os anéis de
     * dentro do <b>novo</b> centro, que é onde o lote é mais provável.
     *
     * <p>Vinte blocos é a decisão do autor em 2026-08-26, e a frase dele:
     * <i>movimento pequeno não atrapalha; movimento grande justifica
     * recomeçar</i>. Os movimentos daquela sessão foram todos abaixo
     * disso, e teriam mantido o cursor.
     */
    private static final int CENTER_DRIFT = 20;

    /**
     * Onde uma varredura parou: o anel, a coluna, e de que centro.
     *
     * @param ring o anel em que o orçamento acabou
     * @param column a posição, na casca desse anel, da primeira coluna
     *     que <b>não</b> chegou a ser olhada
     * @param from o centro a partir do qual esses anéis foram medidos —
     *     sem ele o cursor não sabe dizer se ainda fala do mesmo lugar
     */
    private record Sweep(int ring, int column, ColonyPos from) {
    }

    /**
     * As colunas de rua que a última varredura <b>completa</b> achou.
     *
     * <p><b>A medição que pediu isto — 2026-08-27.</b> Lendo o save do
     * mundo do autor: das 16.641 colunas do quadrado de raio 64, só
     * <b>698</b> eram calçamento — 4,19%, e o mesmo em três centros
     * (3,35% a 4,63%). O teto é 1.024 por passagem, e 698 cabem numa só.
     * A varredura passa de dezessete ciclos para um.
     *
     * <p><b>Por que índice e não seguir o traçado.</b> Alastrar a partir
     * de uma semente foi medido e reprovado: aquelas 698 colunas são
     * <b>catorze pedaços soltos</b>, o maior com 421. Um alastramento
     * acharia 60% delas, e os 40% de fora podem ser onde está o único
     * lote livre — que é a família do E14, a colônia dizendo "não há
     * lote" com lote existindo.
     *
     * <p>O índice não corre esse risco: só nasce de uma varredura que
     * visitou o raio inteiro. Coluna que deixou de ser rua é reconferida
     * quando visitada — o {@code siteBesideRoadAt} já pergunta —, e rua
     * nova entra por {@link #remember}, chamado de onde a Regra 15 calça.
     */
    private static final Map<UUID, ColonyRoads> ROADS = new HashMap<>();

    /**
     * As colônias cujo índice deu a volta inteira sem achar lote — P1.5,
     * 2026-09-19.
     *
     * <p><b>O laço que isto abre.</b> O chamador devolve o resultado de
     * {@link #findAmongRoads} <b>incondicionalmente</b>: havendo índice, a
     * varredura não roda. E o índice só era descartado quando o centro se
     * mudava ou quando uma coluna era consumida — <b>nunca por ter
     * falhado</b>. A colônia reperguntava à mesma lista para sempre.
     * Medido em duas sessões seguidas no deserto:
     * {@code 32 planner runs, 0 passes over 0 columns, 32 answered by the
     * index, 0 complete rounds}, e zero obras.
     *
     * <p><b>Por que descartar é o certo, e está escrito no {@link #ROADS}:</b>
     * o índice <i>"só nasce de uma varredura que visitou o raio
     * inteiro"</i>. Ele <b>promete cobertura completa</b>. Dar a volta sem
     * um lote é essa promessa falhando, e a resposta é refazer a
     * varredura — não reperguntar à mesma lista.
     *
     * <p><b>Marca em vez de apagar, e a separação é o conserto de um furo
     * que a bateria pegou.</b> A primeira versão removia o índice dentro
     * do {@code findAmongRoads}, e dois testes caíram:
     * {@code theCompletedSweepLeavesTheRoadColumnsIndexed} e
     * {@code removingAPlayerRoadRemovesOnlyThatIndexedColumn}. Eles
     * estavam certos — a varredura completa <b>cria</b> o índice no fim da
     * mesma chamada, e apagar antes matava um índice recém-nascido que
     * ninguém tinha consultado. Marcando, quem decide é o caminho de
     * fora, depois de a varredura ter tido sua vez.
     *
     * <p><b>Só o esgotamento, e nunca o orçamento.</b> A saída pelo
     * {@code MAX_COLUMNS} guarda o cursor e volta: ali a lista não acabou,
     * só o tique. Confundir as duas faria a colônia varrer o raio inteiro
     * a cada passagem.
     */
    private static final Map<UUID, Integer> EXHAUSTED = new HashMap<>();

    /**
     * Quantas voltas seguidas sem lote antes de o índice cair.
     *
     * <p><b>Uma volta vazia é normal</b>, e a bateria defende isso: o
     * cenário impossível de {@code removingAPlayerRoadRemovesOnlyThatIndexedColumn}
     * consulta o índice e não acha nada, e o índice tem de ficar — a
     * vila muda, o jogador abre espaço, e o lote de ontem existe amanhã.
     *
     * <p><b>Trinta e duas voltas seguidas não é.</b> É o número que o
     * deserto mediu, e a essa altura a lista já provou não ter resposta.
     * Quatro dá à vila folga para mudar sozinha — duas passagens de ciclo
     * por minuto — e corta o laço em dois minutos em vez de nunca.
     */
    private static final int EMPTY_ROUNDS_BEFORE_DROP = 4;

    /**
     * O índice em construção, enquanto a varredura não terminou o raio.
     *
     * <p>Separado do {@link #ROADS} de propósito: índice pela metade é
     * pior que índice nenhum, porque mente sobre ter visto tudo.
     */
    private static final Map<UUID, Set<Long>> BUILDING = new HashMap<>();

    /**
     * Onde a passagem anterior parou de perguntar ao índice — 2026-09-11.
     *
     * <p><b>O índice deixou de caber numa passagem, e por isso ele
     * precisa de cursor.</b> Ele cabia por imposição do {@link #fits},
     * que recusava índice maior que {@link #MAX_COLUMNS} — e o preço
     * dessa recusa era a vila grande perder o atalho <i>justamente por
     * ter crescido</i>, voltando para as dezessete passagens do
     * quadrado. Ver o javadoc do {@code fits}.
     *
     * <p>É posição na lista, e não anel: a lista só cresce pelo fim —
     * {@link #remember} acrescenta —, então um cursor posicional
     * continua apontando para a mesma coluna entre uma passagem e outra.
     * Quando o índice é <b>substituído</b>, o cursor sai junto.
     */
    private static final Map<UUID, Integer> ROAD_CURSOR = new HashMap<>();

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

        ColonyRoads roads = ROADS.get(colonyId);

        if (roads != null && drifted(roads.from(), center)) {
            SweepLog.drifted(colonyId, roads.from(), center);

            // <b>O cursor cai, o índice fica</b> — 2026-09-17. O cursor é
            // anel relativo ao centro e vira lixo quando ele anda; o
            // índice guarda coluna absoluta do mundo, e rua não deixa de
            // ser rua porque a vila descobriu que era maior.
            //
            // O preço de descartar os dois está medido: no playtest de
            // 09-17 a colônia respondeu 16 de 32 consultas pelo índice —
            // as de antes do movimento — e depois varreu do zero sem
            // nunca mais fechar volta, em 22.116 colunas contra as 16.641
            // que uma volta pede. Ver ColonyRoads.rebasedTo.
            Optional<ColonyRoads> kept = roads.rebasedTo(center, radius);

            ROAD_CURSOR.remove(colonyId);
            BUILDING.remove(colonyId);
            SWEEPS.remove(colonyId);

            if (kept.isPresent()) {
                ROADS.put(colonyId, kept.get());

                VillageColonyMod.LOGGER.info(
                        "Colony {} kept {} of {} road columns after the center moved"
                                + " — the index is measured from {} now",
                        colonyId,
                        kept.get().columns().size(),
                        roads.columns().size(),
                        center);
            } else {
                ROADS.remove(colonyId);
            }

            roads = kept.orElse(null);
        }

        if (roads != null) {
            SweepLog.indexed(colonyId);

            Optional<Site> fromIndex =
                    findAmongRoads(world, colonyId, from, roads, radius, plans);

            // <b>Índice esgotado sai agora</b> — P1.5, 2026-09-19. A marca
            // foi posta lá dentro e é consumida aqui, fora da chamada que
            // a varredura usa para criar índice novo — ver EXHAUSTED.
            //
            // A varredura roda na passagem SEGUINTE, e não nesta: o
            // orçamento desta já foi gasto perguntando ao índice, e varrer
            // por cima dobraria o custo do tique que este caminho existe
            // para evitar.
            if (fromIndex.isPresent()) {
                // Achou: a lista serve, e a conta de voltas vazias morre.
                EXHAUSTED.remove(colonyId);
            }

            if (fromIndex.isEmpty()
                    && EXHAUSTED.getOrDefault(colonyId, 0) >= EMPTY_ROUNDS_BEFORE_DROP) {

                EXHAUSTED.remove(colonyId);
                ROADS.remove(colonyId);

                VillageColonyMod.LOGGER.info(
                        "Colony {} dropped its road index — {} columns asked over {}"
                                + " rounds and no lot came of any. The next pass sweeps"
                                + " the ground again",
                        colonyId,
                        roads.columns().size(),
                        EMPTY_ROUNDS_BEFORE_DROP);
            }

            return fromIndex;
        }

        Sweep paused = SWEEPS.get(colonyId);

        if (paused != null && drifted(paused.from(), center)) {
            // O centro andou demais para estes anéis ainda falarem do
            // mesmo lugar — ver CENTER_DRIFT.
            SweepLog.drifted(colonyId, paused.from(), center);

            paused = null;
        }

        int startRing = paused == null || paused.ring() > radius ? 0 : paused.ring();

        int startColumn = startRing == 0 ? 0 : paused.column();

        if (startRing == 0) {
            // A conta que separa "reiniciou" de "ninguém chamou" — ver
            // SweepLog, e a sessão de 19:11 que deixou a dúvida.
            SweepLog.restarted(colonyId);

            // Varredura nova: a ponta que a anterior anotou pode não
            // existir mais, e o mundo é a única fonte que continua certa.
            //
            // Só as pontas candidatas — 2026-09-10. Apagar aqui o trecho
            // em crescimento era o ziguezague da rua: este método roda
            // uma vez por ciclo, e matava a inércia de rumo antes de ela
            // poder ser usada. Ver RoadExtension.forgetEnds.
            RoadExtension.forgetEnds(colonyId);

            // E o índice recomeça junto: o que a lapa anterior juntou
            // falava de uma volta que não é esta.
            BUILDING.put(colonyId, new LinkedHashSet<>());
        }

        for (int ring = startRing; ring <= radius; ring++) {

            // Onde esta coluna fica na casca deste anel. Conta todas,
            // inclusive as que a passagem anterior já respondeu: é a
            // posição absoluta que se retoma, e não quantas foram vistas
            // agora.
            int inRing = 0;

            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {

                    // Só a casca do anel; o miolo já foi visto.
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        dz = ring - 1;

                        continue;
                    }

                    int column = inRing++;

                    if (ring == startRing && column < startColumn) {
                        // Já respondida na passagem anterior. Sair daqui
                        // não custa leitura de mundo e não gasta
                        // orçamento — é só recolocar o cursor.
                        continue;
                    }

                    if (++columns > MAX_COLUMNS) {
                        SWEEPS.put(colonyId, new Sweep(ring, column, center));

                        // Menos um: esta coluna foi contada e não chegou
                        // a ser olhada.
                        SweepLog.pass(colonyId, columns - 1);

                        return Optional.empty();
                    }

                    Optional<Site> site = siteBesideRoadAt(
                            world, colonyId, from,
                            from.getX() + dx, from.getZ() + dz, from.getY(), plans);

                    if (site.isPresent()) {
                        // O cursor FICA, e é a decisão 8 aplicada como
                        // ela foi escrita: o conserto é no jeito de
                        // procurar, não no volume — 2026-08-27.
                        //
                        // Apagá-lo aqui fazia a varredura seguinte
                        // recomeçar do centro, e o centro custa dezessete
                        // passagens: 16.641 colunas, mil por vez, trinta
                        // segundos por ciclo. Oito minutos e meio entre
                        // uma casa e a próxima ter chance de nascer, e
                        // três sessões pagaram isso — 23:25:22 recomeçou
                        // do zero logo depois da primeira casa, e as das
                        // 23:06 e 01:33 acabaram sem sair da primeira
                        // varredura.
                        //
                        // Retomar é o certo e não só o barato: os anéis
                        // de perto acabaram de responder não, e agora
                        // estão MAIS ocupados, porque a casa nova está
                        // neles. Uma coluna adiante para não reperguntar
                        // esta; quando o raio acaba o cursor sai sozinho
                        // e a próxima recomeça do centro, que é onde a
                        // vila muda e o lote de ontem pode existir.
                        SWEEPS.put(colonyId, new Sweep(ring, column + 1, center));

                        SweepLog.pass(colonyId, columns);

                        // Há lote: a rua não precisa crescer, e a ponta
                        // anotada até aqui sai — junto com o trecho em
                        // curso, que perdeu a razão de ser. A Regra 15 é
                        // o que fazer quando NÃO há.
                        RoadExtension.lotFound(colonyId);

                        return site;
                    }
                }
            }
        }

        // Varreu tudo sem achar. Recomeçar do centro: a vila muda, o
        // jogador abre espaço, e o lote de ontem pode existir amanhã.
        SWEEPS.remove(colonyId);

        SweepLog.pass(colonyId, columns);
        SweepLog.completed(colonyId);

        // E aqui, e só aqui, o índice fica pronto: o raio inteiro foi
        // visitado, então a lista de ruas é a lista de ruas.
        indexWhatWasSeen(colonyId, center);

        return Optional.empty();
    }

    /**
     * Promove a índice o que a volta completa juntou.
     *
     * <p><b>Índice maior que o orçamento também vira índice</b> —
     * 2026-09-11. Até esta data ele era recusado, e a recusa tirava o
     * atalho justamente da vila que cresceu: ver o javadoc do
     * {@link #fits}. Quem paga o custo de um índice grande agora é o
     * {@link #ROAD_CURSOR}, que pagina a volta como a varredura do
     * quadrado.
     */
    private static void indexWhatWasSeen(UUID colonyId, ColonyPos center) {
        Set<Long> seen = BUILDING.remove(colonyId);

        // Índice novo é outra lista: a posição guardada falava da
        // anterior, e mantê-la faria a volta começar no meio de uma
        // lista que ela nunca viu.
        ROAD_CURSOR.remove(colonyId);

        if (seen == null || !fits(seen)) {
            ROADS.remove(colonyId);

            return;
        }

        ROADS.put(colonyId, new ColonyRoads(colonyId, center, List.copyOf(seen)));
    }

    /**
     * Se estas colunas podem ser um índice.
     *
     * <p>Vazio não pode: ele não diz "esta vila não tem rua", diz "varri
     * o raio inteiro e não achei nenhuma", e uma colônia que acreditasse
     * nisso pararia de procurar lote para sempre.
     *
     * <p><b>Maior que o orçamento pode</b> — 2026-09-11, e este teto era
     * o defeito. A regra anterior recusava índice com mais de
     * {@link #MAX_COLUMNS} colunas, dizendo que <i>"perguntar por um que
     * não cabe custa o mesmo que varrer"</i>. Não custa: o índice tem as
     * colunas <b>calçadas</b>, e o quadrado tem todas as do raio — numa
     * vila de duas mil colunas de rua a diferença é de duas passagens
     * contra dezessete.
     *
     * <p>E o preço do teto era pago pela vila que dava certo: ela cresce,
     * a rua cresce com ela, o índice passa de mil e vinte e quatro
     * colunas, e na volta seguinte o atalho morre. A colônia volta para
     * as dezessete passagens — oito minutos e meio de jogo sem nenhuma
     * obra abrir — e nunca mais sai de lá, porque a vila não encolhe. Na
     * sessão de 2026-09-11 às 00:04, <b>dezessete das dezenove colônias
     * do mundo estavam sem índice</b>, e a que ciclou passou as quinze
     * passagens da sessão sem completar uma volta.
     *
     * <p>O que substitui o teto é o {@link #ROAD_CURSOR}: perguntar ao
     * índice passou a gastar orçamento e a parar na conta como a
     * varredura do quadrado. O custo por tique continua o mesmo; o que
     * muda é quantas passagens uma resposta custa.
     */
    private static boolean fits(Collection<Long> columns) {
        return !columns.isEmpty();
    }

    /**
     * O índice de cada colônia, para o disco — 2026-08-27.
     *
     * <p>Só os prontos: o {@link #BUILDING} fica de fora de propósito,
     * pela mesma razão que o separa aqui dentro. Índice pela metade
     * mente sobre ter visto o raio inteiro, e gravado ele mentiria
     * também na sessão seguinte, quando ninguém mais lembra que a
     * varredura tinha parado no meio.
     */
    public static List<ColonyRoads> saved() {
        return List.copyOf(ROADS.values());
    }

    /**
     * O índice de ruas desta colônia, se ela já o tem — E46, 2026-09-16.
     *
     * <p>Quem pergunta é o guarda de alcance da obra. Ele media do
     * centro, e o centro é o lugar errado: a rua cresce <b>pela ponta
     * mais distante</b> — ver {@code RoadExtension.consider}, que ordena
     * as candidatas da mais longe para a mais perto —, então uma vila que
     * se estende ao longo da estrada larga a própria obra.
     *
     * <p>Vazio tem dois significados aqui, e os dois levam o guarda a
     * <b>não</b> largar nada: colônia que ainda não varreu o raio, e
     * colônia cujo índice foi descartado por deriva do centro. Em nenhum
     * dos casos se sabe onde estão as ruas, e largar obra por ignorância
     * é o defeito que este conserto veio tirar.
     */
    public static Optional<ColonyRoads> roadsOf(UUID colonyId) {
        return colonyId == null ? Optional.empty() : Optional.ofNullable(ROADS.get(colonyId));
    }

    /**
     * Recoloca um índice que veio do disco — 2026-08-27.
     *
     * <p>É o que apaga os dezessete ciclos da primeira busca de lote de
     * cada sessão. Não é acreditado de olhos fechados: cada coluna é
     * reconferida no mundo quando visitada, e o centro que veio junto faz
     * o {@link #find} descartar o índice inteiro se a colônia tiver
     * andado demais desde que ele foi medido.
     *
     * <p>Índice que não passa no {@link #fits} é ignorado em silêncio, e
     * a colônia varre o quadrado como antes desta versão.
     */
    public static void restore(ColonyRoads roads) {
        if (!fits(roads.columns())) {
            return;
        }

        // Lista nova, cursor novo: o mundo abriu agora, e ninguém parou
        // no meio desta volta.
        ROAD_CURSOR.remove(roads.colonyId());

        ROADS.put(roads.colonyId(), roads);
    }

    /**
     * A varredura pela metade de cada colônia, para o disco —
     * 2026-08-27.
     *
     * <p>Vai junto o que ela já achou: sem isso a volta terminaria com
     * meia lista de ruas e viraria um índice que mente sobre ter visto o
     * raio inteiro. Ver {@link ColonySweepCursor}.
     *
     * <p><b>Cursor cuja memória não caberia num índice fica de fora.</b>
     * Ele nunca chegaria a virar índice — o {@link #fits} recusaria no
     * fim da volta — e gravá-lo custaria disco para adiar a mesma
     * recusa. Vila assim varre do centro, que é o que ela já fazia.
     */
    public static List<ColonySweepCursor> pausedSweeps() {
        List<ColonySweepCursor> saving = new ArrayList<>();

        SWEEPS.forEach((colonyId, paused) -> {
            Set<Long> seen = BUILDING.get(colonyId);

            if (seen == null || seen.size() > MAX_COLUMNS) {
                return;
            }

            saving.add(new ColonySweepCursor(
                    colonyId, paused.from(), paused.ring(), paused.column(),
                    List.copyOf(seen)));
        });

        return List.copyOf(saving);
    }

    /**
     * Recoloca uma varredura que veio do disco — 2026-08-27.
     *
     * <p>É o que faz catorze passagens de dezessete deixarem de ser
     * jogadas fora quando o mundo fecha. As duas metades voltam juntas: o
     * lugar onde ela parou e o que ela achou até ali.
     *
     * <p>O que ela achou <b>não</b> vira índice — meia volta não viu o
     * raio inteiro, e o {@link #ROADS} continua vazio até a volta
     * terminar de verdade.
     */
    public static void restore(ColonySweepCursor cursor) {
        SWEEPS.put(cursor.colonyId(), new Sweep(cursor.ring(), cursor.column(), cursor.from()));

        BUILDING.put(cursor.colonyId(), new LinkedHashSet<>(cursor.found()));
    }

    /**
     * A passagem que pergunta só às ruas.
     *
     * <p><b>E ela pode não terminar numa passagem</b> — 2026-09-11.
     * Terminava por imposição do {@link #fits}, que recusava índice
     * maior que o orçamento; o preço era a vila grande perder o atalho
     * por ter crescido, e voltar às dezessete passagens do quadrado.
     * Agora o índice tem o tamanho que a vila tem, e perguntar a ele
     * gasta orçamento e para na conta, como a varredura.
     *
     * <p><b>Parar no meio não autoriza a Regra 15.</b> É a mesma
     * disciplina do cursor do quadrado, e pela mesma razão: crescer a
     * rua é o que se faz quando <i>não há</i> lote, e quem parou no meio
     * não sabe disso. Quem responde ao planejador é o
     * {@link #stillLookingForALot}.
     */
    private static Optional<Site> findAmongRoads(
            ServerWorld world, UUID colonyId, BlockPos from, ColonyRoads roads,
            int radius, List<ColonyPos> plans) {

        List<Long> columns = roads.columns();

        int start = ROAD_CURSOR.getOrDefault(colonyId, 0);

        if (start >= columns.size()) {
            // O índice encolheu debaixo do cursor — coluna que deixou de
            // ser rua sai quando reconferida. Recomeça do princípio.
            start = 0;
        }

        if (start == 0) {
            // Volta nova, pontas novas: a mesma razão da varredura do
            // quadrado, e o mesmo lugar onde a Regra 15 as recolhe. O
            // trecho em crescimento fica — ver RoadExtension.forgetEnds.
            RoadExtension.forgetEnds(colonyId);
        }

        int looked = 0;

        for (int at = start; at < columns.size(); at++) {
            if (++looked > MAX_COLUMNS) {
                ROAD_CURSOR.put(colonyId, at);

                // Aqui houve passagem de verdade: o orçamento foi gasto
                // inteiro e a volta parou no meio.
                SweepLog.pass(colonyId, looked - 1);

                return Optional.empty();
            }

            long column = columns.get(at);

            Optional<Site> site = siteBesideRoadAt(
                    world, colonyId, from,
                    ColonyRoads.xOf(column), ColonyRoads.zOf(column), from.getY(), plans);

            if (site.isPresent()) {
                // Uma adiante, pelo mesmo motivo do cursor do quadrado:
                // esta acabou de responder, e a passagem seguinte tem
                // mais o que perguntar.
                ROAD_CURSOR.put(colonyId, at + 1);

                if (start > 0) {
                    countTheIndexPass(colonyId, looked);
                }

                RoadExtension.lotFound(colonyId);

                warnIfBeyondTheRadius(colonyId, site.get(), from, radius);

                return site;
            }
        }

        // Perguntou a todas: aqui, e só aqui, a Regra 15 está autorizada.
        ROAD_CURSOR.remove(colonyId);

        if (start > 0) {
            countTheIndexPass(colonyId, looked);
        }

        // <b>E a volta vazia é contada</b> — P1.5, 2026-09-19. Quem
        // decide descartar é o chamador; ver {@link #EXHAUSTED}.
        EXHAUSTED.merge(colonyId, 1, Integer::sum);

        return Optional.empty();
    }

    /**
     * A volta pelo índice que <b>custou passagem</b> entra na conta do
     * relatório — 2026-09-11, e a assimetria é de propósito.
     *
     * <p>O {@code SweepLog} existe para separar duas coisas que o
     * diagnóstico de sessão precisa distinguir: <i>a colônia respondeu
     * pelo índice, de graça</i> e <i>a colônia varreu</i>. Foi essa
     * distinção que achou este defeito — a linha <code>23 planner runs,
     * 0 passes, 23 answered by the index</code> de uma sessão contra
     * <code>15 passes over 15360 columns, 0 answered by the index</code>
     * da seguinte.
     *
     * <p>Uma volta pelo índice que <b>cabe numa chamada</b> continua
     * sendo de graça, e não conta passagem: é a vila saudável, e o
     * relatório dela tem de continuar dizendo <code>0 passes</code>.
     * Contar aqui apagaria a assinatura, que foi o achado do
     * {@code gauntlet-verifier} contra a primeira versão deste conserto.
     */
    private static void countTheIndexPass(UUID colonyId, int looked) {
        SweepLog.pass(colonyId, looked);
    }

    /**
     * Se esta colônia ainda não terminou de procurar lote — 2026-09-11.
     *
     * <p>Cobre os <b>dois</b> jeitos de procurar: o quadrado em anéis e
     * a volta pelo índice de ruas. O planejador pergunta isto antes de
     * mandar a rua crescer, porque a Regra 15 é o que se faz quando não
     * há lote — e quem parou no meio não sabe se há.
     */
    public static boolean stillLookingForALot(UUID colonyId) {
        return SWEEPS.containsKey(colonyId) || ROAD_CURSOR.containsKey(colonyId);
    }

    /**
     * Guarda uma coluna calçada agora — a rua que a Regra 15 acabou de
     * abrir, 2026-08-27.
     *
     * <p>Sem isto o índice envelheceria no pior momento: a vila cresce a
     * rua justamente quando não achou lote, e o lote novo nasce
     * <b>encostado no que ela acabou de calçar</b>. Um índice que não
     * soubesse da beira nova nunca mais acharia nada.
     */
    public static void remember(UUID colonyId, BlockPos road) {
        long column = ColonyRoads.column(road.getX(), road.getZ());

        Set<Long> building = BUILDING.get(colonyId);

        if (building != null) {
            building.add(column);
        }

        ColonyRoads roads = ROADS.get(colonyId);

        if (roads == null || roads.columns().contains(column)) {
            return;
        }

        List<Long> grown = new ArrayList<>(roads.columns());

        grown.add(column);

        ROADS.put(colonyId, new ColonyRoads(colonyId, roads.from(), grown));
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
            ServerWorld world, UUID colonyId, ColonyPos origin, ColonyPos size) {

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

                if (isRoadArea(world, colonyId, new BlockPos(x, roadY, z))) {
                    return Optional.of(side);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Se o centro andou o bastante para os anéis deixarem de valer.
     *
     * <p>Compara ao quadrado para não tirar raiz: a conta é a mesma e o
     * resultado é inteiro.
     */
    private static boolean drifted(ColonyPos from, ColonyPos now) {
        long dx = (long) from.x() - now.x();
        long dz = (long) from.z() - now.z();

        return dx * dx + dz * dz > (long) CENTER_DRIFT * CENTER_DRIFT;
    }

    /**
     * O lote que o índice serviu de fora do raio — E46, 2026-09-16.
     *
     * <p><b>A varredura em anéis tem teto; o índice de ruas não.</b> Ela
     * para em {@code ring <= radius}, e por isso nunca devolve coluna de
     * fora. O índice é uma lista, percorrida inteira, e
     * {@link #remember} acrescenta a ele <b>qualquer</b> rua nova — a
     * vila calça estrada para fora do raio e o índice a absorve. Daí em
     * diante o lote sai de onde a estrada chegou, e não de onde o centro
     * alcança.
     *
     * <p>Isso não é sintoma: é o E46. O playtest de 2026-09-16 21:41
     * respondeu <b>40 de 40</b> consultas pelo índice, com zero deriva de
     * centro, e as duas obras nasceram a 67 e 71 blocos em quadrado — de
     * um raio de 64. As duas foram largadas no ciclo seguinte.
     *
     * <p><b>Avisa, e não corrige.</b> Recusar a coluna aqui mudaria onde
     * a vila constrói, e essa é decisão do autor — ver o C1 do E46, que
     * pergunta qual das duas réguas fica. Enquanto ela não vem, o log
     * deixa de ser mudo.
     *
     * <p>Fala das duas contas porque é a divergência entre elas que
     * condena a obra: quadrado é como se acha, reta é como se mantém.
     */
    private static void warnIfBeyondTheRadius(
            UUID colonyId, Site site, BlockPos centre, int radius) {

        int dx = Math.abs(site.origin().x() - centre.getX());
        int dz = Math.abs(site.origin().z() - centre.getZ());

        int square = Math.max(dx, dz);

        if (square <= radius) {
            return;
        }

        VillageColonyMod.LOGGER.warn(
                "Colony {} — the road index served a lot at {} from outside the sweep:"
                        + " {} blocks square and {} straight from {}, and the radius is {}."
                        + " The sweep would never have offered it",
                colonyId,
                site.origin(),
                square,
                Math.round(Math.sqrt((long) dx * dx + (long) dz * dz)),
                MinecraftTypeAdapter.toColonyPos(centre),
                radius);
    }

    /** Esquece os cursores. Chamado ao descarregar o mundo. */
    public static void clearAll() {
        SWEEPS.clear();
        ROADS.clear();
        ROAD_CURSOR.clear();
        BUILDING.clear();
        EXHAUSTED.clear();
    }

    /** Reconcilia uma coluna depois de uma alteração efetiva do jogador. */
    public static void reconcileWorldChange(
            UUID colonyId, ServerWorld world, BlockPos changed, ColonyPos center) {
        if (colonyId == null) {
            return;
        }

        long column = ColonyRoads.column(changed.getX(), changed.getZ());
        Set<Long> building = BUILDING.get(colonyId);

        if (building != null) {
            building.remove(column);
        }

        ColonyRoads roads = ROADS.get(colonyId);

        if (roads != null && roads.columns().contains(column)) {
            List<Long> reconciled = new ArrayList<>(roads.columns());
            reconciled.remove(column);

            if (reconciled.isEmpty()) {
                ROADS.remove(colonyId);
            } else {
                ROADS.put(colonyId, new ColonyRoads(colonyId, roads.from(), reconciled));
            }
        }

        // A área indexada permanece válida, mas candidatos anteriores ao
        // cursor precisam ser reconsiderados contra o terreno recém-editado.
        ROAD_CURSOR.remove(colonyId);

        if (!world.isChunkLoaded(changed)) {
            return;
        }

    }

    /**
     * Quantas colunas de rua esta colônia tem indexadas, se tem índice.
     *
     * <p>Vazio quer dizer que nenhuma varredura completou o raio ainda, e
     * que a próxima passagem vai perguntar o quadrado inteiro.
     */
    public static OptionalInt roadIndexSize(UUID colonyId) {
        ColonyRoads roads = ROADS.get(colonyId);

        return roads == null ? OptionalInt.empty() : OptionalInt.of(roads.columns().size());
    }

    /**
     * Se a coluna e uma rua reservada desta colonia.
     *
     * <p>O material oficial evita que qualquer bloco seja promovido a rua;
     * o indice ou a protecao estrutural da vila original fornece o contexto
     * espacial. Uma troca manual de bloco jamais cria essa reserva sozinha.
     */
    public static boolean isRoadArea(ServerWorld world, UUID colonyId, BlockPos pos) {
        if (!VillageRoad.isPaving(world, world.getBlockState(pos))) {
            return false;
        }

        long column = ColonyRoads.column(pos.getX(), pos.getZ());
        ColonyRoads roads = ROADS.get(colonyId);

        if (roads != null && roads.columns().contains(column)) {
            return true;
        }

        Set<Long> building = BUILDING.get(colonyId);

        return (building != null && building.contains(column))
                || BlockProtection.isVillageOriginal(world, pos);
    }

    /**
     * Se esta coluna é reserva que <b>impede</b> lote — P1.0, 2026-09-17.
     *
     * <p><b>Por que não basta o {@link #isRoadArea}.</b> Aquela pergunta
     * é usada nos dois sentidos, e são opostos: em três lugares ela
     * responde <i>"isto é rua, a casa pode encostar aqui"</i>, e num
     * quarto responde <i>"isto é reserva, não construa"</i>. Enquanto as
     * duas foram a mesma pergunta, afrouxar uma afrouxava a outra — e
     * tirar a vila original de ambas faria a colônia deixar de reconhecer
     * as ruas Vanilla, que é justamente por onde ela cresce.
     *
     * <p><b>A decisão do autor, 2026-09-17:</b> a reserva vale para a rua
     * que a <b>colônia</b> calçou, e não para o calçamento que já estava
     * na vila. Numa vila gerada a rua é onde o terreno é plano, e reservar
     * todo ele era recusar o melhor chão que existe.
     *
     * <p><b>O número que decidiu.</b> Playtest de 2026-09-17, 42 minutos:
     * <b>960.672 colunas testadas e zero aprovadas</b>, com 54,8% das
     * recusas nesta reserva e 18,5% na Regra 3 — 73% do território
     * bloqueado por proteção. A linha
     * {@code lot columns: 0 survived every check} é o instrumento que
     * separou "não há chão" de "faltou tempo de varredura", e ela
     * respondeu a primeira.
     *
     * <p><b>A Regra 3 continua inteira, e isto não a toca.</b> Quem
     * protege a vila do jogador é a recusa {@code PROTECTED}, uma
     * pergunta adiante em {@code flatGroundAt}: bloco original que não é
     * calçamento segue intocável. O que muda é só que o <b>calçamento</b>
     * original deixa de contar como reserva espacial da colônia.
     */
    public static boolean isReservedAgainstLots(
            ServerWorld world, UUID colonyId, BlockPos pos) {

        if (!VillageRoad.isPaving(world, world.getBlockState(pos))) {
            return false;
        }

        long column = ColonyRoads.column(pos.getX(), pos.getZ());
        ColonyRoads roads = ROADS.get(colonyId);

        if (roads != null && roads.columns().contains(column)) {
            return true;
        }

        Set<Long> building = BUILDING.get(colonyId);

        return building != null && building.contains(column);
    }

    /**
     * Em que anel a busca desta colônia parou por falta de orçamento.
     *
     * <p>Existe para separar duas respostas que {@link #find} devolve
     * iguais: "varri o raio inteiro e não há lote" e "o orçamento deste
     * ciclo acabou no meio". Vazio quer dizer a primeira.
     *
     * <p>Não é estado novo — é o cursor que já existia, lido de fora. Ele
     * fica gravado quando o teto de colunas estoura <b>e também quando o
     * lote é achado</b>, e sai só quando a varredura completa o raio ou
     * quando o centro anda demais. Achar lote deixou de apagá-lo em
     * 2026-08-27: recomeçar do centro custava dezessete passagens.
     *
     * <p>Escrito depois da sessão de 2026-08-15, 00:28, em que a Fase 10
     * afirmou "no free lot beside a road within 64 blocks" quatorze vezes
     * sem nunca ter varrido raio nenhum inteiro: dezesseis mil colunas,
     * mil por ciclo, e a sessão não durou os dezessete ciclos que a conta
     * pede. Ver o E14 do §17.
     */
    public static OptionalInt sweepPausedAt(UUID colonyId) {
        Sweep paused = SWEEPS.get(colonyId);

        return paused == null ? OptionalInt.empty() : OptionalInt.of(paused.ring());
    }

    /**
     * Um lote encostado <b>nestas</b> colunas, sem varrer o raio — E26.
     *
     * <p>Existe por causa de uma conta que a sessão de 2026-08-25 tornou
     * concreta: uma resposta de lote custa dezessete ciclos, oito minutos
     * e meio. Quando a Regra 15 acaba de calçar um trecho, a colônia
     * <b>sabe</b> onde nasceu beira nova — e mandá-la redescobrir isso
     * varrendo o raio inteiro é pagar oito minutos por uma informação
     * que ela tem na mão.
     *
     * <p>Não substitui a varredura: ela continua sendo quem acha lote em
     * vila que ainda tem beira livre, e quem anota as pontas de rua. Isto
     * é o atalho do caso em que a colônia acabou de criar a beira.
     *
     * @param road as colunas recém-calçadas, na ordem em que saíram
     */
    public static Optional<Site> findBeside(
            ServerWorld world, UUID colonyId, ColonyPos center,
            List<ColonyPos> plans, List<ColonyPos> road) {

        BlockPos from = MinecraftTypeAdapter.toBlockPos(center);

        for (ColonyPos column : road) {
            Optional<Site> site = siteBesideRoadAt(
                    world, colonyId, from, column.x(), column.z(), from.getY(), plans);

            if (site.isPresent()) {
                // Achou: a varredura em curso perde o sentido, e o cursor
                // dela sai para a próxima começar limpa.
                SWEEPS.remove(colonyId);

                return site;
            }
        }

        return Optional.empty();
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
            ServerWorld world, UUID colonyId, BlockPos center,
            int x, int z, int aroundY, List<ColonyPos> plans) {

        Optional<BlockPos> ground = groundInColumn(world, x, z, aroundY);

        // Material oficial so vira rua quando pertence a ROAD_AREA.
        if (ground.isEmpty() || !isRoadArea(world, colonyId, ground.get())) {
            return Optional.empty();
        }

        // A Regra 15 pega carona aqui — 2026-08-21. Esta coluna é rua, e
        // esta varredura é a única que passa por todas elas. Perguntar
        // agora se ela é ponta custa algumas leituras nas poucas colunas
        // calçadas; perguntar depois custaria o raio inteiro de novo.
        RoadExtension.consider(world, colonyId, ground.get(), center);

        // Esta coluna é rua, e esta varredura é a única que passa por
        // todas elas — o índice se enche exatamente aqui, e de graça.
        Set<Long> building = BUILDING.get(colonyId);

        if (building != null) {
            building.add(ColonyRoads.column(x, z));
        }

        // A altura da rua, que a Regra 19 usa como régua do lote.
        int roadY = ground.get().getY();

        for (ColonyPos size : plans) {
            Optional<Site> site = siteFor(world, colonyId, x, z, aroundY, roadY, size);

            if (site.isPresent()) {
                return site;
            }
        }

        return Optional.empty();
    }

    /** O lote desta planta ao lado desta rua, se houver. */
    private static Optional<Site> siteFor(
            ServerWorld world, UUID colonyId, int x, int z, int aroundY, int roadY,
            ColonyPos size) {

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
                    flatGroundAt(world, colonyId, originX, originZ, aroundY, roadY, size);

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
                || state.isReplaceable()
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
            ServerWorld world, UUID colonyId, int originX, int originZ, int aroundY,
            int roadY, ColonyPos size) {

        // Quantas colunas em cada altura de chão — a conta da regra dos
        // 90%, decisão do autor de 2026-09-19. O nível é medido ENTRE AS
        // COLUNAS, e não contra a rua: ver o portão depois do laço.
        Map<Integer, Integer> groundLevels = new HashMap<>();

        for (int dx = 0; dx < size.x(); dx++) {
            for (int dz = 0; dz < size.z(); dz++) {
                int x = originX + dx;
                int z = originZ + dz;

                Optional<BlockPos> found = groundInColumn(world, x, z, aroundY);

                if (found.isEmpty()) {
                    LotRefusals.refused(colonyId, LotRefusals.Reason.NO_GROUND);

                    return Optional.empty();
                }

                BlockPos ground = found.get();

                // <b>Do mais barato para o mais caro</b> — 2026-09-15. A
                // ordem desta fila é decisão de custo, e não de regra:
                // toda pergunta aqui reprova o lote inteiro, então a
                // resposta final não depende de quem pergunta primeiro —
                // só o preço de chegar nela depende.
                //
                // <b>O que o log do autor mediu:</b> ciclos de 98, 57 e 54
                // ms, acima do tique de 50 ms, com o planejador levando 72
                // ms do pior; 192.448 recusas num ciclo, das quais 126.315
                // pela Regra 3. A Regra 3 era a SEGUNDA pergunta da fila e
                // é a mais cara de todas — {@code isVillageOriginal}
                // consulta o {@code StructureAccessor} —, enquanto a
                // comparação de dois inteiros da Regra 19, que respondeu
                // por 24.350 recusas, era a SEXTA. Toda coluna reprovada
                // pela régua da rua pagava a consulta de estrutura antes
                // de chegar à comparação que a reprovaria de graça.
                //
                // <b>A guarda barata do isVillageOriginal não salvava o
                // caso</b>, e é o que torna a troca valiosa: ela sai cedo
                // quando o bloco não tem referência de estrutura nenhuma,
                // e dentro de uma vila os blocos têm — que é justamente
                // onde a colônia procura lote.
                //
                // <b>O que muda no log</b>, sem nada mudar no jogo: uma
                // coluna reprovável por mais de um motivo passa a ser
                // contada pelo motivo mais barato. Espere a Regra 3 cair e
                // a régua da rua subir. Ver
                // {@code theCheapRefusalAnswersBeforeTheExpensiveOne}.

                // A Regra 19: no nível da rua, e não apenas plano entre
                // si. Um lote inteiro dois blocos acima do caminho é
                // plano e é uma varanda sem escada — a porta da Regra 17
                // daria para o alto de um degrau que ninguém sobe.
                //
                // Primeira da fila por ser a única que não lê o mundo: o
                // chão já está na mão, e a pergunta é a comparação de dois
                // inteiros.
                if (Math.abs(ground.getY() - roadY) > ROAD_LEVEL_TOLERANCE) {
                    // A contagem que decide a terraplanagem — 2026-09-11.
                    // Ver docs/research/terraplanagem-da-vila.md.
                    LotRefusals.refused(colonyId, LotRefusals.Reason.OFF_ROAD_LEVEL);

                    return Optional.empty();
                }

                // <b>E quantas colunas estão no nível EXATO da rua</b> —
                // decisão do autor, 2026-09-19. A tolerância acima aceita
                // um bloco de degrau por coluna, e a casa assenta em
                // {@code roadY + 1} venha o que vier: um lote em que
                // <i>toda</i> coluna está um abaixo é aceito e a casa sai
                // <b>voando</b>, com um vão de um bloco sob o piso
                // inteiro. Era limite conhecido — o javadoc do
                // {@link #ROAD_LEVEL_TOLERANCE} o descreve — e o autor o
                // viu em jogo.
                //
                // A conta fecha depois do laço: <i>"aceitar uma base que
                // tenha mais de 90% dos blocos no mesmo nível"</i>.
                groundLevels.merge(ground.getY(), 1, Integer::sum);

                // <b>E as três do meio ficam na ordem em que sempre
                // estiveram</b>, de propósito. Custam a mesma coisa — uma
                // consulta em memória, uma leitura de bloco, uma leitura
                // de bloco —, então trocá-las não compra desempenho e
                // <b>estraga o diagnóstico</b>: pôr o {@code isLotGround}
                // na frente fez o caminho de terra reservado como estrada
                // passar a ser recusado por "não é solo natural", porque
                // {@code dirt_path} não é cubo inteiro. A recusa continuava
                // certa e o log passava a mentir sobre o motivo. Foi o
                // {@code everyReservedRoadMaterialBlocksTheWholeFootprint}
                // que pegou, na primeira tentativa desta mudança.

                // Consulta em memória, ao registro de obras da colônia.
                if (BlockProtection.isColonyBuilt(ground)) {
                    VolumeSample.colonyBuilt();

                    LotRefusals.refused(colonyId, LotRefusals.Reason.OCCUPIED);

                    return Optional.empty();
                }

                // Uma leitura de bloco e uma consulta ao índice de ruas, e
                // a guarda do isPaving sai cedo no caso comum.
                //
                // <b>A reserva é da rua que a colônia calçou</b> — P1.0,
                // 2026-09-17. Era o isRoadArea, que conta o calçamento
                // original da vila como reserva; numa vila gerada isso é
                // 54,8% das recusas, e o playtest de 42 minutos fechou com
                // 960.672 colunas testadas e zero aprovadas. Ver
                // isReservedAgainstLots — a Regra 3 continua adiante, e
                // bloco original que não é calçamento segue intocável.
                if (isReservedAgainstLots(world, colonyId, ground)) {
                    LotRefusals.refused(colonyId, LotRefusals.Reason.ROAD);

                    return Optional.empty();
                }

                // Uma leitura do bloco que já está na mão.
                if (!isLotGround(world, ground)) {
                    LotRefusals.refused(colonyId, LotRefusals.Reason.NOT_NATURAL_GROUND);

                    return Optional.empty();
                }

                // A Regra 3, e a pergunta mais cara da fila: por último,
                // depois de as baratas terem tirado o que podiam.
                //
                // <b>Mas o chão do bioma não é peça de vila</b> — P1.3,
                // 2026-09-18, e o número que decidiu veio do jogo. Num
                // mundo só de deserto: 16.016 colunas, <b>zero
                // aprovadas</b>, 69% recusadas aqui — contra 19% na
                // planície. A amostra disse de que eram feitas:
                //
                //   5696 smooth_sandstone; 5403 sand; 1548 chest; 2 oak_log
                //
                // <b>87% é areia e arenito</b>, que no deserto é o terreno
                // em que a vila foi assentada — o gerador inclui o chão na
                // caixa da estrutura. Proibir construir sobre o chão do
                // bioma é proibir a colônia de trabalhar dentro da própria
                // vila, que é literalmente o que o javadoc de
                // {@code isVillageOriginal} diz que não deve acontecer.
                //
                // <b>A Regra 3 continua inteira para o que ela existe.</b>
                // Baú, tronco, porta, cama — peça posta pelo gerador —
                // seguem intocáveis; o que deixa de recusar é o solo
                // natural. É o mesmo movimento de 09-15 em
                // {@code isReservedAgainstLots}, que separou calçamento de
                // reserva e abriu 313 lotes na planície.
                if (BlockProtection.isVillageOriginal(world, ground)
                        && !isBiomeGround(world, ground)) {

                    LotRefusals.refused(colonyId, LotRefusals.Reason.PROTECTED);

                    return Optional.empty();
                }

                // E a Regra 22: a casa não sobe onde já há coisa. Não
                // basta o chão estar bom; a coluna inteira, até o teto
                // da planta, precisa estar livre.
                //
                // <b>Conta a partir do chão DESTA coluna</b> — 2026-09-15,
                // e foi a tolerância de um bloco que expôs o defeito. A
                // conta era sempre {@code roadY + 1}, o que estava certo
                // enquanto toda coluna tinha o chão exatamente em
                // {@code roadY}: aí as duas alturas eram a mesma. Com um
                // bloco de tolerância, a coluna um acima tem o próprio
                // chão em {@code roadY + 1} — e a pergunta lia esse chão
                // como coisa no caminho, reprovando por OCCUPIED o lote que
                // a régua acabara de aprovar.
                //
                // O teto continua sendo o da planta contado da rua, e é o
                // certo: a casa assenta em {@code roadY + 1} e sobe
                // {@code size.y()} dali. O que muda é só onde a conferência
                // começa.
                int floorOf = Math.max(roadY, ground.getY()) + 1;

                if (!isClearAbove(world, x, z, floorOf, roadY + 1 + size.y() - floorOf)) {
                    LotRefusals.refused(colonyId, LotRefusals.Reason.OCCUPIED);

                    return Optional.empty();
                }
            }
        }

        // <b>A base tem de estar quase toda no mesmo nível</b> —
        // decisão do autor, 2026-09-19: <i>"aceitar uma base da
        // construção que tenha mais de 90% dos blocos no mesmo
        // nível"</i>.
        //
        // <b>O que isto conserta, visto em jogo.</b> A tolerância de um
        // bloco é <b>por coluna</b>, e nada exigia que as colunas
        // concordassem entre si: um lote em que todas estão um abaixo da
        // rua passa inteiro, e a casa assenta em {@code roadY + 1} —
        // <b>voando</b>, com um vão de um bloco sob o piso todo. O
        // javadoc do ROAD_LEVEL_TOLERANCE já descrevia o vão como limite
        // conhecido; o autor o viu e mandou fechar.
        //
        // Noventa por cento, e não cem: o degrau isolado é o que a
        // tolerância existe para aceitar, e exigir o lote perfeito
        // devolveria os 35% de recusa por desnível que a régua exata
        // produzia.
        int columns = size.x() * size.z();

        int mostCommon = groundLevels.values().stream().mapToInt(Integer::intValue).max()
                .orElse(0);

        if (mostCommon * 100 < columns * LEVEL_BASE_PERCENT) {
            LotRefusals.refused(colonyId, LotRefusals.Reason.OFF_ROAD_LEVEL);

            return Optional.empty();
        }

        // <b>E a casa assenta no nível da BASE, e não no da rua</b> — é a
        // outra metade da decisão, e sem ela a regra dos 90% não
        // consertaria nada: um lote inteiro um acima da rua tem 100% das
        // colunas no mesmo nível e passaria, e a casa continuaria
        // assentando em {@code roadY + 1} — <b>voando</b> sobre o próprio
        // terreno.
        int baseY = groundLevels.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(roadY);

        // <b>E a caixa inteira não pode pisar em casa que já existe</b> —
        // 2026-09-19, visto em jogo: uma obra nova nasceu EM CIMA de uma
        // casa pronta, acavalando as duas.
        //
        // <b>Por que as perguntas de cima não pegaram.</b> A Regra 22
        // pergunta {@code isColonyBuilt(ground)} — <b>uma posição por
        // coluna</b>, a do chão encontrado — e a conferência de volume
        // começa <i>acima</i> dela. Um prédio cuja caixa cubra a coluna
        // em outra altura não é visto por nenhuma das duas: nem no ponto
        // do chão, nem na janela que começa depois dele.
        //
        // Aqui a pergunta é caixa contra caixa, que é a forma do que se
        // quer impedir. Uma vez por lote, e não por coluna — é o último
        // portão, e a essa altura só sobrou um candidato.
        ColonyPos floor = new ColonyPos(originX, baseY + 1, originZ);

        ColonyPos ceiling = new ColonyPos(
                originX + size.x() - 1,
                baseY + size.y(),
                originZ + size.z() - 1);

        if (VillageColonyMod.BUILDINGS.anythingBuiltInside(floor, ceiling)
                || anyOpenSiteInside(colonyId, floor, ceiling)) {

            LotRefusals.refused(colonyId, LotRefusals.Reason.OCCUPIED);

            return Optional.empty();
        }

        // <b>A pegada inteira passou</b> — 2026-09-17. É o par que
        // faltava ao LotRefusals: ele contava só o que some, e um
        // denominador sem numerador não diz se a vila está apertada ou
        // sem chão nenhum. Ver LotRefusals.accepted.
        LotRefusals.accepted(colonyId, columns);

        // O piso da casa vai sobre o chão, e não dentro dele — e o chão
        // é o da BASE, que é onde 90% das colunas estão. Era
        // {@code roadY + 1} até 2026-09-19, e era isso que fazia a casa
        // voar quando o lote inteiro estava acima da rua: o piso ficava
        // no nível do caminho e o terreno, mais alto, passava por baixo
        // dele.
        return Optional.of(baseY + 1);
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
    /**
     * Se alguma obra <b>em andamento</b> ocupa esta caixa — 2026-09-19.
     *
     * <p><b>O buraco que isto fecha, e ele era meu.</b> O portão de caixa
     * contra caixa de 15:49 consultava só o {@code BUILDINGS}, que é o
     * registro das obras <b>terminadas</b>. Uma obra em andamento não
     * está nele — ela só entra quando fecha —, então um lote novo podia
     * nascer em cima dela.
     *
     * <p>E era o caso exato desta vila: a obra do {@code cut_sandstone}
     * ficou parada em {@code WAITING_RESOURCES} por quase meia hora,
     * ocupando o terreno e <b>invisível</b> para o portão.
     *
     * <p>O {@code BlockProtection.isOpenSite} já percorre as obras
     * abertas, mas responde por <b>ponto</b>; aqui a pergunta é da caixa,
     * que é a forma do que se quer impedir.
     */
    /**
     * Se esta obra ocupa o mesmo espaço de algo já construído —
     * 2026-09-19.
     *
     * <p>Existe para a <b>retomada</b>: o portão de caixa governa quem
     * escolhe lote, e a obra gravada no save não passa por ele. Um lote
     * ruim escolhido por uma versão anterior sobrevivia a todo conserto
     * do scanner e voltava a cada carregamento.
     *
     * <p>Ignora a própria obra, e é obrigatório: ela está no registro de
     * obras abertas quando esta pergunta é feita, e sem isso toda obra
     * se acusaria de pisar em si mesma.
     */
    public static boolean overlapsSomethingBuilt(ConstructionProject project) {
        Building box = Building.of(project);

        if (VillageColonyMod.BUILDINGS.anythingBuiltInside(box.min(), box.max())) {
            return true;
        }

        for (ConstructionProject other : VillageColonyMod.CONSTRUCTIONS.all()) {
            if (other.id().equals(project.id()) || !other.state().isOpen()) {
                continue;
            }

            Building site = Building.of(other);

            if (box.min().x() <= site.max().x() && box.max().x() >= site.min().x()
                    && box.min().y() <= site.max().y() && box.max().y() >= site.min().y()
                    && box.min().z() <= site.max().z() && box.max().z() >= site.min().z()) {

                return true;
            }
        }

        return false;
    }

    private static boolean anyOpenSiteInside(UUID colonyId, ColonyPos min, ColonyPos max) {
        for (ConstructionProject project : VillageColonyMod.CONSTRUCTIONS.all()) {
            if (!project.state().isOpen()) {
                continue;
            }

            // <b>Só as obras DESTA colônia</b>, e isso a bateria cobrou:
            // percorrer todas fazia a arena de um gametest enxergar a
            // obra do vizinho, e o {@code ColonyDetectionGameTest}
            // passou a falhar de forma determinística — três rodadas em
            // três.
            //
            // E é o certo também em jogo: obra de outra colônia fica
            // longe por construção — duas vilas a menos de
            // {@code DUPLICATE_DISTANCE} viram uma só —, e a pergunta
            // que importa é sobre o próprio canteiro.
            if (!project.colonyId().equals(colonyId)) {
                continue;
            }

            Building site = Building.of(project);

            if (min.x() <= site.max().x() && max.x() >= site.min().x()
                    && min.y() <= site.max().y() && max.y() >= site.min().y()
                    && min.z() <= site.max().z() && max.z() >= site.min().z()) {

                return true;
            }
        }

        return false;
    }

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
                // <b>Qual bloco barrou</b> — P1.6, 2026-09-19. A Regra 22
                // é 70% das recusas em duas sessões seguidas e não dizia
                // de que era feita. Aqui, e só aqui, porque este é o
                // bloco que parou a conferência. Ver VolumeSample.
                VolumeSample.inTheColumn(world, at);

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
                // Miolo da propria cabana: e a colonia cheia, nao coisa
                // no caminho. Separar as duas e o P1.6. Ver VolumeSample.
                VolumeSample.colonyBuilt();

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
    static boolean isNaturalGround(BlockState state) {
        return state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.DIRT)
                || state.isOf(Blocks.COARSE_DIRT)
                || state.isOf(Blocks.PODZOL)
                || state.isIn(BlockTags.SAND);
    }

    /**
     * Se este bloco é o chão do bioma, e não peça que o gerador pôs —
     * P1.3, 2026-09-18.
     *
     * <p><b>O número que decidiu.</b> Playtest num mundo só de deserto:
     * 16.016 colunas, <b>zero aprovadas</b>, 69% recusadas pela Regra 3
     * contra 19% na planície. A amostra de
     * {@code ProtectionSample} disse de que eram feitas:
     *
     * <pre>
     * 5696 smooth_sandstone;  5403 sand;  1548 chest;  2 oak_log
     * </pre>
     *
     * <p><b>87% é areia e arenito</b> — o terreno em que a vila foi
     * assentada. O gerador de vilas inclui o chão na caixa da estrutura,
     * e no deserto esse chão é quase tudo que existe. Proibir construir
     * sobre ele é proibir a colônia de trabalhar dentro da própria vila,
     * que é justamente o que o javadoc de
     * {@code BlockProtection.isVillageOriginal} diz que não deve
     * acontecer.
     *
     * <p><b>Por que não bastava {@link #isNaturalGround}.</b> Ele já
     * aceita areia pela etiqueta {@code SAND}, e <b>não</b> aceita
     * {@code smooth_sandstone}, que é 45% da amostra: arenito liso é
     * pedra, e para quem procura chão de lote ele continua não sendo
     * solo. A pergunta aqui é outra — <i>isto é peça de construção ou é
     * o material de que este bioma é feito?</i> — e por isso o predicado
     * é próprio em vez de um alargamento daquele, que mudaria a resposta
     * de quem pergunta "dá para assentar casa aqui?".
     *
     * <p><b>Pela etiqueta do jogo onde existe etiqueta</b> — a ADR-009.
     * {@code BASE_STONE_OVERWORLD} traz pedra, granito, diorito e
     * andesito; {@code TERRACOTTA} cobre o barro cozido dos ermos;
     * {@code SAND} e {@code DIRT} vêm de {@link #isNaturalGround}.
     *
     * <p><b>O arenito é a exceção, e ela é do jogo e não deste mod:</b>
     * <b>não existe</b> etiqueta de arenito em 1.21.1 — conferido no
     * {@code BlockTags} do jar mapeado, que tem {@code SAND},
     * {@code TERRACOTTA} e {@code BASE_STONE_OVERWORLD} e nenhuma
     * família de {@code sandstone}. Os seis nomes ficam escritos porque
     * a alternativa seria pior: casar por substring {@code "sandstone"}
     * pegaria escada, laje e muro de arenito, que <b>são</b> peça de
     * construção — exatamente o que esta pergunta precisa continuar
     * recusando.
     *
     * <p><b>A Regra 3 continua inteira para o que ela existe:</b> baú,
     * tronco, porta, cama — peça posta pelo gerador — seguem intocáveis.
     */
    public static boolean isBiomeGround(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        return isNaturalGround(state)
                || state.isIn(BlockTags.BASE_STONE_OVERWORLD)
                || state.isIn(BlockTags.TERRACOTTA)
                || state.isOf(Blocks.SANDSTONE)
                || state.isOf(Blocks.SMOOTH_SANDSTONE)
                || state.isOf(Blocks.CUT_SANDSTONE)
                || state.isOf(Blocks.RED_SANDSTONE)
                || state.isOf(Blocks.SMOOTH_RED_SANDSTONE)
                || state.isOf(Blocks.CUT_RED_SANDSTONE)
                || state.isOf(Blocks.GRAVEL)
                || state.isOf(Blocks.CLAY)
                || state.isOf(Blocks.SNOW_BLOCK)
                || state.isOf(Blocks.PACKED_ICE);
    }

    /**
     * Se a coluna sólida pode sustentar um lote no P0.7.
     *
     * <p>A composição não identifica origem: terreno natural e preparo do
     * jogador são elegíveis. Proteção, construção existente, {@code ROAD_AREA},
     * nível e volume já foram verificados antes desta pergunta.
     */
    static boolean isLotGround(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).isSolidBlock(world, pos);
    }
}
