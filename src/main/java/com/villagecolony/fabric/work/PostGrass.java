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
 *
 * <p><b>No curral também</b> (autor, 2026-10-21): no deserto jogado o pastor levou as ovelhas para o
 * curral, e não para o poste, e o chão de areia dele não recriava a lã. Ali a grama nasce no chão do
 * próprio curral, perto da ovelha: o chão é peça do curral (da vila ou da colônia), e só o bloco
 * posto pelo jogador fica de fora.
 */
final class PostGrass {

    /** Até onde, em volta do poste, se procura o lugar da grama. */
    private static final int AROUND = 2;

    private PostGrass() {
    }

    /** A ovelha acaba de ser amarrada, ou tosquiada: garante a grama ao pé do poste dela, ou no curral. */
    static void forSheep(ServerWorld world, SheepEntity sheep) {
        if (sheep.getLeashHolder() instanceof LeashKnotEntity knot) {
            ensure(world, knot.getAttachedBlockPos());
        } else if (ShepherdFlock.isPenned(world, sheep)) {
            inPen(world, sheep.getBlockPos());
        }
    }

    /** A grama no chão do curral, perto da ovelha que está em {@code feet}. */
    static Optional<BlockPos> inPen(ServerWorld world, BlockPos feet) {
        return around(world, feet.down(), 0, true, feet);
    }

    /**
     * A grama ao pé deste poste: a que já existe fica, a terra que a ovelha deixou volta a ser grama,
     * e sem nenhuma das duas nasce grama no chão do bioma mais perto do poste.
     *
     * @return onde está a grama, se houver lugar para ela
     */
    static Optional<BlockPos> ensure(ServerWorld world, BlockPos post) {
        return around(world, post.down(), 1, false, post);
    }

    /**
     * A grama em volta de {@code ground}, de {@code from} a {@link #AROUND} blocos: a que já existe, a
     * terra comida, ou o chão do bioma. {@code structureFloor}: o chão pode ser peça (o do curral).
     */
    private static Optional<BlockPos> around(
            ServerWorld world, BlockPos ground, int from, boolean structureFloor, BlockPos anchor) {
        BlockPos eaten = null;
        BlockPos natural = null;

        for (int radius = from; radius <= AROUND; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    BlockPos spot = ground.add(dx, 0, dz);

                    if (!isFree(world, spot, structureFloor)) {
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
        VillageColonyMod.LOGGER.info("Grass grew at {} for the sheep {} {}",
                chosen.toShortString(), structureFloor ? "in the pen at" : "beside the post at", anchor.toShortString());

        return Optional.of(chosen);
    }

    /** Chão de pé, com ar em cima para a ovelha pisar; fora de estrutura, salvo o chão do curral. */
    private static boolean isFree(ServerWorld world, BlockPos spot, boolean structureFloor) {
        BlockState state = world.getBlockState(spot);

        return world.isChunkLoaded(spot)
                && state.isSolidBlock(world, spot)
                && world.getBlockState(spot.up()).isAir()
                && world.getBlockState(spot.up(2)).isAir()
                && world.getBlockEntity(spot) == null
                && !BlockProtection.isPlayerPlaced(state)
                && (structureFloor
                        || (!BlockProtection.isColonyBuilt(spot) && !BlockProtection.isVillageOriginal(world, spot)));
    }
}
