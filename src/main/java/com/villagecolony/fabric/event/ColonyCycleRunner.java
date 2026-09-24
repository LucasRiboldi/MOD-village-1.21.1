package com.villagecolony.fabric.event;

import com.villagecolony.core.coordination.PlanningBudget;
import com.villagecolony.fabric.integration.SweepDeadline;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.ClusterRejection;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.colony.service.ColonyAbandonment;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.coordination.ColonyCycle;
import com.villagecolony.core.coordination.ColonyGoals;
import com.villagecolony.core.coordination.WorkDemand;
import com.villagecolony.core.resource.model.ColonyResources;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.HiringLog;
import com.villagecolony.core.worker.service.ProfessionAssigner;
import com.villagecolony.core.worker.service.VacancyEnforcer;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.ChestMarker;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.SiteMarker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageBiomes;
import com.villagecolony.fabric.integration.VillageScanner;
import com.villagecolony.fabric.integration.VillageFoundation;
import com.villagecolony.fabric.integration.VanillaBedChests;
import com.villagecolony.fabric.integration.BigHouseFoundation;
import com.villagecolony.fabric.integration.VillagerScanner;
import com.villagecolony.fabric.integration.WorkerEquipment;
import com.villagecolony.fabric.integration.WorkerNameplate;
import com.villagecolony.fabric.work.IdleLog;
import com.villagecolony.fabric.work.MinerWork;
import com.villagecolony.fabric.work.FarmerWork;
import com.villagecolony.fabric.work.ShepherdWork;
import com.villagecolony.fabric.work.SmelterWork;
import com.villagecolony.fabric.work.SurfaceGatheringWork;
import com.villagecolony.fabric.work.WaitingWork;
import com.villagecolony.fabric.work.ChestRelief;
import com.villagecolony.fabric.work.WorkMaterials;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.LumberjackWork;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.work.StrandedEscape;
import com.villagecolony.fabric.work.VillageMeals;
import com.villagecolony.fabric.work.ConstructionDemand;
import com.villagecolony.fabric.work.ConstructionPlanner;
import com.villagecolony.fabric.work.CraftingWork;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.world.poi.PointOfInterestTypes;

/**
 * O ciclo de cada colônia: quais rodam (só perto de um jogador), o que cada ofício faz nele e o relatório de mãos — separado de
 * {@link VillageDetectionHandler} em 2026-09-24, quando ele passou de 500
 * linhas. Os comentários vieram junto sem mudança.
 */
final class ColonyCycleRunner {

    private ColonyCycleRunner() {
    }

    /**
     * O assunto do ciclo inteiro no {@link IdleLog}.
     *
     * <p>Os outros assuntos são profissões — "lumberjacks", "building".
     * Este é a colônia toda, e é o único que significa <b>nada
     * aconteceu, ponto</b>.
     */
    static final String CYCLE_SUBJECT = "cycle";

    /** O último estoque que cada colônia mandou para o log. */
    static final Map<UUID, String> lastStock = new HashMap<>();

    /** O tempo de planejador deste ciclo, para ajustar a cota do próximo. */
    private static long plannerNanos;

    /** Se o planejador deste ciclo tem prazo de relógio (só em jogo). */
    private static boolean plannerDeadline;

