package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quem ficou preso onde a navegação não o tira — E47, 2026-09-24.
 *
 * <p><b>O defeito, medido no playtest de 24-09.</b> Um pedreiro ficou parado
 * em {@code -202, 62, -937} das 03:10 às 06:47, dez blocos abaixo da vila, e
 * a colônia o escalou para construir de novo a cada volta. Três construtores
 * diferentes caíram no mesmo ponto em {@code -211, 66, -954}, quatro em
 * {@code -196, 66, -949}. O guarda de imobilidade disparava e devolvia a
 * tarefa, mas nada tirava o aldeão do buraco — e a obra nunca terminava.
 *
 * <p><b>Dois congelamentos no mesmo lugar marcam o encalhado.</b> O primeiro
 * pode ser porta fechada ou outro aldeão no caminho; o segundo no mesmo
 * ponto é o terreno. A marca sai da escala de trabalho — ver
 * {@code Worker.strand} — e o {@link StrandedEscape} cava a saída.
 *
 * <p>Estado de sessão, esquecido ao parar o servidor. Um aldeão que continue
 * preso depois de recarregar é marcado de novo em dois congelamentos.
 */
public final class StrandedWorkers {

    /** Até quantos blocos de distância dois congelamentos são "o mesmo lugar". */
    private static final int SAME_SPOT = 2;

    /** Onde cada trabalhador congelou da última vez, antes de ser marcado. */
    private static final Map<UUID, BlockPos> LAST_FREEZE = new HashMap<>();

    /** Os encalhados, e quantos degraus cada um já cavou nesta fuga. */
    private static final Map<UUID, Integer> STRANDED = new HashMap<>();

    private StrandedWorkers() {
    }

    /**
     * O guarda de imobilidade disparou para este trabalhador aqui.
     *
     * <p>Chamado por {@link WorkStall} no instante em que ele estoura, e só
     * então: é o sinal que as sete profissões já compartilham.
     *
     * @return se esta foi a vez que o marcou como encalhado
     */
    public static boolean frozeAt(UUID workerId, BlockPos where) {
        if (STRANDED.containsKey(workerId)) {
            return false;
        }

        BlockPos before = LAST_FREEZE.put(workerId, where.toImmutable());

        if (before == null || !sameSpot(before, where)) {
            return false;
        }

        return VillageColonyMod.WORKERS.find(workerId).map(worker -> {
            worker.strand();
            STRANDED.put(workerId, 0);
            LAST_FREEZE.remove(workerId);

            VillageColonyMod.LOGGER.info(
                    "Worker {} is stranded at {} — frozen twice on the same spot;"
                            + " it leaves the work queue and digs its way out",
                    workerId.toString().substring(0, 8),
                    where.toShortString());

            return true;
        }).orElse(false);
    }

    /** Se este trabalhador está marcado como encalhado. */
    public static boolean isStranded(UUID workerId) {
        return STRANDED.containsKey(workerId);
    }

    /** Os encalhados agora. Cópia, para quem percorre poder soltar no meio. */
    static List<UUID> all() {
        return List.copyOf(STRANDED.keySet());
    }

    /** Quantos degraus este encalhado já cavou. */
    static int stepsDug(UUID workerId) {
        return STRANDED.getOrDefault(workerId, 0);
    }

    /** Mais um degrau cavado. */
    static void dugAStep(UUID workerId) {
        STRANDED.computeIfPresent(workerId, (id, steps) -> steps + 1);
    }

    /**
     * Ele saiu: volta à escala. Chamado pela fuga quando o aldeão chega ao
     * nível do terreno em volta.
     */
    static void release(UUID workerId) {
        STRANDED.remove(workerId);
        LAST_FREEZE.remove(workerId);

        VillageColonyMod.WORKERS.find(workerId).ifPresent(worker -> worker.free());
    }

    /** Esquece um trabalhador que saiu do registro. */
    static void forget(UUID workerId) {
        STRANDED.remove(workerId);
        LAST_FREEZE.remove(workerId);
    }

    /** Esquece tudo. Chamado ao abrir e ao parar o servidor. */
    public static void clearAll() {
        STRANDED.clear();
        LAST_FREEZE.clear();
    }

    private static boolean sameSpot(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) <= SAME_SPOT
                && Math.abs(a.getY() - b.getY()) <= SAME_SPOT
                && Math.abs(a.getZ() - b.getZ()) <= SAME_SPOT;
    }
}
