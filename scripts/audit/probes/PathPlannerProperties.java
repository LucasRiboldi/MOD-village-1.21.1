package com.villagecolony.audit;

import com.villagecolony.core.movement.Cell;
import com.villagecolony.core.movement.DetourMoves;
import com.villagecolony.core.movement.DetourPlanner;
import com.villagecolony.core.movement.Terrain;
import com.villagecolony.core.type.ColonyPos;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.statistics.Statistics;
import net.jqwik.api.constraints.IntRange;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Sonda da auditoria de 2026-10-10 (D2/D7): propriedades do planejador de
 * desvio. Não faz parte da bateria; {@code scripts/audit/check-nbt-migration.sh}
 * copia este arquivo para {@code src/test}, roda o {@code propertyTest} e o
 * remove.
 *
 * <p>O terreno é sorteado por semente: abaixo de y=60 é rocha, acima é ar, e
 * cada coluna recebe rocha, terra solta, fluido ou barreira com probabilidades
 * fixas. A obra protegida ({@code keep}) é um cubo sorteado perto da origem.
 */
class PathPlannerProperties {

    private static final Cell[] MIX = {
        Cell.OPEN, Cell.OPEN, Cell.OPEN, Cell.ROCK, Cell.ROCK, Cell.FIRM,
        Cell.LOOSE, Cell.FLUID, Cell.BARRIER, Cell.PASSAGE};

    private static final Cell[] MILD = {Cell.OPEN, Cell.OPEN, Cell.OPEN, Cell.ROCK, Cell.FIRM, Cell.PASSAGE};

    private static Terrain terrain(long seed) {
        return terrain(seed, MIX);
    }

    /** Terreno sem fluido nem barreira, para a propriedade de chegada medir casos que chegam. */
    private static Terrain terrain(long seed, Cell[] mix) {
        return pos -> {
            if (pos.y() < 60) {
                return Cell.ROCK;
            }
            if (pos.y() > 66) {
                return Cell.OPEN;
            }
            long h = seed ^ (pos.x() * 73_856_093L) ^ (pos.y() * 19_349_663L) ^ (pos.z() * 83_492_791L);
            h ^= (h >>> 33);
            h *= 0xff51afd7ed558ccdL;
            h ^= (h >>> 33);
            return mix[(int) Math.floorMod(h, (long) mix.length)];
        };
    }

    private static Set<ColonyPos> keep(long seed) {
        Set<ColonyPos> keep = new HashSet<>();
        int cx = (int) Math.floorMod(seed, 9L) - 4;
        int cz = (int) Math.floorMod(seed >>> 8, 9L) - 4;
        for (int x = cx; x < cx + 3; x++) {
            for (int y = 60; y < 64; y++) {
                for (int z = cz; z < cz + 3; z++) {
                    keep.add(new ColonyPos(x, y, z));
                }
            }
        }
        return keep;
    }

    private static Optional<DetourPlanner.Detour> plan(long seed, int gx, int gz) {
        return plan(terrain(seed), seed, gx, gz);
    }

    private static Optional<DetourPlanner.Detour> plan(Terrain t, long seed, int gx, int gz) {
        ColonyPos start = new ColonyPos(0, 61, 0);
        ColonyPos goal = new ColonyPos(gx, 61, gz);
        Predicate<ColonyPos> reached = p -> p.equals(goal);
        return DetourPlanner.plan(t, start, goal, reached, keep(seed), true);
    }

    /** P1 — todo passo é um movimento legal: um bloco na horizontal, no máximo um na vertical. */
    @Property(tries = 300)
    void everyStepIsALegalMove(@ForAll long seed,
                               @ForAll @IntRange(min = -12, max = 12) int gx,
                               @ForAll @IntRange(min = -12, max = 12) int gz) {
        plan(seed, gx, gz).ifPresent(d -> {
            ColonyPos prev = new ColonyPos(0, 61, 0);
            for (ColonyPos step : d.steps()) {
                int dx = Math.abs(step.x() - prev.x());
                int dy = Math.abs(step.y() - prev.y());
                int dz = Math.abs(step.z() - prev.z());
                if (dx + dz != 1 || dy > 1) {
                    throw new AssertionError("passo ilegal " + prev + " -> " + step);
                }
                prev = step;
            }
        });
    }

