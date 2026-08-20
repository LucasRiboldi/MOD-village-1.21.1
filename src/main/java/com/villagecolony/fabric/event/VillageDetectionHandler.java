package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.ClusterRejection;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.colony.service.ColonyAbandonment;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.coordination.ColonyCycle;
import com.villagecolony.core.coordination.ColonyGoals;
import com.villagecolony.core.resource.model.ColonyResources;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.ProfessionAssigner;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.ChestMarker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageBiomes;
import com.villagecolony.fabric.integration.VillageScanner;
import com.villagecolony.fabric.integration.VillagerScanner;
import com.villagecolony.fabric.integration.WorkerEquipment;
import com.villagecolony.fabric.integration.WorkerNameplate;
import com.villagecolony.fabric.work.HouseFurnishing;
import com.villagecolony.fabric.work.MinerWork;
import com.villagecolony.fabric.work.ShepherdWork;
import com.villagecolony.fabric.work.SmelterWork;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.LumberjackWork;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.work.ConstructionPlanner;
import com.villagecolony.fabric.work.ManufacturerWork;
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
import java.util.UUID;
import net.minecraft.world.poi.PointOfInterestTypes;

/**
 * Dispara a detecção de vilas.
 *
 * <p>Dois gatilhos, conforme ADR-003 §3:
 *
 * <ul>
 *   <li>chunk carregado que contenha POI de cama;
 *   <li>ciclo de {@link VillageDetector#CYCLE_TICKS} em área já carregada.
 * </ul>
 *
 * <p>O mundo nunca é varrido. Ambos os gatilhos partem de um ponto e usam
 * o raio limitado do scanner.
 */
public final class VillageDetectionHandler {

    /**
     * Quanta lã cada colônia pediu na passagem anterior.
     *
     * <p><b>Um ciclo atrasado, e de propósito.</b> Quem sabe que uma casa
     * está sem cama é a passagem de mobília, e ela roda <b>depois</b> do
     * construtor — a casa que terminou neste ciclo já é olhada nele. A
     * meta, por sua vez, é montada antes. Adiantar a mobília para antes
     * do construtor resolveria a ordem e criaria outra: a casa recém
     * terminada esperaria um ciclo inteiro pela cama.
     *
     * <p>Trinta segundos de atraso numa meta de lã não custam nada, e a
     * alternativa custaria.
     */
    private static final Map<UUID, Integer> WOOL_WANTED = new HashMap<>();

    /** Quanta lã esta colônia pediu, ou zero se ainda não pediu nada. */
    private static int woolWanted(Colony colony) {
        return WOOL_WANTED.getOrDefault(colony.id(), 0);
    }

    private static final VillageScanner SCANNER = new VillageScanner();

    /**
     * Quantos gatilhos de chunk cabem esperando.
     *
     * <p>Existe para que abrir o mundo não guarde uma varredura por
     * chunk carregado. O ciclo longo cobre o que passar do teto.
     */
    private static final int PENDING_LIMIT = 256;

    /** Duração de um tick do servidor, em milissegundos. */
    private static final int TICK_MILLIS = 50;

    /**
     * Chunks com cama esperando varredura, um por chunk.
     *
     * <p>{@code LinkedHashMap} para drenar na ordem em que chegaram: os
     * primeiros chunks a carregar são os mais perto do jogador.
     */
    private static final Map<ChunkPos, BlockPos> pending = new LinkedHashMap<>();

    /**
     * Pares de colônias sobrepostas já avisados nesta sessão.
     *
     * <p>Ver {@link #warnIfOverlapping}: a sobreposição não se resolve
     * sozinha, e sem esta memória o aviso sairia a cada ciclo.
     */
    private static final Set<String> overlapsReported = new HashSet<>();

    private static int tickCounter;

