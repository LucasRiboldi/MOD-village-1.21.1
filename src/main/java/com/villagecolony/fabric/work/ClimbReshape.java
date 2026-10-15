package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * O túnel pelo qual um mineiro encalhado cava para o lado até achar por onde subir — §9b-A. Separado
 * do {@link ClimbOut} em 2026-10-15 (F2 do plano, para ele caber no teto de 500 linhas) sem mudar a
 * lógica. É escavação, então só o mineiro chega aqui (quem decide é o {@link ClimbOut#next}, via
 * {@link ClimbPolicy}).
 */
final class ClimbReshape {

    /** Quantos blocos para o lado o túnel abre antes de tentar subir dali. */
    static final int TUNNEL_LENGTH = 3;

    private static final long WARN_EVERY = 1_200;

    private ClimbReshape() {
    }

    /** Abre (ou segue) o túnel lateral; devolve para {@code STAIRS} quando acaba ou emperra. */
    static void tunnel(ServerWorld world, VillagerEntity villager, ClimbOut.Climb climb, UUID workerId,
            BlockPos feet, BlockPos home, long now) {

        if (climb.tunnelWay == null) {
            climb.tunnelWay = ClimbTerrain.tunnelWay(world, feet, home, climb.breakPieces).orElse(null);
            climb.tunnelLeft = TUNNEL_LENGTH;

            if (climb.tunnelWay == null && !climb.breakPieces) {
                // Fechado (§9b-A): a peça da própria colônia pode sair e o reparo a reconstrói; vila
                // original e bloco do jogador continuam intocáveis (Regra 3).
                climb.tunnelWay = ClimbTerrain.tunnelWay(world, feet, home, true).orElse(null);

                if (climb.tunnelWay != null) {
                    climb.breakPieces = true;
                    VillageColonyMod.LOGGER.info(
                            "Stranded worker {} is boxed in at {} — it cuts through the colony's own"
                                    + " building toward {}; the repair rebuilds it",
                            ClimbOut.shortId(workerId), feet.toShortString(), climb.tunnelWay.asString());
                }
            }

            if (climb.tunnelWay == null) {
                if (now - climb.toldPlayersAt >= StrandedNotice.EVERY) {
                    climb.toldPlayersAt = now;
                    StrandedNotice.tell(world, ClimbOut.shortId(workerId), feet);
                }

                if (now - climb.warnedAt >= WARN_EVERY) {
                    climb.warnedAt = now;
                    VillageColonyMod.LOGGER.warn(
                            "Stranded worker {} is boxed in at {} by blocks it may not break — it keeps"
                                    + " trying every few seconds",
                            ClimbOut.shortId(workerId), feet.toShortString());
                }

                climb.mode = ClimbOut.Mode.STAIRS;
                climb.noGain = 0;

                return;
            }
        }

        BlockPos ahead = feet.offset(climb.tunnelWay);

        for (BlockPos cell : List.of(ahead, ahead.up())) {
            if (!ClimbTerrain.isOpen(world, cell)) {
                if (ClimbTerrain.mayBreak(world, cell, climb.breakPieces)) {
                    ClimbOut.breakOne(world, villager, climb, workerId, cell);
                } else {
                    climb.tunnelWay = null;
                }

                return;
            }
        }

        ClimbOut.moveTo(climb, ahead, now);

        if (--climb.tunnelLeft <= 0) {
            climb.tunnelWay = null;
            climb.mode = ClimbOut.Mode.STAIRS;
            climb.noGain = 0;
        }
    }
}