    /** P2 — o desvio nunca sai do raio e nunca gasta mais nós que o orçamento. */
    @Property(tries = 300)
    void staysWithinRadiusAndBudget(@ForAll long seed,
                                    @ForAll @IntRange(min = -20, max = 20) int gx,
                                    @ForAll @IntRange(min = -20, max = 20) int gz) {
        plan(seed, gx, gz).ifPresent(d -> {
            if (d.nodes() > DetourPlanner.MAX_NODES) {
                throw new AssertionError("nós " + d.nodes() + " > " + DetourPlanner.MAX_NODES);
            }
            for (ColonyPos step : d.steps()) {
                if (Math.abs(step.x()) > DetourPlanner.RADIUS || Math.abs(step.y() - 61) > DetourPlanner.RADIUS
                        || Math.abs(step.z()) > DetourPlanner.RADIUS) {
                    throw new AssertionError("fora do raio: " + step);
                }
            }
        });
    }

    /** P3 — quando diz que chegou, o último passo é o alvo. */
    @Property(tries = 300)
    void reachingTheGoalEndsOnTheGoal(@ForAll long seed,
                                      @ForAll @IntRange(min = -8, max = 8) int gx,
                                      @ForAll @IntRange(min = -8, max = 8) int gz) {
        Optional<DetourPlanner.Detour> p = plan(terrain(seed, MILD), seed, gx, gz);
        Statistics.collect(p.isEmpty() ? "sem plano" : p.get().reachesGoal() ? "chegou" : "parcial");
        p.filter(DetourPlanner.Detour::reachesGoal).ifPresent(d -> {
            ColonyPos goal = new ColonyPos(gx, 61, gz);
            ColonyPos last = d.steps().isEmpty() ? new ColonyPos(0, 61, 0) : d.steps().get(d.steps().size() - 1);
            if (!last.equals(goal)) {
                throw new AssertionError("reachesGoal com último passo " + last + " != " + goal);
            }
        });
    }

    /** P4 — mesma entrada, mesmo desvio: o planejador é determinístico. */
    @Property(tries = 150)
    void sameInputSamePlan(@ForAll long seed,
                           @ForAll @IntRange(min = -12, max = 12) int gx,
                           @ForAll @IntRange(min = -12, max = 12) int gz) {
        Optional<DetourPlanner.Detour> a = plan(seed, gx, gz);
        Optional<DetourPlanner.Detour> b = plan(seed, gx, gz);
        if (!a.equals(b)) {
            throw new AssertionError("dois planos diferentes para a mesma entrada");
        }
    }

    /**
     * P5 — obra protegida: um movimento nunca cava nem coloca bloco numa
     * célula protegida, nunca cava fluido ou barreira, e só coloca piso onde
     * o terreno aceita.
     */
    @Property(tries = 1000)
    void aMoveNeverTouchesProtectedCells(@ForAll long seed,
                                         @ForAll @IntRange(min = -6, max = 6) int x,
                                         @ForAll @IntRange(min = 59, max = 66) int y,
                                         @ForAll @IntRange(min = -6, max = 6) int z,
                                         @ForAll @IntRange(min = 0, max = 11) int way) {
        Terrain t = terrain(seed);
        Set<ColonyPos> keep = keep(seed);
        int[][] h = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        ColonyPos from = new ColonyPos(x, y, z);
        ColonyPos to = new ColonyPos(x + h[way % 4][0], y + (way / 4) - 1, z + h[way % 4][1]);
        Optional<DetourMoves.Actions> act = DetourMoves.actions(t, from, to, keep);
        Statistics.collect(act.isEmpty() ? "recusado" : act.get().dig().isEmpty() && act.get().place().isEmpty() ? "livre" : "cava/coloca");
        act.ifPresent(a -> {
            for (ColonyPos dug : a.dig()) {
                if (keep.contains(dug)) {
                    throw new AssertionError("cavou célula protegida " + dug);
                }
                Cell kind = t.at(dug);
                if (kind == Cell.FLUID || kind == Cell.BARRIER) {
                    throw new AssertionError("cavou " + kind + " em " + dug);
                }
            }
            for (ColonyPos placed : a.place()) {
                if (keep.contains(placed)) {
                    throw new AssertionError("colocou bloco em célula protegida " + placed);
                }
                if (!t.at(placed).placeable()) {
                    throw new AssertionError("colocou piso onde o terreno não aceita: " + placed);
                }
            }
        });
    }

    /** Controle: o gerador produz terreno variado (sem isto as propriedades poderiam passar vazias). */
    @Property(tries = 50)
    void generatorIsNotDegenerate(@ForAll long seed) {
        Terrain t = terrain(seed);
        Set<Cell> seen = new HashSet<>();
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                seen.add(t.at(new ColonyPos(x, 62, z)));
            }
        }
        if (seen.size() < 4) {
            throw new AssertionError("terreno pobre demais: " + List.copyOf(seen));
        }
    }
}
