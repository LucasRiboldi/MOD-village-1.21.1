package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * O canteiro limpo antes de a casa subir — o estado PREPARING.
 *
 * <p>Previsto em {@code Construction-System.md} desde o começo e nunca
 * escrito. O código dizia, com franqueza, que não havia o que limpar:
 * o lote só era aceito sem nada em cima. Só que "nada em cima" tratava
 * grama alta e flor como nada — e elas continuavam lá, dentro da casa,
 * porque o miolo da planta não põe bloco nenhum e portanto nunca as
 * cobria.
 *
 * <p>O autor pediu a limpeza em 2026-08-19: <i>o construtor deve ser
 * capaz de retirar plantas do caminho da construção</i>. É o par da
 * Regra 22 — a planta não reprova o lote, e por não reprovar precisa de
 * alguém que a tire.
 *
 * <p><b>O que ele tira, e só isso:</b> o que o jogo considera
 * substituível — grama alta, samambaia, flor, camada de neve, muda
 * solta. Bloco sólido não entra nesta lista, e não entra por acidente:
 * a Regra 3 manda não tocar no que é do jogador, e um lote com bloco
 * sólido dentro nem devia ter sido escolhido (Regra 22).
 *
 * <p><b>Sem drop.</b> A flor some, não vira item no chão. É o mesmo que
 * acontece quando um jogador põe um bloco sobre grama alta: o mundo a
 * substitui e nada cai. Fazer cair encheria o canteiro de itens que
 * ninguém recolhe, e a colônia passaria a criar entulho onde constrói.
 */
public final class SitePreparation {

    private SitePreparation() {
    }

    /**
     * Limpa a planta que estiver dentro da caixa da obra.
     *
     * <p>Roda uma vez, quando o projeto nasce e quando ele volta do
     * save — o jogador pode ter plantado alguma coisa entre uma sessão e
     * outra, e a Regra 23 diz que o que já foi olhado se olha de novo.
     *
     * @return quantos blocos foram tirados
     */
    public static int clear(ServerWorld world, ConstructionProject project) {
        ColonyPos origin = project.origin();
        ColonyPos size = project.blueprint().size();

        int cleared = 0;

        for (int dx = 0; dx < size.x(); dx++) {
            for (int dy = 0; dy < size.y(); dy++) {
                for (int dz = 0; dz < size.z(); dz++) {
                    BlockPos pos = new BlockPos(
                            origin.x() + dx, origin.y() + dy, origin.z() + dz);

                    if (isPlant(world.getBlockState(pos))) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);

                        cleared++;
                    }
                }
            }
        }

        if (cleared > 0) {
            VillageColonyMod.LOGGER.info(
                    "Project {} cleared {} plants off the site at {}",
                    project.id(),
                    cleared,
                    origin);
        }

        return cleared;
    }

    /**
     * Planta que sai do caminho, e não bloco que fica.
     *
     * <p>A mesma pergunta que {@code BuildSiteScanner.isNothing} faz
     * para não reprovar o lote, e é de propósito que sejam a mesma: o
     * que não reprova é exatamente o que alguém precisa tirar. Se as
     * duas divergissem, ou o lote seria recusado por uma flor, ou a flor
     * ficaria dentro da casa.
     *
     * <p>Ar fica de fora porque não há o que limpar nele.
     */
    private static boolean isPlant(BlockState state) {
        if (state.isAir()) {
            return false;
        }

        return state.isIn(BlockTags.REPLACEABLE) || state.isIn(BlockTags.SMALL_FLOWERS);
    }
}
