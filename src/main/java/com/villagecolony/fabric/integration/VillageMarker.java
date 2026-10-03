package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Optional;

/**
 * A área da vila à vista do jogador — pedido do autor, 2026-10-03: a mesma
 * ideia das chamas que contornam o lote da obra ({@link SiteMarker}), agora
 * para a caixa inteira da vila.
 *
 * <p>Partícula verde de aldeão feliz, para não se confundir com a chama e a
 * fumaça do lote. Cada jogador recebe só o trecho da borda até
 * {@link #WITHIN} blocos dele, um ponto a cada {@link #STEP}, uma vez por
 * segundo: o pacote vai só para ele, e nada é lido de chunk descarregado.
 */
public final class VillageMarker {

    private static final int EVERY_TICKS = 20;

    /** Até onde do jogador a borda é desenhada. */
    static final int WITHIN = 48;

    /** Um ponto a cada dois blocos: a linha se lê inteira e custa metade. */
    static final int STEP = 2;

    private static int tickCounter;

    private VillageMarker() {
    }

    /** Chamada do tique do servidor. */
    public static void tick(ServerWorld world) {
        if (++tickCounter < EVERY_TICKS) {
            return;
        }

        tickCounter = 0;

        for (ServerPlayerEntity player : world.getPlayers()) {
            int x = player.getBlockX();
            int z = player.getBlockZ();

            for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
                Optional<VillageBounds> box = colony.bounds();

                if (box.isPresent() && box.get().containsColumn(x, z, WITHIN)) {
                    draw(world, player, box.get().borderNear(x, z, WITHIN, STEP));
                }
            }
        }
    }

    private static void draw(ServerWorld world, ServerPlayerEntity player, List<ColonyPos> border) {
        for (ColonyPos column : border) {
            if (!world.isChunkLoaded(column.x() >> 4, column.z() >> 4)) {
                continue;
            }

            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column.x(), column.z());

            world.spawnParticles(player, ParticleTypes.HAPPY_VILLAGER, true,
                    column.x() + 0.5, y + 0.3, column.z() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