    /**
     * O ciclo de simulação da ADR-002, uma vez por colônia ativa.
     *
     * <p>Roda por último de propósito: a colônia decide sobre o que a
     * detecção acabou de ver, e não sobre a fotografia do ciclo passado.
     *
     * <p>Só colônia ACTIVE. Uma colônia dormente tem os chunks
     * descarregados, e o estoque lido dela seria zero — a colônia
     * concluiria que falta tudo e encheria a fila de pedidos que ninguém
     * pode atender.
     */
    static void runColonyCycles(ServerWorld overworld, boolean onlyNearPlayers) {
        List<Colony> active = List.copyOf(VillageColonyMod.COLONIES.all()).stream()
                .filter(Colony::isActive)
                .filter(colony -> !onlyNearPlayers || VillageFocus.isNearAPlayer(overworld, colony))
                .toList();

        // <b>A vez de planejar é repartida</b> — 2026-09-15. O log do autor
        // mediu o ciclo em 98 ms contra os 50 do tique, com 72 ms de
        // planejador e 29 colônias planejando todas aqui dentro. A
        // varredura de lote já tinha teto por colônia — 1.024 colunas por
        // passagem —, e faltava o teto global: mil colunas vezes vinte e
        // nove cabem num tique só, e coube.
        //
        // Só o planejamento espera a vez. O resto do ciclo continua
        // rodando para todas, pelo mesmo motivo que a guarda de abandono
        // registrou em 09-02: pular o ciclo inteiro faz o trabalhador
        // andar aos soluços. Ver PlannerTurns.
        //
        // <b>E em jogo só a vila foco planeja</b> — 2026-09-24, decisão do
        // autor; ver VillageFocus. O planejador tem prazo de relógio e a
        // cota se ajusta pelo custo do ciclo; ver PlanningBudget.
        Set<UUID> watched = VillageFocus.coloniesNearPlayers(overworld, active);
        List<UUID> eligible = onlyNearPlayers
                ? VillageFocus.planners(active, watched)
                : active.stream().map(Colony::id).toList();
        Set<UUID> planners = PlannerTurns.chooseFrom(eligible, watched);

        plannerNanos = 0;
        plannerDeadline = onlyNearPlayers;

        for (Colony colony : active) {
            runCycleOf(overworld, colony, planners.contains(colony.id()));
        }

        if (onlyNearPlayers) {
            PlannerTurns.observeCost(plannerNanos / 1_000_000L);
        }

        // As tarefas encerradas saem do registro depois de todas as
        // colônias terem decidido. `purgeClosed` existia desde a Fase 7 e
        // nunca tinha sido chamado: tarefa é objeto em memória, e nada as
        // removia. Era a metade do E1 que a Regra 1 não resolve sozinha.
        VillageColonyMod.TASKS.purgeClosed();

        // E as obras terminadas, pelo mesmo motivo: canteiro é objeto em
        // memória, e sem alguém que o remova o registro só cresce. A casa
        // fica em BUILDINGS.
        VillageColonyMod.CONSTRUCTIONS.purgeFinished();
    }

