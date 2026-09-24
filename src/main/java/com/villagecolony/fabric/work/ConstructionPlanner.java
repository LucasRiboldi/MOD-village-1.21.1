package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.SweepState;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.integration.LotClearance;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionReach;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.VillageRoad;
import com.villagecolony.fabric.integration.RoadExtension;
import com.villagecolony.fabric.integration.SweepLog;
import com.villagecolony.fabric.integration.SitePreparation;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

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
     * Até onde a colônia procura lote. É o raio da vila, menos nos testes.
     *
     * <p>A bateria roda arenas lado a lado no mesmo mundo, e uma
     * varredura de 64 blocos sai da arena e acha a rua do teste vizinho.
     * E há um motivo prático junto: 64 são dezessete passagens de mil
     * colunas, e a Regra 15 só age quando a varredura <b>termina</b> —
     * um teste que quisesse ver a rua crescer teria de rodar as
     * dezessete.
     */
    static int searchRadius = VillageDetector.SEARCH_RADIUS;

    /** Encurta a busca de lote. Só os testes precisam disso. */
    public static void shortenSearchTo(int blocks) {
        if (blocks <= 0) {
            throw new IllegalArgumentException("Radius must be positive: " + blocks);
        }

        searchRadius = blocks;
    }

    /** Devolve o raio ao valor de jogo. */
    public static void restoreSearch() {
        searchRadius = VillageDetector.SEARCH_RADIUS;
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

            if (ConstructionReach.isOutOfReach(
                    open.get().origin(), colony.center(), searchRadius, toTheRoad)) {

                VillageColonyMod.LOGGER.info(
                        "Colony {} lets go of {} at {} — the village centre is at {},"
                                + " the radius is {}, and the nearest road is {}."
                                + " The half-built house and its lot stay taken",
                        colony.id(),
                        open.get().blueprint().id(),
                        open.get().origin(),
                        colony.center(),
                        searchRadius,
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

        // A seleção é global, mas a descoberta do lote continua única: a
        // família escolhida aqui passa pelo mesmo scanner e pelas mesmas
        // recusas físicas, seja casa, roça, cercado ou templo.
        //
        // A ordem permanente é casa -> tipo A -> casa -> tipo B. O tipo B
        // não repete o último tipo não residencial concluído; a decisão
        // deriva do registro de construções, sem estado paralelo.
        List<Blueprint> plans = HousePlans.plansForNext(world, colony);

        if (plans.isEmpty()) {
            return silent(
                    colony,
                    IdleReason.NOT_IN_GAME,
                    "this game has no buildable village structure for the "
                            + colony.id() + " style");
        }

        Blueprint blueprint = plans.get(0);

        // <b>Roça não sai atrás da estrada</b> — 2026-09-05, visto em
        // jogo. A primeira roça da colônia nasceu em {x=1517, z=113} com
        // o centro em {x=1435, z=47}: <b>105 blocos</b>, porque o lote
        // veio da ponta da estrada que a vila estava esticando. O
        // fazendeiro procura lavoura a 32 do centro, então ele nunca a
        // veria — e a linha do log logo depois de ela ficar pronta era
        // exatamente "no empty plot within 32 blocks of the village".
        //
        // Pior: o pedido de roça nunca se fechava, e a colônia levantou
        // <b>duas</b> em quatro minutos, a caminho de encher o mapa.
        //
        // A extensão de rua existe para a vila <b>crescer</b>, e casa
        // nova na ponta é o que ela quer. Roça é o contrário: o autor
        // pediu "um espaço livre <b>dentro da vila</b>", e a varredura em
        // anéis a partir do centro já devolve o lote livre mais perto.
        boolean farming = FarmPlans.isFarm(blueprint.id());

        // A ponta que já rendeu continua rendendo, e isso vem antes da
        // varredura — 2026-08-26. Sem isto a colônia pagava dezessete
        // ciclos por bloco de rua, e a sessão das 03:11 mediu o custo:
        // um bloco calçado, e o lote de sete por sete continuou sem
        // caber. Quem autorizou a rua a crescer foi a varredura que
        // terminou sem lote, e essa autorização vale para o trecho.
        if (!farming && RoadExtension.isGrowing(colony.id())) {
            Optional<ConstructionProject> onTheStretch =
                    RoadGrowthPlanning.keepGrowing(world, colony, blueprint, plans, builders);

            if (onTheStretch.isPresent() || RoadExtension.isGrowing(colony.id())) {
                // Ou nasceu lote no trecho novo, ou a passagem foi gasta
                // crescendo. Nos dois casos a varredura espera.
                return onTheStretch;
            }

            // A ponta parou de render. A varredura volta a mandar, e
            // nesta mesma passagem.
        }

        // O denominador da conta do SweepLog: quantas vezes o planejador
        // chegou a pedir lote. Obra já aberta, falta de construtor e
        // planta ausente saem antes daqui e não são dívida da varredura.
        SweepLog.asked(colony.id());

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                world, colony.id(), colony.center(), searchRadius,
                SiteOpening.sizesOf(plans));

        if (site.isEmpty()) {
            // Duas respostas, e a diferença importa: uma diz que não há
            // lote, a outra diz que ninguém terminou de olhar. A linha
            // anterior dizia a primeira nos dois casos, e no segundo isso
            // era mentira — ver o E14 do §17.
            //
            // Sem o número do anel de propósito: IdleLog só registra
            // quando o motivo muda, e um anel diferente por ciclo faria a
            // linha voltar toda vez.
            // Os dois jeitos de procurar contam — 2026-09-11. Esta
            // linha perguntava só pelo cursor do quadrado, e desde que a
            // volta pelo índice de ruas também pode parar no meio, ela
            // deixaria a Regra 15 crescer a rua sem ninguém ter visto o
            // raio inteiro. Ver SweepState.stillLookingForALot.
            if (SweepState.stillLookingForALot(colony.id())) {
                return silent(colony, IdleReason.SWEEP_INCOMPLETE, "looking for a lot");
            }

            // A Regra 15, e é aqui que ela cabe: a varredura terminou o
            // raio inteiro e não há beira de rua livre. Antes desta
            // linha a vila parava para sempre — a rua era algo que a
            // colônia encontrava, e nunca algo que ela produzia.
            //
            // O lote novo não nasce nesta passagem de propósito: a
            // varredura seguinte é que vai encontrá-lo, e ela recomeça
            // do centro no ciclo que vem. Trinta segundos, e a ordem da
            // regra fica respeitada — estrada primeiro, casa depois.
            return RoadGrowthPlanning.extendTheRoad(world, colony, blueprint, plans, builders);
        }

        // A recusa de lote sobre casa da colônia morava aqui, e daqui não
        // funcionava. O comentário dizia "a próxima passagem tenta outro
        // anel", e era falso: achar um lote apaga o cursor da varredura,
        // então a passagem seguinte recomeçava do centro e reencontrava o
        // mesmo lugar. Em 2026-08-20 a vila do autor ficou nesse laço.
        //
        // A pergunta desceu para `LotClearance.isClearAbove`, que é
        // onde a varredura ainda pode seguir para o anel seguinte.

        if (farming && !ConstructionDemand.withinTheFarmersReach(colony, site.get())) {
            // Lote livre, mas longe demais: o fazendeiro procura lavoura
            // a FarmerWork.reach() do centro, e roça que ele não vê é
            // roça que ninguém planta — e que não fecha o pedido, então
            // a colônia levantaria outra, e outra.
            //
            // <b>E a recusa não pode parar a vila</b> — 2026-09-09. Ela
            // encerrava a passagem inteira, e como o lote de amanhã é o
            // mesmo de hoje, a colônia repetia a recusa para sempre sem
            // nunca tentar uma casa: uma hora de jogo, 108 passagens do
            // planejador, nenhuma obra aberta, e o autor sem ver
            // trabalhador nenhum trabalhando. A roça cede a vez por
            // vinte ciclos e as casas passam — ver FarmPlans.postponed.
            FarmPlans.postpone(colony.id(), world.getTime());

            return silent(
                    colony,
                    IdleReason.NO_TARGET,
                    "the only free lot is outside the farmer's reach"
                            + " — the houses go first for now");
        }

        return SiteOpening.open(world, colony, site.get(), plans, blueprint, builders);
    }

}
