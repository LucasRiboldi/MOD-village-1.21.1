package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ResourceId;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Optional;
import java.util.Set;

/**
 * Onde há pedra que o mineiro pode tirar — 2026-08-20.
 *
 * <p>"Pode" é a palavra que carrega a regra. A Regra 3 diz o que nunca
 * se destrói, e pedra é justamente o material de que a vila gerada e as
 * construções do jogador são feitas. Um mineiro que cavasse qualquer
 * pedra derrubaria a igreja da vila no primeiro ciclo.
 *
 * <p>Três condições, e as três juntas:
 *
 * <pre>
 * é a pedra que esta vila usa   pedregulho vem de stone; o deserto tira
 *                               arenito, que ali é a parede
 * está exposta                  ar logo acima. Pedra enterrada exigiria
 *                               cavar túnel, e túnel é outro assunto
 * não é de ninguém              nem peça de vila gerada, nem construção
 *                               da colônia. A Regra 3 nas duas pontas
 * </pre>
 *
 * <p><b>O que isto não faz, e é limite conhecido:</b> o buraco fica. O
 * lenhador replanta o que derruba (Regra 7) e o mineiro não tem
 * equivalente — pedra não cresce. A vila vai ficando com covas rasas em
 * volta, e o dia em que isso incomodar é o dia de decidir se ela
 * preenche, se cava em galeria, ou se aceita.
 */
public final class StoneFace {

    /**
     * Quanto abaixo do centro da vila ainda se procura pedra.
     *
     * <p>Mesmo espírito da janela da busca de lote: a vila não minera no
     * alto do morro que a olha de cima, nem no fundo do desfiladeiro. O
     * lado de baixo é mais largo porque é para lá que a rocha aflora.
     */
    private static final int WINDOW_UP = 4;

    private static final int WINDOW_DOWN = 12;

    /** O que dá pedregulho quando quebrado. */
    private static final Set<Block> STONE = Set.of(
            Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE);

    /** O que a vila de deserto tira da duna. */
    private static final Set<Block> SANDSTONE = Set.of(
            Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE);

    private StoneFace() {
    }

    /**
     * A pedra exposta nesta coluna, se houver e se puder ser tirada.
     *
     * @param wanted a pedra desta vila, da {@code VillagePalette}
     */
    public static Optional<BlockPos> in(
            ServerWorld world, BlockPos column, int aroundY, ResourceId wanted) {

        WorldChunk chunk = world.getChunkManager()
                .getWorldChunk(column.getX() >> 4, column.getZ() >> 4);

        if (chunk == null) {
            // Chunk descarregado. Pedir por ele aqui forçaria carregamento
            // dentro do tique, que já travou este servidor duas vezes.
            return Optional.empty();
        }

        Set<Block> accepted = VillagePalette.SANDSTONE.equals(wanted) ? SANDSTONE : STONE;

        for (int y = aroundY + WINDOW_UP; y >= aroundY - WINDOW_DOWN; y--) {
            BlockPos at = new BlockPos(column.getX(), y, column.getZ());

            BlockState state = chunk.getBlockState(at);

            if (!accepted.contains(state.getBlock())) {
                continue;
            }

            if (!chunk.getBlockState(at.up()).isAir()) {
                // Enterrada. A de cima é a que está exposta, e ela já foi
                // olhada nesta mesma varredura de cima para baixo.
                return Optional.empty();
            }

            if (BlockProtection.isVillageOriginal(world, at)
                    || BlockProtection.isColonyBuilt(at)) {
                // Muro de vila e casa da colônia são de pedra, e são de
                // alguém. A Regra 3 nas duas pontas.
                return Optional.empty();
            }

            return Optional.of(at);
        }

        return Optional.empty();
    }
}
