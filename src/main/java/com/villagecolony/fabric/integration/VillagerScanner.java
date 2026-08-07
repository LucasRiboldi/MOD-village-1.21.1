package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.core.worker.service.WorkerService;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Registra como trabalhadores os aldeões vivos de uma colônia.
 *
 * <p>Detecta aldeões dentro de uma colônia — não confundir com
 * {@link VillageScanner}, que detecta vilas. A ADR-003 §7 exige que os
 * dois nomes não se misturem.
 *
 * <p>Nunca busca aldeões no mundo inteiro: a caixa parte do centro da
 * colônia, conforme Performance-Rules.md §5.
 */
public final class VillagerScanner {

    private VillagerScanner() {
    }

    /**
     * Varre os arredores do centro da colônia e registra quem encontrar.
     *
     * <p>Usa o mesmo raio da detecção de vila: é o alcance que a ADR-003
     * já define como "os arredores desta vila", e inventar um segundo
     * número aqui só criaria duas noções de perto.
     *
     * <p>Aldeões bebês são registrados. Eles crescem, e a colônia perde
     * menos ao já conhecê-los do que ao redescobri-los depois.
     *
     * <p>O baú de cada aldeão é procurado na mesma passagem: são os
     * mesmos aldeões, e uma segunda consulta de entidades por ciclo só
     * para isso seria desperdício. Quem já tem baú não é revisitado.
     *
     * @return quantos trabalhadores e quantos baús foram registrados
     *     agora — zero e zero quando nada mudou
     */
    public static ScanResult scan(
            ServerWorld world,
            Colony colony,
            WorkerService workers,
            StorageRegistry storages) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());

        Box area = Box.of(
                center.toCenterPos(),
                VillageDetector.SEARCH_RADIUS * 2.0,
                VillageDetector.SEARCH_RADIUS * 2.0,
                VillageDetector.SEARCH_RADIUS * 2.0);

        int registered = 0;
        int storagesFound = 0;

        for (VillagerEntity villager
                : world.getEntitiesByClass(VillagerEntity.class, area, VillagerEntity::isAlive)) {

            if (!workers.isRegistered(villager.getUuid())) {
                workers.register(villager.getUuid(), colony.id());
                registered++;
            }

            // Fora do if: o aldeão pode já ser conhecido e ainda não ter
            // baú, seja porque o jogador o construiu depois, seja porque
            // o chunk dele não estava carregado no ciclo anterior.
            if (ChestScanner.scan(world, villager, storages).isPresent()) {
                storagesFound++;
            }
        }

        return new ScanResult(registered, storagesFound);
    }

    /** O que uma varredura mudou. Zero em ambos é o caso comum. */
    public record ScanResult(int registeredWorkers, int registeredStorages) {

        public boolean changedNothing() {
            return registeredWorkers == 0 && registeredStorages == 0;
        }
    }
}