    /**
     * Um ciclo de uma colônia.
     *
     * <p>A contagem parcial é motivo para não decidir. Baú em chunk
     * descarregado sai da soma sem avisar, e uma colônia que conclui
     * "falta madeira" com metade dos baús fora de alcance mandaria um
     * trabalhador buscar o que ela já tem. Ver
     * {@code ChestInventoryReader.ChestSurvey} e a entrada de §15 de
     * 2026-08-07.
     */
    static void runCycleOf(ServerWorld overworld, Colony colony, boolean mayPlan) {
        long mark = System.nanoTime();

        // <b>Uma lista, e os três consumidores dela</b> — P0.3, 2026-09-11.
        // A varredura e as duas medidas de espaço montavam cada uma a
        // própria lista, a partir do registro de trabalhadores, e por
        // isso nenhuma delas via o baú da boca da mina. Contar num
        // conjunto e consumir de outro é a discordância de 2026-09-10.
        List<ColonyPos> chests = ColonyChests.nearestFirst(
                overworld, colony.id(), colony.center());

        ChestInventoryReader.ChestSurvey survey =
                ChestInventoryReader.survey(
                        overworld, chests, ResourceGroup.WOOD, ResourceGroup.PLANKS);

        if (survey.isPartial()) {
            // A leitura aconteceu e custou, mesmo sem decidir nada: cobrar
            // só o caminho feliz esconderia justamente a colônia cara que
            // não produz — que é o caso que o P2.1 foi medir.
            CycleCost.since(CycleCost.Phase.CHESTS, mark);

            // <b>E agora ele diz.</b> Pular era certo desde 2026-08-07;
            // pular calado custou a sessão de 2026-09-04 inteira em
            // dúvida — não havia como saber, do log, se uma colônia
            // parada tinha decidido não decidir. Uma colônia inteira sem
            // fazer nada é a maior omissão que este log podia ter.
            IdleLog.record(
                    colony.id(),
                    CYCLE_SUBJECT,
                    IdleReason.COUNT_PARTIAL,
                    survey.chestsUnreachable() + " of "
                            + (survey.chestsRead() + survey.chestsUnreachable())
                            + " chests are in unloaded chunks");

            return;
        }

        IdleLog.clear(colony.id(), CYCLE_SUBJECT);

        // O estoque a cada ciclo, e não só quando um baú novo entra —
        // 2026-09-04. A sessão daquele dia teve o último retrato às
        // 00:08 e mais trinta e cinco minutos de escuro, justamente
        // enquanto a obra parava por falta de material. A varredura já
        // está em mãos: sai de graça.
        VillagerRegistration.logResources(colony, survey);

        // A Regra 1: a meta é o que está guardado mais o que ainda cabe.
        // O espaço é medido aqui porque é aqui que os baús existem — o
        // Core não conhece baú, só recebe o número. Ver ColonyGoals.
        int room = survey.freeSpaceForGroup(ResourceGroup.WOOD);

        // E a Regra 5, a da Fase 9: metade do que os baús comportam em
        // tábua. Medida do mesmo jeito e pelo mesmo motivo.
        int plankRoom = survey.freeSpaceForGroup(ResourceGroup.PLANKS);

        // Até aqui é baú: uma só fotografia produz estoque e as duas
        // medidas de espaço, sem reler os mesmos inventários no ciclo.
        mark = CycleCost.since(CycleCost.Phase.CHESTS, mark);

        // A obra é decidida antes de a colônia pensar: o que ela pede
        // entra na conta do mesmo ciclo, e não do seguinte. Planejar
        // depois faria a colônia passar um ciclo inteiro sem saber que
        // tem uma casa para levantar.
        // E a conta do trabalhador que o registro tem e o mundo não. Só
        // mede — ver PhantomWorkerLog e a auditoria de estado órfão.
        PhantomWorkerLog.probe(overworld, colony.id());

        // Colônia sem vila não planeja obra — 2026-09-02. O planejamento
        // carrega a varredura de lote e o crescimento de rua, e a
        // varredura tem teto de 1.024 colunas por passagem: cada colônia
        // abandonada com chunks carregados cobrava isso por ciclo sem ter
        // o que construir. A regra é do Core; aqui mora a aplicação.
        //
        // Só o planejamento. O resto do ciclo continua rodando para ela,
        // porque a marca de abandono oscila — é o E9 — e pular o ciclo
        // inteiro faria o trabalhador dela andar aos soluços.
        // <b>E a vez dela no rodízio</b> — 2026-09-15. A guarda de abandono
        // pergunta "esta colônia tem o que construir?"; esta pergunta "é a
        // vez dela?". São duas perguntas distintas e ambas dizem não à
        // mesma chamada. Ver PlannerTurns.
        if (mayPlan && ColonyAbandonment.plansConstruction(colony)) {
            long planning = System.nanoTime();

            if (plannerDeadline) {
                SweepDeadline.within(PlanningBudget.DEADLINE_MS,
                        () -> ConstructionPlanner.plan(overworld, colony));
            } else {
                ConstructionPlanner.plan(overworld, colony);
            }

            plannerNanos += System.nanoTime() - planning;
        }

        // A tábua da vila, e não sempre a de carvalho — a Regra 20. A
        // obra de uma colônia de taiga pede pinheiro, e perguntar por
        // carvalho devolveria zero: a meta perderia a demanda da obra e
        // cairia na metade do baú, que é a conta de quando não há obra.
        int planksForWork = ConstructionDemand.planksNeededBy(
                VillageBiomes.woodAt(overworld, colony.center())
                        .orElse(MinecraftTypeAdapter.toResourceId(Blocks.OAK_PLANKS)),
                colony);

        // A pedra que a obra pede, e a pedra desta vila — 2026-08-20. No
        // deserto é arenito; perguntar por pedregulho daria zero, e a
        // vila voltaria a não construir por falta de meta.
        VillagePalette palette = HousePlans.paletteOf(overworld, colony.center());

        ResourceType stone = MinecraftTypeAdapter.toBlock(palette.stone())
                .flatMap(block -> MinecraftTypeAdapter.toResourceType(block.asItem()))
                .orElse(ResourceType.COBBLESTONE);

        // Por família desde 2026-08-22, e foi a vila de deserto que
        // cobrou: a casa dela é de arenito LISO, e perguntar pelo
        // arenito puro devolvia quase zero. Ver WorkMaterials.stone.
        // <b>E o bau que enche puxa a producao para a frente</b> —
        // decisao do autor, 2026-09-19. Sem isto a demanda so nasce de
        // obra aberta: na sessao de 12:09 o bau do mineiro encheu, 365
        // sandstone foram para o chao, e a obra esperava sandstone.
        // Ver ChestRelief.
        int stoneForWork = ChestRelief.stoneToAskFor(
                overworld, colony, WorkMaterials.stone(palette, colony));

        // O que a obra pede em peça, traduzido para o que a colônia sabe
        // produzir. A casa não pede vidro, pede vidraça; não pede carvão,
        // pede tocha; não pede lã, pede cama. Perguntar pelo material
        // devolvia zero, e com zero ninguém recebia tarefa.
        //
        // <b>As quatro saem do mesmo lugar desde 2026-08-21</b>: a obra
        // aberta. A lã e o ferro vinham da passagem de mobília, que a
        // Regra 21 sustentava, e ela morreu.
        WorkDemand work = new WorkDemand(
                planksForWork,
                stone,
                stoneForWork,
                WorkMaterials.wool(overworld, colony),
                WorkMaterials.glass(overworld, palette, colony),
                WorkMaterials.coal(overworld, colony),
                WorkMaterials.iron(overworld, colony),
                WorkMaterials.smeltedNeeds(overworld, colony),
                WorkMaterials.surfaceGatheredNeeds(overworld, colony));

        // E a placa da obra fica sabendo do estoque — 2026-09-15. O ciclo
        // acabou de ler os baús; a placa desenha uma vez por segundo e
        // reler ali multiplicaria por trinta o custo da fase `chests`.
        // Ver SiteMarker.remember.
        SiteMarker.remember(colony.id(), survey.resources().total());

        // A obra inteira: varredura de lote, crescimento de rua, paleta e
        // a conta do que a construção pede. É a fase que o plano suspeita
        // ser a cara, e agora ela responde por si.
        mark = CycleCost.since(CycleCost.Phase.PLANNER, mark);

        int assigned = ColonyCycle.run(
                colony.id(),
                survey.resources().total(),
                ColonyGoals.of(
                        colony, survey.resources().total(), room, plankRoom, work),
                VillageColonyMod.TASKS,
                VillageColonyMod.WORKERS,
                VillageColonyMod.STORAGES::hasStorage,
                (resource, type, hands) -> reportHands(colony.id(), resource, type, hands),
                work.constructionMaterials());

        // Sem o `if (assigned > 0)` que estava aqui. A linha calava
        // exatamente quando havia algo a dizer: distribuição parada é
        // `assigned == 0`, e era então que a contagem de tarefas abertas
        // — a única prova de que a fila não está vazia — desaparecia do
        // log. Na sessão de 2026-08-15 ela sumiu às 11:21 e não voltou
        // mais, e foram trinta e dois minutos sem saber se a colônia
        // tinha tarefa parada ou tarefa nenhuma.
        //
        // É o mesmo remédio do E10 e do E2: número nenhum não é silêncio
        // barato, é a pergunta seguinte ficando sem resposta.
        VillageColonyMod.LOGGER.info(
                "Colony {} assigned {} tasks ({} open)",
                colony.id(),
                assigned,
                VillageColonyMod.TASKS.availableFor(colony.id()).size());

        // A linha entra na conta da distribuição, e não na das profissões:
        // o `availableFor` que ela chama é trabalho de fila.
        mark = CycleCost.since(CycleCost.Phase.ASSIGN, mark);

        // Depois da distribuição: quem recebeu tarefa neste ciclo já
        // começa a andar nele, em vez de esperar o próximo.
        LumberjackWork.run(overworld, colony);
        MinerWork.run(overworld, colony);
        SmelterWork.run(overworld, colony);
        SurfaceGatheringWork.run(overworld, colony);
        ShepherdWork.run(overworld, colony);
        FarmerWork.run(overworld, colony);

        // <b>A peça que a obra espera e ninguém faz</b> — P1.1,
        // 2026-09-17. Vem antes do fabricante, para a tarefa aberta agora
        // já ser atendida neste ciclo — a mesma razão de todo este bloco
        // vir depois da distribuição.
        //
        // <b>E aqui, e não no planejador.</b> O ConstructionPlanner só
        // roda para as colônias da vez no rodízio — oito por ciclo desde
        // 2026-09-15 —, e uma obra parada esperando escada não pode
        // depender de sorteio para ser destravada. Ver
        // WaitingWork.askTheCraftsmanFor.
        WaitingWork.askForWhatTheWorkIsWaitingOn(overworld, colony);

        CraftingWork.run(overworld, colony);
        BuilderWork.run(overworld, colony);

        CycleCost.since(CycleCost.Phase.WORKERS, mark);
    }

