package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * O degrau na frente da porta de um lote elevado — decisão do autor,
 * 2026-09-30.
 *
 * <p>A base pode ficar um bloco acima da rua. O aldeão sobe um degrau, mas
 * não dois: se quem está parado logo à frente da porta ficar dois ou mais
 * abaixo do piso, a abertura da obra assenta solo do bioma até a diferença
 * voltar a um. Na frente de rua comum a diferença já é um, e nada é posto
 * sobre a rua.
 *
 * <p><b>O degrau de cima é escada de madeira</b> — pedido do autor,
 * 2026-10-02: <i>"se a entrada da estrutura tiver 2 blocos em relação à rua,
 * adicionar automaticamente uma escada de madeira"</i>. O último bloco de cada
 * coluna é {@code oak_stairs}, subindo para a casa; o que vai por baixo dele,
 * quando a diferença passa de dois, continua solo do bioma.
 */
public final class DoorStep {

    /** Até onde procurar o chão abaixo do piso. */
    private static final int LOOK_DOWN = 4;

    private DoorStep() {
    }

    /**
     * Põe os degraus que faltam na fileira em frente ao lado da porta.
     *
     * @return quantos blocos foram assentados
     */
    public static int placeIfNeeded(ServerWorld world, BuildSiteScanner.Site site) {
        Direction door = site.doorSide();
        int floor = site.origin().y();
        int placed = 0;

        boolean alongX = door.getOffsetX() == 0;
        int width = alongX ? site.size().x() : site.size().z();

        for (int step = 0; step < width; step++) {
            int x = alongX
                    ? site.origin().x() + step
                    : door.getOffsetX() < 0 ? site.origin().x() - 1 : site.origin().x() + site.size().x();
            int z = alongX
                    ? door.getOffsetZ() < 0 ? site.origin().z() - 1 : site.origin().z() + site.size().z()
                    : site.origin().z() + step;

            placed += raise(world, x, z, floor, door.getOpposite());
        }

        if (placed > 0) {
            VillageColonyMod.LOGGER.info(
                    "Laid {} step blocks (oak stairs on top) in front of the door of the lot at {} — it sits above the street",
                    placed, site.origin());
        }

        return placed;
    }

    /**
     * Assenta solo até que quem pise nesta coluna fique um abaixo do piso; o
     * bloco de cima é a escada, subindo em {@code up}.
     */
    private static int raise(ServerWorld world, int x, int z, int floor, Direction up) {
        int ground = Integer.MIN_VALUE;

        for (int y = floor - 1; y >= floor - LOOK_DOWN; y--) {
            BlockState state = world.getBlockState(new BlockPos(x, y, z));

            if (!LotGround.isNothing(state)) {
                ground = y;
                break;
            }
        }

        if (ground == Integer.MIN_VALUE) {
            return 0;
        }

        int placed = 0;
        int top = ground;

        // Quem pisa em cima de `top` fica em top + 1; o piso é `floor`.
        while (floor - (top + 1) >= 2) {
            BlockPos at = new BlockPos(x, top + 1, z);
            BlockState state = world.getBlockState(at);

            if (!LotGround.isNothing(state) || !state.getFluidState().isEmpty()) {
                break;
            }

            boolean last = floor - (top + 2) < 2;

            world.setBlockState(at, last
                    ? Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, up)
                    : VillageBiomes.foundationGroundAt(world, at).getDefaultState());
            top++;
            placed++;
        }

        return placed;
    }
}
