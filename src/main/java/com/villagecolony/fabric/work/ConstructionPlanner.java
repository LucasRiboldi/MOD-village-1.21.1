package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionReach;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.VillageRoad;
import com.villagecolony.fabric.integration.SweepLog;
import org.jspecify.annotations.Nullable;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * A colônia decide construir — TASK-033.
 *
 * <p>Uma obra por colônia de cada vez, e só quando há quem a execute, e
 * só onde a Regra 6 deixa. Não põe bloco algum: quem põe é
 * {@link BuilderWork}.
 *
 * <p><b>A torneira por último.</b> Não se abre obra sem construtor na
 * vila. Uma obra sem executor ficaria aberta para sempre, e a meta de
 * tábua da Regra 5 passaria a apontar para uma casa que ninguém levanta
 * — o fabricante encheria os baús de tábua por causa de um canteiro
 * fantasma. É a lição do §11, e é a mesma ordem que a Fase 9 seguiu.
 *
 * <p><b>Quando a vila para de crescer.</b> Por regra, nunca: o autor
 * decidiu em 2026-08-14 que se constrói enquanto houver material e
 * espaço. O freio é o mundo — só há obra onde há lote livre encostado em
 * rua, e é {@code BuildSiteScanner} quem responde isso.
 *
 * <p><b>O que saiu daqui em 2026-08-20</b>, quando este arquivo passou
 * de setecentas linhas. Eram três perguntas independentes morando
 * juntas, e cada uma virou um arquivo com nome:
 *
 * <pre>
 * {@link HousePlans}    qual planta, e virada para que lado
 * {@link WaitingWork}   a obra que espera material: acordar ou largar
 * </pre>
 *
 * <p>O que ficou é a decisão de abrir obra: há construtor, há lote, e
 * então nasce o projeto e a tarefa por onde alguém o pega. Mais a
 * retomada do save, que é a mesma decisão vista de trás para frente.
 */
public final class ConstructionPlanner {

    static {
        ServerMemory.register(ConstructionPlanner.class, ConstructionPlanner::clearAll);
    }

    /**
     * Como esta fase aparece na linha de {@link IdleLog}.
     *
     * <p>O motivo de não haver obra existe por causa da sessão de
     * 2026-08-14, à noite: a Fase 10 não produziu linha nenhuma — nem
     * obra, nem recusa — e havia cinco caminhos silenciosos por onde ela
     * podia ter saído. Do lado de fora, "não tem construtor", "o jogo não
     * tem essa casa" e "não há lote" eram o mesmo silêncio. É o §11: a
     * linha que expõe o defeito precisa existir antes de alguém
     * desconfiar dele.
     *
     * <p>A memória do último motivo, que morava aqui, virou
     * {@link IdleLog} em 2026-08-15 — o lenhador e o fabricante
     * precisavam da mesma regra.
     */
    static final String SUBJECT = "building";

    /**
     * Até onde a colônia procura lote: cresce com a vila, sem teto — 2026-10-01,
     * pedido do autor: <i>"a vila deve crescer infinitamente para qualquer lado
     * possível"</i>.
     *
     * <p>Era 64 do centro, fixo. Com o centro no meio da caixa (Emenda 6), a
     * vila crescia até uns 64 para cada lado e parava: lote, índice de ruas e
     * ponta de rua saem todos deste raio. Agora, com a vila medida, é metade da
     * diagonal da caixa mais {@link VillageBounds#GROWTH_MARGIN}: cobre a caixa
     * inteira, cantos inclusive, e passa da borda. Lote aberto ali empurra a
     * borda, a caixa cresce, e o raio cresce junto. Nunca menor que 64.
     *
     * <p><b>O preço, aceito pelo autor:</b> a varredura em anéis olha ~1.024
     * colunas por ciclo de 30 s, e a área cresce com o quadrado do raio — uma
     * volta inteira passa de ~8,5 min (raio 64) a ~26 min numa caixa de 144 e
     * ~1 h 40 numa de 300. Vila grande cresce mais devagar.
     *
     * <p>Encurtado pelos testes:
     * <p>A bateria roda arenas lado a lado no mesmo mundo, e uma
     * varredura de 64 blocos sai da arena e acha a rua do teste vizinho.
     * E há um motivo prático junto: 64 são dezessete passagens de mil
     * colunas, e a Regra 15 só age quando a varredura <b>termina</b> —
     * um teste que quisesse ver a rua crescer teria de rodar as
     * dezessete.
     */
    private static @Nullable Integer shortened;

    /** O raio de busca de lote desta colônia, a partir do centro dela. */
    static int searchRadius(Colony colony) {
        if (shortened != null) {
            return shortened;
        }

        return colony.bounds().map(ConstructionPlanner::reachOf).orElse(VillageDetector.SEARCH_RADIUS);
    }