    /**
     * Diz que a colônia não tem quem faça um material — 2026-09-09.
     *
     * <p><b>O silêncio que ela quebra.</b> {@code ColonyCycle} pula o
     * pedido de material que ninguém sabe fazer, e pular está certo:
     * tarefa sem executor possível fica na fila para sempre. O que estava
     * errado é que ele pulava <b>sem uma linha</b>, e o que o autor via
     * era {@code assigned 0 tasks (0 open)} sem causa — o mesmo sintoma
     * da roça que travou a vila nesta mesma data, e que custou uma hora
     * de sessão até ser diagnosticado.
     *
     * <p><b>Assunto por tarefa, e não por material.</b> Uma colônia sem
     * fundidor não sabe fazer vidro <b>nem</b> lingote <b>nem</b> arenito
     * liso: três linhas iguais diriam a mesma coisa três vezes. O
     * {@code IdleLog} compara só o motivo, então a primeira fala e as
     * outras calam sozinhas — e o detalhe, que fica fora da comparação
     * de propósito, nomeia o material que chegou primeiro.
     *
     * <p><b>O {@code clear} é metade da correção.</b> Sem ele, uma
     * colônia que perde o fundidor, contrata outro e o perde de novo
     * ficaria muda na segunda vez: o motivo guardado ainda seria
     * {@code NO_WORKER}, e o registrador trataria como repetição de um
     * silêncio que já tinha acabado. É o caso que o javadoc de
     * {@code IdleLog.clear} descreve.
     */
    static void reportHands(
            UUID colonyId, ResourceType resource, TaskType type, int hands) {

        String subject = type.name().toLowerCase(Locale.ROOT);

        if (hands == 0) {
            IdleLog.record(
                    colonyId,
                    subject,
                    IdleReason.NO_WORKER,
                    resource + " needs " + type.required());

            return;
        }

        IdleLog.clear(colonyId, subject);
    }
}
