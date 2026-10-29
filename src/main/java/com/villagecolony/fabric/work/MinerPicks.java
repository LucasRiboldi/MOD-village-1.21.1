package com.villagecolony.fabric.work;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A pedra inviável sai antes de virar alvo — T4 do diagnóstico da 0.3.103, decisão B do autor (2026-10-28).
 *
 * <p>Numa passagem o mineiro olha até {@link #PICKS_PER_SEARCH} pedras: a recusada pelo {@link MiningTarget#judge}
 * (alta demais, sem caminho) é posta de lado e a próxima é pedida na hora, em vez de a passagem inteira acabar numa
 * recusa. O teto mantém o tique barato — cada julgamento de perto pode buscar até {@code SEARCH_BUDGET} posições.
 */
final class MinerPicks {

    /** Pedras julgadas por passagem; nas cinco vilas simuladas, as cadeias de recusa tinham de 2 a 4 pedras. */
    static final int PICKS_PER_SEARCH = 4;

    private MinerPicks() {
    }

    /**
     * A primeira pedra aceita, pulando as recusadas com {@code skip}. Vazio quando acabam as pedras, quando a mesma
     * volta (a fonte não avançou) ou depois de {@link #PICKS_PER_SEARCH} recusas.
     */
    static <T> Optional<T> firstAccepted(Supplier<Optional<T>> next, Predicate<T> accepted, Consumer<T> skip) {
        Set<T> seen = new HashSet<>();

        for (int pick = 0; pick < PICKS_PER_SEARCH; pick++) {
            Optional<T> stone = next.get();

            if (stone.isEmpty() || !seen.add(stone.get())) {
                return Optional.empty();
            }

            if (accepted.test(stone.get())) {
                return stone;
            }

            skip.accept(stone.get());
        }

        return Optional.empty();
    }

    /**
     * Se a recusa sai antes da escolha. Sem lugar de pé, não: essa vai ao {@link MinerHands#passOver}, que tenta
     * abrir o pé cavando ao lado (ADR-038 P6).
     */
    static boolean skipsBeforeChoosing(MiningTarget.Verdict verdict) {
        return !verdict.eligible() && !MinerHands.worthRerouting(verdict.rejection());
    }

    /**
     * Põe a pedra de lado sem ela virar alvo: a mesma marca e a mesma conta para a curva do {@code passOver}, e no
     * diário {@code SKIPPED} com o motivo — {@code TARGET_REJECTED} fica para a recusa de quem já andou até a pedra.
     */
    static void skip(ServerWorld world, UUID workerId, UUID colonyId, BlockPos stone, MiningTarget.Rejection why) {
        ActionJournal.action(workerId, "SKIPPED", "stone", 0, stone, why.name());

        MineMarks.refuse(world, stone);
        MineTrouble.couldNotReach(colonyId, stone);
    }
}
