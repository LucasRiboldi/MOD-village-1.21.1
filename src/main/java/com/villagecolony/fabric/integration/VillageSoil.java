package com.villagecolony.fabric.integration;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * O chão em que a água da vila vira na geração — Regra 288: o topo e o subsolo secos mais comuns do próprio chunk,
 * para o lago aterrado ficar igual ao chão em volta. Sem coluna seca no chunk, o chão do bioma (Regra 363).
 */
record VillageSoil(BlockState top, BlockState under) {

    static final VillageSoil GRASS = new VillageSoil(Blocks.GRASS_BLOCK.getDefaultState(),
            Blocks.DIRT.getDefaultState());

    /** O par mais comum entre as colunas secas do chunk; no empate, o primeiro achado, para a geração se repetir. */
    static VillageSoil of(Chunk chunk) {
        Map<VillageSoil, Integer> counts = new LinkedHashMap<>();
        BlockPos.Mutable at = new BlockPos.Mutable();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = chunk.getPos().getStartX() + lx;
                int z = chunk.getPos().getStartZ() + lz;
                int top = VillageGround.topOf(chunk, x, z,
                        chunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR_WG, lx, lz) + VillageGround.SCAN_ABOVE);
                BlockState surface = chunk.getBlockState(at.set(x, top, z));
                BlockState below = chunk.getBlockState(at.set(x, top - 1, z));

                if (surface.getFluidState().isEmpty() && chunk.getFluidState(at.set(x, top + 1, z)).isEmpty()
                        && SiteSurroundings.isNaturalGround(surface) && SiteSurroundings.isNaturalGround(below)) {
                    counts.merge(new VillageSoil(surface, below), 1, Integer::sum);
                }
            }
        }

        VillageSoil most = fallback(chunk);
        int best = 0;

        for (Map.Entry<VillageSoil, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > best) {
                most = entry.getKey();
                best = entry.getValue();
            }
        }

        return most;
    }

    /** Sem coluna seca no chunk: o chão do bioma — areia sobre arenito no deserto, areia vermelha na terra vermelha (Regra 363). */
    static VillageSoil fallback(Chunk chunk) {
        net.minecraft.registry.entry.RegistryEntry<net.minecraft.world.biome.Biome> biome = chunk.getBiomeForNoiseGen(
                net.minecraft.world.biome.source.BiomeCoords.fromBlock(chunk.getPos().getCenterX()),
                net.minecraft.world.biome.source.BiomeCoords.fromBlock(63),
                net.minecraft.world.biome.source.BiomeCoords.fromBlock(chunk.getPos().getCenterZ()));

        if (biome.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_DESERT)) {
            return new VillageSoil(Blocks.SAND.getDefaultState(), Blocks.SANDSTONE.getDefaultState());
        }

        if (biome.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_BADLANDS)) {
            return new VillageSoil(Blocks.RED_SAND.getDefaultState(), Blocks.RED_SANDSTONE.getDefaultState());
        }

        return GRASS;
    }

    /** Se a coluna tem água no bloco {@code y} do topo ou logo acima dele. */
    static boolean isWet(Chunk chunk, int x, int z, int y) {
        return !chunk.getFluidState(new BlockPos(x, y, z)).isEmpty()
                || !chunk.getFluidState(new BlockPos(x, y + 1, z)).isEmpty();
    }

    /**
     * Regra 288: a água da coluna vira chão do leito até a superfície dela, ou até {@code target} quando a vila quer
     * mais alto, com este subsolo e este topo. Só troca água, ar e o que se substitui (alga, vitória-régia).
     */
    void fill(Chunk chunk, int x, int z, int from, int target) {
        BlockPos.Mutable at = new BlockPos.Mutable(x, from, z);
        int surface = from;
        int bed = from;

        while (surface + 1 < chunk.getTopY() && !chunk.getFluidState(at.setY(surface + 1)).isEmpty()) {
            surface++;
        }

        while (bed > chunk.getBottomY() && !chunk.getFluidState(at.setY(bed)).isEmpty()) {
            bed--;
        }

        int to = Math.max(target, surface);

        for (int y = bed + 1; y <= to; y++) {
            BlockState state = chunk.getBlockState(at.setY(y));

            if (!state.getFluidState().isEmpty() || state.isAir() || state.isReplaceable()) {
                chunk.setBlockState(at, y == to ? top() : under(), false);
            }
        }

        GenerationHeights.retrack(chunk, x, z, to + 2);
    }
}
