package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

/**
 * A luz da mina: tocha de chão, a cada {@link MineShaft#LIGHT_SPACING} blocos, no salão do nível e no do
 * ramal ({@link MineShaft#lightSpots}) — escolha do autor, 2026-10-27.
 *
 * <p>Nunca na parede: o mineiro cavava a parede depois e a tocha caía (playtest 0.3.97). Nunca no degrau:
 * a escada é caminho. O chão debaixo de cada tocha não é cavado nem por este nível nem pelo seguinte
 * ({@code MineShaftLayoutTest}). A tocha não custa material, como a mobília da boca (Regra 30).
 *
 * <p>O mineiro não cava a própria luz: uma posição com luz é espaço aberto ({@link #isLight}), e é por ela
 * que {@code MineRock} pula a tocha em vez de tratá-la como pedra.
 */
public final class MineLighting {

    private MineLighting() {
    }

    /**
     * Se este bloco é luz solta na passagem, e não rocha: acende e não fecha a passagem. Tocha, lanterna,
     * vela e o bloco de luz entram; magma e pedra luminosa continuam pedra a cavar (ADR-009). Vale para a
     * luz do jogador também (Regra 3).
     */
    public static boolean isLight(ServerWorld world, BlockPos at, BlockState state) {
        return state.getLuminance() > 0 && !state.isSolidBlock(world, at);
    }

    /**
     * Uma tocha de chão no primeiro lugar da grade deste ramal que já está aberto e ainda está escuro.
     * Idempotente: o lugar com tocha não está mais vazio. Chamada a cada passagem em que a mina existe.
     *
     * @return onde a tocha foi posta, ou vazio quando não havia o que fazer
     */
    public static Optional<BlockPos> light(ServerWorld world, Mine mine, MineArm arm) {
        for (ColonyPos spot : arm.shaft().lightSpots()) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(spot);

            if (world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4) == null) {
                // Nunca forçar carregamento de dentro do ciclo — §11.
                continue;
            }

            if (placeTorch(world, at)) {
                VillageColonyMod.LOGGER.info("Mine {} lit the hall floor at {}", mine.colonyId(), at.toShortString());

                return Optional.of(at);
            }
        }

        return Optional.empty();
    }

    /**
     * A tocha no chão em {@code at}: só com o salão aberto em volta (pés e cabeça livres) e chão firme
     * embaixo. Salão ainda por cavar não recebe nada; a passagem volta aqui quando ele abrir.
     */
    static boolean placeTorch(ServerWorld world, BlockPos at) {
        if (!world.getBlockState(at).isAir() || !world.getBlockState(at.up()).isAir()) {
            return false;
        }

        BlockPos floor = at.down();

        if (!world.getBlockState(floor).isSideSolidFullSquare(world, floor, Direction.UP)) {
            return false;
        }

        world.setBlockState(at, Blocks.TORCH.getDefaultState());

        return true;
    }
}
