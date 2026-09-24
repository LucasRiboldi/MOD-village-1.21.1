package com.villagecolony.fabric.integration;

import com.villagecolony.fabric.integration.SweepState.RoadScan;
import com.villagecolony.fabric.integration.SweepState.Sweep;
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
 * O índice de ruas da colônia: as colunas de rua vistas, a busca de lote entre elas, a reserva contra lote e a reconciliação quando o jogador muda o mundo — separado de
 * {@link BuildSiteScanner} em 2026-09-24, quando ele passou de 2.000 linhas.
 * Os comentários vieram junto sem mudança.
 */
public final class RoadIndex {

    private RoadIndex() {
    }

    /**
     * Promove a índice o que a volta completa juntou.
     *
     * <p><b>Índice maior que o orçamento também vira índice</b> —
     * 2026-09-11. Até esta data ele era recusado, e a recusa tirava o
     * atalho justamente da vila que cresceu: ver o javadoc do
     * {@link #fits}. Quem paga o custo de um índice grande agora é o
     * {@link SweepState#ROAD_CURSOR}, que pagina a volta como a varredura do
     * quadrado.
     */
    static void indexWhatWasSeen(UUID colonyId, ColonyPos center) {
        Set<Long> seen = SweepState.BUILDING.remove(colonyId);

        // Índice novo é outra lista: a posição guardada falava da
        // anterior, e mantê-la faria a volta começar no meio de uma
        // lista que ela nunca viu.
        SweepState.ROAD_CURSOR.remove(colonyId);

        if (seen == null || !fits(seen)) {
            SweepState.ROADS.remove(colonyId);

            return;
        }

        SweepState.ROADS.put(colonyId, new ColonyRoads(colonyId, center, List.copyOf(seen)));
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
     * {@link BuildSiteScanner#MAX_COLUMNS} colunas, dizendo que <i>"perguntar por um que
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
     * <p>O que substitui o teto é o {@link SweepState#ROAD_CURSOR}: perguntar ao
     * índice passou a gastar orçamento e a parar na conta como a
     * varredura do quadrado. O custo por tique continua o mesmo; o que
     * muda é quantas passagens uma resposta custa.
     */
    static boolean fits(Collection<Long> columns) {
        return !columns.isEmpty();
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
        return colonyId == null ? Optional.empty() : Optional.ofNullable(SweepState.ROADS.get(colonyId));
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
     * {@link SweepState#stillLookingForALot}.
     */
    static RoadScan findAmongRoads(
            ServerWorld world, UUID colonyId, BlockPos from, ColonyRoads roads,
            int radius, List<ColonyPos> plans) {

        List<Long> columns = roads.columns();

        int start = SweepState.ROAD_CURSOR.getOrDefault(colonyId, 0);

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
            if (++looked > BuildSiteScanner.MAX_COLUMNS) {
                SweepState.ROAD_CURSOR.put(colonyId, at);

                // Aqui houve passagem de verdade: o orçamento foi gasto
                // inteiro e a volta parou no meio.
                SweepLog.pass(colonyId, looked - 1);

                return new RoadScan(Optional.empty(), looked - 1);
            }

            long column = columns.get(at);

            Optional<Site> site = RoadsideSites.siteBesideRoadAt(
                    world, colonyId, from,
                    ColonyRoads.xOf(column), ColonyRoads.zOf(column), from.getY(), plans);

            if (site.isPresent()) {
                // Uma adiante, pelo mesmo motivo do cursor do quadrado:
                // esta acabou de responder, e a passagem seguinte tem
                // mais o que perguntar.
                SweepState.ROAD_CURSOR.put(colonyId, at + 1);

                if (start > 0) {
                    countTheIndexPass(colonyId, looked);
                }

                RoadExtension.lotFound(colonyId);

                SweepState.warnIfBeyondTheRadius(colonyId, site.get(), from, radius);

                return new RoadScan(site, looked);
            }
        }

        // Perguntou a todas: aqui, e só aqui, a Regra 15 está autorizada.
        SweepState.ROAD_CURSOR.remove(colonyId);

        if (start > 0) {
            countTheIndexPass(colonyId, looked);
        }

        // <b>E a volta vazia é contada</b> — P1.5, 2026-09-19. Quem
        // decide descartar é o chamador; ver {@link #EXHAUSTED}.
        SweepState.EXHAUSTED.merge(colonyId, 1, Integer::sum);

        return new RoadScan(Optional.empty(), looked);
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
    static void countTheIndexPass(UUID colonyId, int looked) {
        SweepLog.pass(colonyId, looked);
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

        Set<Long> building = SweepState.BUILDING.get(colonyId);

        if (building != null) {
            building.add(column);
        }

        ColonyRoads roads = SweepState.ROADS.get(colonyId);

        if (roads == null || roads.columns().contains(column)) {
            return;
        }

        List<Long> grown = new ArrayList<>(roads.columns());

        grown.add(column);

        SweepState.ROADS.put(colonyId, new ColonyRoads(colonyId, roads.from(), grown));
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

    /** Reconcilia uma coluna depois de uma alteração efetiva do jogador. */
    public static void reconcileWorldChange(
            UUID colonyId, ServerWorld world, BlockPos changed, ColonyPos center) {
        if (colonyId == null) {
            return;
        }

        long column = ColonyRoads.column(changed.getX(), changed.getZ());
        Set<Long> building = SweepState.BUILDING.get(colonyId);

        if (building != null) {
            building.remove(column);
        }

        ColonyRoads roads = SweepState.ROADS.get(colonyId);

        if (roads != null && roads.columns().contains(column)) {
            List<Long> reconciled = new ArrayList<>(roads.columns());
            reconciled.remove(column);

            if (reconciled.isEmpty()) {
                SweepState.ROADS.remove(colonyId);
            } else {
                SweepState.ROADS.put(colonyId, new ColonyRoads(colonyId, roads.from(), reconciled));
            }
        }

        // A área indexada permanece válida, mas candidatos anteriores ao
        // cursor precisam ser reconsiderados contra o terreno recém-editado.
        SweepState.ROAD_CURSOR.remove(colonyId);

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
        ColonyRoads roads = SweepState.ROADS.get(colonyId);

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
        ColonyRoads roads = SweepState.ROADS.get(colonyId);

        if (roads != null && roads.columns().contains(column)) {
            return true;
        }

        Set<Long> building = SweepState.BUILDING.get(colonyId);

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
        ColonyRoads roads = SweepState.ROADS.get(colonyId);

        if (roads != null && roads.columns().contains(column)) {
            return true;
        }

        Set<Long> building = SweepState.BUILDING.get(colonyId);

        return building != null && building.contains(column);
    }
}
