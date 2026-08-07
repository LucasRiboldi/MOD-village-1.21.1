package com.villagecolony.fabric.adapter;

import com.villagecolony.core.type.ColonyPos;
import net.minecraft.util.math.BlockPos;

/**
 * Converte tipos do Minecraft para tipos do Core e vice-versa.
 *
 * <p>Esta é a fronteira. Nenhuma classe do Core conhece {@code BlockPos};
 * nenhuma conversão acontece fora daqui. Ver ADR-005 §4.
 *
 * <p>{@code Identifier <-> ResourceId} e
 * {@code BlockRotation <-> ColonyRotation} entram quando houver uso.
 */
public final class MinecraftTypeAdapter {

    private MinecraftTypeAdapter() {
    }

    public static ColonyPos toColonyPos(BlockPos pos) {
        return new ColonyPos(pos.getX(), pos.getY(), pos.getZ());
    }

    public static BlockPos toBlockPos(ColonyPos pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }
}
