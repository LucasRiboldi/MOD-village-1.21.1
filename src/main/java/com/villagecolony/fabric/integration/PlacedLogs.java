package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

/**
 * Regra 281: todo tronco que o mod põe no mundo sai no log, com a posição e quem pôs — a linha {@code VC_LOG_PLACED}.
 * O autor viu troncos surgindo na vila no jogo da 0.4.79 e nenhum diário dizia de onde vinham.
 *
 * <p>Não há um ponto único por onde passa todo bloco posto pelo mod; quem põe peça que pode ser tora chama
 * {@link #note} (obra, arco da mina, árvore da vila), e quem ergue um molde inteiro chama {@link #scan} depois.
 */
public final class PlacedLogs {

    private PlacedLogs() {
    }

    /**
     * Registra a peça, se for tronco.
     *
     * @param source quem pôs, curto: {@code build:<planta>}, {@code mine_arch}, {@code village_tree}...
     * @return se a peça era tronco e saiu no log
     */
    public static boolean note(BlockPos at, BlockState state, String source) {
        if (!state.isIn(BlockTags.LOGS)) {
            return false;
        }

        VillageColonyMod.LOGGER.info("VC_LOG_PLACED source={} block={} at={}", source,
                Registries.BLOCK.getId(state.getBlock()).getPath(), at.toShortString());

        return true;
    }

    /** Registra cada tronco dentro da caixa — depois de um molde inteiro erguido de uma vez. */
    public static int scan(ServerWorld world, BlockBox box, String source) {
        int logs = 0;

        for (BlockPos at : BlockPos.iterate(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ())) {
            if (note(at, world.getBlockState(at), source)) {
                logs++;
            }
        }

        return logs;
    }
}
