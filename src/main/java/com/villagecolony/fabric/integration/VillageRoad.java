package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ResourceId;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

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

    /**
     * Se há calçamento colado na caixa desta obra — a um ou dois blocos das
     * bordas, na altura do piso. Sessão longa de 2026-09-26: o planejador pôs a
     * casa encostada numa rua, o índice de ruas ainda não estava montado, e o
     * guarda de alcance mediu do centro e largou a obra um minuto depois. A
     * rua que o lote encosta é a resposta que o índice daria.
     */
    public static boolean besidePaving(ServerWorld world, BlockPos min, int sizeX, int sizeY, int sizeZ) {
        for (int x = min.getX() - 2; x <= min.getX() + sizeX + 1; x++) {
            for (int z = min.getZ() - 2; z <= min.getZ() + sizeZ + 1; z++) {
                boolean inside = x >= min.getX() && x < min.getX() + sizeX
                        && z >= min.getZ() && z < min.getZ() + sizeZ;

                if (inside || !world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }

                for (int y = min.getY() - 2; y <= min.getY() + 3; y++) {
                    if (isPaving(world, world.getBlockState(new BlockPos(x, y, z)))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** Nao ha catalogo transitivo para limpar, mantido como gancho de ciclo de vida. */
    public static void clearAll() {
    }
}
