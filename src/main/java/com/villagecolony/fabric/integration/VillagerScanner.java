package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
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
     * @return quantos trabalhadores foram registrados agora — zero quando
     *     todos já eram conhecidos
     */
    public static int scan(ServerWorld world, Colony colony, WorkerService workers) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());

        Box area = Box.of(
                center.toCenterPos(),
                VillageDetector.SEARCH_RADIUS * 2.0,
                VillageDetector.SEARCH_RADIUS * 2.0,
                VillageDetector.SEARCH_RADIUS * 2.0);

        int registered = 0;

        for (VillagerEntity villager
                : world.getEntitiesByClass(VillagerEntity.class, area, VillagerEntity::isAlive)) {

            if (workers.isRegistered(villager.getUuid())) {
                continue;
            }

            workers.register(villager.getUuid(), colony.id());
            registered++;
        }

        return registered;
    }
}
