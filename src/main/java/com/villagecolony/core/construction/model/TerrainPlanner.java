package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Avalia candidatos já amostrados pelo adaptador, sem ler ou escrever mundo — desenho MAE Fase 2. */
public final class TerrainPlanner {
    public static final int MAX_CUT_FILL = 2;
    public static final int SUPPORT_LAYERS = 2;
    public static final int MAX_TRANSITION = 6;
    public static final int MAX_STEP = 1;
    public static final int CLEARANCE = 2;
    public static final int MAX_CANDIDATES = 8;
    private static final int MAX_CELLS = 4096;
    private static final int MAX_CHANGES = 1024;

    private TerrainPlanner() { }

    /** Apoio e colisão correspondem à superfície final; o adaptador confere os volumes reais. */
    public record Column(ColonyPos original, int finalY, int supportLayers, int clearance,
                         boolean openDepression, boolean protectedColumn, int extraChanges, int plants, int mergedPlants) {
        public Column(ColonyPos original, int finalY, int supportLayers, int clearance,
                      boolean openDepression, boolean protectedColumn, int extraChanges, int plants) {
            this(original, finalY, supportLayers, clearance, openDepression, protectedColumn, extraChanges, plants, 0);
        }

        public Column {
            Objects.requireNonNull(original);
            if (supportLayers < 0 || clearance < 0 || extraChanges < 0 || plants < 0
                    || mergedPlants < 0 || mergedPlants > plants || mergedPlants > Math.max(0L, (long) finalY - original.y())) {
                throw new IllegalArgumentException("negative_column_measure");
            }
        }

        public ColonyPos surface() {
            return new ColonyPos(original.x(), finalY, original.z());
        }
    }

    /** Impedimento físico entre superfícies, inclusive barreira de cabeça num degrau. */
    public record Barrier(ColonyPos from, ColonyPos to) {
        public Barrier {
            Objects.requireNonNull(from);
            Objects.requireNonNull(to);
        }
    }

    /** Âncora é somente leitura e já deve estar certificada pelo adaptador. */
    public record Candidate(ColonyPos origin, WorkArea footprint, ColonyPos entry, @Nullable ColonyPos anchor,
                            List<Column> columns, List<Barrier> barriers, List<ColonyPos> corridor, int pathLength) {
        public Candidate(ColonyPos origin, WorkArea footprint, ColonyPos entry, @Nullable ColonyPos anchor,
                         List<Column> columns, List<Barrier> barriers, int pathLength) {
            this(origin, footprint, entry, anchor, columns, barriers, List.of(), pathLength);
        }

        public Candidate {
            Objects.requireNonNull(origin);
            Objects.requireNonNull(footprint);
            Objects.requireNonNull(entry);
            columns = List.copyOf(columns);
            barriers = List.copyOf(barriers);
            corridor = List.copyOf(corridor);
            if (pathLength < 0) {
                throw new IllegalArgumentException("negative_path_length");
            }
        }
    }

    public record Surface(List<Candidate> candidates) {
        public Surface {
            candidates = List.copyOf(candidates);
        }
    }

    public record Request(WorkArea authorized, ColonyPos center) {
        public Request {
            Objects.requireNonNull(authorized);
            Objects.requireNonNull(center);
        }
    }

    /** Enumeração local limitada e comparação lexicográfica; sem ótimo global ou autorização implícita. */
    public static TerrainPlan plan(Surface sample, Request request) {
        if (sample.candidates().isEmpty() || sample.candidates().size() > MAX_CANDIDATES) {
            return TerrainPlan.refused("candidate_budget");
        }
        @Nullable TerrainPlan best = null;
        String refusal = "no_feasible_candidate";
        for (Candidate candidate : sample.candidates()) {
            TerrainPlan result = evaluate(candidate, request);
            if (result.feasible() && (best == null || order(request).compare(result, best) < 0)) {
                best = result;
            } else if (!result.feasible()) {
                refusal = result.reason();
            }
        }
        return best == null ? TerrainPlan.refused(refusal) : best;
    }

