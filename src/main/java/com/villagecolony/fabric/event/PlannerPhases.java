package com.villagecolony.fabric.event;

import com.villagecolony.core.type.ServerMemory;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Onde a fase {@code planner} do ciclo de uma colônia gasta o tempo — VF-001 A.
 *
 * <p>A fase {@code planner} do {@link CycleCost} cobre mais que o {@code ConstructionPlanner.plan}: a sonda de
 * fantasmas, a tábua das peças, a paleta, as peças do bioma e todo o {@code WorkMaterials}. O prazo de
 * {@code PlanningBudget.DEADLINE_MS} cobre só a varredura de lote. A linha {@code VC_COST_PHASES} reparte a fase em
 * subfases, em microssegundos e na ordem fixa do enum, e sai quando o {@code planner} da colônia passa de
 * {@link #SLOW_MS} e sempre no primeiro ciclo de trabalho dela depois de o servidor abrir.
 *
 * <p>Uma instância por ciclo de colônia; uma thread só, a do servidor.
 */
public final class PlannerPhases {

    static {
        ServerMemory.register(PlannerPhases.class, PlannerPhases::clearAll);
    }

    /** As subfases do {@code planner}, na ordem em que o ciclo as roda. */
    enum Sub {
        PROBE("probe"),
        PLAN("plan"),
        PLANKS("planks"),
        PALETTE("palette"),
        BIOME_PIECES("biome_pieces"),
        STONE("stone"),
        WOOL("wool"),
        GLASS("glass"),
        COAL("coal"),
        IRON("iron"),
        SMELTED("smelted"),
        SURFACE("surface"),
        MINED("mined"),
        RAW_WOOD("raw_wood");

        private final String label;

        Sub(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    /** Acima disto o {@code planner} de uma colônia sai em {@code VC_COST_PHASES} — VF-001 A. */
    static final long SLOW_MS = 100;

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private static final long NANOS_PER_MICRO = 1_000L;

    /** As colônias que já tiveram o primeiro ciclo de trabalho nesta sessão do servidor. */
    private static final Set<UUID> FIRST_CYCLE_SEEN = new HashSet<>();

    private final Map<Sub, Long> nanos = new EnumMap<>(Sub.class);

    /** Soma à subfase o tempo dado. */
    void add(Sub sub, long elapsedNanos) {
        nanos.merge(sub, elapsedNanos, Long::sum);
    }

    /** Roda o trabalho e cobra o tempo dele à subfase. */
    void run(Sub sub, Runnable work) {
        long start = System.nanoTime();

        work.run();
        add(sub, System.nanoTime() - start);
    }

    /** Roda o trabalho, cobra o tempo dele à subfase e devolve o resultado. */
    <T> T time(Sub sub, Supplier<T> work) {
        long start = System.nanoTime();
        T result = work.get();

        add(sub, System.nanoTime() - start);

        return result;
    }

    /**
     * A linha {@code VC_COST_PHASES} desta colônia, quando ela é devida.
     *
     * @param plannerNanos a fase {@code planner} inteira desta colônia, para o {@code other} sair da subtração
     * @return a linha no primeiro ciclo de trabalho da colônia ou com o {@code planner} acima de {@link #SLOW_MS}
     */
    Optional<String> lineIfDue(UUID colonyId, long plannerNanos) {
        boolean first = FIRST_CYCLE_SEEN.add(colonyId);

        if (!first && plannerNanos <= SLOW_MS * NANOS_PER_MILLI) {
            return Optional.empty();
        }

        return Optional.of(line(colonyId, plannerNanos, first));
    }

    /** A linha em si, sem a decisão de quando sai — o que o teste confere. */
    String line(UUID colonyId, long plannerNanos, boolean first) {
        StringBuilder line = new StringBuilder("VC_COST_PHASES version=1 colony=").append(colonyId)
                .append(" first=").append(first)
                .append(" planner_us=").append(plannerNanos / NANOS_PER_MICRO);
        long named = 0;

        for (Sub sub : Sub.values()) {
            long value = nanos.getOrDefault(sub, 0L);

            named += value;
            line.append(' ').append(sub.label()).append("_us=").append(value / NANOS_PER_MICRO);
        }

        return line.append(" other_us=").append(Math.max(0L, plannerNanos - named) / NANOS_PER_MICRO).toString();
    }

    /** Esquece quais colônias já tiveram o primeiro ciclo, junto com o resto do estado em memória. */
    public static void clearAll() {
        FIRST_CYCLE_SEEN.clear();
    }
}
