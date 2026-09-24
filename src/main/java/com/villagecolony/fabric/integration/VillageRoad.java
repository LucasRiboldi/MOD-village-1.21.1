package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ResourceId;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;
import java.util.Set;

/**
 * Materiais oficiais de pavimentacao da colonia.
 *
 * <p>P0.7 fixa o conjunto de materiais da estrada. O bloco, sozinho, nao
 * identifica uma rua: {@link BuildSiteScanner} tambem exige que a coluna
 * esteja na area espacial reservada pela colonia.
 */
public final class VillageRoad {

    static {
        ServerMemory.register(VillageRoad.class, VillageRoad::clearAll);
    }

    private static final ResourceId DEFAULT_PAVING = VillagePalette.DIRT_PATH;

    private static final Set<Block> ROAD_MATERIALS = Set.of(
            Blocks.DIRT_PATH,
            Blocks.GRAVEL,
            Blocks.TERRACOTTA);

    private VillageRoad() {
    }

    /** O material que a colonia usa ao criar um novo trecho de rua. */
    public static Optional<ResourceId> pavingFor(ServerWorld world, String style) {
        return Optional.of(DEFAULT_PAVING);
    }

    /** Se o bloco pertence ao conjunto oficial de materiais de rua. */
    public static boolean isPaving(ServerWorld world, BlockState state) {
        return ROAD_MATERIALS.contains(state.getBlock());
    }

    /** Nao ha catalogo transitivo para limpar, mantido como gancho de ciclo de vida. */
    public static void clearAll() {
    }
}
