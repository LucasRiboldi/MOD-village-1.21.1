package com.villagecolony.fabric.event;

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
import com.villagecolony.core.type.ServerMemory;
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

    static {
        ServerMemory.register(VillageDetectionHandler.class, VillageDetectionHandler::clearPending);
    }

    static final VillageScanner SCANNER = new VillageScanner();

    /**
     * Quantos gatilhos de chunk cabem esperando.
     *
     * <p>Existe para que abrir o mundo não guarde uma varredura por
     * chunk carregado. O ciclo longo cobre o que passar do teto.
     */
    static final int PENDING_LIMIT = 256;

    /** Duração de um tick do servidor, em milissegundos. */
    static final int TICK_MILLIS = 50;

    /**
     * Até onde uma colônia trabalha — decisão do autor, 2026-09-15.
     *
     * <p>A frase dele: <i>"não trabalhar nas vilas que o jogador não está
     * perto"</i>. O log de 23:41 mostrou <b>seis colônias</b> reportando
     * atividade no mesmo período, e o jogador estava numa.
     *
     * <p><b>O dobro do raio da vila</b>, e a folga é o ponto. Com os 64 de
     * {@code SEARCH_RADIUS} a colônia congelaria assim que o autor
     * caminhasse para a borda dela ou descesse à mina — e o centro da vila
     * oscila entre oito posições, como a sessão de 21:50 mediu, de modo que
     * a régua justa ficaria piscando.
     *
     * <p>Horizontal, como todo raio deste projeto: o jogador no fundo da
     * mina continua sendo o jogador daquela vila.
     */
    static final int WORKING_DISTANCE = 2 * VillageDetector.SEARCH_RADIUS;

    /**
     * Chunks com cama esperando varredura, um por chunk.
     *
     * <p>{@code LinkedHashMap} para drenar na ordem em que chegaram: os
     * primeiros chunks a carregar são os mais perto do jogador.
     */
    static final Map<ChunkPos, BlockPos> pending = new LinkedHashMap<>();

    /**
     * Pares de colônias sobrepostas já avisados nesta sessão.
     *
     * <p>Ver {@link VillageAdoption#warnIfOverlapping}: a sobreposição não se resolve
     * sozinha, e sem esta memória o aviso sairia a cada ciclo.
     */
    static final Set<String> overlapsReported = new HashSet<>();

    static int tickCounter;

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
        VillageAdoption.detectAround(world, trigger);

        VillageAdoption.updateLifecycles(world);

        VillageAdoption.detectFromColonyCenters(world);

        // <b>Sem o filtro de proximidade</b> — 2026-09-15. O gametest não
        // tem jogador no mundo, e a regra de "só trabalha perto de alguém"
        // pararia toda colônia de teste em silêncio: três casos de ciclo
        // caíram assim, dizendo "abriu 0 tarefas".
        //
        // Isto NÃO é um atalho que esconde a regra. Esta porta existe para
        // o teste poder rodar um ciclo sem esperar trinta segundos, e o
        // ponto dela é exercitar o que a colônia DECIDE. Quem afirma a
        // regra de proximidade é o caminho de produção, em onServerTick.
        ColonyCycleRunner.runColonyCycles(world, false);
    }

    /**
     * Executa somente a decisão de estoque das colônias para GameTests.
     *
     * <p>Não faz detecção, fundação nem atualização de lifecycle: esses
     * passos pertencem ao ciclo completo e podem invalidar um fixture que
     * está testando apenas o planejador de recursos.
     */
    public static void runColonyCycleNow(ServerWorld world) {
        ColonyCycleRunner.runColonyCycles(world, false);
    }

    /**
     * Executa a garantia de fundação para uma colônia já conhecida.
     *
     * <p>É a mesma sequência usada por {@link VillageAdoption#detectAround}: registra os
     * aldeões existentes, completa a população física e repete o registro
     * para atribuir profissões e reivindicar os baús. A entrada explícita
     * da colônia mantém o teste de contrato isolado de outras arenas do
     * servidor de GameTest; em produção, {@code detectAround} é quem chama
     * esta sequência.
     */
    public static void runFoundationNow(ServerWorld world, Colony colony) {
        BigHouseFoundation.Result house = BigHouseFoundation.ensure(world, colony);
        VillagerRegistration.registerVillagers(world, colony, colony.center());

        VillageFoundation.Result foundation = VillageFoundation.ensure(
                world, colony, colony.center(), VillageColonyMod.WORKERS, house.placed());

        if (foundation.changed()) {
            VillagerRegistration.registerVillagers(world, colony, colony.center());
            VillagerRegistration.registerVillagers(world, colony, colony.center());
        }
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
    static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
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
    static void drainOnePending(ServerWorld overworld) {
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

            VillageAdoption.detectAround(overworld, bed);

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
    static void onServerTick(net.minecraft.server.MinecraftServer server) {
        drainOnePending(server.getOverworld());

        // A Regra 2 mora aqui: o lenhador quebra um bloco de cada vez, no
        // tempo que um jogador com machado de ferro levaria, e para isso
        // precisa de um passo por tick — não de um passo a cada 600. O
        // custo é um contador por lenhador; a parte cara, a busca por
        // árvore, tem orçamento próprio dentro de LumberjackWork.
        MinerWork.tick(server.getOverworld());
        SmelterWork.tick(server.getOverworld());
        SurfaceGatheringWork.tick(server.getOverworld());
        ShepherdWork.tick(server.getOverworld());
        FarmerWork.tick(server.getOverworld());
        LumberjackWork.tick(server.getOverworld());
        CraftingWork.tick(server.getOverworld());
        BuilderWork.tick(server.getOverworld());

        // Quem ficou preso cava a própria saída — E47, 2026-09-24. Uma
        // passagem por segundo, só para os encalhados; ver StrandedEscape.
        StrandedEscape.tick(server.getOverworld());

        // A comida do fim do expediente, para a vila crescer por
        // procriação — N1, 2026-09-24; ver VillageMeals.
        VillageMeals.tick(server.getOverworld());

        // O contorno do lote escolhido — 2026-09-15, pedido do autor. Sai
        // de graça em 19 de cada 20 tiques; ver SiteMarker.EVERY_TICKS.
        SiteMarker.tick(server.getOverworld());

        tickCounter++;

        if (tickCounter < VillageDetector.CYCLE_TICKS) {
            return;
        }

        tickCounter = 0;

        long startedAt = System.nanoTime();

        // O bastão do P2.1: cada fase cobra o próprio trecho, e o que
        // nenhuma cobrar sai como `other`. Ver CycleCost.
        CycleCost.startOver();

        long mark = startedAt;

        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                VillageAdoption.detectAround(world, player.getBlockPos());
            }
        }

        mark = CycleCost.since(CycleCost.Phase.DETECT, mark);

        VillageAdoption.updateLifecycles(server.getOverworld());

        mark = CycleCost.since(CycleCost.Phase.LIFECYCLE, mark);

        VillageAdoption.detectFromColonyCenters(
                server.getOverworld(), colony -> VillageFocus.isAnalyzed(server.getOverworld(), colony));

        CycleCost.since(CycleCost.Phase.DETECT, mark);

        // As fases de dentro se cobram sozinhas, em ColonyCycleRunner.runCycleOf.
        ColonyCycleRunner.runColonyCycles(server.getOverworld(), true);

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
     *
     * <p><b>E desde 2026-09-11 ela diz onde</b> — P2.1. Dizer só o total
     * é meia notícia: o ciclo de 112 ms da sessão de 09-04 mandou abrir
     * uma frente de performance sem que nada no log apontasse o culpado,
     * e otimizar por palpite é o que o §11 existe para impedir. A
     * repartição sai da fase mais cara para a mais barata, então o
     * primeiro nome da linha é por onde começar. Ver {@link CycleCost}.
     */
    static void reportIfSlow(long startedAt) {
        long elapsed = System.nanoTime() - startedAt;

        long millis = elapsed / 1_000_000L;

        if (millis < TICK_MILLIS) {
            return;
        }

        VillageColonyMod.LOGGER.warn(
                "Colony cycle took {} ms — longer than a server tick"
                        + " ({} colonies, {} pending chunks) — {}",
                millis,
                VillageColonyMod.COLONIES.count(),
                pending.size(),
                CycleCost.breakdown(elapsed));
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
        ColonyCycleRunner.lastStock.clear();
    }

}
