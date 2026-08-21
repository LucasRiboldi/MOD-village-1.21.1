package com.villagecolony.fabric.integration;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Optional;
import java.util.Set;

/**
 * Onde há areia que o mineiro pode tirar — 2026-08-20.
 *
 * <p><b>Por que a areia é de superfície, e a pedra não.</b> A Regra 29
 * mandou o mineiro descer: pedra está em toda parte abaixo do chão, e uma
 * mina rende mais que raspar afloramentos. Areia é o contrário — ela mora
 * na praia, na duna e na margem do lago, e a vinte blocos de profundidade
 * não há nenhuma fora do deserto. Descer atrás de areia seria cavar vinte
 * blocos para não achar.
 *
 * <p>É por isso que este caminho existe separado, e é ele que devolve
 * função ao {@link RingSweep}: a espiral nasceu para o mineiro de
 * superfície que a mina aposentou no mesmo dia, e ficou sem quem a
 * chamasse.
 *
 * <p>Três condições, e as três juntas — as mesmas que a pedra exposta
 * pedia antes da mina:
 *
 * <pre>
 * é areia            areia comum ou vermelha. As duas fundem em vidro
 * está exposta       ar logo acima. Areia sob água ficaria de fora, e
 *                    é de propósito: aldeão não mergulha
 * não é de ninguém   nem peça de vila gerada, nem construção da
 *                    colônia. A Regra 3 nas duas pontas
 * </pre>
 *
 * <p><b>O buraco fica</b>, e é o mesmo limite conhecido da pedra: o
 * lenhador replanta o que derruba e o mineiro não tem equivalente. Areia
 * ainda cai por gravidade, o que torna a cova mais rasa e mais larga.
 */
public final class SandPatch {

    /**
     * A janela de altura em volta do centro da vila.
     *
     * <p>A mesma que a pedra exposta usava, e pelo mesmo motivo: a vila
     * não vai buscar areia no alto do morro que a olha de cima nem no
     * fundo do desfiladeiro. Para baixo é mais largo porque praia e
     * margem de lago ficam abaixo do miolo da vila.
     */
    private static final int WINDOW_UP = 4;

    private static final int WINDOW_DOWN = 12;

    /** O que funde em vidro. */
    private static final Set<Block> SAND = Set.of(Blocks.SAND, Blocks.RED_SAND);

    private SandPatch() {
    }

    /** A areia exposta nesta coluna, se houver e se puder ser tirada. */
    public static Optional<BlockPos> in(ServerWorld world, BlockPos column, int aroundY) {
        WorldChunk chunk = world.getChunkManager()
                .getWorldChunk(column.getX() >> 4, column.getZ() >> 4);

        if (chunk == null) {
            // Chunk descarregado. Pedir por ele aqui forçaria carregamento
            // dentro do tique, que já travou este servidor duas vezes.
            return Optional.empty();
        }

        for (int y = aroundY + WINDOW_UP; y >= aroundY - WINDOW_DOWN; y--) {
            BlockPos at = new BlockPos(column.getX(), y, column.getZ());

            BlockState state = chunk.getBlockState(at);

            if (!SAND.contains(state.getBlock())) {
                continue;
            }

            if (!chunk.getBlockState(at.up()).isAir()) {
                // Enterrada, ou sob água. A de cima é a que estaria
                // exposta, e ela já foi olhada nesta mesma descida.
                return Optional.empty();
            }

            if (BlockProtection.isVillageOriginal(world, at)
                    || BlockProtection.isColonyBuilt(at)) {
                // No deserto a vila inteira assenta sobre areia, e parte
                // dela é a própria vila. A Regra 3 nas duas pontas.
                return Optional.empty();
            }

            return Optional.of(at);
        }

        return Optional.empty();
    }
}