    private static TerrainPlan evaluate(Candidate candidate, Request request) {
        if (candidate.columns().isEmpty() || candidate.columns().size() > MAX_CELLS) {
            return TerrainPlan.refused("surface_budget");
        }
        Map<Key, Column> columns = new HashMap<>();
        if (candidate.corridor().size() > MAX_CELLS
                || new HashSet<>(candidate.corridor()).size() != candidate.corridor().size()
                || candidate.corridor().stream().anyMatch(at -> candidate.columns().stream()
                        .noneMatch(column -> column.surface().equals(at)))) return TerrainPlan.refused("invalid_corridor");
        long earthwork = 0;
        long changed = 0;
        long plants = 0;
        int maximum = 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Column column : candidate.columns()) {
            ColonyPos pos = column.surface();
            long delta = Math.abs((long) column.finalY() - column.original().y());
            if (delta > MAX_CUT_FILL) return TerrainPlan.refused("cut_fill_limit");
            if (column.protectedColumn()) return TerrainPlan.refused("protected_column");
            if (column.supportLayers() < SUPPORT_LAYERS) return TerrainPlan.refused("insufficient_support");
            if (column.clearance() < CLEARANCE) return TerrainPlan.refused("clearance");
            if (column.finalY() > column.original().y() && !column.openDepression()) {
                return TerrainPlan.refused("unsafe_fill");
            }
            int bottom = Math.min(column.finalY() - SUPPORT_LAYERS, column.original().y());
            int top = Math.max(column.finalY() + CLEARANCE, column.original().y());
            if (!request.authorized().contains(new ColonyPos(pos.x(), bottom, pos.z()))
                    || !request.authorized().contains(new ColonyPos(pos.x(), top, pos.z()))) {
                return TerrainPlan.refused("outside_work_area");
            }
            long dx = Math.max(0L, Math.max((long) candidate.footprint().min().x() - pos.x(),
                    (long) pos.x() - candidate.footprint().max().x()));
            long dz = Math.max(0L, Math.max((long) candidate.footprint().min().z() - pos.z(),
                    (long) pos.z() - candidate.footprint().max().z()));
            long transition = Math.max(dx, dz);
            for (ColonyPos at : candidate.corridor()) {
                transition = Math.min(transition, Math.max(Math.abs((long) at.x() - pos.x()), Math.abs((long) at.z() - pos.z())));
            }
            if (transition > MAX_TRANSITION) return TerrainPlan.refused("transition_limit");
            if (columns.put(new Key(pos.x(), pos.z()), column) != null) {
                return TerrainPlan.refused("duplicate_column");
            }
            if (candidate.anchor() != null && key(candidate.anchor()).equals(key(pos))) {
                return TerrainPlan.refused("anchor_write");
            }
            earthwork += delta;
            changed += delta + column.extraChanges() + column.plants() - column.mergedPlants();
            plants += column.plants();
            maximum = Math.max(maximum, (int) delta);
            minX = Math.min(minX, pos.x()); maxX = Math.max(maxX, pos.x());
            minZ = Math.min(minZ, pos.z()); maxZ = Math.max(maxZ, pos.z());
            minY = Math.min(minY, bottom); maxY = Math.max(maxY, top);
        }
        if ((long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1) > MAX_CELLS
                || changed > MAX_CHANGES) return TerrainPlan.refused("work_budget");
        Column entry = columns.get(key(candidate.entry()));
        if (entry == null || !entry.surface().equals(candidate.entry())) return TerrainPlan.refused("missing_entry");
        Set<Barrier> barriers = new HashSet<>(candidate.barriers());
        Set<Key> visited = new HashSet<>();
        ArrayDeque<Column> pending = new ArrayDeque<>();
        pending.add(entry);
        visited.add(key(entry.surface()));
        boolean anchorReached = candidate.anchor() == null;
        while (!pending.isEmpty()) {
            Column current = pending.remove();
            ColonyPos pos = current.surface();
            if (candidate.anchor() != null && adjacent(pos, candidate.anchor())
                    && !blocked(barriers, pos, candidate.anchor())) anchorReached = true;
            for (Key neighbor : List.of(new Key(pos.x() - 1, pos.z()), new Key(pos.x() + 1, pos.z()),
                    new Key(pos.x(), pos.z() - 1), new Key(pos.x(), pos.z() + 1))) {
                Column next = columns.get(neighbor);
                if (next != null && adjacent(pos, next.surface()) && !blocked(barriers, pos, next.surface())
                        && visited.add(neighbor)) pending.add(next);
            }
        }
        if (visited.size() != columns.size()) return TerrainPlan.refused("disconnected_surface");
        if (!anchorReached) return TerrainPlan.refused("incompatible_anchor");
        long footprintColumns = ((long) candidate.footprint().max().x() - candidate.footprint().min().x() + 1)
                * ((long) candidate.footprint().max().z() - candidate.footprint().min().z() + 1);
        if (footprintColumns > columns.size()) return TerrainPlan.refused("incomplete_footprint");
        for (int x = candidate.footprint().min().x(); x <= candidate.footprint().max().x(); x++) {
            for (int z = candidate.footprint().min().z(); z <= candidate.footprint().max().z(); z++) {
                if (!columns.containsKey(new Key(x, z))) return TerrainPlan.refused("incomplete_footprint");
            }
        }
        return new TerrainPlan(true, candidate, changed, earthwork, maximum, plants,
                candidate.anchor() == null ? "external_access_unverified" : "planned_access_to_anchor");
    }

    private static boolean adjacent(ColonyPos first, ColonyPos second) {
        return Math.abs((long) first.x() - second.x()) + Math.abs((long) first.z() - second.z()) == 1
                && Math.abs((long) first.y() - second.y()) <= MAX_STEP;
    }

    private static boolean blocked(Set<Barrier> barriers, ColonyPos first, ColonyPos second) {
        return barriers.contains(new Barrier(first, second)) || barriers.contains(new Barrier(second, first));
    }

    private record Key(int x, int z) { }

    private static Key key(ColonyPos position) {
        return new Key(position.x(), position.z());
    }

    private static Comparator<TerrainPlan> order(Request request) {
        return Comparator.comparingLong(TerrainPlan::changedPositions).thenComparingLong(TerrainPlan::earthwork)
                .thenComparingInt(TerrainPlan::maximumDelta).thenComparingDouble(plan -> {
                    Candidate candidate = Objects.requireNonNull(plan.candidate());
                    return LotScore.of(request.center().x(), request.center().y(), request.center().z(),
                            candidate.origin().x(), candidate.origin().y(), candidate.origin().z(),
                            candidate.footprint().max().x() - candidate.footprint().min().x() + 1,
                            candidate.footprint().max().z() - candidate.footprint().min().z() + 1, candidate.pathLength());
                }).thenComparingInt(plan -> Objects.requireNonNull(plan.candidate()).origin().x())
                .thenComparingInt(plan -> Objects.requireNonNull(plan.candidate()).origin().y())
                .thenComparingInt(plan -> Objects.requireNonNull(plan.candidate()).origin().z());
    }
}
