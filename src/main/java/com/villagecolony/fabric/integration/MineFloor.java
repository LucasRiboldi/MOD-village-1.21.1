package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

/**
 * O piso da passagem — 2026-09-25, ADR-025 fase 1.
 *
 * <p><b>O problema.</b> A mina é cavada por geometria fixa e o mundo tem
 * caverna. Quando a célula que a picareta abriu fica sobre um vão que a mina
 * não planejou, o degrau que devia ser pisado vira queda: a navegação Vanilla
 * não põe bloco, então o mineiro ou cai, ou não acha caminho e empaca. É o
 * que MineColonies e Workers fazem ao cavar — o túnel sai com chão.
 *
 * <p><b>O que conta como vão.</b> Bloco sem colisão logo abaixo da célula
 * cavada, e que a mina não vai abrir ela mesma: a camada de baixo da sala e o
 * degrau seguinte da escada são planejados, e tapá-los seria desfazer o
 * próximo passo. Fluido fica com o {@link MineFlooding}, que já roda antes.
 *
 * <p><b>Só na passagem.</b> Fora das células planejadas — o veio que o
 * mineiro segue — o chão não é dele: tapar ali cimentaria a caverna inteira
 * que o veio atravessa.
 *
 * <p>Mesmo bloco e mesma regra de proteção do {@link MineFlooding}: pedregulho,
 * e nada que seja da vila gerada ou construído pela colônia. Como a vedação,
 * não consome pedregulho do baú — a fase 1 fecha o buraco; a conta do material
 * entra com o planejador da fase 2.
 */
public final class MineFloor {

    private static final BlockState FLOOR = Blocks.COBBLESTONE.getDefaultState();

    private MineFloor() {
    }

    /**
     * Põe chão sob a célula cavada quando ela é passagem e está sobre vão.
     *
     * @param dug a posição de onde o bloco saiu
     * @param planned as células que a mina planeja abrir no nível atual
     * @return se pôs o bloco
     */
    public static boolean patch(ServerWorld world, BlockPos dug, Set<ColonyPos> planned) {
        if (!planned.contains(MinecraftTypeAdapter.toColonyPos(dug))) {
            return false;
        }

        BlockPos below = dug.down();

        if (!world.isInBuildLimit(below)
                || planned.contains(MinecraftTypeAdapter.toColonyPos(below))) {
            return false;
        }

        BlockState state = world.getBlockState(below);

        if (!state.getCollisionShape(world, below).isEmpty() || !state.getFluidState().isEmpty()) {
            return false;
        }

        if (BlockProtection.isVillageOriginal(world, below) || BlockProtection.isColonyBuilt(below)) {
            return false;
        }

        world.setBlockState(below, FLOOR);

        VillageColonyMod.LOGGER.info(
                "The mine floored {} — the pick opened the passage over a gap",
                below.toShortString());

        return true;
    }
}
