package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.colony.service.VillageDetector;
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
     */
    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        boolean hasBed = world.getPointOfInterestStorage()
                .getInChunk(
                        poi -> poi.matchesKey(PointOfInterestTypes.HOME),
                        chunk.getPos(),
                        PointOfInterestStorage.OccupationStatus.ANY)
                .findAny()
                .isPresent();

        if (!hasBed) {
            return;
        }

        detectAround(world, chunk.getPos().getStartPos());
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

    private static void detectAround(ServerWorld world, BlockPos trigger) {
        for (VillageCandidate candidate : SCANNER.scan(world, trigger)) {
            int before = VillageColonyMod.COLONIES.count();

            Colony colony = VillageColonyMod.COLONIES.adopt(candidate);

            if (VillageColonyMod.COLONIES.count() > before) {
                VillageColonyMod.LOGGER.info(
                        "Colony created at {} with {} beds",
                        colony.center(),
                        candidate.bedCount());
            }
        }
    }
}