    /** Metade da diagonal da caixa, mais a margem de crescimento; nunca menos que 64. */
    static int reachOf(VillageBounds box) {
        double halfDiagonal = Math.hypot(box.sizeX() / 2.0, box.sizeZ() / 2.0);

        return Math.max(VillageDetector.SEARCH_RADIUS,
                (int) Math.ceil(halfDiagonal) + VillageBounds.GROWTH_MARGIN);
    }

    /** Encurta a busca de lote. Só os testes precisam disso. */
    public static void shortenSearchTo(int blocks) {
        if (blocks <= 0) {
            throw new IllegalArgumentException("Radius must be positive: " + blocks);
        }

        shortened = blocks;
    }

    /** Devolve o raio ao valor de jogo. */
    public static void restoreSearch() {
        shortened = null;
    }

    private ConstructionPlanner() {
    }

    /**
     * Registra por que não houve obra, uma vez por motivo.
     *
     * <p>A memória do último motivo saiu daqui em 2026-08-15 e virou
     * {@link IdleLog}: o lenhador e o fabricante precisavam da mesma
     * regra, e ela estava escrita só aqui. O que ficou é a tradução das
     * cinco recusas desta fase para o vocabulário do {@link IdleReason}.
     *
     * @return sempre vazio, para servir de {@code return} das recusas
     */
    static Optional<ConstructionProject> silent(
            Colony colony, IdleReason why, String detail) {

        IdleLog.record(colony.id(), SUBJECT, why, detail);

        return Optional.empty();
    }

    /** Esquece o motivo guardado. Chamado ao parar o servidor. */
    public static void clearAll() {
        IdleLog.clearAll();
        BuildingRepairPlanner.clearAll();
    }

