package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A rua cresce com a vila — a Regra 15, 2026-08-21.
 *
 * <pre>
 * quando não houver mais lote livre encostado em rua, o construtor
 * estende a rua — e o lote novo nasce na beira do trecho novo
 * </pre>
 *
 * <p>Era a pendência declarada no cabeçalho do {@link BuildSiteScanner}
 * desde 08-14: "a vila cresce enquanto houver beira de rua livre, e para
 * quando não houver". É esse <b>para</b> que esta regra tira.
 *
 * <p><b>A ponta sai da varredura que já acontece.</b> Procurar a ponta da
 * rua numa varredura própria custaria o raio de 64 inteiro — dezessete
 * passagens, oito minutos e meio de relógio — logo depois da varredura de
 * lote que acabou de falhar percorrendo exatamente as mesmas colunas.
 * Então quem acha a ponta é a própria busca de lote: ela já visita cada
 * coluna e já pergunta se aquilo é rua. O que se acrescenta é uma
 * pergunta a mais nas poucas colunas que <b>são</b>, e a resposta fica
 * guardada para quando a varredura terminar sem lote.
 *
 * <p><b>Da ponta mais distante do centro</b>, que é a frase da regra:
 * estrada que cresce pelo meio racha a vila.
 *
 * <p><b>Calçar não custa material</b> — decisão registrada no Backlog. O
 * que a colônia gasta é a passagem, e o teto de {@link #STRETCH} blocos é
 * o "trecho curto por casa" da regra: rua que cresce sozinha vira rua sem
 * nada em volta.
 */
public final class RoadExtension {

    /**
     * Quantos blocos por vez.
     *
     * <p>Curto de propósito. A rua só cresce quando a vila não tem mais
     * onde construir, e cinco blocos abrem beira para uma casa — que é a
     * medida da regra: um trecho por casa.
     */
    public static final int STRETCH = 5;

    /**
     * Quanto a rua pode subir ou descer por bloco.
     *
     * <p>Um. Rua que sobe mais que isso por passo não é rua, é escada — e
     * o aldeão que a percorre fica preso no degrau.
     */
    private static final int MAX_STEP = 1;

    /**
     * Quantas pontas candidatas se guarda por colônia.
     *
     * <p><b>Era uma só, e por isso a vila parava — 2026-08-25.</b> A
     * varredura guardava a ponta mais distante e nenhuma outra. Quando
     * essa ponta específica não se deixava calçar — vila gerada à
     * frente, casa da colônia, água, um degrau alto —, {@code pave}
     * devolvia zero, a colônia não tinha alternativa, e a varredura
     * seguinte reescolhia a <b>mesma</b> ponta, porque o critério é
     * determinístico e o mundo não mudou. Oito minutos de varredura para
     * bater no mesmo bloco, para sempre.
     *
     * <p>É a mesma forma do defeito que custou três sessões à mina, e a
     * saída é a mesma: mais de um candidato. Doze pelo mesmo motivo que
     * a boca da mina tem doze — é alternativa bastante para um terreno
     * ruim sem virar uma lista que cresce com o tamanho da vila.
     */
    public static final int CANDIDATES = 12;

    /**
     * As pontas que esta varredura viu, da mais distante para a mais
     * perto.
     */
    private static final Map<UUID, List<End>> ENDS = new HashMap<>();

    /**
     * As pontas que não se deixaram calçar, e desde quando.
     *
     * <p><b>A recusa envelhece</b> — é a Regra 23, e o mesmo molde de
     * {@code TreeMarks}. Sem envelhecer, uma ponta impossível sairia da
     * lista para sempre e a vila perderia candidatos a cada terreno
     * ruim; sem recusa nenhuma, as doze mesmas pontas seriam tentadas
     * toda varredura e a décima terceira nunca teria vez.
     *
     * <p>O jogador aplaina o barranco, tira a árvore, quebra a cerca — e
     * dez ciclos depois a ponta volta a valer.
     */
    private static final Map<BlockPos, Long> REFUSED = new HashMap<>();

    /** Por quantos ticks uma ponta recusada fica de fora. Dez ciclos. */
    private static final int REFUSED_MEMORY = 10 * VillageDetector.CYCLE_TICKS;

    /**
     * Quantas recusas se guarda antes de esquecer tudo.
     *
     * <p>Teto, e não regra: uma vila cercada de construção encheria o
     * mapa sem limite. Esquecer tudo custa uma tentativa perdida por
     * ponta, e é melhor que crescer para sempre.
     */
    private static final int MAX_REFUSED = 1024;

    /** Uma ponta de rua, e para que lado ela continuaria. */
    private record End(BlockPos at, Direction towards, double fromCenter) {
    }

    /**
     * As colunas que a última extensão calçou, por colônia.
     *
     * <p>Existe para o E26: quando a rua cresce, a colônia <b>sabe</b>
     * onde nasceu beira nova. Sem guardar isso, o planejamento manda
     * varrer o raio de 64 inteiro para redescobrir um lugar que acabou de
     * criar — dezessete ciclos, oito minutos e meio, por uma informação
     * que estava na mão.
     *
     * <p>Vale para a passagem seguinte e só: quem lê, lê e apaga.
     */
    private static final Map<UUID, List<ColonyPos>> JUST_PAVED = new HashMap<>();

    /**
     * A ponta que já rendeu, e quanto ela já rendeu — 2026-08-26.
     *
     * <p><b>O gargalo que a sessão das 03:11 mediu.</b> A rua cresceu um
     * bloco — o {@code pave} para no primeiro que recusa — e um bloco de
     * beira nova não abre lote para uma casa de sete por sete. A colônia
     * então voltava a varrer o raio inteiro para, oito minutos e meio
     * depois, calçar mais um.
     *
     * <p>Agora a ponta que rendeu é retomada na passagem seguinte, sem
     * pagar outra varredura. Cinco blocos saem em cinco ciclos em vez de
     * cinco varreduras — dois minutos e meio em vez de quarenta.
     *
     * <p><b>Isto não afrouxa a Regra 15.</b> A rua só começa a crescer
     * quando uma varredura inteira termina sem lote, e é essa varredura
     * que autoriza a insistência. O que mudou é que a autorização vale
     * para o trecho, e não para um bloco.
     */
    private static final Map<UUID, Growth> GROWING = new HashMap<>();

    /** Uma ponta em crescimento, e quanto já saiu dela. */
    private record Growth(End end, int laid) {
    }

    /**
     * Quantos blocos uma ponta pode render antes de a vila reavaliar.
     *
     * <p>Dezesseis, e o número tem razão: a maior casa do catálogo é de
     * sete, e dezesseis dão beira para duas delas com folga. Passar disso
     * sem que nenhum lote tenha nascido quer dizer que o problema não é
     * falta de rua — e aí a varredura tem mais a dizer que a picareta.
     *
     * <p>É também o que impede a Regra 15 de virar outra coisa: rua que
     * cresce sozinha e sem limite vira rodovia com mato dos dois lados.
     */
    public static final int MAX_RUN = 16;

    /** O que aconteceu na tentativa de estender. */
    public enum Outcome {

        /** Varreu tudo e não há ponta de rua que dê para prolongar. */
        NO_END,

        /** Achou a ponta, e nem o primeiro bloco pôde ser calçado. */
        BLOCKED,

        /** Calçou. O lote novo nasce na beira deste trecho. */
        EXTENDED
    }

    private RoadExtension() {
    }

    /**
     * Esta coluna é rua — vale a pena olhar se ela é ponta?
     *
     * <p>Chamada pela busca de lote, uma vez por coluna de rua. Guarda as
     * {@value #CANDIDATES} mais distantes do centro e descarta o resto:
     * guardar todas seria uma lista que cresce com o tamanho da vila, e
     * guardar uma só era a vila parando na primeira ponta ruim.
     */
    static void consider(ServerWorld world, UUID colonyId, BlockPos road, BlockPos center) {
        if (isRefused(world, road)) {
            // Já se tentou calçar esta, e não deu. Ela volta a valer
            // sozinha em dez ciclos — ver REFUSED.
            return;
        }

        Optional<Direction> towards = openSideOf(world, road);

        if (towards.isEmpty()) {
            return;
        }

        List<End> found = ENDS.computeIfAbsent(colonyId, id -> new ArrayList<>());

        double distance = center.getSquaredDistance(road);

        if (found.size() == CANDIDATES && distance <= found.get(found.size() - 1).fromCenter()) {
            // Não entra nem no fim da lista: sai barato, sem ordenar.
            return;
        }

        found.add(new End(road, towards.get(), distance));

        // Da mais distante para a mais perto, que é a frase da regra: a
        // rua cresce pela ponta, e não pelo meio.
        found.sort(Comparator.comparingDouble(End::fromCenter).reversed());

        if (found.size() > CANDIDATES) {
            found.remove(found.size() - 1);
        }
    }

    /** Esquece as pontas desta colônia. A varredura recomeçou. */
    public static void forgetEnds(UUID colonyId) {
        ENDS.remove(colonyId);

        // A insistência é filha da varredura que terminou sem lote. Uma
        // varredura nova refaz a pergunta, e a resposta velha sai junto.
        GROWING.remove(colonyId);
    }

    /** Se esta ponta está de castigo, e ainda não envelheceu. */
    private static boolean isRefused(ServerWorld world, BlockPos road) {
        Long since = REFUSED.get(road);

        if (since == null) {
            return false;
        }

        if (world.getTime() - since < REFUSED_MEMORY) {
            return true;
        }

        REFUSED.remove(road);

        return false;
    }

    /** Anota que esta ponta não se deixou calçar agora. */
    private static void refuse(ServerWorld world, BlockPos road) {
        if (REFUSED.size() >= MAX_REFUSED) {
            REFUSED.clear();
        }

        REFUSED.put(road, world.getTime());
    }

    /** Esvazia os registros. Chamado ao parar o servidor. */
    public static void clearAll() {
        ENDS.clear();

        REFUSED.clear();

        JUST_PAVED.clear();

        GROWING.clear();
    }

    /** Se esta colônia tem ponta rendendo, para retomar sem varrer. */
    public static boolean isGrowing(UUID colonyId) {
        return GROWING.containsKey(colonyId);
    }

    /**
     * Continua calçando a ponta que já estava rendendo — 2026-08-26.
     *
     * <p>Sem varredura nenhuma: quem autorizou a rua a crescer foi a
     * varredura que terminou sem lote, e essa autorização vale para o
     * trecho inteiro. Ver {@link #GROWING}.
     *
     * @return {@code EXTENDED} quando saiu bloco novo; {@code NO_END}
     *     quando não havia ponta rendendo ou ela já rendeu o bastante;
     *     {@code BLOCKED} quando ela parou de render agora
     */
    public static Outcome keepGrowing(ServerWorld world, UUID colonyId, ResourceId paving) {
        Growth growth = GROWING.get(colonyId);

        if (growth == null) {
            return Outcome.NO_END;
        }

        if (growth.laid() >= MAX_RUN) {
            // Rendeu o bastante e nenhum lote nasceu: o problema não é
            // falta de rua, e a varredura tem mais a dizer.
            GROWING.remove(colonyId);

            VillageColonyMod.LOGGER.info(
                    "Colony {} stops growing the road at {} blocks — no lot came of it,"
                            + " and the sweep has more to say than the pickaxe",
                    colonyId,
                    growth.laid());

            return Outcome.NO_END;
        }

        Optional<Block> block = MinecraftTypeAdapter.toBlock(paving);

        if (block.isEmpty()) {
            return Outcome.NO_END;
        }

        List<ColonyPos> laidAt = pave(world, growth.end(), block.get());

        if (laidAt.isEmpty()) {
            // A ponta parou de render. Ela sai de castigo em dez ciclos,
            // e a varredura volta a mandar.
            refuse(world, growth.end().at());

            GROWING.remove(colonyId);

            return Outcome.BLOCKED;
        }

        JUST_PAVED.put(colonyId, laidAt);

        remember(colonyId, growth.end(), laidAt, growth.laid());

        VillageColonyMod.LOGGER.info(
                "Colony {} grew the road {} blocks further {} — {} of {} on this stretch",
                colonyId,
                laidAt.size(),
                growth.end().towards(),
                growth.laid() + laidAt.size(),
                MAX_RUN);

        return Outcome.EXTENDED;
    }

    /**
     * Guarda a ponta nova: a última pedra assentada, mesmo rumo.
     *
     * <p>A ponta anda com o trecho. Guardar a antiga faria a passagem
     * seguinte tentar calçar o que já é rua e concluir que ela recusou.
     */
    private static void remember(
            UUID colonyId, End from, List<ColonyPos> laidAt, int alreadyLaid) {

        ColonyPos tip = laidAt.get(laidAt.size() - 1);

        GROWING.put(
                colonyId,
                new Growth(
                        new End(
                                MinecraftTypeAdapter.toBlockPos(tip),
                                from.towards(),
                                from.fromCenter()),
                        alreadyLaid + laidAt.size()));
    }

    /**
     * O que a última extensão desta colônia calçou — e some ao ser lido.
     *
     * <p>Some porque é informação de uma passagem: beira nova só é nova
     * uma vez, e guardá-la faria o planejamento reexaminar as mesmas
     * colunas a cada ciclo em vez de varrer.
     */
    public static List<ColonyPos> justPaved(UUID colonyId) {
        List<ColonyPos> laid = JUST_PAVED.remove(colonyId);

        return laid == null ? List.of() : laid;
    }

    /**
     * Prolonga a rua desta colônia, tentando as pontas mais distantes.
     *
     * <p>Só faz sentido depois de uma varredura de lote que terminou sem
     * achar nada: é ela que enche o registro de pontas, e é o "não há
     * mais lote" dela que autoriza a rua a crescer.
     *
     * @param paving o bloco com que esta vila calça — ver
     *     {@link VillageRoad}
     */
    public static Outcome extend(ServerWorld world, UUID colonyId, ResourceId paving) {
        List<End> ends = ENDS.remove(colonyId);

        if (ends == null || ends.isEmpty()) {
            return Outcome.NO_END;
        }

        Optional<Block> block = MinecraftTypeAdapter.toBlock(paving);

        if (block.isEmpty()) {
            return Outcome.NO_END;
        }

        // Da mais distante para a mais perto, e a primeira que aceitar
        // calçamento vence. Tentar todas custa poucas leituras de bloco e
        // é o que impede a vila de parar por causa de uma ponta ruim.
        for (End end : ends) {
            List<ColonyPos> laidAt = pave(world, end, block.get());

            int laid = laidAt.size();

            if (laid == 0) {
                refuse(world, end.at());

                continue;
            }

            JUST_PAVED.put(colonyId, laidAt);

            remember(colonyId, end, laidAt, 0);

            VillageColonyMod.LOGGER.info(
                    "Colony {} extended the road {} blocks {} from {}",
                    colonyId,
                    laid,
                    end.towards(),
                    end.at().toShortString());

            return Outcome.EXTENDED;
        }

        // Todas recusaram. O fracasso tem voz e diz quantas foram — sem
        // isto, "a rua não cresceu" e "a rua nem foi tentada" são a mesma
        // linha, que é o que custou as sessões da mina.
        VillageColonyMod.LOGGER.info(
                "Colony {} found no road end it may pave — tried {} of them, and they sit out"
                        + " {} cycles before being tried again",
                colonyId,
                ends.size(),
                REFUSED_MEMORY / VillageDetector.CYCLE_TICKS);

        return Outcome.BLOCKED;
    }

    /**
     * Assenta o trecho, bloco a bloco, e para no primeiro que recusar.
     *
     * <p>Para de verdade, e não pula: rua com buraco no meio é rua que o
     * aldeão não atravessa, e a beira depois do buraco não serve de lote.
     *
     * @return as colunas que entraram, na ordem
     */
    private static List<ColonyPos> pave(ServerWorld world, End end, Block paving) {
        BlockPos previous = end.at();

        List<ColonyPos> laid = new ArrayList<>();

        for (int step = 0; step < STRETCH; step++) {
            BlockPos ahead = previous.offset(end.towards());

            Optional<BlockPos> ground = groundNear(world, ahead, previous.getY());

            if (ground.isEmpty()) {
                return laid;
            }

            BlockPos at = ground.get();

            BlockState state = world.getBlockState(at);

            if (VillageRoad.isPaving(world, state)) {
                // Já é rua: a ponta encostou noutro trecho. Segue por
                // cima dela sem gastar nada, que é o que dois calçamentos
                // que se encontram fazem.
                previous = at;

                continue;
            }

            // A Regra 3 nas duas pontas, e aqui ela morde: a vila gerada
            // é feita de bloco que passaria por chão.
            if (BlockProtection.isVillageOriginal(world, at)
                    || BlockProtection.isColonyBuilt(at)
                    || !BuildSiteScanner.isNaturalGround(state)
                    || !world.getBlockState(at.up()).isAir()) {

                return laid;
            }

            world.setBlockState(at, paving.getDefaultState());

            previous = at;

            laid.add(MinecraftTypeAdapter.toColonyPos(at));
        }

        return laid;
    }

    /**
     * O chão desta coluna, se ele estiver ao alcance de um degrau.
     *
     * <p>Um bloco acima ou um abaixo do anterior. Mais que isso e a rua
     * vira escada — e a regra do autor manda parar onde o desnível passa
     * do limite, não escalar.
     */
    private static Optional<BlockPos> groundNear(ServerWorld world, BlockPos column, int fromY) {
        for (int dy = MAX_STEP; dy >= -MAX_STEP; dy--) {
            BlockPos at = new BlockPos(column.getX(), fromY + dy, column.getZ());

            if (!world.isInBuildLimit(at)) {
                continue;
            }

            if (world.getBlockState(at).isAir()) {
                continue;
            }

            return Optional.of(at);
        }

        return Optional.empty();
    }

    /**
     * Para que lado esta rua acaba, se acabar.
     *
     * <p>Duas perguntas, e as duas precisam: <b>atrás</b> tem rua — senão
     * é um bloco solto, e prolongar calçamento perdido no mato não faz
     * vila —, e <b>à frente</b> não tem. Aí este é o fim daquele trecho.
     *
     * <p>Olha um acima e um abaixo junto com o nível: a rua de vila sobe e
     * desce, e exigir o mesmo y faria toda ladeira parecer uma ponta.
     */
    private static Optional<Direction> openSideOf(ServerWorld world, BlockPos road) {
        for (Direction side : Direction.Type.HORIZONTAL) {
            if (!isRoadNear(world, road.offset(side.getOpposite()), road.getY())) {
                continue;
            }

            if (isRoadNear(world, road.offset(side), road.getY())) {
                continue;
            }

            return Optional.of(side);
        }

        return Optional.empty();
    }

    private static boolean isRoadNear(ServerWorld world, BlockPos column, int aroundY) {
        for (int dy = MAX_STEP; dy >= -MAX_STEP; dy--) {
            BlockPos at = new BlockPos(column.getX(), aroundY + dy, column.getZ());

            if (VillageRoad.isPaving(world, world.getBlockState(at))) {
                return true;
            }
        }

        return false;
    }
}
