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
     * <p>Idempotente por construção: se já houver baú entre os vizinhos,
     * nada é criado. Chamada a cada passagem em que a mina existe, e
     * silenciosa em todas menos na primeira.
     *
     * @return onde está o baú da boca, ou vazio quando nenhum vizinho
     *     serve — encosta, água, ou chunk fora de memória
     */
    public static Optional<BlockPos> furnish(ServerWorld world, BlockPos mouth) {
        Optional<BlockPos> known = chestAt(world, mouth);

        if (known.isPresent()) {
            return known;
        }

        if (chunkAt(world, mouth) == null) {
            // Nunca forçar carregamento de dentro do ciclo — §11.
            return Optional.empty();
        }

        Optional<BlockPos> spot = freeSpotNear(world, mouth, null);

        if (spot.isEmpty()) {
            return Optional.empty();
        }

        world.setBlockState(spot.get(), Blocks.CHEST.getDefaultState(), Block.NOTIFY_ALL);

        ChestMarker.markAt(world, spot.get(), ProfessionType.MINER);

        // A lanterna vai do outro lado, e é o que faz a boca ser achável
        // de longe no escuro — que é para o que o autor a pediu.
        Optional<BlockPos> lamp = freeSpotNear(world, mouth, spot.get());

        lamp.ifPresent(at ->
                world.setBlockState(at, Blocks.LANTERN.getDefaultState(), Block.NOTIFY_ALL));

        VillageColonyMod.LOGGER.info(
                "Mine mouth at {} furnished — miner chest at {}, lantern at {}",
                mouth.toShortString(),
                spot.get().toShortString(),
                lamp.map(BlockPos::toShortString).orElse("nowhere it fits"));

        return spot;
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
