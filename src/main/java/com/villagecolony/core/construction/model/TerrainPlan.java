package com.villagecolony.core.construction.model;

import org.jspecify.annotations.Nullable;

/** Resultado imutável do planejamento; não concede permissão para escrever no mundo — ADR-044, Fase 2. */
public record TerrainPlan(boolean feasible, TerrainPlanner.@Nullable Candidate candidate, long changedPositions,
                          long earthwork, int maximumDelta, long plants, String reason) {
    public TerrainPlan {
        if (reason == null || feasible != (candidate != null)
                || changedPositions < 0 || earthwork < 0 || maximumDelta < 0 || plants < 0) {
            throw new IllegalArgumentException("invalid_terrain_result");
        }
    }

    static TerrainPlan refused(String reason) {
        return new TerrainPlan(false, null, 0, 0, 0, 0, reason);
    }
}
