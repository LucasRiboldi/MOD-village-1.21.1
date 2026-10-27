package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Quem não sai do cercado por nenhuma saída, {@link #ROUNDS} rodadas seguidas, é posto do lado de
 * fora da cerca — Regra 114 (taiga, 24-10: o mineiro passou 3,5 h cercado, 0 portões, e o caminho
 * até as duas cercas a pular nunca se achou).
 *
 * <p>É um passo de um ou dois blocos, para o chão logo atrás da cerca, onde o corpo inteiro cabe
 * ({@link SafeLanding}); nunca para dentro do que ele alcançava de dentro.
 */
final class PenLift {

    static {
        ServerMemory.register(PenLift.class, PenLift::clearAll);
    }

    /** Rodadas com todas as saídas falhando antes de pôr do lado de fora. */
    static final int ROUNDS = 3;

    private static final Map<UUID, Integer> FAILED = new HashMap<>();

    private PenLift() {
    }

    /**
     * Mais uma rodada sem sair; na {@link #ROUNDS}-ésima, ele vai para fora da cerca.
     *
     * @return se ele foi posto do lado de fora
     */
    static boolean failedRound(ServerWorld world, MobEntity mob, List<FencedIn.Exit> exits, Set<BlockPos> reach) {
        UUID id = mob.getUuid();
        int rounds = FAILED.merge(id, 1, Integer::sum);

        if (rounds < ROUNDS) {
            return false;
        }

        for (FencedIn.Exit exit : exits) {
            Optional<Vec3d> spot = SafeLanding.on(world, mob, exit.outside().down());

            if (spot.isEmpty() || reach.contains(BlockPos.ofFloored(spot.get()))) {
                continue;
            }

            BlockPos from = mob.getBlockPos();

            mob.getNavigation().stop();
            mob.requestTeleport(spot.get().x, spot.get().y, spot.get().z);
            FAILED.remove(id);

            VillageColonyMod.LOGGER.info("{} {} was lifted from {} over {} at {} to {} — {} rounds of every way out"
                            + " failed",
                    mob instanceof net.minecraft.entity.passive.VillagerEntity ? "Worker" : "Golem",
                    id.toString().substring(0, 8), from.toShortString(), exit.gate() ? "the gate" : "the fence",
                    exit.barrier().toShortString(), BlockPos.ofFloored(spot.get()).toShortString(), rounds);

            return true;
        }

        return false;
    }

    /** Saiu sozinho, ou não está mais cercado: a conta recomeça. */
    static void forget(UUID id) {
        FAILED.remove(id);
    }

    static int rounds(UUID id) {
        return FAILED.getOrDefault(id, 0);
    }

    static void clearAll() {
        FAILED.clear();
    }
}
