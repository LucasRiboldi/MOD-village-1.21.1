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
 * Onde há pedra exposta que o mineiro pode tirar — 2026-08-25.
 *
 * <p><b>Isto já existiu, e a mina o aposentou.</b> Antes da Regra 29 o
 * mineiro raspava afloramento; quando a escada entrou, este caminho saiu,
 * porque uma mina rende mais que um afloramento e pedra está em toda
 * parte abaixo do chão. O {@code SandPatch} guarda essa história escrita:
 * "as mesmas que a pedra exposta pedia antes da mina".
 *
 * <p><b>Ele volta como alternativa, e não como substituto.</b> A mina
 * continua sendo o caminho: é ela que traz carvão e ferro, e é ela que
 * rende. Isto só entra quando a boca <b>não pode nascer</b> — vinte e
 * quatro colunas tentadas e nenhuma serve, que é o que a sessão de
 * 2026-08-25 mostrou numa vila cercada de água. Sem alternativa, aquela
 * colônia ficou sem a única fonte de pedra que tinha, a obra morreu de
 * fome esperando pedregulho, e a vila parou de crescer por causa do
 * terreno em volta.
 *
 * <p>Três condições, e as três juntas — as mesmas do {@link SandPatch},
 * que é o molde:
 *
 * <pre>
 * é pedra            a família que a colônia constrói: pedra e suas
 *                    variantes, e o arenito, que é a pedra do deserto
 * está alcançável    dois blocos de ar acima, que é a altura do aldeão.
 *                    Pedra sob água e teto de caverna ficam de fora
 * não é de ninguém   nem peça de vila gerada, nem construção da
 *                    colônia. A Regra 3 nas duas pontas
 * </pre>
 *
 * <p><b>O buraco fica</b>, e é o limite conhecido: o lenhador replanta o
 * que derruba e o mineiro não tem equivalente.
 */
public final class StonePatch {

    /**
     * A janela de altura em volta do centro da vila.
     *
     * <p><b>Apertada dos dois lados, e o de baixo é o que importa.</b> A
     * areia usa doze para baixo porque praia e margem ficam abaixo do
     * miolo da vila, e areia não existe no subsolo. Pedra existe: uma
     * janela funda faz a busca enxergar o <b>teto de uma caverna</b>
     * através do buraco e mandar o aldeão para dentro dele. Quatro é
     * barranco e afloramento — o que se alcança andando.
     */
    private static final int WINDOW_UP = 4;

    private static final int WINDOW_DOWN = 4;

    /**
     * Quanto ar precisa haver em cima para o aldeão chegar.
     *
     * <p>Dois, que é a altura dele. Um bloco só deixaria passar teto de
     * caverna e fresta entre blocos — lugares onde a pedra está exposta
     * e o aldeão não cabe.
     *
     * <p><b>O que isto ainda deixa passar</b>, e fica dito: caverna a céu
     * aberto larga o bastante, perto o bastante e na altura da vila. É
     * pedra de verdade e alcançável a pé, então passar não é erro — mas
     * é a razão de a janela ser curta.
     */
    private static final int HEADROOM = 2;

    /**
     * O que a colônia reconhece como pedra de construção.
     *
     * <p>A família da pedra e a do arenito, que é a pedra da vila de
     * deserto — a casa de lá pede noventa e três blocos dela. Ardósia e
     * tufo entram porque afloram em barranco e caverna a céu aberto, e
     * o que o mineiro traz de lá a colônia usa igual.
     *
     * <p><b>Minério não entra.</b> Ele é da mina, onde a veia é seguida;
     * aqui a colônia está raspando o que dá para alcançar, e um
     * afloramento de carvão a quarenta blocos não vale a viagem que a
     * escada faria melhor.
     */
    private static final Set<Block> STONE = Set.of(
            Blocks.STONE,
            Blocks.COBBLESTONE,
            Blocks.ANDESITE,
            Blocks.DIORITE,
            Blocks.GRANITE,
            Blocks.TUFF,
            Blocks.DEEPSLATE,
            Blocks.COBBLED_DEEPSLATE,
            Blocks.SANDSTONE,
            Blocks.RED_SANDSTONE);

    private StonePatch() {
    }

    /** A pedra exposta nesta coluna, se houver e se puder ser tirada. */
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

            if (!STONE.contains(state.getBlock())) {
                continue;
            }

            for (int air = 1; air <= HEADROOM; air++) {
                if (!chunk.getBlockState(at.up(air)).isAir()) {
                    // Enterrada, sob água, ou numa fresta em que o aldeão
                    // não cabe. A de cima é a que estaria exposta, e ela
                    // já foi olhada nesta mesma descida.
                    return Optional.empty();
                }
            }

            if (BlockProtection.isVillageOriginal(world, at)
                    || BlockProtection.isColonyBuilt(at)) {
                // A vila de deserto assenta sobre arenito, e parte dele é
                // a própria vila. A Regra 3 nas duas pontas.
                return Optional.empty();
            }

            return Optional.of(at);
        }

        return Optional.empty();
    }
}
