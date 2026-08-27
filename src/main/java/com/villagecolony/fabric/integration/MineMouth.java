package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

/**
 * A boca da mina, mobiliada — a Regra 30.
 *
 * <p><b>Regra do autor, 2026-08-22:</b> onde o mineiro decide começar a
 * cavar aparecem duas coisas — uma <b>lanterna</b> de um lado do buraco
 * e um <b>baú marcado como do mineiro</b> do outro. Esse baú guarda só
 * minério, e quando lotar o minério passa a ir para o baú principal.
 *
 * <p><b>Não custa material</b>, e é a mesma decisão que a mina inteira
 * já carrega: a escada, as duas salas e a galeria são <b>cavadas</b>, e
 * ninguém paga por elas. É também o precedente do {@code ChestPlacer},
 * que põe baú ao lado da cama sem tirar tábua de baú nenhum. Cobrar
 * material aqui faria a mina não abrir até a colônia ter lanterna — e
 * lanterna pede ferro, que vem da mina.
 *
 * <p><b>Sem estado novo em disco.</b> Onde está o baú é lido do mundo:
 * dos vizinhos da boca, o que for um baú é ele. Gravar a posição seria
 * uma segunda verdade que o jogador desfaz com uma picareta.
 */
public final class MineMouth {

    /** Quantos blocos abaixo da boca ainda contam como "ao lado dela". */
    private static final int DROP = 1;

    private MineMouth() {
    }

    /**
     * Põe a lanterna e o baú, se ainda não estiverem lá.
     *
     * <p>Idempotente, e as <b>duas</b> peças o são — 2026-08-27. Até
     * aqui só o baú era conferido: quem já tinha baú voltava na primeira
     * linha, e a lanterna nunca chegava a ser tentada. O autor achou o
     * buraco em jogo, e a frase dele foi <i>"faltou o lampião na entrada
     * da mina, eu mesmo botei"</i>.
     *
     * <p>Duas bocas caíam nesse caso, e as duas são comuns: a mina que
     * volta de um save anterior à Regra 30, e a boca em que a primeira
     * tentativa achou lugar para o baú e não para a lanterna — encosta,
     * água, borda de chunk. Nas duas a segunda chance não existia.
     *
     * <p>Chamada a cada passagem em que a mina existe, e silenciosa em
     * todas menos naquelas em que põe alguma coisa.
     *
     * @return onde está o baú da boca, ou vazio quando nenhum vizinho
     *     serve — encosta, água, ou chunk fora de memória
     */
    public static Optional<BlockPos> furnish(ServerWorld world, BlockPos mouth) {
        if (chunkAt(world, mouth) == null) {
            // Nunca forçar carregamento de dentro do ciclo — §11.
            return Optional.empty();
        }

        Optional<BlockPos> chest = chestAt(world, mouth);

        if (chest.isEmpty()) {
            chest = placeChest(world, mouth);
        }

        if (chest.isEmpty()) {
            // Sem baú não há de que a lanterna ser o outro lado, e a
            // passagem seguinte tenta os dois de novo.
            return Optional.empty();
        }

        placeLanternIfMissing(world, mouth, chest.get());

        return chest;
    }

    /** O baú da boca, recém-posto e marcado como do mineiro. */
    private static Optional<BlockPos> placeChest(ServerWorld world, BlockPos mouth) {
        Optional<BlockPos> spot = freeSpotNear(world, mouth, null);

        if (spot.isEmpty()) {
            return Optional.empty();
        }

        world.setBlockState(spot.get(), Blocks.CHEST.getDefaultState(), Block.NOTIFY_ALL);

        ChestMarker.markAt(world, spot.get(), ProfessionType.MINER);

        VillageColonyMod.LOGGER.info(
                "Mine mouth at {} got its miner chest at {}",
                mouth.toShortString(),
                spot.get().toShortString());

        return spot;
    }

    /**
     * A lanterna, do outro lado do buraco — o que faz a boca ser achável
     * de longe no escuro, que é para o que o autor a pediu.
     *
     * <p>A que já está lá conta, inclusive a que o <b>jogador</b> pôs: a
     * de 08-27 apareceu assim, e pôr uma segunda ao lado dela seria o
     * mod discordando do dono do mundo por nada.
     */
    private static void placeLanternIfMissing(
            ServerWorld world, BlockPos mouth, BlockPos chest) {

        if (lanternAt(world, mouth).isPresent()) {
            return;
        }

        Optional<BlockPos> lamp = freeSpotNear(world, mouth, chest);

        if (lamp.isEmpty()) {
            // Não cabe agora. A passagem seguinte tenta de novo, que é
            // exatamente o que faltava antes desta versão.
            return;
        }

        world.setBlockState(lamp.get(), Blocks.LANTERN.getDefaultState(), Block.NOTIFY_ALL);

        VillageColonyMod.LOGGER.info(
                "Mine mouth at {} got its lantern at {}",
                mouth.toShortString(),
                lamp.get().toShortString());
    }

    /** A lanterna desta boca, se ela existe — lida do mundo, como o baú. */
    private static Optional<BlockPos> lanternAt(ServerWorld world, BlockPos mouth) {
        for (int drop = 0; drop <= DROP; drop++) {
            for (Direction side : Direction.Type.HORIZONTAL) {
                BlockPos at = mouth.offset(side).down(drop);

                if (world.getBlockState(at).isOf(Blocks.LANTERN)) {
                    return Optional.of(at);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * O baú da boca desta mina, se ele existe.
     *
     * <p>Lido do mundo, e por isso sobrevive ao servidor parar sem
     * ocupar um campo no save.
     */
    public static Optional<BlockPos> chestAt(ServerWorld world, BlockPos mouth) {
        if (chunkAt(world, mouth) == null) {
            return Optional.empty();
        }

        for (int drop = 0; drop <= DROP; drop++) {
            for (Direction side : Direction.Type.HORIZONTAL) {
                BlockPos at = mouth.offset(side).down(drop);

                if (world.getBlockState(at).isOf(Blocks.CHEST)) {
                    return Optional.of(at);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Um vizinho da boca onde caiba coisa nova.
     *
     * @param taken o lugar que a peça anterior ocupou, para a seguinte
     *     não disputar com ela; {@code null} na primeira
     */
    private static Optional<BlockPos> freeSpotNear(
            ServerWorld world, BlockPos mouth, BlockPos taken) {

        for (int drop = 0; drop <= DROP; drop++) {
            for (Direction side : Direction.Type.HORIZONTAL) {
                BlockPos at = mouth.offset(side).down(drop);

                if (at.equals(taken) || !isGoodSpot(world, at)) {
                    continue;
                }

                return Optional.of(at);
            }
        }

        return Optional.empty();
    }

    /** Ar sobre chão sólido: onde uma peça da boca pode ficar. */
    private static boolean isGoodSpot(ServerWorld world, BlockPos at) {
        return world.getBlockState(at).isReplaceable()
                && world.getBlockState(at.down()).isSolidBlock(world, at.down());
    }

    private static Object chunkAt(ServerWorld world, BlockPos at) {
        return world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4);
    }
}
