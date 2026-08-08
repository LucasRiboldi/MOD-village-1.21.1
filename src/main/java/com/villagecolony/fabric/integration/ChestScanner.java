package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Map;
import java.util.Optional;

/**
 * Encontra o baú de um trabalhador a partir da sua cama.
 *
 * <p>Percorre o caminho de Storage-System.md §"Registro de
 * Armazenamento": aldeão, casa, cama, baú próximo. A cama é a casa —
 * é ela que o Vanilla guarda como {@code MemoryModuleType.HOME}, e é o
 * mesmo POI que a ADR-003 usa para achar a vila.
 *
 * <p>Não cria nada. O baú tem de existir na casa; aqui só se anota onde
 * ele está. Ver Storage-System.md §"Criação do Baú".
 */
public final class ChestScanner {

    /**
     * Distância máxima entre a cama e o baú dela, em blocos.
     *
     * <p>Seis cobre o quarto e não a casa vizinha. Maior que isso e o
     * baú da cozinha comum viraria propriedade de quem dorme mais perto;
     * menor, e um quarto de canto ficaria sem baú alcançável.
     */
    private static final int SEARCH_RADIUS = 6;

    private ChestScanner() {
    }

    /**
     * Procura um baú livre perto da cama do aldeão e o registra.
     *
     * <p>Não faz nada quando o trabalhador já tem baú: rever o mesmo
     * aldeão a cada ciclo é o caso comum, e reabrir a busca custaria uma
     * varredura de blocos por aldeão por ciclo, contra
     * Performance-Rules.md §6.
     *
     * @return o registro criado, ou vazio quando o aldeão não tem cama,
     *     a cama está noutra dimensão, ou não há baú livre por perto
     */
    public static Optional<WorkerStorage> scan(
            ServerWorld world, VillagerEntity villager, StorageRegistry storages) {

        if (storages.hasStorage(villager.getUuid())) {
            return Optional.empty();
        }

        Optional<GlobalPos> home =
                villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME);

        if (home.isEmpty()) {
            return Optional.empty();
        }

        // Um aldeão do Nether com cama no Overworld não teria baú
        // alcançável, e ler blocos de outra dimensão pela referência
        // deste mundo daria a posição errada, não um erro.
        if (!home.get().dimension().equals(world.getRegistryKey())) {
            return Optional.empty();
        }

        Optional<BlockPos> chest = findFreeChest(world, home.get().pos(), storages);

        if (chest.isEmpty()) {
            return Optional.empty();
        }

        WorkerStorage storage = WorkerStorage.of(
                villager.getUuid(), MinecraftTypeAdapter.toColonyPos(chest.get()));

        storages.register(storage);

        logClaim(villager, home.get().pos(), chest.get());

        return Optional.of(storage);
    }

    /**
     * Registra qual baú ficou com qual aldeão, e a que distância da cama.
     *
     * <p>Existe porque o V4 do §7 — "cada aldeão pegou o baú da sua casa,
     * não o do vizinho" — era impossível de verificar. O log dizia
     * quantos baús foram registrados e nada mais: nem qual baú, nem de
     * quem, nem onde. Não havia como o autor conferir em jogo, e um
     * defeito aqui aparece depois como estoque plausível na contagem da
     * TASK-017.
     *
     * <p>Uma linha por baú reivindicado. Não vira spam: o baú de um
     * aldeão é procurado uma vez e nunca mais, enquanto ele o tiver.
     *
     * <p>As coordenadas são as de ir até lá e abrir o baú — é essa a
     * verificação que a linha existe para permitir.
     */
    private static void logClaim(VillagerEntity villager, BlockPos bed, BlockPos chest) {
        VillageColonyMod.LOGGER.info(
                "Storage claimed by {}: bed {} chest {} ({} blocks apart)",
                villager.getUuid(),
                bed.toShortString(),
                chest.toShortString(),
                String.format("%.1f", Math.sqrt(bed.getSquaredDistance(chest))));
    }

    /**
     * O baú livre mais próximo da cama.
     *
     * <p>Parte das block entities dos chunks vizinhos, e não das posições
     * do cubo de busca: percorrer o cubo custaria mais de dois mil
     * {@code getBlockEntity} por aldeão sem baú, a cada ciclo, contra
     * Performance-Rules.md §6. Uma casa tem um punhado de block entities,
     * e é sobre esse punhado que se itera.
     *
     * <p>Chunk não carregado é pulado sem forçar carregamento — ADR-002
     * §"o mod não segura chunk". Um baú lá não é encontrado agora e será
     * no ciclo em que o chunk estiver carregado.
     *
     * <p>Um baú já reivindicado é pulado: dois aldeões do mesmo cômodo
     * partilhando um baú fariam cada um contar o estoque do outro como
     * seu. Ver Storage-System.md §"Proteção".
     */
    private static Optional<BlockPos> findFreeChest(
            ServerWorld world, BlockPos bed, StorageRegistry storages) {

        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        int minChunkX = (bed.getX() - SEARCH_RADIUS) >> 4;
        int maxChunkX = (bed.getX() + SEARCH_RADIUS) >> 4;
        int minChunkZ = (bed.getZ() - SEARCH_RADIUS) >> 4;
        int maxChunkZ = (bed.getZ() + SEARCH_RADIUS) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {

                WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ);

                if (chunk == null) {
                    continue;
                }

                for (Map.Entry<BlockPos, BlockEntity> entry
                        : chunk.getBlockEntities().entrySet()) {

                    if (!(entry.getValue() instanceof ChestBlockEntity)) {
                        continue;
                    }

                    BlockPos pos = entry.getKey();

                    if (!isWithinRadius(bed, pos)) {
                        continue;
                    }

                    double distance = bed.getSquaredDistance(pos);

                    if (distance >= nearestDistance) {
                        continue;
                    }

                    if (storages.isTaken(MinecraftTypeAdapter.toColonyPos(pos))) {
                        continue;
                    }

                    nearest = pos;
                    nearestDistance = distance;
                }
            }
        }

        return Optional.ofNullable(nearest);
    }

    /**
     * Cubo, e não esfera: o baú no canto do quarto está tão dentro da
     * casa quanto o encostado na cama, e medir por distância euclidiana
     * o deixaria de fora sem motivo visível para quem construiu.
     */
    private static boolean isWithinRadius(BlockPos bed, BlockPos pos) {
        return Math.abs(pos.getX() - bed.getX()) <= SEARCH_RADIUS
                && Math.abs(pos.getY() - bed.getY()) <= SEARCH_RADIUS
                && Math.abs(pos.getZ() - bed.getZ()) <= SEARCH_RADIUS;
    }
}
