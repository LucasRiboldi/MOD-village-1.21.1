package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.LotGround;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * O bloco de grama ao pé do poste da ovelha amarrada — regra do autor, 2026-10-20.
 *
 * <p>A ovelha só recria lã comendo grama, e no deserto não há grama: o pastor tosquiava uma vez e
 * passava o resto da sessão em "no sheep with wool within the radius". Agora, quando ele amarra uma
 * ovelha num poste, nasce grama num bloco de chão ao lado do poste, no nível do solo e fora de
 * estrutura; e cada ovelha tosquiada ali faz a grama voltar (a ovelha come a grama e deixa terra).
 *
 * <p>Fora de estrutura quer dizer: não é peça da vila gerada, nem obra da colônia, nem bloco posto à
 * mão (Regra 3). O chão escolhido é o do bioma, com ar em cima, para a ovelha pisar e comer.
 */
final class PostGrass {

    /** Até onde, em volta do poste, se procura o lugar da grama. */
    private static final int AROUND = 2;

    private PostGrass() {
    }

    /** A ovelha acaba de ser amarrada, ou tosquiada: garante a grama ao pé do poste dela. */
    static void forSheep(ServerWorld world, SheepEntity sheep) {
        if (sheep.getLeashHolder() instanceof LeashKnotEntity knot) {
            ensure(world, knot.getAttachedBlockPos());
        }
    }

    /**
     * A grama ao pé deste poste: a que já existe fica, a terra que a ovelha deixou volta a ser grama,
     * e sem nenhuma das duas nasce grama no chão do bioma mais perto do poste.
     *
     * @return onde está a grama, se houver lugar para ela
     */
    static Optional<BlockPos> ensure(ServerWorld world, BlockPos post) {
        BlockPos ground = post.down();
        BlockPos eaten = null;
        BlockPos natural = null;

        for (int radius = 1; radius <= AROUND; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    BlockPos spot = ground.add(dx, 0, dz);

                    if (!isFree(world, spot)) {
                        continue;
                    }

                    BlockState state = world.getBlockState(spot);

                    if (state.isOf(Blocks.GRASS_BLOCK)) {
                        return Optional.of(spot);
                    }

                    if (eaten == null && state.isOf(Blocks.DIRT)) {
                        eaten = spot;
                    } else if (natural == null && LotGround.isBiomeGround(world, spot)) {
                        natural = spot;
                    }
                }
            }
        }

        BlockPos chosen = eaten != null ? eaten : natural;

        if (chosen == null) {
            return Optional.empty();
        }

        world.setBlockState(chosen, Blocks.GRASS_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
        VillageColonyMod.LOGGER.info("Grass grew at {} beside the sheep's post at {}",
                chosen.toShortString(), post.toShortString());

        return Optional.of(chosen);
    }

    /** Chão de pé, fora de estrutura, com ar em cima para a ovelha pisar. */
    private static boolean isFree(ServerWorld world, BlockPos spot) {
        BlockState state = world.getBlockState(spot);

        return world.isChunkLoaded(spot)
                && state.isSolidBlock(world, spot)
                && world.getBlockState(spot.up()).isAir()
                && world.getBlockState(spot.up(2)).isAir()
                && world.getBlockEntity(spot) == null
                && !BlockProtection.isPlayerPlaced(state)
                && !BlockProtection.isColonyBuilt(spot)
                && !BlockProtection.isVillageOriginal(world, spot);
    }
}
