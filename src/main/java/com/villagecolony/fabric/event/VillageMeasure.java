package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;

/**
 * A primeira medida da vila — ADR-003 Emenda 6, decisão do autor de 2026-09-30.
 *
 * <p>A caixa nasce com a medida da vila gerada pelo jogo que alcança o
 * centro; sem vila gerada, com {@link VillageBounds#TYPICAL_SIDE} de lado em
 * volta do centro. Soma as construções que a colônia já tem, cada uma com a
 * folga de crescimento. Depois disso só cresce — ver {@code VillageGrowth}.
 *
 * <p>É também a migração: a colônia de save antigo volta sem caixa e é medida
 * na primeira vez que o chunk do centro estiver carregado.
 */
final class VillageMeasure {

    private VillageMeasure() {
    }

    /**
     * Mede a colônia que ainda não tem caixa.
     *
     * @param beds as camas do aglomerado que acabou de ser visto, para a
     *     altura da vila sem vila gerada; pode ser vazia
     */
    static void measure(ServerWorld world, Colony colony, List<ColonyPos> beds) {
        if (colony.bounds().isPresent()) {
            return;
        }

        // A vila gerada é procurada no meio das construções: o centro pode ter andado (a caixa
        // de faixas anexadas o levou a 68 blocos das camas, 0.3.42).
        ColonyPos center = middleOfTheBuildings(colony).orElse(colony.center());

        if (!world.isChunkLoaded(center.x() >> 4, center.z() >> 4)) {
            return;
        }

        Optional<VillageBounds> generated = GeneratedVillages.boundsAt(world, center.x(), center.z());

        VillageBounds measured = generated.orElseGet(() -> typical(center, beds));

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            measured = measured.union(VillageBounds.aroundPiece(building.min(), building.max(),
                    com.villagecolony.core.coordination.VillageExpansion.claimOf(building.blueprint())));
        }

        colony.measure(measured);

        VillageColonyMod.LOGGER.info(
                "Colony {} measured at {} from {} — center now {}",
                colony.id(),
                measured,
                generated.isPresent() ? "the generated village" : "the typical size",
                colony.center());
    }

    /** O meio das construções terminadas desta colônia, se há alguma. */
    private static Optional<ColonyPos> middleOfTheBuildings(Colony colony) {
        List<Building> standing = VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                .filter(Building::finished)
                .toList();

        if (standing.isEmpty()) {
            return Optional.empty();
        }

        long x = 0;
        long y = 0;
        long z = 0;

        for (Building building : standing) {
            x += (building.min().x() + building.max().x()) / 2;
            y += building.min().y();
            z += (building.min().z() + building.max().z()) / 2;
        }

        return Optional.of(new ColonyPos((int) (x / standing.size()), (int) (y / standing.size()),
                (int) (z / standing.size())));
    }

    private static VillageBounds typical(ColonyPos center, List<ColonyPos> beds) {
        int minY = center.y() - 4;
        int maxY = center.y() + 12;

        if (!beds.isEmpty()) {
            minY = beds.stream().mapToInt(ColonyPos::y).min().orElse(minY);
            maxY = beds.stream().mapToInt(ColonyPos::y).max().orElse(maxY);
        }

        return VillageBounds.typicalAround(center, minY, maxY);
    }
}
