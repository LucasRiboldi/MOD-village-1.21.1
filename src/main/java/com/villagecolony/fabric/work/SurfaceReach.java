package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.ResourceReach;
import com.villagecolony.core.type.ResourceType;

/** Quanto além da borda da vila a coleta de superfície vai buscar, por recurso. */
final class SurfaceReach {

    /** Quanto além da borda da vila o fundidor busca areia e argila — O6 (autor, 2026-10-28). */
    static final int SMELTER_MARGIN = 2 * ResourceReach.WIDE_MARGIN;

    private SurfaceReach() {
    }

    /** A margem da busca dentro da vila: areia e argila vão mais longe (O6); o resto, a de sempre. */
    static int marginFor(ResourceType raw) {
        return switch (raw) {
            case SAND, CLAY, CLAY_BALL -> SMELTER_MARGIN;
            default -> ResourceReach.EDGE_MARGIN;
        };
    }
}