    private VillageDetectionHandler() {
    }

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register(VillageDetectionHandler::onChunkLoad);
        ServerTickEvents.END_SERVER_TICK.register(VillageDetectionHandler::onServerTick);
    }

    /**
     * Roda um ciclo completo agora, a partir de um ponto.
     *
     * <p>Costura para os testes de jogo, e o único lugar do mod que
     * existe por causa deles. Em jogo os dois gatilhos são o chunk que
     * carrega e o ciclo de {@link VillageDetector#CYCLE_TICKS}; um teste
     * que dependesse disso teria de carregar chunk e esperar trinta
     * segundos por ciclo.
     *
     * <p>Faz o mesmo que o ciclo longo e na mesma ordem — detectar,
     * atualizar lifecycle, sondar do centro, simular. Se aqui divergir
     * de {@link #onServerTick}, o teste passa a verificar um caminho que
     * o jogo não percorre, que é pior do que não ter teste.
     *
     * <p>Não é chamado por nada em produção. Ver o item A do §8 de
     * Project-State.md.
     */
    public static void runCycleNow(ServerWorld world, BlockPos trigger) {
        detectAround(world, trigger);

        updateLifecycles(world);

        detectFromColonyCenters(world);

        runColonyCycles(world);
    }

    /**
     * Chunk carregado — só enfileira.
     *
     * <p>A checagem barata vem primeiro: sem cama neste chunk, não há
     * motivo para pagar nada. A esmagadora maioria dos chunks carregados
     * cai fora aqui.
     *
     * <p>O gatilho é a posição da própria cama encontrada, não o canto do
     * chunk. {@code ChunkPos.getStartPos()} devolve y=0, e
     * {@code getInCircle} mede distância em três dimensões: partindo de
     * y=0, uma cama em y=64 já consome todo o raio de busca antes de
     * qualquer deslocamento horizontal. Ancorado no chunk, este gatilho
     * não encontrava vila nenhuma.
     *
     * <p><b>Por que enfileirar em vez de varrer aqui.</b> Este evento
     * dispara uma vez por chunk, e ao abrir o mundo centenas chegam no
     * mesmo tick. Uma vila de trinta camas ocupa dezenas de chunks, e
     * cada um deles pagava a varredura inteira — POI num raio de 64,
     * caixa de aldeões, baú de cada trabalhador novo, nome de cada
     * trabalhador — dentro do mesmo tick. Era o travamento ao carregar o
     * mapa de 2026-08-08: o servidor não voltava, e os aldeões não
     * andavam porque nenhum tick terminava.
     */
    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        if (pending.size() >= PENDING_LIMIT) {
            // Fila cheia. Descartar é seguro: a varredura a partir do
            // jogador, no ciclo longo, cobre o mesmo terreno.
            return;
        }

        world.getPointOfInterestStorage()
                .getInChunk(
                        poi -> poi.matchesKey(PointOfInterestTypes.HOME),
                        chunk.getPos(),
                        PointOfInterestStorage.OccupationStatus.ANY)
                .findFirst()
                .ifPresent(bed -> pending.putIfAbsent(chunk.getPos(), bed.getPos()));
    }

    /**
     * Um gatilho de chunk por tick, no máximo.
     *
     * <p>É o teto que o evento de chunk não tem. Uma varredura por tick
     * ainda esvazia a fila de uma vila inteira em pouco mais de um
     * segundo, e nenhum tick paga por duas.
     *
     * <p>Cama dentro de colônia conhecida é descartada sem varrer: o que
     * essa varredura descobriria — a vila cresceu, o centro se moveu — a
     * sonda ancorada no centro já descobre a cada ciclo, e ela é a única
     * com autoridade para encolher a colônia.
     */
    private static void drainOnePending(ServerWorld overworld) {
        while (!pending.isEmpty()) {
            Iterator<Map.Entry<ChunkPos, BlockPos>> entries = pending.entrySet().iterator();
            BlockPos bed = entries.next().getValue();

            entries.remove();

            boolean known = VillageColonyMod.COLONIES
                    .findNearest(
                            MinecraftTypeAdapter.toColonyPos(bed),
                            VillageDetector.DUPLICATE_DISTANCE)
                    .isPresent();

            if (known) {
                continue;
            }

            detectAround(overworld, bed);

            return;
        }
    }

    /**
     * Ciclo longo.
     *
     * <p>Reavalia o que já está carregado: uma vila cresce, o jogador
     * constrói camas, o centro se move. Sem isto, uma vila só seria
     * reavaliada ao recarregar o chunk.
     *
     * <p>Parte da posição dos jogadores porque é ali que os chunks estão
     * carregados. Sem jogador não há o que simular.
     *
     * <p>Depois varre de novo a partir do centro de cada colônia ativa.
     * Não é redundância: a varredura do jogador parte de um ponto que
     * muda a cada passo, e uma colônia só pode encolher quando duas
     * varreduras vêm da mesma âncora. O centro da colônia é o único
     * ponto estável entre ciclos. Ver {@code Colony#observe}.
     */
    private static void onServerTick(net.minecraft.server.MinecraftServer server) {
        drainOnePending(server.getOverworld());

        // A Regra 2 mora aqui: o lenhador quebra um bloco de cada vez, no
        // tempo que um jogador com machado de ferro levaria, e para isso
        // precisa de um passo por tick — não de um passo a cada 600. O
        // custo é um contador por lenhador; a parte cara, a busca por
        // árvore, tem orçamento próprio dentro de LumberjackWork.
        MinerWork.tick(server.getOverworld());
        SmelterWork.tick(server.getOverworld());
        ShepherdWork.tick(server.getOverworld());
        LumberjackWork.tick(server.getOverworld());
        ManufacturerWork.tick(server.getOverworld());
        BuilderWork.tick(server.getOverworld());

        tickCounter++;

        if (tickCounter < VillageDetector.CYCLE_TICKS) {
            return;
        }

        tickCounter = 0;

        long startedAt = System.nanoTime();

        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                detectAround(world, player.getBlockPos());
            }
        }

        updateLifecycles(server.getOverworld());

        detectFromColonyCenters(server.getOverworld());

        runColonyCycles(server.getOverworld());

        reportIfSlow(startedAt);
    }

    /**
     * Diz quanto custou o ciclo, quando custou caro.
     *
     * <p>Um tick do servidor tem 50 ms. Um ciclo que passe disso já
     * atrasa o jogo, e sem esta linha o jogador sente a lentidão e o log
     * não a explica — foi o que aconteceu duas vezes na Fase 8. É o
     * "instrumentar antes de suspeitar" do §11: medir custa quase nada e
     * transforma palpite em número.
     *
     * <p>Silencioso no caso normal, de propósito.
     */
    private static void reportIfSlow(long startedAt) {
        long millis = (System.nanoTime() - startedAt) / 1_000_000L;

        if (millis < TICK_MILLIS) {
            return;
        }

        VillageColonyMod.LOGGER.warn(
                "Colony cycle took {} ms — longer than a server tick ({} colonies, {} pending chunks)",
                millis,
                VillageColonyMod.COLONIES.count(),
                pending.size());
    }

    /**
     * Ao trocar de mundo, a fila do mundo anterior não pode viajar.
     *
     * <p>Os pares de sobreposição saem junto: são ids de colônias do
     * mundo que ficou para trás, e mantê-los calaria o aviso do mundo
     * seguinte no dia — improvável, mas gratuito — em que um id se
     * repetisse.
     */
    public static void clearPending() {
        pending.clear();
        overlapsReported.clear();
    }

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
    private static void runColonyCycles(ServerWorld overworld) {
        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (!colony.isActive()) {
                continue;
            }

            runCycleOf(overworld, colony);
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
    private static void runCycleOf(ServerWorld overworld, Colony colony) {
        List<UUID> workerIds = new ArrayList<>();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            workerIds.add(worker.villagerId());
        }

        ChestInventoryReader.ChestSurvey survey = ChestInventoryReader.survey(
                overworld, workerIds, VillageColonyMod.STORAGES);

        if (survey.isPartial()) {
            return;
        }

        // A Regra 1: a meta é o que está guardado mais o que ainda cabe.
        // O espaço é medido aqui porque é aqui que os baús existem — o
        // Core não conhece baú, só recebe o número. Ver ColonyGoals.
        int room = ChestDepositor.freeSpaceForGroup(
                overworld, workerIds, VillageColonyMod.STORAGES, ResourceGroup.WOOD);

        // E a Regra 5, a da Fase 9: metade do que os baús comportam em
        // tábua. Medida do mesmo jeito e pelo mesmo motivo.
        int plankRoom = ChestDepositor.freeSpaceForGroup(
                overworld, workerIds, VillageColonyMod.STORAGES, ResourceGroup.PLANKS);

        // A obra é decidida antes de a colônia pensar: o que ela pede
        // entra na conta do mesmo ciclo, e não do seguinte. Planejar
        // depois faria a colônia passar um ciclo inteiro sem saber que
        // tem uma casa para levantar.
        ConstructionPlanner.plan(overworld, colony);

        // A tábua da vila, e não sempre a de carvalho — a Regra 20. A
        // obra de uma colônia de taiga pede pinheiro, e perguntar por
        // carvalho devolveria zero: a meta perderia a demanda da obra e
        // cairia na metade do baú, que é a conta de quando não há obra.
        int planksForWork = ConstructionPlanner.planksNeededBy(
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

        int stoneForWork = ConstructionPlanner.materialNeededBy(palette.stone(), colony);

        int assigned = ColonyCycle.run(
                colony.id(),
                survey.resources().total(),
                ColonyGoals.of(
                        colony, survey.resources().total(), room, plankRoom, planksForWork,
                        stone, stoneForWork, woolWanted(colony)),
                VillageColonyMod.TASKS,
                VillageColonyMod.WORKERS,
                VillageColonyMod.STORAGES::hasStorage);

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

        // Depois da distribuição: quem recebeu tarefa neste ciclo já
        // começa a andar nele, em vez de esperar o próximo.
        LumberjackWork.run(overworld, colony);
        MinerWork.run(overworld, colony);
        SmelterWork.run(overworld, colony);
        ShepherdWork.run(overworld, colony);
        ManufacturerWork.run(overworld, colony);
        BuilderWork.run(overworld, colony);

        // E a mobília das casas já de pé — a Regra 21. Depois do
        // construtor de propósito: a casa que terminou neste ciclo já é
        // olhada nele.
        WOOL_WANTED.put(colony.id(), HouseFurnishing.run(overworld, colony));
    }

    /**
     * Reavalia cada colônia ativa a partir do próprio centro.
     *
     * <p>É a âncora estável que permite encolher. Roda depois de
     * {@link #updateLifecycles} para não varrer colônia dormente, cujos
     * chunks não estão carregados — a varredura não acharia cama alguma
     * e a colônia se veria vazia.
     *
     * <p>Uma consulta de POI por colônia ativa a cada ciclo. O limite de
     * Performance-Rules.md §5 continua respeitado: a busca é por raio em
     * torno de um ponto, nunca pelo mundo.
     */
    private static void detectFromColonyCenters(ServerWorld overworld) {
        List<Colony> active = new ArrayList<>();

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            if (colony.isActive()) {
                active.add(colony);
            }
        }

        for (Colony colony : active) {
            // Antes da varredura: a adoção move centros, e a pergunta do
            // abandono é sobre o que a sonda enxergou de onde ela partiu.
            ColonyPos probedFrom = colony.center();

            VillageScanner.ScanResult result = detectAround(
                    overworld, MinecraftTypeAdapter.toBlockPos(probedFrom), true);

            judgeAbandonment(colony, probedFrom, result);
        }
    }

    /**
     * Marca — ou desmarca — a colônia cuja própria sonda não achou vila.
     *
     * <p>É o único escritor de {@link com.villagecolony.core.colony.model.ColonyState}
     * em produção. Até 2026-08-13 não havia nenhum: o valor
     * {@code ABANDONED} existia no enum, a ADR-003 §6 o exigia, e nada o
     * atribuía.
     *
     * <p>Roda só aqui, dentro da sonda, e só para colônia ACTIVE — as
     * duas condições que separam "a vila acabou" de "ninguém olhou". A
     * regra em si é de {@code ColonyAbandonment}, no Core; o que mora
     * nesta camada é o log e a aplicação.
     *
     * <p>Silencioso quando nada muda, que é sempre. Uma vila viva
     * produziria uma linha a cada ciclo.
     */
    private static void judgeAbandonment(
            Colony colony, ColonyPos probedFrom, VillageScanner.ScanResult result) {

        ColonyAbandonment.judge(colony, probedFrom, result.candidates(), result.ignoredByBiome())
                .ifPresent(state -> {
                    colony.setState(state);

                    if (state == ColonyState.ABANDONED) {
                        VillageColonyMod.LOGGER.warn(
                                "Colony {} is now ABANDONED — probed from {} and found no village ({})",
                                colony.id(),
                                probedFrom,
                                describe(result));
                    } else {
                        VillageColonyMod.LOGGER.info(
                                "Colony {} is inhabited again — now {}", colony.id(), state);
                    }
                });
    }

    /**
     * O que a sonda viu, para a linha do abandono.
     *
     * <p>É o "instrumentar antes de suspeitar" do §11 aplicado a esta
     * regra: sem o motivo, uma colônia marcada como abandonada manda
     * alguém adivinhar entre camas demolidas, aldeões mortos e uma sonda
     * que não achou nada porque o chunk não estava onde se pensava. As
     * três têm correções diferentes.
     */
    private static String describe(VillageScanner.ScanResult result) {
        if (result.rejected().isEmpty()) {
            return "no bed cluster within range at all";
        }

        StringBuilder text = new StringBuilder();

        for (ClusterRejection rejection : result.rejected()) {
            if (text.length() > 0) {
                text.append("; ");
            }

            text.append(rejection.reason())
                    .append(" at ")
                    .append(rejection.center())
                    .append(" — ")
                    .append(rejection.bedCount())
                    .append(" beds, ")
                    .append(rejection.villagersAsText())
                    .append(" villagers");
        }

        return text.toString();
    }

    /**
     * Registra os aldeões da colônia como trabalhadores e dá função a
     * quem não tem.
     *
     * <p>Registro e atribuição são passos separados de propósito: um
     * trabalhador vindo do save já chega com função, e a atribuição não
     * pode desfazê-la. Ver TASK-012b e Worker#assign.
     *
     * <p>A atribuição roda mesmo quando nada foi registrado agora: um
     * save anterior à TASK-012b traz trabalhadores sem função, e eles
     * precisam recebê-la sem depender de um aldeão novo aparecer.
     *
     * <p>Só produz linha de log quando algo muda. Reencontrar os mesmos
     * aldeões a cada ciclo é o caso comum e deve ser silencioso.
     */
    private static void registerVillagers(ServerWorld world, Colony colony) {
        VillagerScanner.ScanResult result = VillagerScanner.scan(
                world, colony, VillageColonyMod.WORKERS, VillageColonyMod.STORAGES);

        if (result.registeredWorkers() > 0) {
            VillageColonyMod.LOGGER.info(
                    "Registered {} villagers in colony {} ({} total)",
                    result.registeredWorkers(),
                    colony.id(),
                    VillageColonyMod.WORKERS.countOfColony(colony.id()));
        }

        if (result.registeredStorages() > 0) {
            VillageColonyMod.LOGGER.info(
                    "Registered {} storages in colony {} ({} total)",
                    result.registeredStorages(),
                    colony.id(),
                    VillageColonyMod.STORAGES.count());

            logResources(world, colony);
        }

        // Antes de atribuir: um save anterior a 2026-08-12 chega com
        // seis lenhadores gravados, e uma regra que só valesse para
        // aldeão novo nunca os desfaria.
        //
        // E é aqui que o trabalhador sem baú perde a vaga para quem
        // consegue um — a atribuição não o alcança, porque ele já tem
        // função.
        // Baús distintos, e não candidatos: dois aldeões do mesmo cômodo
        // enxergam o mesmo baú, e dispensar um trabalhador por candidato
        // trocava a vaga por alguém que também ficaria sem. É a decisão do
        // autor de 2026-08-15 — só se dispensa quando há baú livre de
        // verdade para o substituto. Ver o E11 do §17.
        dismissExtraWorkers(world, colony, result.freeChests().size());

        // A vaga vai primeiro para quem consegue baú: sem isso ela podia
        // ir para uma cama que não alcança baú nenhum, e o trabalhador
        // passava a sessão pegando a tarefa e devolvendo à fila.
        int assigned = ProfessionAssigner.assignMissing(
                VillageColonyMod.WORKERS,
                colony.id(),
                result.employable(),
                result.equippable()::contains);

        if (assigned > 0) {
            VillageColonyMod.LOGGER.info(
                    "Assigned {} professions in colony {}", assigned, colony.id());
        }

        // Depois da atribuição, e não só quando ela muda algo: um
        // trabalhador vindo do save já chega com função e sem nome.
        int labelled = WorkerNameplate.label(
                world, VillageColonyMod.WORKERS.ofColony(colony.id()));

        if (labelled > 0) {
            VillageColonyMod.LOGGER.info(
                    "Named {} workers in colony {}", labelled, colony.id());
        }

        // A ferramenta vem junto do nome, e pelo mesmo motivo: a
        // profissão foi decidida agora, e Profession-System.md diz que o
        // trabalhador a recebe ao assumir a função. Não muda a velocidade
        // do trabalho — a Regra 2 fixou isso em ferro.
        int equipped = WorkerEquipment.equip(
                world, VillageColonyMod.WORKERS.ofColony(colony.id()));

        if (equipped > 0) {
            VillageColonyMod.LOGGER.info(
                    "Equipped {} workers in colony {}", equipped, colony.id());
        }

        // A marca do baú acompanha a profissão, e por isso vem depois
        // dela: um trabalhador que acabou de perder a função não pode
        // deixar o machado pendurado no baú.
        int marked = ChestMarker.mark(
                world,
                VillageColonyMod.WORKERS.ofColony(colony.id()),
                workerId -> VillageColonyMod.STORAGES.of(workerId)
                        .map(WorkerStorage::chestPosition));

        if (marked > 0) {
            VillageColonyMod.LOGGER.info(
                    "Marked {} chests in colony {}", marked, colony.id());
        }
    }

    /**
     * Aposenta quem excede a vaga da profissão.
     *
     * <p>Uma vila tem um trabalhador de cada tipo. Quem perde a função
     * larga o que segurava: a tarefa volta para a fila, o destino é
     * cedido e a árvore em curso é devolvida — senão a tarefa ficaria
     * reservada para quem já não sabe executá-la, e a árvore ficaria
     * marcada para sempre.
     *
     * <p>O baú fica com ele. Recolhê-lo tiraria da colônia a madeira que
     * já está lá dentro, e o aldeão pode voltar a ter função quando o
     * titular morrer.
     */
    private static void dismissExtraWorkers(
            ServerWorld world, Colony colony, int replacements) {

        Set<UUID> demoted = ProfessionAssigner.enforceVacancies(
                VillageColonyMod.WORKERS,
                colony.id(),
                villagerId -> VillageColonyMod.STORAGES.of(villagerId).isPresent(),
                replacements);

        if (demoted.isEmpty()) {
            return;
        }

        int chestless = 0;

        for (UUID villagerId : demoted) {
            if (VillageColonyMod.STORAGES.of(villagerId).isEmpty()) {
                chestless++;
            }
        }

        for (UUID villagerId : demoted) {
            VillageColonyMod.TASKS.releaseAllOf(villagerId);
            WorkTargets.clear(villagerId);
            MinerWork.forget(villagerId);
            SmelterWork.forget(villagerId);
            ShepherdWork.forget(villagerId);
            LumberjackWork.forget(villagerId);
            ManufacturerWork.forget(villagerId);
            BuilderWork.forget(villagerId);

            // A marca do baú sai: um machado pendurado no baú de quem já
            // não é lenhador mente para quem está jogando. E o da mão
            // sai pelo mesmo motivo.
            VillageColonyMod.STORAGES.of(villagerId)
                    .ifPresent(storage -> ChestMarker.unmark(world, storage.chestPosition()));

            WorkerEquipment.unequip(world, villagerId);

            // E o baú volta para a colônia. Segurá-lo prendia o
            // armazenamento a quem não trabalha: a vila do autor tinha
            // treze baús reivindicados e quatro trabalhadores, e o
            // fazendeiro não conseguia nenhum. O conteúdo fica onde
            // está; o que sai é a reserva.
            VillageColonyMod.STORAGES.remove(villagerId);
        }

        VillageColonyMod.LOGGER.info(
                "Colony {} dismissed {} workers ({} of them had no chest and lost the job"
                        + " to someone who can get one) — at most {} of each profession",
                colony.id(),
                demoted.size(),
                chestless,
                ProfessionAssigner.MAX_PER_PROFESSION);
    }

    /**
     * Registra no log o que a colônia tem guardado.
     *
     * <p>Só quando um baú novo entra no registro. O conteúdo muda a cada
     * baú aberto pelo jogador, e logar isso a cada ciclo encheria o
     * arquivo sem dizer nada — mas sem nenhuma linha, a contagem da
     * TASK-017 seria invisível em jogo, e o §11 do Project-State existe
     * justamente porque defeitos desta camada só aparecem lá.
     *
     * <p>Diz quantos baús foram alcançados, e não só quantos tinham algo
     * dentro. A linha antiga contava apenas os não vazios, e assim
     * "nenhum baú tem madeira" e "não consegui ler baú nenhum" saíam com
     * o mesmo texto — o defeito-que-parece-número do V5.
     */
    private static void logResources(ServerWorld world, Colony colony) {
        List<UUID> workerIds = new ArrayList<>();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            workerIds.add(worker.villagerId());
        }

        ChestInventoryReader.ChestSurvey survey = ChestInventoryReader.survey(
                world, workerIds, VillageColonyMod.STORAGES);

        ColonyResources resources = survey.resources();

        VillageColonyMod.LOGGER.info(
                "Colony {} stores {} in {} of {} chests read{}",
                colony.id(),
                resources.isEmpty() ? "nothing tracked" : resources.total().counts(),
                resources.byChest().size(),
                survey.chestsRead(),
                survey.isPartial()
                        ? " (" + survey.chestsUnreachable() + " unreachable, chunk unloaded)"
                        : "");
    }

    /**
     * Acorda e adormece colônias conforme seus chunks.
     *
     * <p>Sem isto, uma colônia visitada uma vez permaneceria
     * {@link ColonyLifecycle#ACTIVE} pelo resto da sessão, mesmo a
     * milhares de blocos do jogador — e o loop de simulação, que só roda
     * para colônias ACTIVE, gastaria tick com vila que ninguém observa.
     *
     * <p>{@code shouldTick} é o critério certo: pergunta se o chunk está
     * de fato sendo tickado, que é a definição de DORMANT na ADR-002.
     *
     * <p>Limite do MVP: consulta apenas o Overworld. Só existem colônias
     * lá, porque o único bioma aceito é PLAINS.
     */
    private static void updateLifecycles(ServerWorld overworld) {
        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            ChunkPos chunk = new ChunkPos(MinecraftTypeAdapter.toBlockPos(colony.center()));

            ColonyLifecycle current = overworld.shouldTick(chunk)
                    ? ColonyLifecycle.ACTIVE
                    : ColonyLifecycle.DORMANT;

            if (colony.lifecycle() == current) {
                continue;
            }

            colony.setLifecycle(current);

            VillageColonyMod.LOGGER.info("Colony {} is now {}", colony.id(), current);
        }
    }

    /**
     * Registra o resultado de cada detecção.
     *
     * <p>Criação e mudança de centro são logadas; reavaliação que não
     * muda nada é silenciosa. Sem isso o log não distingue "a detecção
     * rodou e a vila já era conhecida" de "a detecção nunca rodou" — foi
     * essa cegueira que escondeu o gatilho de chunk quebrado.
     *
     * <p>Não vira spam: o centro só se move quando o conjunto de camas ao
     * alcance muda. Jogador parado não gera linha. Ver CODE-STANDARDS §8.
     */
    private static void detectAround(ServerWorld world, BlockPos trigger) {
        detectAround(world, trigger, false);
    }

    /**
     * @param isProbe se a varredura é a sonda ancorada no centro de uma
     *     colônia, a única cujas leituras se confirmam entre ciclos
     * @return tudo o que a varredura viu, aprovado e recusado. Só a sonda
     *     usa a parte recusada, para decidir abandono
     */
    private static VillageScanner.ScanResult detectAround(
            ServerWorld world, BlockPos trigger, boolean isProbe) {
        // Uma observação por colônia, e não uma por aglomerado de camas.
        // Entre 32 e 64 blocos existe a faixa em que um punhado de camas
        // é outro aglomerado e a mesma colônia: os dois candidatos
        // chegavam com a mesma âncora, e o segundo era confirmado pelo
        // primeiro dentro do mesmo tick. Ver ColonyService#bestPerColony
        // e §17, E2.
        VillageScanner.ScanResult result = SCANNER.survey(world, trigger, isProbe);

        for (VillageCandidate candidate
                : VillageColonyMod.COLONIES.bestPerColony(result.candidates())) {
            int before = VillageColonyMod.COLONIES.count();

            Optional<Colony> known = VillageColonyMod.COLONIES
                    .findNearest(candidate.center(), VillageDetector.DUPLICATE_DISTANCE);

            ColonyPos previousCenter = known.map(Colony::center).orElse(null);

            logRefusedShrink(known, candidate);

            Colony colony = VillageColonyMod.COLONIES.adopt(candidate);

            registerVillagers(world, colony);

            if (VillageColonyMod.COLONIES.count() > before) {
                VillageColonyMod.LOGGER.info(
                        "Colony created at {} with {} beds",
                        colony.center(),
                        candidate.bedCount());
            } else if (previousCenter != null && !colony.center().equals(previousCenter)) {
                VillageColonyMod.LOGGER.info(
                        "Colony {} moved from {} to {} with {} beds",
                        colony.id(),
                        previousCenter,
                        colony.center(),
                        candidate.bedCount());
            }

            warnIfOverlapping(colony);
        }

        return result;
    }

    /**
     * Avisa quando dois centros ficam perto demais.
     *
     * <p>ADR-003 §5, e é a linha que a ADR pede desde 2026-08-06 sem que
     * ninguém a escrevesse. O MVP não funde as duas colônias — fundir
     * exige nova ADR, e a decisão de 2026-08-12 já disse qual será o
     * critério: um bloco de uma encostando no da outra, o que depende da
     * construção existir.
     *
     * <p>Até lá, o que este aviso dá é o nome do problema quando ele
     * aparecer em jogo: duas colônias sobrepostas disputam trabalhador —
     * a vaga de profissão vale por colônia do registro, não por vila
     * física —, e sem esta linha o sintoma seria um aldeão que troca de
     * vila sem motivo aparente. É o risco aberto do §11 do Project-State,
     * que até aqui acontecia em silêncio.
     *
     * <p>Cada par é avisado uma vez por sessão. A sobreposição não se
     * resolve sozinha, e a sonda passa por aqui a cada 600 ticks: sem a
     * memória do par, seriam cem linhas iguais por hora dizendo a mesma
     * coisa.
     */
    private static void warnIfOverlapping(Colony colony) {
        for (Colony other : VillageColonyMod.COLONIES.overlapping(colony)) {
            if (!overlapsReported.add(pairKey(colony.id(), other.id()))) {
                continue;
            }

            VillageColonyMod.LOGGER.warn(
                    "Overlapping colonies detected — {} at {} and {} at {} are {} blocks apart"
                            + " (less than {}); the MVP does not merge them",
                    colony.id(),
                    colony.center(),
                    other.id(),
                    other.center(),
                    (int) Math.sqrt(colony.center().horizontalDistanceSquared(other.center())),
                    VillageDetector.OVERLAP_DISTANCE);
        }
    }

    /**
     * O par, na mesma ordem venha de que lado vier.
     *
     * <p>A sonda de cada uma das duas encontra a outra, e sem a ordem
     * fixa o mesmo par seria contado duas vezes — uma por colônia.
     */
    private static String pairKey(UUID one, UUID other) {
        return one.compareTo(other) <= 0 ? one + "|" + other : other + "|" + one;
    }

    /**
     * Quando uma observação viu menos camas mas não teve autoridade para
     * baixar a contagem.
     *
     * <p>Existe porque em 2026-08-07 camas foram destruídas em jogo e a
     * colônia não encolheu, e o log não sabia dizer se a regra de
     * completude tinha recusado a observação ou se a observação menor
     * nunca tinha chegado. São causas diferentes com correções
     * diferentes.
     *
     * <p>É o "instrumentar antes de suspeitar" do §11: a linha que expõe
     * o caso precisa existir antes de alguém desconfiar dele.
     *
     * <p>Não vira spam por si: só sai quando a contagem observada está
     * abaixo da registrada, que é justamente o caso raro.
     *
     * <p>Diz onde o candidato estava e de onde a varredura partiu. Sem
     * isso a linha é um número sem lugar, e foi essa cegueira que
     * escondeu o E2 por três dias: "viu 5 de 31" parecia sonda com
     * defeito e era, o tempo todo, um segundo aglomerado de camas a
     * quarenta blocos. Com o centro na linha, dois aglomerados
     * diferentes se distinguem de imediato de uma leitura pobre do
     * mesmo.
     */
    private static void logRefusedShrink(Optional<Colony> known, VillageCandidate candidate) {
        known.ifPresent(colony -> {
            if (candidate.bedCount() >= colony.observedBeds() || candidate.complete()) {
                return;
            }

            VillageColonyMod.LOGGER.info(
                    "Colony {} saw {} beds at {} from anchor {}, keeping {}"
                            + " — view not provably complete",
                    colony.id(),
                    candidate.bedCount(),
                    candidate.center(),
                    candidate.anchor() == null ? "none" : candidate.anchor(),
                    colony.observedBeds());
        });
    }
}
