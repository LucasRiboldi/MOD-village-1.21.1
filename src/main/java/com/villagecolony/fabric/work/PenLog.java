package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** O que a leitura dos cercados achou: cada cercado, e cada portão que não deu em cercado fechado. */
final class PenLog {

    private PenLog() {
    }

    static void write(ServerWorld world, Colony colony, List<ShepherdPens.Pen> pens, List<BlockPos> openGates) {
        StringBuilder line = new StringBuilder();

        for (ShepherdPens.Pen pen : pens) {
            line.append(" pen[gate=").append(pen.gate().toShortString()).append(" cells=").append(pen.reach().size())
                    .append(" animals=").append(ShepherdPens.animalsIn(world, pen)).append(" large=").append(pen.large()).append(']');
        }

        for (BlockPos gate : openGates) {
            line.append(" open[gate=").append(gate.toShortString()).append(']');
        }

        VillageColonyMod.LOGGER.info("VC_PENS colony={}{}", colony.id().toString().substring(0, 8), line);
    }
}
