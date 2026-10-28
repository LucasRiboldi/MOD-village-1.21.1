package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;

import java.util.Optional;
import java.util.UUID;

/**
 * Construção nova nasce a no mínimo {@link #GAP} blocos de qualquer outra construção — Regra 124
 * (decisão D1 do autor, 2026-10-27). O caminho pode encostar no lote: só construção conta.
 */
public final class LotSpacing {

    /** Blocos livres entre a pegada do lote e a caixa de outra construção. */
    public static final int GAP = 3;

    private LotSpacing() {
    }

    /** Se a pegada que começa em {@code (originX, originZ)} fica a menos de {@link #GAP} de alguma construção. */
    public static boolean tooClose(UUID colonyId, int originX, int originZ, ColonyPos size) {
        int minX = originX - GAP;
        int minZ = originZ - GAP;
        int maxX = originX + size.x() - 1 + GAP;
        int maxZ = originZ + size.z() - 1 + GAP;

        for (Building building : VillageColonyMod.BUILDINGS.all()) {
            if (overlaps(minX, minZ, maxX, maxZ, building.min(), building.max())) {
                return true;
            }
        }

        Optional<ConstructionProject> open = VillageColonyMod.CONSTRUCTIONS.openOf(colonyId);

        return open.filter(project -> {
            ColonyPos origin = project.origin();
            ColonyPos box = project.blueprint().size();
            ColonyPos end = new ColonyPos(origin.x() + box.x() - 1, origin.y(), origin.z() + box.z() - 1);

            return !(origin.x() == originX && origin.z() == originZ) && overlaps(minX, minZ, maxX, maxZ, origin, end);
        }).isPresent();
    }

    private static boolean overlaps(int minX, int minZ, int maxX, int maxZ, ColonyPos min, ColonyPos max) {
        return minX <= max.x() && maxX >= min.x() && minZ <= max.z() && maxZ >= min.z();
    }
}
