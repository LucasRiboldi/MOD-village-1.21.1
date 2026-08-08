package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.coordination.ColonyCycle;
import com.villagecolony.core.coordination.ColonyGoals;
import com.villagecolony.core.resource.model.ColonyResources;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.ProfessionAssigner;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageScanner;
import com.villagecolony.fabric.integration.VillagerScanner;
import com.villagecolony.fabric.integration.WorkerNameplate;
import com.villagecolony.fabric.work.LumberjackWork;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.poi.PointOfInterestStorage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /** Ao trocar de mundo, a fila do mundo anterior não pode viajar. */
    public static void clearPending() {
        pending.clear();
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

        int assigned = ColonyCycle.run(
                colony.id(),
                survey.resources().total(),
                ColonyGoals.of(colony),
                VillageColonyMod.TASKS,
                VillageColonyMod.WORKERS);

        if (assigned > 0) {
            VillageColonyMod.LOGGER.info(
                    "Colony {} assigned {} tasks ({} open)",
                    colony.id(),
                    assigned,
                    VillageColonyMod.TASKS.availableFor(colony.id()).size());
        }

        // Depois da distribuição: quem recebeu tarefa neste ciclo já
        // começa a andar nele, em vez de esperar o próximo.
        LumberjackWork.run(overworld, colony);
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
        List<ColonyPos> centers = new ArrayList<>();

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            if (colony.isActive()) {
                centers.add(colony.center());
            }
        }

        for (ColonyPos center : centers) {
            detectAround(overworld, MinecraftTypeAdapter.toBlockPos(center), true);
        }
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

        int assigned = ProfessionAssigner.assignMissing(
                VillageColonyMod.WORKERS, colony.id(), result.employable());

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
     */
    private static void detectAround(ServerWorld world, BlockPos trigger, boolean isProbe) {
        for (VillageCandidate candidate : SCANNER.scan(world, trigger, isProbe)) {
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
        }
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
     */
    private static void logRefusedShrink(Optional<Colony> known, VillageCandidate candidate) {
        known.ifPresent(colony -> {
            if (candidate.bedCount() >= colony.observedBeds() || candidate.complete()) {
                return;
            }

            VillageColonyMod.LOGGER.info(
                    "Colony {} saw {} beds, keeping {} — view not provably complete",
                    colony.id(),
                    candidate.bedCount(),
                    colony.observedBeds());
        });
    }
}
