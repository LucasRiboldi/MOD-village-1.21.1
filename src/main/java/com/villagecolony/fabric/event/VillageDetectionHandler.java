package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.resource.model.ColonyResources;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.ProfessionAssigner;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageScanner;
import com.villagecolony.fabric.integration.VillagerScanner;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.poi.PointOfInterestStorage;

import java.util.ArrayList;
import java.util.List;
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

    private static int tickCounter;

    private VillageDetectionHandler() {
    }

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register(VillageDetectionHandler::onChunkLoad);
        ServerTickEvents.END_SERVER_TICK.register(VillageDetectionHandler::onServerTick);
    }

    /**
     * Chunk carregado.
     *
     * <p>A checagem barata vem primeiro: sem cama neste chunk, não há
     * motivo para pagar a busca de raio 64. A esmagadora maioria dos
     * chunks carregados cai fora aqui.
     *
     * <p>O gatilho é a posição da própria cama encontrada, não o canto do
     * chunk. {@code ChunkPos.getStartPos()} devolve y=0, e
     * {@code getInCircle} mede distância em três dimensões: partindo de
     * y=0, uma cama em y=64 já consome todo o raio de busca antes de
     * qualquer deslocamento horizontal. Ancorado no chunk, este gatilho
     * não encontrava vila nenhuma.
     */
    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        world.getPointOfInterestStorage()
                .getInChunk(
                        poi -> poi.matchesKey(PointOfInterestTypes.HOME),
                        chunk.getPos(),
                        PointOfInterestStorage.OccupationStatus.ANY)
                .findFirst()
                .ifPresent(bed -> detectAround(world, bed.getPos()));
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
        tickCounter++;

        if (tickCounter < VillageDetector.CYCLE_TICKS) {
            return;
        }

        tickCounter = 0;

        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                detectAround(world, player.getBlockPos());
            }
        }

        updateLifecycles(server.getOverworld());

        detectFromColonyCenters(server.getOverworld());
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
            detectAround(overworld, MinecraftTypeAdapter.toBlockPos(center));
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
        for (VillageCandidate candidate : SCANNER.scan(world, trigger)) {
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
