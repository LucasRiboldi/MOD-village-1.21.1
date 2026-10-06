package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.SweepState.RoadScan;
import com.villagecolony.fabric.integration.SweepState.Sweep;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.coordination.ScanRefusalReason;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    static {
        ServerMemory.register(BuildSiteScanner.class, BuildSiteScanner::clearAll);
    }

    /**
     * Quantas colunas uma chamada pode olhar. Ver {@code TreeScanner}.
     *
     * <p>Pública porque é ela que dá o número de passagens que um raio
     * custa, e há teste que afirma esse custo. Deixá-lo escrito à mão lá
     * faria o teste passar por acidente no dia em que este teto mudasse.
     */
    public static final int MAX_COLUMNS = 1024;

    /**
     * Quanto desnível o lote pode ter, em blocos.
     *
     * <p>Dois é o que um jogador aplaina sem pensar. Três já é degrau, e
     * uma casa assentada num degrau fica com o piso enterrado de um lado
     * e no ar do outro.
     */
    public static final int MAX_SLOPE = 2;

    /** Nenhum bloco existente pode ocupar esta janela acima do piso. */
    public static final int VERTICAL_CLEARANCE = 25;

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
     * A pegada que pode ocupar um lote apenas quando a rua estiver deste lado.
     *
     * <p>A orientação da porta faz parte da medida: uma planta retangular pode
     * trocar largura e profundidade ao virar para a rua. Separar os dois deixa
     * o scanner aprovar uma área diferente da que a obra vai ocupar.
     */
    public record Footprint(ColonyPos size, Direction doorSide) {

        public Footprint {
            if (!doorSide.getAxis().isHorizontal()) {
                throw new IllegalArgumentException("a lot footprint needs a horizontal door side");
            }
        }
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
     * candidata é testada na ordem das plantas recebidas, e a primeira que
     * servir vence (desde a ADR-036 a ordem não favorece tamanho).
     *
     * <p>Uma varredura só, e o mesmo teto de colunas: as plantas
     * dividem a passagem em vez de cada uma pedir a sua. Coluna que não
     * é estrada é recusada antes de olhar planta nenhuma, que é a
     * esmagadora maioria.
     *
     * @param plans na ordem de preferência de quem chama
     *     ({@code PlanOrdering.mixed})
     */
    public static Optional<Site> find(
            ServerWorld world, UUID colonyId, ColonyPos center, int radius,
            List<ColonyPos> plans) {

        return findForFootprints(world, colonyId, center, radius, footprintsForAnyRoadSide(plans));
    }

    /**
     * Procura um lote usando a pegada que a planta realmente terá ao olhar a
     * rua. Usado pelo planejador de construções; a sobrecarga por tamanho fica
     * para GameTests e chamadores sem uma planta orientada.
     */
    public static Optional<Site> findForFootprints(
            ServerWorld world, UUID colonyId, ColonyPos center, int radius,
            List<Footprint> footprints) {

        BlockPos from = MinecraftTypeAdapter.toBlockPos(center);

        int columns = 0;
        Map<ScanRefusalReason, Integer> before = SweepState.refusalSnapshot(colonyId);

        try {

        ColonyRoads roads = SweepState.ROADS.get(colonyId);

        if (roads != null && SweepState.drifted(roads.from(), center)) {
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

            SweepState.ROAD_CURSOR.remove(colonyId);
            SweepState.BUILDING.remove(colonyId);
            SweepState.SWEEPS.remove(colonyId);

            if (kept.isPresent()) {
                SweepState.ROADS.put(colonyId, kept.get());

                VillageColonyMod.LOGGER.info(
                        "Colony {} kept {} of {} road columns after the center moved"
                                + " — the index is measured from {} now",
                        colonyId,
                        kept.get().columns().size(),
                        roads.columns().size(),
                        center);
            } else {
                SweepState.ROADS.remove(colonyId);
            }

            roads = kept.orElse(null);
        }

        if (roads != null) {
            SweepLog.indexed(colonyId);

            RoadScan indexed = RoadIndex.findAmongRoads(
                    world, colonyId, from, roads, radius, footprints);
            columns = indexed.columns();
            Optional<Site> fromIndex = indexed.site();

            // <b>Índice esgotado sai agora</b> — P1.5, 2026-09-19. A marca
            // foi posta lá dentro e é consumida aqui, fora da chamada que
            // a varredura usa para criar índice novo — ver SweepState.EXHAUSTED.
            //
            // A varredura roda na passagem SEGUINTE, e não nesta: o
            // orçamento desta já foi gasto perguntando ao índice, e varrer
            // por cima dobraria o custo do tique que este caminho existe
            // para evitar.
            if (fromIndex.isPresent()) {
                // Achou: a lista serve, e a conta de voltas vazias morre.
                SweepState.EXHAUSTED.remove(colonyId);
            }

            if (fromIndex.isEmpty()
                    && SweepState.EXHAUSTED.getOrDefault(colonyId, 0) >= SweepState.EMPTY_ROUNDS_BEFORE_DROP) {

                SweepState.EXHAUSTED.remove(colonyId);
                SweepState.ROADS.remove(colonyId);

                VillageColonyMod.LOGGER.info(
                        "Colony {} dropped its road index — {} columns asked over {}"
                                + " rounds and no lot came of any. The next pass sweeps"
                                + " the ground again",
                        colonyId,
                        roads.columns().size(),
                        SweepState.EMPTY_ROUNDS_BEFORE_DROP);
            }

            return fromIndex;
        }

        Sweep paused = SweepState.SWEEPS.get(colonyId);

        if (paused != null && SweepState.drifted(paused.from(), center)) {
            // O centro andou demais para estes anéis ainda falarem do
            // mesmo lugar — ver SweepState.CENTER_DRIFT.
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
            SweepState.BUILDING.put(colonyId, new LinkedHashSet<>());
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

                    // Pelo teto de colunas ou pelo prazo de relógio — 2026-09-24;
                    // ver SweepDeadline. Os dois guardam o mesmo cursor. Com
                    // prazo armado só o relógio para a passagem — 2026-09-30.
                    if (++columns > SweepDeadline.columnCap(MAX_COLUMNS)
                            || SweepDeadline.expired(columns)) {
                        SweepState.SWEEPS.put(colonyId, new Sweep(ring, column, center));

                        // Menos um: esta coluna foi contada e não chegou
                        // a ser olhada.
                        SweepLog.pass(colonyId, columns - 1);

                        return Optional.empty();
                    }

                    Optional<Site> site = RoadsideSites.siteBesideRoadAt(
                            world, colonyId, from,
                            from.getX() + dx, from.getZ() + dz, from.getY(), footprints);

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
                        SweepState.SWEEPS.put(colonyId, new Sweep(ring, column + 1, center));

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
        SweepState.SWEEPS.remove(colonyId);

        SweepLog.pass(colonyId, columns);
        SweepLog.completed(colonyId);

        // E aqui, e só aqui, o índice fica pronto: o raio inteiro foi
        // visitado, então a lista de ruas é a lista de ruas.
        RoadIndex.indexWhatWasSeen(colonyId, center);

        return Optional.empty();
        } finally {
            SweepState.recordReport(colonyId, columns, before);
        }
    }

    static List<Footprint> footprintsForAnyRoadSide(List<ColonyPos> plans) {
        List<Footprint> footprints = new ArrayList<>();

        for (ColonyPos plan : plans) {
            for (Direction side : Direction.Type.HORIZONTAL) {
                footprints.add(new Footprint(plan, side));
            }
        }

        return List.copyOf(footprints);
    }

    /** Esquece os cursores. Chamado ao descarregar o mundo. */
    public static void clearAll() {
        SweepState.SWEEPS.clear();
        SweepState.ROADS.clear();
        SweepState.ROAD_CURSOR.clear();
        SweepState.BUILDING.clear();
        SweepState.EXHAUSTED.clear();
        SweepState.REPORTS.clear();
    }

    /** Esquece o estado transitório de uma colônia, para testes isolados. */
    public static void clear(UUID colonyId) {
        SweepState.SWEEPS.remove(colonyId);
        SweepState.ROADS.remove(colonyId);
        SweepState.ROAD_CURSOR.remove(colonyId);
        SweepState.BUILDING.remove(colonyId);
        SweepState.EXHAUSTED.remove(colonyId);
        SweepState.REPORTS.remove(colonyId);
    }

}
