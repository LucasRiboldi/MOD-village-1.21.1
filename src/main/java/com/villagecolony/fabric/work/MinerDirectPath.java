package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quando a navegação do jogo já chega ao lugar de pé da pedra, o mineiro anda direto
 * para lá, sem a perna pela ordem de cavar — playtest 0.3.32.
 *
 * <p>No save de 12-10 as pedras do ramal aberto tinham lugar de pé alcançável a pé de
 * onde os mineiros estavam (leitura do save: −372, 21, −1025 ao lado de −372, 20,
 * −1025), mas a perna ({@link MinerLeg}) os mandava de volta à entrada do nível, pelo
 * degrau de três blocos que eles não sobem. Onze desistências e nenhum bloco cavado.
 *
 * <p>A perna continua sendo o caminho quando o jogo não acha caminho — pedra longe
 * demais, rocha maciça no meio. A resposta fica guardada por alvo e é refeita a cada
 * {@link #RECHECK_TICKS}: um caminho de A* por mineiro a cada dez segundos.
 */
final class MinerDirectPath {

    static {
        ServerMemory.register(MinerDirectPath.class, MinerDirectPath::clearAll);
    }

    /** De quanto em quanto tempo a resposta para o mesmo alvo é refeita. */
    static final long RECHECK_TICKS = 200;

    /** A folga entre o fim do caminho e o lugar de pé da pedra (o braço alcança 4). */
    static final int NEAR = 2;

    private record Verdict(BlockPos approach, boolean reaches, long at) {
    }

    private static final Map<UUID, Verdict> VERDICTS = new HashMap<>();

    private MinerDirectPath() {
    }

    /** Se a navegação do jogo leva este mineiro, de onde ele está, até {@code approach}. */
    static boolean reaches(ServerWorld world, VillagerEntity villager, BlockPos approach) {
        Verdict known = VERDICTS.get(villager.getUuid());
        long now = world.getTime();

        if (known != null && known.approach().equals(approach) && now - known.at() < RECHECK_TICKS) {
            return known.reaches();
        }

        // Com folga: o caminho do jogo para um ou dois blocos antes do alvo, e de lá a
        // picareta ainda alcança a pedra.
        Path path = villager.getNavigation().findPathTo(approach, 0);
        boolean reaches = path != null
                && (path.reachesTarget() || (path.getEnd() != null
                        && path.getEnd().getBlockPos().getManhattanDistance(approach) <= NEAR));

        VERDICTS.put(villager.getUuid(), new Verdict(approach.toImmutable(), reaches, now));

        return reaches;
    }

    static void forget(UUID workerId) {
        VERDICTS.remove(workerId);
    }

    static void clearAll() {
        VERDICTS.clear();
    }
}
