package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * A ruína da obra MAE de um save antigo — Regra 228 (autor, 2026-11-10): a obra que uma versão anterior deixou pela metade
 * e que não volta para a fila sai do mundo. Só sai o bloco igual ao da planta naquela posição: o que o jogador ou a vila
 * puseram ali fica. Com chunk descarregado, espera.
 */
final class MaeRuins {

    private MaeRuins() {
    }

    /** Se todos os chunks da pegada estão carregados: sem isso, a limpeza espera. */
    static boolean loaded(ServerWorld world, Blueprint blueprint, BlockPos origin) {
        for (int x = origin.getX() >> 4; x <= (origin.getX() + blueprint.size().x() - 1) >> 4; x++) {
            for (int z = origin.getZ() >> 4; z <= (origin.getZ() + blueprint.size().z() - 1) >> 4; z++) {
                if (!world.isChunkLoaded(x, z)) {
                    return false;
                }
            }
        }

        return true;
    }

    /** Tira da pegada o que é igual à planta; devolve quantos blocos saíram. */
    static int clear(ServerWorld world, Blueprint blueprint, BlockPos origin) {
        int removed = 0;

        for (BlueprintBlock block : blueprint.blocks()) {
            BlockPos at = origin.add(block.offset().x(), block.offset().y(), block.offset().z());
            var state = world.getBlockState(at);

            if (!state.isAir() && MinecraftTypeAdapter.toResourceId(state.getBlock()).equals(block.block())) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
                removed++;
            }
        }

        VillageColonyMod.LOGGER.info("VC_MAE_SITE version=2 model={} at={} result=ruin_cleared blocks={}", blueprint.id(),
                origin.toShortString(), removed);

        return removed;
    }
}
