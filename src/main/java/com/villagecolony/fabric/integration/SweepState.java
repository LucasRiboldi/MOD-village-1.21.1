package com.villagecolony.fabric.integration;

import com.villagecolony.fabric.integration.BuildSiteScanner.Site;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ColonySweepCursor;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.coordination.ScanRefusalReason;
import com.villagecolony.core.coordination.ScanReport;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.PlanPlacement;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
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
 * O estado da varredura de lotes: o cursor que retoma de onde parou, a memória por colônia, a deriva do centro e os relatórios de recusa — separado de
 * {@link BuildSiteScanner} em 2026-09-24, quando ele passou de 2.000 linhas.
 * Os comentários vieram junto sem mudança.
 */
public final class SweepState {

    private SweepState() {
    }

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
    static final Map<UUID, Sweep> SWEEPS = new HashMap<>();

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
    static final int CENTER_DRIFT = 20;

    /**
     * Onde uma varredura parou: o anel, a coluna, e de que centro.
     *
     * @param ring o anel em que o orçamento acabou
     * @param column a posição, na casca desse anel, da primeira coluna
     *     que <b>não</b> chegou a ser olhada
     * @param from o centro a partir do qual esses anéis foram medidos —
     *     sem ele o cursor não sabe dizer se ainda fala do mesmo lugar
     */
    record Sweep(int ring, int column, ColonyPos from) {
    }

    record RoadScan(Optional<Site> site, int columns) {
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
     * nova entra por {@link RoadExtension#remember}, chamado de onde a Regra 15 calça.
     */
    static final Map<UUID, ColonyRoads> ROADS = new HashMap<>();

    /**
     * As colônias cujo índice deu a volta inteira sem achar lote — P1.5,
     * 2026-09-19.
     *
     * <p><b>O laço que isto abre.</b> O chamador devolve o resultado de
     * {@link RoadIndex#findAmongRoads} <b>incondicionalmente</b>: havendo índice, a
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
    static final Map<UUID, Integer> EXHAUSTED = new HashMap<>();

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
    static final int EMPTY_ROUNDS_BEFORE_DROP = 4;

    /**
     * O índice em construção, enquanto a varredura não terminou o raio.
     *
     * <p>Separado do {@link #ROADS} de propósito: índice pela metade é
     * pior que índice nenhum, porque mente sobre ter visto tudo.
     */
    static final Map<UUID, Set<Long>> BUILDING = new HashMap<>();

    /**
     * Onde a passagem anterior parou de perguntar ao índice — 2026-09-11.
     *
     * <p><b>O índice deixou de caber numa passagem, e por isso ele
     * precisa de cursor.</b> Ele cabia por imposição do {@link RoadIndex#fits},
     * que recusava índice maior que {@link BuildSiteScanner#MAX_COLUMNS} — e o preço
     * dessa recusa era a vila grande perder o atalho <i>justamente por
     * ter crescido</i>, voltando para as dezessete passagens do
     * quadrado. Ver o javadoc do {@code fits}.
     *
     * <p>É posição na lista, e não anel: a lista só cresce pelo fim —
     * {@link RoadExtension#remember} acrescenta —, então um cursor posicional
     * continua apontando para a mesma coluna entre uma passagem e outra.
     * Quando o índice é <b>substituído</b>, o cursor sai junto.
     */
    static final Map<UUID, Integer> ROAD_CURSOR = new HashMap<>();

    /** O diagnóstico imutável da última fatia de cada colônia. */
    static final Map<UUID, ScanReport> REPORTS = new HashMap<>();

    static final int MAX_REPORTS = 256;

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
     * Se o centro andou o bastante para os anéis deixarem de valer.
     *
     * <p>Compara ao quadrado para não tirar raiz: a conta é a mesma e o
     * resultado é inteiro.
     */
    static boolean drifted(ColonyPos from, ColonyPos now) {
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
     * {@link RoadExtension#remember} acrescenta a ele <b>qualquer</b> rua nova — a
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
    static void warnIfBeyondTheRadius(
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

    /** Último diagnóstico da fatia da colônia, sem alterar sua telemetria acumulada. */
    public static Optional<ScanReport> latestReport(UUID colonyId) {
        return Optional.ofNullable(REPORTS.get(colonyId));
    }

    static Map<ScanRefusalReason, Integer> refusalSnapshot(UUID colonyId) {
        Map<ScanRefusalReason, Integer> snapshot = new EnumMap<>(ScanRefusalReason.class);

        for (LotRefusals.Reason reason : LotRefusals.Reason.values()) {
            ScanRefusalReason category = refusalCategory(reason);
            snapshot.merge(category, LotRefusals.countOf(colonyId, reason), Integer::sum);
        }
        return snapshot;
    }

    static void recordReport(
            UUID colonyId, int columns, Map<ScanRefusalReason, Integer> before) {
        Map<ScanRefusalReason, Integer> after = refusalSnapshot(colonyId);
        Map<ScanRefusalReason, Integer> delta = new EnumMap<>(ScanRefusalReason.class);

        for (ScanRefusalReason reason : ScanRefusalReason.values()) {
            int count = after.getOrDefault(reason, 0) - before.getOrDefault(reason, 0);
            if (count > 0) {
                delta.put(reason, count);
            }
        }
        if (REPORTS.size() >= MAX_REPORTS && !REPORTS.containsKey(colonyId)) {
            REPORTS.clear();
        }
        REPORTS.put(
                colonyId,
                new ScanReport(
                        colonyId,
                        Math.min(columns, BuildSiteScanner.MAX_COLUMNS),
                        delta,
                        !stillLookingForALot(colonyId)));
    }

    static ScanRefusalReason refusalCategory(LotRefusals.Reason reason) {
        return switch (reason) {
            case BED -> ScanRefusalReason.BED;
            case ROAD -> ScanRefusalReason.ROAD;
            case PROTECTED, OCCUPIED -> ScanRefusalReason.LOT;
            case NO_GROUND, NOT_NATURAL_GROUND, OFF_ROAD_LEVEL -> ScanRefusalReason.TERRAIN;
        };
    }

    /**
     * Em que anel a busca desta colônia parou por falta de orçamento.
     *
     * <p>Existe para separar duas respostas que {@link BigHouseFoundation#find} devolve
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
}
