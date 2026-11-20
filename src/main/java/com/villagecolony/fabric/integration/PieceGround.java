package com.villagecolony.fabric.integration;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;

/**
 * A coluna da peça fixa da vila (casa, poste, enfeite) na geração do chunk, antes de a peça ser posta: livre acima do
 * piso (Regra 337) e com chão até o piso embaixo (Regras 288 e 304). A peça desce ou sobe ao solo suavizado (Regra 296);
 * na montanha ela desce dentro do morro, e o que ficava acima do piso sobrava solto sobre ela.
 */
final class PieceGround {

    private PieceGround() {
    }

    /**
     * A base da peça fixa: tudo acima do piso sai (Regra 337); o chão sobre água (Regra 288) e o chão seco abaixo do
     * piso (Regra 304) sobem até o piso.
     */
    static void found(StructureWorldAccess world, Chunk chunk, int x, int z, int top, int floor, VillageSoil soil) {
        if (top > floor || surfaceOf(chunk, x, z) > floor) {
            clearAbove(world, chunk, x, z, Math.max(top, floor), floor);
            top = VillageGround.topOf(chunk, x, z, floor);
        }

        if (VillageSoil.isWet(chunk, x, z, top) || top < floor) {
            soil.fill(chunk, x, z, top, floor);
        }
    }

    /**
     * Regra 337: a coluna fica livre acima do piso — pedra, terra, caminho, líquido, árvore, qualquer bloco — do mais
     * alto do mapa de altura da geração (mais {@link VillageGround#SCAN_ABOVE}) até o bloco de cima do piso. A árvore
     * com tronco na coluna sai inteira.
     */
    static void clearAbove(StructureWorldAccess world, Chunk chunk, int x, int z, int top, int floor) {
        int ceiling = Math.min(Math.max(top, surfaceOf(chunk, x, z)) + VillageGround.SCAN_ABOVE, chunk.getTopY() - 1);
        BlockPos.Mutable at = new BlockPos.Mutable(x, ceiling, z);

        for (int y = ceiling; y > floor; y--) {
            BlockState state = chunk.getBlockState(at.setY(y));

            if (state.isIn(BlockTags.LOGS)) {
                VillageGround.removeTree(world, at.toImmutable());
            }

            if (!chunk.getBlockState(at).isAir()) {
                chunk.setBlockState(at, Blocks.AIR.getDefaultState(), false);
            }
        }

        GenerationHeights.retrack(chunk, x, z, ceiling);
    }

    /** O bloco mais alto da coluna no mapa de altura da geração. */
    private static int surfaceOf(Chunk chunk, int x, int z) {
        return chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, x & 15, z & 15) - 1;
    }
}