    /**
     * Garante que a obra aberta tenha uma tarefa por onde alguém a pegue.
     *
     * <p><b>É o defeito que a sessão de 2026-08-15 achou, e o último do
     * MVP.</b> A obra existia, o construtor existia com baú, e as duas
     * casas ficaram em {@code 151 blocks left} por cinco horas e quarenta
     * minutos com {@code 0 working}. A corrente arrebentava aqui: nada em
     * produção criava tarefa de construção.
     *
     * <p>{@code tasks.create} só era chamado de {@code ColonyCycle
     * .requestMissing}, que traduz falta de recurso em tarefa; e
     * {@code typeFor} só devolve {@code BUILD} para recurso de categoria
     * {@code CONSTRUCTION}, que <b>nenhum {@code ResourceType} tem</b>.
     * A tarefa de obra era estruturalmente impossível, e por isso
     * {@code BuilderWork} nunca teve o que fazer — em vila nenhuma.
     *
     * <p>Os 82 testes verdes não pegaram porque {@code BuilderGameTest}
     * criava a tarefa à mão. É o §11 pela segunda vez, com a mesma frase
     * que o E10 rendeu: a pergunta não é "este código funciona?", é
     * <em>"quem põe esta coisa aqui, em jogo?"</em>.
     *
     * <p>Quem põe passou a ser este método, e não o ciclo: a obra não é
     * uma falta de recurso — é um projeto aberto precisando de mão. O
     * ciclo continua dono do que nasce de falta, e a vida desta tarefa
     * pertence ao projeto. Ver {@code TaskType.isResourceRequest}.
     *
     * <p>Uma tarefa por vez, e só enquanto a obra está em
     * {@link ConstructionState#BUILDING}: em {@code WAITING_RESOURCES} não
     * há o que colocar, e abrir tarefa ali poria um construtor a andar
     * até um canteiro para não fazer nada.
     */
    static void ensureTask(Colony colony, ConstructionProject project) {
        if (project.state() != ConstructionState.BUILDING) {
            return;
        }

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() == TaskType.BUILD && task.isOpen()) {
                return;
            }
        }

        if (project.nextBlock().isEmpty()) {
            // Todas as peças restantes aguardam o apoio que o mundo ainda
            // não oferece. A tarefa anterior já foi fechada pelo
            // construtor; recriá-la aqui repetiria a mesma falha.
            return;
        }

        int blocks = project.remainingCount();

        if (blocks == 0) {
            // A obra acabou e ainda não foi encerrada. Quem a fecha é o
            // construtor ao pôr o último bloco; abrir tarefa para zero
            // blocos seria ocupar uma mão por nada — e Task.create
            // recusaria, com razão.
            return;
        }

        VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.BUILD,
                TaskPriority.CONSTRUCTION,
                // Nominal, e é seguro que seja: a tarefa de obra não é
                // pedido de recurso, e quem paga cada bloco é
                // BuilderMaterials.takeMaterial, lendo o projeto. O que este
                // campo carrega de útil é o número — quantos blocos
                // faltam —, que aparece no log.
                ResourceType.OAK_PLANKS,
                blocks);

        VillageColonyMod.LOGGER.info(
                "Colony {} opened a build task — {} blocks left of {}",
                colony.id(),
                blocks,
                project.blueprint().id());
    }

    /**
     * Decide, se for o caso, a próxima obra desta colônia.
     *
     * @return a obra recém-planejada, quando nasce uma agora
     */
    public static Optional<ConstructionProject> plan(ServerWorld world, Colony colony) {
        ConstructionResume.resume(world, colony);

        Optional<ConstructionProject> open = VillageColonyMod.CONSTRUCTIONS.openOf(colony.id());

        if (open.isPresent()) {
            WaitingWork.wakeIfSupplied(world, open.get());
            CraftsmanRequest.askBeforeTheWorkWaits(world, open.get());
            BuilderPlacement.reconsiderDeferredPieces(world, open.get());

            // <b>E a obra que o centro deixou para trás</b> — 2026-09-15.
            // Vem antes do relógio de paciência porque não é caso dele: ele
            // só conta para WAITING_RESOURCES, e esta obra fica em BUILDING
            // para sempre, calada, com a vaga única ocupada. Ver
            // ConstructionReach.isOutOfReach, que traz a aritmética do log
            // do autor.
            //
            // <b>E quem responde "alcançável" é a rua, não o centro</b> —
            // E46, 2026-09-16. A rua cresce pela ponta mais distante do
            // centro, de propósito, e medir do centro largava a obra que a
            // própria estrada acabara de alcançar. Sem índice de ruas a
            // pergunta cai no centro, que é o comportamento de antes.
            OptionalInt toTheRoad = RoadIndex.roadsOf(colony.id())
                    .map(roads -> roads.blocksToTheNearestRoad(open.get().origin()))
                    .orElseGet(OptionalInt::empty);

            // <b>Sem índice, a rua que o lote encosta responde</b> — sessão
            // longa de 2026-09-26: a casa média, posta ao lado de uma rua, foi
            // largada um minuto depois porque o índice ainda não existia e a
            // conta caiu no centro (72 > 64). Ver VillageRoad.besidePaving.
            boolean besideARoad = toTheRoad.isEmpty() && VillageRoad.besidePaving(
                    world,
                    MinecraftTypeAdapter.toBlockPos(open.get().origin()),
                    open.get().blueprint().size().x(),
                    open.get().blueprint().size().y(),
                    open.get().blueprint().size().z());

            // <b>Dentro da vila, a obra está ao alcance</b> — ADR-003 Emenda 6,
            // 2026-09-30. A caixa cresce com a rua e com o lote, e o centro
            // passou a ser o meio dela: medir do centro largaria a obra que a
            // vila acabou de alcançar.
            boolean insideTheVillage = colony.bounds()
                    .map(bounds -> bounds.containsColumn(
                            open.get().origin().x(), open.get().origin().z(),
                            VillageBounds.IDENTITY_MARGIN))
                    .orElse(false);

            if (!insideTheVillage && !besideARoad && ConstructionReach.isOutOfReach(
                    open.get().origin(), colony.center(), searchRadius(colony), toTheRoad)) {

                VillageColonyMod.LOGGER.info(
                        "Colony {} lets go of {} at {} — the village centre is at {},"
                                + " the radius is {}, and the nearest road is {}."
                                + " The half-built house and its lot stay taken",
                        colony.id(),
                        open.get().blueprint().id(),
                        open.get().origin(),
                        colony.center(),
                        searchRadius(colony),
                        toTheRoad.isPresent()
                                ? toTheRoad.getAsInt() + " blocks away"
                                : "not indexed yet");

                // A planta nao leva a culpa: nao faltou material, a obra ficou
                // longe. Ver WaitingWork.giveUp(colony, project, blamePlan).
                WaitingWork.giveUp(colony, open.get(), false);
            } else if (!WaitingWork.giveUpIfStalled(world, colony, open.get())) {
                ensureTask(colony, open.get());

                // <b>E este ciclo conta</b> — 2026-09-19. A vaga de obra
                // é única, e enquanto ela estiver ocupada o planejador
                // volta aqui sem nunca pedir lote. Sem registrar, a soma
                // da varredura dizia "9 planner runs" numa sessão de
                // noventa minutos e parecia varredura lenta; eram cento
                // e oitenta ciclos presos numa casa que não andava. Ver
                // SweepLog.busy.
                SweepLog.busy(colony.id());

                return silent(colony, IdleReason.ALREADY_OPEN, "");
            }
        }

        int builders = WorkAssignment.countCapableOf(
                colony.id(), TaskType.BUILD.required(), VillageColonyMod.WORKERS);

        if (builders == 0) {
            // Sem detalhe: a frase do motivo já diz "no worker in the
            // village can do it", e o assunto da linha já diz que o
            // trabalho é de construção. Repetir "no builder" aqui só
            // alonga a linha.
            return silent(colony, IdleReason.NO_WORKER, "");
        }

        Optional<ConstructionProject> repair = BuildingRepairPlanner.open(world, colony);

        if (repair.isPresent()) {
            ensureTask(colony, repair.get());
            return repair;
        }

        return LotSeeking.seek(world, colony, builders);
    }

}
