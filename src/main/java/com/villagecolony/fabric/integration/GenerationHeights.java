package com.villagecolony.fabric.integration;

import net.minecraft.block.Blocks;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

/**
 * As alturas de geração da coluna que o solo da vila muda (Regra 250). A rua do Vanilla ({@code TERRAIN_MATCHING}) é
 * posta por {@code WORLD_SURFACE_WG}, e o chunk em geração não acompanha essas alturas nos blocos que mudam: sem isto,
 * a rua nasce na altura de antes do corte, no ar, ou enterrada no aterro.
 */
final class GenerationHeights {

    /** As alturas que a geração lê antes das features — a rua e as outras peças que seguem o chão. */
    private static final List<Heightmap.Type> TYPES =
            List.of(Heightmap.Type.WORLD_SURFACE_WG, Heightmap.Type.OCEAN_FLOOR_WG);

    private GenerationHeights() {
    }

    /**
     * Relê a altura de geração da coluna {@code x}, {@code z} a partir dos blocos dela. O registro sobe até
     * {@code ceiling}, acima de tudo o que a coluna pode ter, e desce lendo os blocos de verdade — o único jeito de
     * baixá-lo pela interface do {@link Heightmap}.
     */
    static void retrack(Chunk chunk, int x, int z, int ceiling) {
        int lx = x & 15;
        int lz = z & 15;

        for (Heightmap.Type type : TYPES) {
            Heightmap heights = chunk.getHeightmap(type);
            int above = Math.min(Math.max(heights.get(lx, lz), ceiling), chunk.getTopY() - 1);

            heights.trackUpdate(lx, above, lz, Blocks.STONE.getDefaultState());
            heights.trackUpdate(lx, above, lz, Blocks.AIR.getDefaultState());
        }
    }
}
