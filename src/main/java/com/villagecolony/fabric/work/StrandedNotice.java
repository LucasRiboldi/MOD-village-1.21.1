package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Avisa quem está no mundo que um trabalhador ficou preso — estudo de 01-10,
 * §9b-C. Não resolve, mas tira o caso do log: o jogador vê onde ele está e
 * pode abrir caminho. Só depois de a peça da colônia também não dar saída
 * ({@link ColonyPieces}).
 */
final class StrandedNotice {

    /** O chat é do jogador: a cada cinco minutos, não a cada um. */
    static final long EVERY = 6_000;

    private StrandedNotice() {
    }

    static void tell(ServerWorld world, String worker, BlockPos feet) {
        Text message = Text.literal("[Village Colony] Um trabalhador da colônia está preso em "
                + feet.toShortString() + ", cercado de blocos que ele não pode quebrar.")
                .formatted(Formatting.YELLOW);

        for (ServerPlayerEntity player : world.getPlayers()) {
            player.sendMessage(message, false);
        }

        VillageColonyMod.LOGGER.info("Stranded worker {} is boxed in at {} — told {} player(s)",
                worker, feet.toShortString(), world.getPlayers().size());
    }
}
