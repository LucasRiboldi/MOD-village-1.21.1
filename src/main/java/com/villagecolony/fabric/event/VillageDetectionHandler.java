package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.VillageScanner;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
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

            ColonyPos previousCenter = VillageColonyMod.COLONIES
                    .findNearest(candidate.center(), VillageDetector.DUPLICATE_DISTANCE)
                    .map(Colony::center)
                    .orElse(null);

            Colony colony = VillageColonyMod.COLONIES.adopt(candidate);

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
}
